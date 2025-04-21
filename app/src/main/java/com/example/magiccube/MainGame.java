package com.example.magiccube;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.magiccube.utils.CubeWebSocket;
import com.example.magiccube.utils.MyTimer;

public class MainGame extends AppCompatActivity {
    private CubeWebSocket cubeWebSocket;
    private WebView webview;
    private TextView timerTextView;
    private Button btn_scramble,btn_palse,btn_undo;
    private MyTimer myTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_game);

        InitWebView();
        InitTimer();
        InitButtons();
    }

    void InitWebView(){
        cubeWebSocket = new CubeWebSocket(this);
        webview = findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);

        webview.addJavascriptInterface(new WebAppInterface(), "Android");
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webview.getSettings().setAllowContentAccess(true);
        webview.setVerticalScrollBarEnabled(false);
        webview.setHorizontalScrollBarEnabled(false);
        webview.loadUrl("file:///android_asset/cube.html");
    }

    void InitTimer(){
        timerTextView = findViewById(R.id.timerSingleTextView);
        myTimer=new MyTimer();
        myTimer.setOnTimeUpdateListener(time -> timerTextView.setText(time));
    }

    void InitButtons() {

        btn_scramble = findViewById(R.id.btn_scramble);
        btn_scramble.setOnClickListener(v -> {
            myTimer.reset();
            webview.evaluateJavascript("scrambleCube('RLR\\'L');", null);
        });

        btn_palse = findViewById(R.id.btn_start);
        btn_palse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(myTimer.isRunning()){
                    myTimer.pause();
                }
            }
        });

        btn_undo = findViewById(R.id.btn_undo);
        btn_undo.setOnClickListener(v -> {
            webview.evaluateJavascript("undoMove();",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if(value.equals("false")){
                                System.out.println("a");
                                showMyDialog("已回到初始状态");
                            }
                        }
                    }
                    );
        });
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void sendMove(String axis,int value,int angle) {
            cubeWebSocket.sendMove(axis,value,angle);
        }
        @JavascriptInterface
        public void onCubeSolved() {
            runOnUiThread(() -> {
                myTimer.pause();
                showMyDialog("魔方已还原");
            });
        }

        @JavascriptInterface
        public void onRotateStart() {
            runOnUiThread(() -> {
                myTimer.start();
            });
        }
    }
    private void showMyDialog(String mes) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(mes)
                .show();
    }

    //初始化
    public void initializeCube(String scramble) {

    }
    @Override
    protected void onDestroy() {
        cubeWebSocket.close();
        super.onDestroy();
    }
}