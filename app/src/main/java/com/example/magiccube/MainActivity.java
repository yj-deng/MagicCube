package com.example.magiccube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.magiccube.utils.MyPreferences;

public class MainActivity extends AppCompatActivity {

    private WebView webview;
    private BackgroundMusic musicService;
    private boolean isBound = false;
    private boolean isMusicOn = false;
    private boolean isVibrateOn = true;
    private int cubeLevel;
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
        InitUserSetting();
        InitWebView();
        InitButtons();
    }

    private void InitUserSetting() {
        SharedPreferences sharedPreferences = getSharedPreferences("userSetting",MODE_PRIVATE);
        isMusicOn=sharedPreferences.getBoolean("musicStatus",false);
        isVibrateOn=sharedPreferences.getBoolean("vibrateOn",false);
        cubeLevel = sharedPreferences.getInt("cubeLevel",5);
    }

    void InitButtons(){

        ImageButton btn_left = findViewById(R.id.btn_left_arrow);

        btn_left.setOnClickListener(v->{
            cubeLevel = 3;
            String url = "file:///android_asset/cubePreview.html?level=" + cubeLevel + "&t=" + System.currentTimeMillis();
            webview.loadUrl(url);
            SharedPreferences sharedPreferences = getSharedPreferences("userSetting",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("cubeLevel",cubeLevel);
            editor.commit();
        });

        ImageButton btn_right = findViewById(R.id.btn_right_arrow);
        btn_right.setOnClickListener(v->{
            cubeLevel = 5;
            String url = "file:///android_asset/cubePreview.html?level=" + cubeLevel + "&t=" + System.currentTimeMillis();
            webview.loadUrl(url);
            SharedPreferences sharedPreferences = getSharedPreferences("userSetting",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("cubeLevel",cubeLevel);
            editor.commit();
        });

        ImageButton btnPlay = findViewById(R.id.btn_music);
        Intent intent1 = new Intent(this, BackgroundMusic.class);
        bindService(intent1, connection, Context.BIND_AUTO_CREATE);
        btnPlay.setOnClickListener(v -> {
            if (isBound) {
                SharedPreferences sharedPreferences=getSharedPreferences("userSetting",MODE_PRIVATE);
                isMusicOn=sharedPreferences.getBoolean("musicStatus",false);
                if(isMusicOn)
                    btnPlay.setImageResource(R.drawable.ic_music_off);
                else
                    btnPlay.setImageResource(R.drawable.ic_music_on);
                //保存用户设置
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("musicStatus",!isMusicOn);
                editor.commit();
                isMusicOn = !isMusicOn;
                musicService.togglePlayback();
            }
        });

        ImageButton btn = findViewById(R.id.btn_shake);
        btn.setOnClickListener(v->{
            SharedPreferences sharedPreferences=getSharedPreferences("userSetting",MODE_PRIVATE);
            isVibrateOn=sharedPreferences.getBoolean("vibrateStatus",false);
            if(isVibrateOn)
                    btn.setImageResource(R.drawable.ic_vibrate_off);
                else
                    btn.setImageResource(R.drawable.ic_vibrate_on);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("vibrateStatus",!isVibrateOn);
                editor.commit();
                isVibrateOn=!isVibrateOn;
                String jsCode = "window.enableVibrate = "+isVibrateOn;
                webview.evaluateJavascript(jsCode, null);
            });

        ImageButton btn2 = findViewById(R.id.btn_start);
        btn2.setOnClickListener(v->{
                Intent intent2 = new Intent(MainActivity.this, MainGame.class);
                startActivity(intent2);

        });

        ImageButton btn1 = findViewById(R.id.btn_multiple);
        btn1.setOnClickListener(v-> {
                Intent intent3 = new Intent(MainActivity.this, MultipleGame.class);
                startActivity(intent3);
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
        webview.loadUrl("file:///android_asset/cubePreview.html?level="+cubeLevel);
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