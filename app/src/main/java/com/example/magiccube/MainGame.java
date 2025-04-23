package com.example.magiccube;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.magiccube.utils.Cube;
import com.example.magiccube.utils.MyTimer;

public class MainGame extends AppCompatActivity {
    private int cubeLevel=3;
    private WebView webview;
    private TextView timerTextView;
    private Button btn_scramble,btn_palse,btn_undo,btn_restore;
    private MyTimer myTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_game);

        InitUserSetting();
        InitWebView();
        InitTimer();
        InitButtons();
    }

    void InitUserSetting(){
        SharedPreferences sharedPreferences = getSharedPreferences("userSetting",MODE_PRIVATE);
        cubeLevel = sharedPreferences.getInt("cubeLevel",3);
    }

    void InitWebView(){
        webview = findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);

        webview.addJavascriptInterface(new WebAppInterface(), "Android");
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webview.getSettings().setAllowContentAccess(true);
        webview.setVerticalScrollBarEnabled(false);
        webview.setHorizontalScrollBarEnabled(false);
        webview.loadUrl("file:///android_asset/cube.html?level="+cubeLevel);
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
            String scrambleCode = Cube.generateScramble(5,cubeLevel);
            String jsCode = String.format("scrambleCube('%s')", scrambleCode);
            webview.evaluateJavascript(jsCode,null);
        });

        btn_restore = findViewById(R.id.btn_restore);
        btn_restore.setOnClickListener(v -> {
            webview.evaluateJavascript("restore()",null);
        });

//        btn_palse = findViewById(R.id.btn_start);
//        btn_palse.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(myTimer.isRunning()){
//                    String jsCode = "window.timerFlat = false;";
//                    webview.evaluateJavascript(jsCode, null);
//                    myTimer.pause();
//                }
//            }
//        });

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

        }
        @JavascriptInterface
        public void onCubeSolved() {
            runOnUiThread(() -> {
                long temptime=myTimer.getElapsedMillis();
                Toast.makeText(MainGame.this,"还原耗时"+temptime/1000+"."+temptime%1000+"秒",Toast.LENGTH_LONG).show();
                myTimer.reset();
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
        super.onDestroy();
    }
}