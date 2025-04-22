package com.example.magiccube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {


    private int currentIconIndex = 0;

    private WebView webview;
    private BackgroundMusic musicService;
    private boolean isBound = false;
    private boolean isMusicOn = false;
    private boolean isVibrateOn = true;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BackgroundMusic.MusicBinder binder = (BackgroundMusic.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //音乐服务控制
        InitBackgroundMusic();
        InitWebView();
        InitButtons();
    }
    void InitBackgroundMusic(){
        ImageButton btnPlay = findViewById(R.id.btn_music);
        Intent intent = new Intent(this, BackgroundMusic.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        btnPlay.setOnClickListener(v -> {
            if (isBound) {
                if(isMusicOn)
                    btnPlay.setImageResource(R.drawable.ic_music_off);
                else
                    btnPlay.setImageResource(R.drawable.ic_music_on);
                isMusicOn=!isMusicOn;
                musicService.togglePlayback();
            }
        });
    }

    void InitButtons(){
        ImageButton btn = findViewById(R.id.btn_shake);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isVibrateOn)
                    btn.setImageResource(R.drawable.ic_vibrate_on);
                else
                    btn.setImageResource(R.drawable.ic_vibrate_off);
                isVibrateOn=!isVibrateOn;
                String jsCode = "window.enableVibrate = "+isVibrateOn;
                webview.evaluateJavascript(jsCode, null);
            }
        });

        ImageButton btn2 = findViewById(R.id.btn_start);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MainGame.class);
                startActivity(intent);
            }
        });

        ImageButton btn1 = findViewById(R.id.btn_multiple);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MultipleGame.class);
                startActivity(intent);
            }
        });

    }

    void InitWebView(){
        webview = findViewById(R.id.webviewPreview);
        webview.getSettings().setJavaScriptEnabled(true);

        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webview.getSettings().setAllowContentAccess(true);
        webview.setVerticalScrollBarEnabled(false);
        webview.setHorizontalScrollBarEnabled(false);
        webview.loadUrl("file:///android_asset/cubePreview.html");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}