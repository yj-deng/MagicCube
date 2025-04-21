package com.example.magiccube.utils;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.magiccube.utils.MyTimer;
import com.example.magiccube.utils.CubeWebSocket;

public class WebViewManager {
    private final WebView webView;
    private final MyTimer myTimer;
    private final Activity activity;
    private final String roomId;
    private final boolean isMultipleMode;
    private CubeWebSocket cubeWebSocket;

    public WebViewManager(Activity activity, WebView webView, MyTimer timer, boolean isMultipleMode, String roomId) {
        this.activity = activity;
        this.webView = webView;
        this.myTimer = timer;
        this.isMultipleMode = isMultipleMode;
        this.roomId = roomId;

        initWebView();
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("file:///android_asset/cube.html");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.evaluateJavascript("window.isMultipleMode = " + isMultipleMode + ";", null);

                if (isMultipleMode) {
                    cubeWebSocket = new CubeWebSocket((com.example.magiccube.MultipleGame) activity, roomId);
                } else {
                    cubeWebSocket = new CubeWebSocket((com.example.magiccube.MainGame) activity);
                }
            }
        });
    }

    public void close() {
        if (cubeWebSocket != null) {
            cubeWebSocket.close();
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void sendMove(String axis, int value, int angle) {
            if (cubeWebSocket != null) {
                cubeWebSocket.sendMove(axis, value, angle);
            }
        }

        @JavascriptInterface
        public void onRotateStart() {
            activity.runOnUiThread(() -> myTimer.start());
        }

        @JavascriptInterface
        public void onCubeSolved() {
            activity.runOnUiThread(() -> {
                myTimer.pause();
            });
        }
    }
}
