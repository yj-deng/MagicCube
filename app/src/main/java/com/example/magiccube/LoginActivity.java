package com.example.magiccube;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.magiccube.Server.Result;
import com.example.magiccube.Server.ServerManager;
import com.example.magiccube.Server.ServerManager.TokenValidationCallback;
import com.example.magiccube.utils.Md5Utils;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private SharedPreferences sharedPreferences;

    // SharedPreferences 键值
    private static final String PREFS_NAME = "userSetting";
    private static final String KEY_USERID = "userid";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_LOGGED = "logged";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initComponents();
    }

    private void initComponents() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        // 加载保存的登录信息
        loadSavedLogin();

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validateInput(username, password)) {
            performServerLogin(username, Md5Utils.md5(password));
        }
    }

    private boolean validateInput(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("用户名不能为空");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("密码不能为空");
            return false;
        }
        return true;
    }

    private void performServerLogin(String username, String password) {
        ServerManager.login(username, password, new ServerManager.LoginCallback() {
            @Override
            public void onSuccess(Result result) {
                try {
                    JSONObject data = new JSONObject(result.getData().toString());
                    int userId = data.getInt("user_id");
                    String token = data.getString("token");
                    saveLoginState(userId, username, token);
                    startMainActivity();
                } catch (Exception e) {
                    showMessage("登录数据解析失败");
                }
            }

            @Override
            public void onFailure(String errorMsg) {
                showMessage(errorMsg);
            }
        });
    }

    private void saveLoginState(int userId, String username, String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_USERID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_TOKEN, token);
        editor.putBoolean(KEY_LOGGED, true);
        editor.apply();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("token",true);
        startActivity(intent);
        finish();
    }

    private void loadSavedLogin() {
        etUsername.setText(sharedPreferences.getString(KEY_USERNAME, ""));
    }

    private void clearLoginState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USERID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_TOKEN);
        editor.putBoolean(KEY_LOGGED, false);
        editor.apply();
    }

    private void showMessage(String message) {
        runOnUiThread(() ->
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show()
        );
    }
}