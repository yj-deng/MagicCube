package com.example.magiccube.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
    private final String roomId;
    private final int userId;
    private static final String WS_BASE_URL = "ws://10.0.2.2:8080/ws";
    private boolean is_Connected;
    private boolean is_exit=false;
    private int retryCount = 0;
    private String token;

    public CubeWebSocket(MultipleGame activity, String roomId,int userId,String token) {
        this.multipleActivity = activity;
        this.roomId = roomId;
        this.userId=userId;
        this.client = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        this.token=token;
        connect();
    }

    private void connect() {
        String wsUrl = WS_BASE_URL + "?roomId=" + roomId + "&userId=" + userId;

        Request request = new Request.Builder().url(wsUrl).addHeader("Authorization", token).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                is_Connected = true;
                is_exit = false;
                retryCount = 0;
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                is_Connected = false;
                if (!is_exit) reconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                is_Connected = false;
                if (!is_exit) {
                    if (multipleActivity != null)
                        multipleActivity.onWebSocketConnectionFailed();
                    Log.e("WebSocket", "Connection failed", t);
                    reconnect();
                }
            }
        });
    }


    private void handleMessage(String message) {
        if (multipleActivity != null) {
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
        }else if(message.startsWith("CHAT:"))
        {
            String chatJson = message.substring(5);
            activity.runOnUiThread(() -> activity.receiveChatMessage(chatJson));
        }
    }

    public void sendMove(String axis, int value, int angle) {
        if (webSocket != null&&is_Connected) {
            String data=axis+value+angle;
            String message = "UPDATE:" + data;
            webSocket.send(message);
        }
    }

    private void reconnect() {
        int max_retry = 5;
        if(retryCount> max_retry)
            return;
        retryCount++;
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable reconnectRunnable = this::connect;

        if (multipleActivity!=null) {
            handler.postDelayed(reconnectRunnable, 10000);
        }
    }

    public void close() {
        is_exit=true;
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
        }
        client.dispatcher().executorService().shutdown();
    }

    public void sendChatMessage(String sender, String message) {
        if (webSocket != null && is_Connected) {
            try {
                JSONObject chatData = new JSONObject();
                chatData.put("sender", sender);
                chatData.put("message", message);

                String chatMessage = "CHAT:" + chatData.toString();
                webSocket.send(chatMessage);
            } catch (JSONException ignored) {}
        }
    }

}
