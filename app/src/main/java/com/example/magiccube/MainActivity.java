package com.example.magiccube;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private BackgroundMusic musicService;
    private boolean isBound = false;
    private ImageButton btnPlay;
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
        InitButtons();
    }
    void InitBackgroundMusic(){
        btnPlay = findViewById(R.id.btn_music);
        Intent intent = new Intent(this, BackgroundMusic.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        btnPlay.setOnClickListener(v -> {
            if (isBound) {
                musicService.togglePlayback();
            }
        });
    }

    void InitButtons(){
        ImageButton btn = findViewById(R.id.btn_start);
        btn.setOnClickListener(new View.OnClickListener() {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}