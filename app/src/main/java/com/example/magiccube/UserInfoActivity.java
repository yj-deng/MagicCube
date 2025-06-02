package com.example.magiccube;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class UserInfoActivity extends AppCompatActivity {

    private TextView tvUserId, tvUsername,tvUserScore;
    private Button btnLogout;
    private static final String PREFS_NAME = "userSetting";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LOGGED= "logged";
    private static final String KEY_USERID = "userid";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_info);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvUserId = findViewById(R.id.tvUserId);
        tvUsername = findViewById(R.id.tvUsername);
        tvUserScore=findViewById(R.id.tvUserScore);
        btnLogout = findViewById(R.id.btnLogout);

        int userId = sharedPreferences.getInt(KEY_USERID,0);
        String username = sharedPreferences.getString(KEY_USERNAME,"默认昵称");

        tvUserId.setText("用户ID: " + userId);
        tvUsername.setText("用户名: " + username);
//
//        tvUserId.setText("用户ID: " + 123);
//        tvUsername.setText("用户名: " + "张三");
//        tvUserScore.setText("最好成绩："+"12:23"+"s");

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserInfoActivity.this, "已退出登录", Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putString(KEY_USERNAME,"").apply();
                sharedPreferences.edit().putString(KEY_PASSWORD,"").apply();
                sharedPreferences.edit().putInt(KEY_USERID,-1).apply();
                sharedPreferences.edit().putBoolean(KEY_LOGGED,false).apply();
                Intent intent = new Intent(UserInfoActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
