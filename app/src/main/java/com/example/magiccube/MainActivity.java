package com.example.magiccube;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.magiccube.Server.ServerManager;
import com.example.magiccube.utils.MyPreferences;
import com.example.magiccube.utils.RoomIdCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webview;
    private BackgroundMusic musicService;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "userSetting";
    private static final String KEY_MUSIC = "musicStatus";
    private static final String KEY_VIBRATE = "vibrateStatus";
    private static final String KEY_LOGGED= "logged";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USERID = "userid";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ROOM="roomid";
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

        sharedPreferences = getSharedPreferences(PREFS_NAME,MODE_PRIVATE);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        InitUserSetting();
        InitWebView();
        InitButtons();

        AlertDialog alertDialog =new AlertDialog.Builder(this)
                .setMessage("请登陆账号")
                .setPositiveButton("登陆", new DialogInterface.OnClickListener() {//添加"Yes"按钮
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                })

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {//添加取消
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "取消登陆，可正常单人游戏", Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        if(!sharedPreferences.getBoolean(KEY_LOGGED,false))
            alertDialog.show();
        else if(!getIntent().getBooleanExtra("token",false))
            validateToken(sharedPreferences.getString(KEY_TOKEN,""));
    }

    private void validateToken(String token) {
        ServerManager.validateToken(token, new ServerManager.TokenValidationCallback() {
            @Override
            public void onValid() {
                sharedPreferences.edit().putBoolean(KEY_LOGGED,true).apply();
            }

            @Override
            public void onInvalid(String error) {
                runOnUiThread(() -> {
                    clearLoginState();
                    Toast.makeText(MainActivity.this, "会话已过期，请重新登录", Toast.LENGTH_SHORT).show();
                    startLoginActivity();
                });
            }
        });
    }
    private void clearLoginState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USERID);
        editor.remove(KEY_USERNAME);
        editor.putBoolean(KEY_LOGGED, false);
        editor.apply();
    }
    private void InitUserSetting() {
        isMusicOn=sharedPreferences.getBoolean(KEY_MUSIC,false);
        isVibrateOn=sharedPreferences.getBoolean("vibrateOn",false);
        cubeLevel = sharedPreferences.getInt("cubeLevel",5);
    }

    void InitButtons(){

        ImageButton btn_left = findViewById(R.id.btn_left_arrow);
        btn_left.setOnClickListener(v->{
            cubeLevel = 3;
            String url = "file:///android_asset/cubePreview.html?level=" + cubeLevel + "&t=" + System.currentTimeMillis();
            webview.loadUrl(url);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("cubeLevel",cubeLevel);
            editor.commit();
        });

        ImageButton btn_right = findViewById(R.id.btn_right_arrow);
        btn_right.setOnClickListener(v->{
            cubeLevel = 5;
            String url = "file:///android_asset/cubePreview.html?level=" + cubeLevel + "&t=" + System.currentTimeMillis();
            webview.loadUrl(url);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("cubeLevel",cubeLevel);
            editor.commit();
        });

        ImageButton btnPlay = findViewById(R.id.btn_music);
        Intent intent1 = new Intent(this, BackgroundMusic.class);
        bindService(intent1, connection, Context.BIND_AUTO_CREATE);
        btnPlay.setOnClickListener(v -> {
            if (isBound) {
                SharedPreferences sharedPreferences=getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
                isMusicOn=sharedPreferences.getBoolean(KEY_MUSIC,false);
                if(isMusicOn)
                    btnPlay.setImageResource(R.drawable.ic_music_off);
                else
                    btnPlay.setImageResource(R.drawable.ic_music_on);
                //保存用户设置
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_MUSIC,!isMusicOn);
                editor.commit();
                isMusicOn = !isMusicOn;
                musicService.togglePlayback();
            }
        });

        ImageButton btn = findViewById(R.id.btn_shake);
        btn.setOnClickListener(v->{
            SharedPreferences sharedPreferences=getSharedPreferences(PREFS_NAME,MODE_PRIVATE);
            isVibrateOn=sharedPreferences.getBoolean(KEY_VIBRATE,false);
            if(isVibrateOn)
                    btn.setImageResource(R.drawable.ic_vibrate_off);
                else
                    btn.setImageResource(R.drawable.ic_vibrate_on);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(KEY_VIBRATE,!isVibrateOn);
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

        ImageButton btn3 =findViewById(R.id.btn_info);

        btn3.setOnClickListener(v->{
            Intent intent;
            boolean flat=sharedPreferences.getBoolean(KEY_LOGGED,false);
            if(flat){
                intent = new Intent(MainActivity.this, UserInfoActivity.class);
            }
            else{
                intent = new Intent(MainActivity.this, LoginActivity.class);
            }
            startActivity(intent);
        });

        ImageButton btn1 = findViewById(R.id.btn_multiple);
        btn1.setOnClickListener(v -> {
            if (!sharedPreferences.getBoolean(KEY_LOGGED, false)) {
                Toast.makeText(MainActivity.this, "请登陆后使用联机功能", Toast.LENGTH_SHORT).show();
            } else {
                getRoomId(roomId -> {
                    Intent intent = new Intent(MainActivity.this, MultipleGame.class);
                    intent.putExtra(KEY_USERID,sharedPreferences.getInt(KEY_USERID,0));
                    intent.putExtra(KEY_USERNAME, sharedPreferences.getString(KEY_USERNAME, "默认昵称"));
                    intent.putExtra(KEY_ROOM, roomId);
                    intent.putExtra(KEY_TOKEN,sharedPreferences.getString(KEY_TOKEN,""));
                    startActivity(intent);
                });
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
    private void getRoomId(RoomIdCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入房间号");

        final EditText input = new EditText(this);
        input.setHint("房间号");
        builder.setView(input);

        builder.setCancelable(false);
        builder.setPositiveButton("确认", (dialog, which) -> {
            String inputRoomId = input.getText().toString().trim();
            if (!inputRoomId.isEmpty()) {
                callback.onRoomIdEntered(inputRoomId);
            } else {
                Toast.makeText(this, "房间号不能为空", Toast.LENGTH_SHORT).show();
                getRoomId(callback);
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.cancel();
        });

        builder.show();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}