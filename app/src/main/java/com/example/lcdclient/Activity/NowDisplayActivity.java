package com.example.lcdclient.Activity;


import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.example.lcdclient.Config;
import com.example.lcdclient.Listener.OnUIChangeListener;
import com.example.lcdclient.R;
import com.example.lcdclient.socket.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

public class NowDisplayActivity extends AppCompatActivity {

    private static final String TAG = "NowDisplay";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_display);
        //处理toolbar
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle("LCD显示内容");
        }
        final SocketManager socketManager=SocketManager.getInstance(this);
        socketManager.getTcpSocket().setOnUIChangeListener(new OnUIChangeListener() {
            @Override
            public void onChange(int msgCode, final String msg) {
                if(msgCode== Config.MsgCode.DISPLAY_NSG){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "run: 接收到了msg "+msg);
                            try {
                                JSONObject jsonObject=new JSONObject(msg);
                                String message=jsonObject.optString("msg");
                                TextView textView=findViewById(R.id.content_display_now);
                                if(!message.equals("")){
                                    textView.setText(message);
                                }else {
                                    textView.setText("未获取到消息\\t或\\t未设置消息");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(int errorCode) {

            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("request", Config.MsgCode.DISPLAY_NSG);
                    socketManager.getTcpSocket().sendMsg(jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
