package com.example.lcdclient.Activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lcdclient.Config;
import com.example.lcdclient.Fragment.CommuStateFragment;
import com.example.lcdclient.Fragment.LcdInfoDisplayFragment;
import com.example.lcdclient.Fragment.LoginFragment;
import com.example.lcdclient.Listener.OnUIChangeListener;
import com.example.lcdclient.R;
import com.example.lcdclient.socket.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG="MainActivity";

    private DrawerLayout mDrawerLayout;
    private SocketManager socketManager;
    private OnUIChangeListener tcpRecvListener;
    private AlertDialog.Builder builder;
    private boolean isLogin=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        socketManager=SocketManager.getInstance(this);
        //处理toolbar和drawerLayout
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout =(DrawerLayout)findViewById(R.id.drawer_layout);
        NavigationView navigationView =(NavigationView)findViewById(R.id.nav_view);



        final ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null){
            actionBar.setTitle("LCD服务器登录");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        navigationView.setCheckedItem(R.id.LCD_display);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.LCD_display:
                        if(socketManager.isServerFind()){
                            newFragment(new LcdInfoDisplayFragment(),"lcd_info_display");
                            actionBar.setTitle("LCD显示");
                        }else {
                            Toast.makeText(MainActivity.this,"没有搜索到服务器",Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.communication_state:
                        if(socketManager.isServerFind()){
                            newFragment(new CommuStateFragment(),"commu_state");
                            actionBar.setTitle("通信状态");
                        }else {
                            Toast.makeText(MainActivity.this,"没有搜索到服务器",Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

        socketStart();
    }

    private void socketStart() {
        tcpRecvListener = new OnUIChangeListener() {
            @Override
            public void onChange(int msgCode, final String msg) {
                switch (msgCode){
                    case Config.MsgCode.LOGIN_SUCCESS:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isLogin=true;
                                Toast.makeText(MainActivity.this,"登陆成功",Toast.LENGTH_SHORT).show();
                                getSupportFragmentManager().popBackStackImmediate(null,1);
                                NavigationView navigationView =(NavigationView)findViewById(R.id.nav_view);
                                TextView textView=navigationView.getHeaderView(0).findViewById(R.id.username);
                                textView.setText("用户名："+SocketManager.getInstance(getApplicationContext()).getLastAccount());
                                newFragment(new LcdInfoDisplayFragment(),"lcd_info_display");
                            }
                        });
                        break;
                    case Config.MsgCode.LOGIN_FAIL:
                        Log.d(TAG, "run: 收到了LOGIN_FAIL");
                        isLogin=false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Log.d(TAG, "run: 收到了LOGIN_FAIL");
                                    if(findViewById(R.id.login_button)!=null) {
                                        findViewById(R.id.login_button).setEnabled(true);
                                    }
                                    JSONObject jsonObject=new JSONObject(msg);
                                    String message=jsonObject.optString("msg");
                                    Toast.makeText(MainActivity.this,"登陆失败，"+message,Toast.LENGTH_SHORT).show();
                                    getSupportFragmentManager().popBackStackImmediate(null,1);
                                    LoginFragment loginFragment=new LoginFragment();
                                    Bundle bundle=new Bundle();
                                    bundle.putString("serverIP",socketManager.getLastServerIP());
                                    loginFragment.setArguments(bundle);
                                    newFragment(loginFragment,"login");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    case Config.MsgCode.OFFLINE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject jsonObject=new JSONObject(msg);
                                    String message=jsonObject.optString("msg");
                                    Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                                    getSupportFragmentManager().popBackStackImmediate(null,1);
                                    NavigationView navigationView =(NavigationView)findViewById(R.id.nav_view);
                                    TextView textView=navigationView.getHeaderView(0).findViewById(R.id.username);
                                    textView.setText("用户：未登录");
                                    Log.d(TAG, "offline");
                                    socketManager.stopSocket();
                                    socketManager.startUdpConnection();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    case Config.MsgCode.CHANGE_PASSWORD:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"密码已经修改，请重新登陆",Toast.LENGTH_SHORT).show();
                                offline();
                            }
                        });
                        break;
                    case Config.MsgCode.CHANGE_FAIL:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject jsonObject=new JSONObject(msg);
                                    String message=jsonObject.optString("msg");
                                    Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int errorCode) {

            }
        };
        socketManager.setOnUIChangeListener(new OnUIChangeListener() {
            @Override
            public void onChange(int msgCode, final String msg) {
                switch (msgCode){
                    case Config.MsgCode.GET_SERVER_IP:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.now_searching_server).setVisibility(View.GONE);
                                findViewById(R.id.retry_searching_server).setVisibility(View.GONE);
                                socketManager.getTcpSocket().setOnUIChangeListener(tcpRecvListener);
                                //加载login_fragment
                                Log.d(TAG, "onChange: +"+msg);
                                LoginFragment loginFragment=new LoginFragment();
                                Bundle bundle=new Bundle();
                                bundle.putString("serverIP",msg);
                                loginFragment.setArguments(bundle);
                                newFragment(loginFragment,"login");
                            }
                        });
                        break;
                    case Config.MsgCode.LINK_SUCCESS:
                        socketManager.getTcpSocket().setOnUIChangeListener(tcpRecvListener);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int errorCode) {
                switch (errorCode){
                    case Config.ErrorCode.TCP_CONNECT_ERROR:
                    case Config.ErrorCode.UDP_PING_TIME_OUT:
                        //后续应该有处理
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isLogin=false;
                                getSupportFragmentManager().popBackStackImmediate(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                if(socketManager.isServerFind()&&socketManager.getLastAccount()!=null){
                                    newFragment(new CommuStateFragment(),"commu_state");
                                }else {
                                    UdpRetryLink("未找到服务器，连接超时");
                                }
                            }
                        });
                        break;
                    case Config.ErrorCode.NO_WIFI:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                UdpRetryLink("Wi-Fi未开启，请打开");
                            }
                        });
                    default:
                        break;
                }
            }
        });
        socketManager.startUdpConnection();
    }

    private void UdpRetryLink(String s) {
        Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT).show();
        findViewById(R.id.now_searching_server).setVisibility(View.GONE);
        findViewById(R.id.retry_searching_server).setVisibility(View.VISIBLE);
        findViewById(R.id.retry_searching_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socketManager.startUdpConnection();
                findViewById(R.id.now_searching_server).setVisibility(View.VISIBLE);
                findViewById(R.id.retry_searching_server).setVisibility(View.GONE);
            }
        });
        Log.d(TAG, "UdpRetryLink: retry");
    }

    public void newFragment(Fragment fragment,String Tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frag_container,fragment,Tag);
        if(!Tag.equals("login")){
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.change_password:
                if(isLogin()){
                    changePassword();
                }else {
                    Toast.makeText(MainActivity.this,"现在是未登录状态",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.offline:
                if(isLogin()){
                    offline();
                }else {
                    Toast.makeText(MainActivity.this,"现在是未登录状态",Toast.LENGTH_SHORT).show();
                }
                default:
                    break;
        }
        return true;
    }

    private void offline() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    isLogin=false;
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("request", Config.MsgCode.OFFLINE);
                    socketManager.getTcpSocket().sendMsg(jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void changePassword() {
        View view= LayoutInflater.from(this).inflate(R.layout.change_password_dialogs,null);
        final TextInputLayout oldPassword=view.findViewById(R.id.old_change_password);
        final TextInputLayout newPassword=view.findViewById(R.id.new_change_password);
        final TextInputLayout confirmPassword=view.findViewById(R.id.confirm_change_password);
        builder= new AlertDialog.Builder(this).setView(view).setTitle("修改密码").setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String oldPasswordString=oldPassword.getEditText().getText().toString();
                final String newPasswordString=newPassword.getEditText().getText().toString();
                String confirmPasswordString=confirmPassword.getEditText().getText().toString();
                Log.d(TAG, "onClick: "+oldPasswordString.length());
                if(oldPasswordString.length()==6
                        &&newPasswordString.length()==6
                        &&confirmPasswordString.length()==6){
                    if(newPasswordString.equals(confirmPasswordString)){
                        if(!newPasswordString.equals(oldPasswordString)){
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSONObject jsonObject=new JSONObject();
                                        jsonObject.put("request", Config.MsgCode.CHANGE_PASSWORD);
                                        jsonObject.put("old",oldPasswordString);
                                        jsonObject.put("new",newPasswordString);
                                        socketManager.getTcpSocket().sendMsg(jsonObject.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }else {
                            Toast.makeText(MainActivity.this,"新旧密码不能一致",Toast.LENGTH_SHORT).show();
                            changePassword();
                        }
                    }else {
                        Toast.makeText(MainActivity.this,"两次输入的密码不一致",Toast.LENGTH_SHORT).show();
                        changePassword();
                    }
                }else {
                    Toast.makeText(MainActivity.this,"输入不正确",Toast.LENGTH_SHORT).show();
                    changePassword();
                }
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.create().show();
    }

    public boolean isLogin(){
        return isLogin;
    }
}
