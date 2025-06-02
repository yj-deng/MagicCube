package com.example.magiccube;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.magiccube.utils.CubeWebSocket;
import com.example.magiccube.utils.MyTimer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultipleGame extends AppCompatActivity {
    private CubeWebSocket cubeWebSocket;
    private static final String KEY_ROOM="roomid";
    private static final String KEY_USER="userid";
    private static final String KEY_TOKEN="token";
    private WebView webView;
    private TextView timerTextView,chatRoom;
    String roomId = "";
    int userId=0;
    String token="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_multiple_game);

        EditText et=findViewById(R.id.chatInput);
        chatRoom=findViewById(R.id.chatRoom);
        chatRoom.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        Button btn=findViewById(R.id.sendChat);
        btn.setOnClickListener(v -> {
            String username = getIntent().getStringExtra("username");
            String message = et.getText().toString();
            cubeWebSocket.sendChatMessage(username, message);
            et.setText("");
        });
        roomId = getIntent().getStringExtra(KEY_ROOM);
        userId=getIntent().getIntExtra(KEY_USER,0);
        token=getIntent().getStringExtra(KEY_TOKEN);
        InitWebView();
    }

    public void initializeCube(String scramble) {
        try {
            String escapedScramble = scramble.replace("'", "\\'");
            String jsCode = String.format("scrambleCube('%s')", escapedScramble);
            System.out.println(jsCode);
            webView.evaluateJavascript(jsCode,null);
        } catch (Exception e) {
            Log.e("Cube", "Failed to initialize cube", e);
        }
    }
    // 远程同步
    public void applyRemoteMove(String data) {
        try {
            Pattern pattern = Pattern.compile("([A-Za-z])(-?\\d+)(-?\\d+)");
            Matcher matcher = pattern.matcher(data);

            if (matcher.matches()) {
                String axis = matcher.group(1);
                int layer = Integer.parseInt(matcher.group(2));
                int value = Integer.parseInt(matcher.group(3));
                System.out.println(axis+""+layer+""+value);
            String jsCode = String.format("applyRemoteMove('%s', %d, %d);", axis, layer, value);

            webView.evaluateJavascript(jsCode, null);
            }
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
        public void onRotateStart() {
        }

        @JavascriptInterface
        public void onCubeSolved() {
            runOnUiThread(() -> {
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
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.loadUrl("file:///android_asset/cube.html?level=3");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String jsCode = "window.isMultipleMode = true;";
                webView.evaluateJavascript(jsCode, null);
                cubeWebSocket = new CubeWebSocket(MultipleGame.this,roomId,userId,token);
            }
        });

    }
    private void showMyDialog(String mes) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(mes)
                .show();
    }

    public void onWebSocketConnectionFailed(){
        runOnUiThread(()->{
            Toast.makeText(this,"网络连接失败，正在退出多人模式",Toast.LENGTH_LONG).show();
        });
    }

    public void onWebSocketReconnectionSuccess(){
        //同步魔方状态 SYN
        runOnUiThread(()->{
            Toast.makeText(this,"网络重连成功，正在同步魔方状态",Toast.LENGTH_LONG).show();
        });
    }
    public void receiveChatMessage(String json) {
        try {
            JSONObject chatObj = new JSONObject(json);
            String sender = chatObj.getString("sender");
            String message = chatObj.getString("message");
            long time=chatObj.getLong("timestamp");
            showChatMessage(sender, message,time);
        } catch (JSONException e) {
            Log.e("Chat", "Invalid chat message", e);
        }
    }
    private void showChatMessage(String sender, String message,long time) {
        runOnUiThread(() -> {
            String currentText = chatRoom.getText().toString();
            String newMessage = sender + ": " + message + "\n";
            chatRoom.setText(currentText + newMessage);

            final int scrollAmount = chatRoom.getLayout() == null ? 0 :
                    chatRoom.getLayout().getLineTop(chatRoom.getLineCount()) - chatRoom.getHeight();
            if (scrollAmount > 0)
                chatRoom.scrollTo(0, scrollAmount);
            else
                chatRoom.scrollTo(0, 0);
        });
    }
}