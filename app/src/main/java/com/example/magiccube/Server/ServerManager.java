package com.example.magiccube.Server;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServerManager {
    private static final String WS_BASE_URL = "http://10.0.2.2:8080";
    private static final String USER_BASE_URL="/user";
    private static final String SESSION_BASE_URL="/session";

    public static void validateToken(String token, ServerManager.TokenValidationCallback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(WS_BASE_URL + SESSION_BASE_URL + "/validateToken")
                .addHeader("Authorization", token)
                .get()
                .build();
        System.out.println("创建新链接"+token);
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call,IOException e) {
                callback.onInvalid("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call,Response response) {
                if (response.isSuccessful()) {
                    callback.onValid();
                } else {
                    String error = "Token validation failed";
                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("message")) {
                            error = json.getString("message");
                        }
                    } catch (Exception e) {
                        Log.e("TokenValidation", "Error parsing response", e);
                    }
                    callback.onInvalid(error + " (HTTP " + response.code() + ")");
                }
                response.close();
            }
        });
    }


    // 定义回调接口
    public interface TokenValidationCallback {
        void onValid();
        void onInvalid(String error);
    }
    public interface LoginCallback {
        void onSuccess(Result result);

        void onFailure(String errorMsg);
    }

    public static void login(String name, String pas, LoginCallback callback) {
        String userURL = WS_BASE_URL + USER_BASE_URL + "/login";

        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("username", name)
                .add("password", pas)
                .build();

        Request request = new Request.Builder()
                .post(formBody)
                .url(userURL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure("网络请求失败: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Gson gson = new Gson();
                Result result = gson.fromJson(body, Result.class);

                new Handler(Looper.getMainLooper()).post(() -> {
                    if ("200".equals(result.getCode())) {
                        callback.onSuccess(result);
                    } else {
                        callback.onFailure("登录失败: " + result.getMsg());
                    }
                });
            }
        });
    }
}
