package com.example.lcdclient.Activity;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.example.lcdclient.Config;
import com.example.lcdclient.Listener.OnUIChangeListener;
import com.example.lcdclient.R;
import com.example.lcdclient.socket.SocketManager;
import com.example.lcdclient.socket.TcpSocket;

import org.json.JSONException;
import org.json.JSONObject;

public class ContentSettingActivity extends AppCompatActivity implements View.OnClickListener {

    private TcpSocket tcpSocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_setting);
        //处理toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("LCD显示内容设定");
        }

        findViewById(R.id.text_content_setting).setOnClickListener(this);
        tcpSocket=SocketManager.getInstance(this).getTcpSocket();
        if(tcpSocket!=null){
            tcpSocket.setOnUIChangeListener(new OnUIChangeListener() {
                @Override
                public void onChange(int msgCode, String msg) {
                    switch (msgCode){
                        case Config.MsgCode.SET_SUCCESS:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ContentSettingActivity.this,"设定成功",Toast.LENGTH_SHORT).show();
                                    TextInputLayout textInputLayout=findViewById(R.id.text_content);
                                    if(textInputLayout.getEditText()!=null)textInputLayout.getEditText().setText("");
                                    findViewById(R.id.text_content_setting).setEnabled(true);
                                    finish();
                                }
                            });
                            break;
                        case Config.MsgCode.SET_FAIL:
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onError(int errorCode) {

                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.text_content_setting:
                findViewById(R.id.text_content_setting).setEnabled(false);
                TextInputLayout textInputLayout=findViewById(R.id.text_content);
                final String text=textInputLayout.getEditText().getText().toString();
                if(text.equals("")){
                    Toast.makeText(ContentSettingActivity.this,"输入不能为空",Toast.LENGTH_SHORT).show();
                }else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject=new JSONObject();
                                jsonObject.put("request", Config.MsgCode.SET_DISPLAY);
                                jsonObject.put("msg",text);
                                SocketManager.getInstance(getApplicationContext()).getTcpSocket().sendMsg(jsonObject.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                break;
            default:
                break;
        }
    }
}
