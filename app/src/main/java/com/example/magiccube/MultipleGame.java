package com.example.magiccube;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.magiccube.utils.CubeWebSocket;
import com.example.magiccube.utils.MyTimer;

import org.json.JSONException;
import org.json.JSONObject;

public class MultipleGame extends AppCompatActivity {
    private CubeWebSocket cubeWebSocket;
    private WebView webView;
    private TextView timerTextView;
    private MyTimer myTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_multiple_game);

        InitWebView();
        InitTimer();
    }

    public void initializeCube(String scramble) {
        try {
            String escapedScramble = scramble.replace("'", "\\'");
            String jsCode = String.format("scrambleCube('%s')", escapedScramble);
            webView.evaluateJavascript(jsCode,null);
        } catch (Exception e) {
            Log.e("Cube", "Failed to initialize cube", e);
        }
    }

    // 远程同步
    public void applyRemoteMove(String moveJson) {
        try {
            JSONObject moveData = new JSONObject(moveJson);
            String axis = moveData.getString("axis");
            int value = moveData.getInt("value");
            int angle = moveData.getInt("angle");

            String jsCode = String.format("applyRemoteMove('%s', %d, %d);", axis, value, angle);

            webView.evaluateJavascript(jsCode, null);
        }catch (Exception e) {
            Log.e("Cube", "Failed to apply remote move", e);
        }
    }

    //重连更新完整状态
    public void syncCubeState(String newState) {

    }

    public class WebAppInterface {
        @JavascriptInterface
        public void sendMove(String axis,int value,int angle) {
            cubeWebSocket.sendMove(axis,value,angle);
        }

        @JavascriptInterface
        public void onRotateStart(){
            runOnUiThread(() -> {
                myTimer.start();
            });
        }

        @JavascriptInterface
        public void onCubeSolved() {
            runOnUiThread(() -> {
                myTimer.pause();
                showMyDialog("魔方已还原");
            });
        }
    }

    @Override
    protected void onDestroy() {
        cubeWebSocket.close();
        super.onDestroy();
    }

    void InitWebView(){
        webView = findViewById(R.id.multiplewebview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("file:///android_asset/cube.html");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 注入标记：告知JS当前接口已就绪
                String jsCode = "window.isMultipleMode = true;";
                webView.evaluateJavascript(jsCode, null);
                cubeWebSocket = new CubeWebSocket(MultipleGame.this,"123");
            }
        });

    }

    void InitTimer(){
        timerTextView = findViewById(R.id.timerMultipleTextView);
        myTimer=new MyTimer();
        myTimer.setOnTimeUpdateListener(time -> timerTextView.setText(time));
    }
    private void showMyDialog(String mes) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(mes)
                .show();
    }
}