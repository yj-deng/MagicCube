package com.example.magiccube.utils;

import android.os.Handler;
import android.util.Log;

import com.example.magiccube.MainGame;
import com.example.magiccube.MultipleGame;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class CubeWebSocket {
    private WebSocket webSocket;
    private final OkHttpClient client;
    private final MultipleGame multipleActivity;
    private final MainGame singleActivity;
    private final String roomId;
    private final String mode;
    private static final String WS_BASE_URL = "ws://10.0.2.2:8080/ws";

    public CubeWebSocket(MultipleGame activity, String roomId) {
        this.multipleActivity = activity;
        this.singleActivity = null;
        this.roomId = roomId;
        this.mode = "multiple";

        this.client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        connect();
    }

    public CubeWebSocket(MainGame activity) {
        this.singleActivity = activity;
        this.multipleActivity = null;
        this.roomId = "0";
        this.mode = "single";

        this.client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        connect();
    }

    private void connect() {
        String wsUrl;

        if ("multiple".equals(mode)) {
            wsUrl = WS_BASE_URL + "?mode=multiple&roomId=" + roomId;
        } else {
            wsUrl = WS_BASE_URL + "?mode=single";
        }

        Request request = new Request.Builder().url(wsUrl).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WebSocket", "Connected in mode: " + mode + (mode.equals("multiple") ? (" to room: " + roomId) : ""));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WebSocket", "Connection closed");
                reconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocket", "Connection failed", t);
                reconnect();
            }
        });
    }

    private void handleMessage(String message) {
        if (mode.equals("single") && singleActivity != null) {
            handleMessageForActivity(singleActivity, message);
        } else if (mode.equals("multiple") && multipleActivity != null) {
            handleMessageForActivity(multipleActivity, message);
        }
        else
            System.out.println("有消息接受失败");
    }

    private void handleMessageForActivity(MainGame activity, String message) {
        if (message.startsWith("INIT:")) {
            String scramble = message.substring(5);
            activity.runOnUiThread(() -> activity.initializeCube(scramble));
        }
    }

    private void handleMessageForActivity(MultipleGame activity, String message) {
        if (message.startsWith("INIT:")) {
            String scramble = message.substring(5);
            activity.runOnUiThread(() -> activity.initializeCube(scramble));
        } else if (message.startsWith("SYNC:")) {
            String newState = message.substring(5);
            activity.runOnUiThread(() -> activity.syncCubeState(newState));
        } else if (message.startsWith("UPDATE:")) {
            String move = message.substring(7);
            activity.runOnUiThread(() -> activity.applyRemoteMove(move));
        }
    }

    public void sendMove(String axis, int value, int angle) {
        if (webSocket != null) {
            try {

                JSONObject moveData = new JSONObject();
                moveData.put("axis", axis);
                moveData.put("value", value);
                moveData.put("angle", angle);

                String message = "UPDATE:" + moveData.toString();

                webSocket.send(message);
                Log.d("WebSocket", "Sent move: " + message);
            } catch (JSONException e) {
                Log.e("WebSocket", "Failed to create move message", e);
            }
        }
    }

    private void reconnect() {
        Handler handler = new Handler();

        if (mode.equals("single") && singleActivity != null) {
            singleActivity.runOnUiThread(() -> handler.postDelayed(this::connect, 10000));
        } else if (mode.equals("multiple") && multipleActivity != null) {
            multipleActivity.runOnUiThread(() -> handler.postDelayed(this::connect, 10000));
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
        }
        client.dispatcher().executorService().shutdown();
    }
}
