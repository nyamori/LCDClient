package com.example.lcdclient.socket;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.example.lcdclient.Config;
import com.example.lcdclient.Listener.OnConnectionStateListener;
import com.example.lcdclient.Listener.OnRecvMessageListener;
import com.example.lcdclient.Listener.OnUIChangeListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SocketManager {
    private static volatile SocketManager instance = null;
    private Context mContext;
    private TcpSocket tcpSocket;
    private UdpSocket udpSocket;

    private String lastServerIP;
    private String lastServerPort;
    private String lastAccount;
    private String lastPassword;

    private boolean isTcpLink=false;
    private boolean isServerFind=false;
    private List<OnUIChangeListener> onUIChangeListenerList;

    private SocketManager(Context context){
        mContext=context.getApplicationContext();
        onUIChangeListenerList=new ArrayList<>();
    }

    public static  SocketManager getInstance(Context context) {
        if(instance==null){
            synchronized (SocketManager.class){
                if(instance==null){
                    instance = new SocketManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void setOnUIChangeListener(OnUIChangeListener listener) {
        onUIChangeListenerList.add(listener);
    }

    public void startUdpConnection() {
        //for (OnUIChangeListener listener:onUIChangeListenerList) listener.onChange(MsgCode.GET_SERVER_IP, "192.168.1.1");
        isServerFind=false;
        if(udpSocket==null){
            udpSocket = new UdpSocket(mContext);
        }
        udpSocket.addOnRecvMessageListener(new OnRecvMessageListener() {
            @Override
            public void onRecvMessage(String message) {
                handleUdpMessage(message);
            }

            @Override
            public void onError(int errorCode) {
                switch (errorCode){
                    case Config.ErrorCode.UDP_PING_TIME_OUT:
                        if(udpSocket!=null)udpSocket.stopHeartBeatTimer();
                        for (OnUIChangeListener listener:onUIChangeListenerList){
                            listener.onError(Config.ErrorCode.UDP_PING_TIME_OUT);
                        }
                        stopSocket();
                        break;
                    case Config.ErrorCode.NO_WIFI:
                        if(udpSocket!=null)udpSocket.stopHeartBeatTimer();
                        for (OnUIChangeListener listener:onUIChangeListenerList){
                            listener.onError(Config.ErrorCode.NO_WIFI);
                        }
                        stopSocket();
                        break;
                    default:
                        break;
                }
            }
        });
        udpSocket.startUdpSocket();

    }


    private void handleUdpMessage(String message) {
        try {
            JSONObject jsonObject=new JSONObject(message);
            int request = jsonObject.optInt("request");
            if(request==Config.MsgCode.GET_SERVER_IP){
                String ip =jsonObject.optString("ip");
                String port=jsonObject.optString("port");
                if((!TextUtils.isEmpty(ip))&&(!TextUtils.isEmpty(port))){
                    lastServerIP=ip;
                    lastServerPort=port;
                    if(!isServerFind){
                        startTcpConnection(ip,port);
                        for (OnUIChangeListener listener:onUIChangeListenerList){
                            listener.onChange(Config.MsgCode.GET_SERVER_IP,ip);
                        }
                        isServerFind=true;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean retryTcpConnection(){
        if((lastServerPort!=null)&&(lastServerIP!=null)){
            startTcpConnection(lastServerIP,lastServerPort);
            return true;
        }else{
            //暂时这样写，未测试的逻辑
            startUdpConnection();
            return false;
        }
    }

    private void startTcpConnection(String ip, String port) {
        if(tcpSocket==null){
            tcpSocket=new TcpSocket(mContext);
            tcpSocket.connectSocket(ip,port);
            tcpSocket.setOnConnectionStateListener(new OnConnectionStateListener() {
                @Override
                public void onLink() {
                    isTcpLink=true;
                    if(udpSocket!=null) udpSocket.stopHeartBeatTimer();
                    for (OnUIChangeListener listener:onUIChangeListenerList)listener.onChange(Config.MsgCode.LINK_SUCCESS,"");
                }
                @Override
                public void onError(int errorCode) {
                    switch (errorCode){
                        case Config.ErrorCode.TCP_CONNECT_ERROR:
                            isServerFind=false;
                            for (OnUIChangeListener listener:onUIChangeListenerList)listener.onError(Config.ErrorCode.TCP_CONNECT_ERROR);
                            if(tcpSocket!=null){
                                tcpSocket.closeSocket();
                                tcpSocket=null;
                            }
                            break;
                        case Config.ErrorCode.PING_TIME_OUT:
                            isTcpLink=false;
                            if(udpSocket!=null)udpSocket.startHeartBeatTimer();
                            if(tcpSocket!=null){
                                tcpSocket.stopHeartBeatTimer();
                                tcpSocket.closeSocket();
                                tcpSocket=null;
                            }
                            break;
                        default:
                            break;
                    }
                }
            });
            tcpSocket.setOnUIChangeListener(new OnUIChangeListener() {
                @Override
                public void onChange(int msgCode, String msg) {
                    if(msgCode==Config.MsgCode.LOGIN_FAIL){
                        lastAccount=null;
                        lastPassword=null;
                    }else if(msgCode==Config.MsgCode.OFFLINE){
                        isTcpLink=false;
                    }
                }

                @Override
                public void onError(int errorCode) {

                }
            });
        }
    }

    public void stopSocket() {
       // isServerFind=false;
        isTcpLink=false;
        if(udpSocket!=null) {
            udpSocket.stopUDPSocket();
            udpSocket=null;
        }
        if(tcpSocket!=null){
            tcpSocket.closeSocket();
            tcpSocket=null;
        }
    }

    public boolean isTcpLink(){return isTcpLink;}

    public boolean isServerFind(){return isServerFind;}

    public TcpSocket getTcpSocket(){return tcpSocket;}

    public String getLastAccount() {
        return lastAccount;
    }

    public String getLastPassword() {
        return lastPassword;
    }

    public void Login(final String account, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("request",Config.MsgCode.LOGIN_SUCCESS);
                    jsonObject.put("account",account);
                    jsonObject.put("password",password);
                    jsonObject.put("deviceName",getDeviceName());
                    jsonObject.put("deviceID",getMacFromHardware());
                    lastAccount=account;
                    lastPassword=password;
                    if(tcpSocket!=null){
                        tcpSocket.sendMsg(jsonObject.toString());
                    } else {
                        for(OnUIChangeListener onUIChangeListener:onUIChangeListenerList){
                            onUIChangeListener.onError(Config.ErrorCode.TCP_CONNECT_ERROR);
                            isServerFind=false;
                            stopSocket();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public String getLastServerIP(){
        return lastServerIP;
    }

    public static String getDeviceName(){
        String deviceName="";
        deviceName= Build.DEVICE;
        Log.i("socketManager", "getDeviceName: "+deviceName);
        return deviceName;
    }

    public static String getMacFromHardware() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }
}
