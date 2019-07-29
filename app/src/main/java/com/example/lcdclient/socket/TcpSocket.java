package com.example.lcdclient.socket;

import android.content.Context;
import android.util.Log;

import com.example.lcdclient.Config;
import com.example.lcdclient.Listener.OnConnectionStateListener;
import com.example.lcdclient.Listener.OnUIChangeListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TcpSocket {
    private static final String TAG = "TcpSocket";
    private static final long TIME_OUT = 30 * 1000;
    private static final long PING_PERIOD = 3 * 1000;
    private static final String KEYSTOREPASSWORD = "123456";    //密钥库密码
    private static final String KEYSTOREPATH_CLIENT = "kclient.bks";    //本地密钥库
    private static final String KEYSTOREPATH_TRUST = "tclient.bks";        //信任密钥库


    private Context mContext;

    private SSLContext sslContext;
    private KeyStore clientKeyStore;
    private KeyStore trustKeyStore;
    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;

    private ExecutorService mThreadPool;
    private SSLSocket mSocket = null;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private InputStream inputStream;
    private OutputStream outputStream;
    private OnConnectionStateListener mListener;
    private List<OnUIChangeListener> listenerList;
    private HeartBeatTimer timer;
    private long lastRecvTime = 0;

    public TcpSocket(Context context) {
        mContext = context;
        mThreadPool = Executors.newCachedThreadPool();
        lastRecvTime = System.currentTimeMillis();
        listenerList = new ArrayList<>();
    }

    public void setOnConnectionStateListener(OnConnectionStateListener connectionStateListener) {
        mListener = connectionStateListener;
    }

    public void setOnUIChangeListener(OnUIChangeListener listener) {
        listenerList.add(listener);
    }


    public boolean connectSocket(final String ip, final String port) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (initSocket(ip, Integer.valueOf(port))) {
                    if (mListener != null) mListener.onLink();
                    if (listenerList != null)
                        for (OnUIChangeListener listener : listenerList)
                            listener.onChange(Config.MsgCode.LINK_SUCCESS, "");
                    recvMsg();
                    startHeartBeatTimer();
                } else {
                    if (mListener != null) mListener.onError(Config.ErrorCode.TCP_CONNECT_ERROR);
                }
            }
        });
        return false;
    }


    private void startHeartBeatTimer() {
        if (timer == null) {
            timer = new HeartBeatTimer();
        }
        Log.d(TAG, "startHeartBeatTimer: start");
        timer.setOnScheduleListener(new HeartBeatTimer.OnScheduleListener() {
            @Override
            public void onSchedule() {
                Log.d(TAG, "timer is onSchedule...");
                long duration = System.currentTimeMillis() - lastRecvTime;
                if (duration > TIME_OUT) {
                    Log.d(TAG, "30s未收到心跳包，超时");
                    if (mListener != null) mListener.onError(Config.ErrorCode.PING_TIME_OUT);
                    closeSocket();
                } else {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("request", Config.MsgCode.PING);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    sendMsg(jsonObject.toString());
                }
            }
        });
        timer.startTimer(0, 5 * 1000);
    }

    public void stopHeartBeatTimer() {
        if (timer != null) {
            timer.exit();
            timer = null;
        }
    }

    public void recvMsg() {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                String line = "";
                try {
                    while ((line = bufferedReader.readLine()) != null) {
                        Log.d(TAG, "run: 接受到了一行消息" + line);
                        onRecvMsg(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (mSocket != null) {
                        if (mListener != null) mListener.onError(Config.ErrorCode.PING_TIME_OUT);
                        closeSocket();
                    }
                }
            }
        });
    }

    private void onRecvMsg(String line) {
        Log.d(TAG, "onRecvMsg: " + line);
        try {
            JSONObject jsonObject = new JSONObject(line);
            int msgCode = jsonObject.optInt("answer");
            //后续处理
            Log.d(TAG, "onRecvMsg: answer" + msgCode);
            for (OnUIChangeListener listener : listenerList) {
                listener.onChange(msgCode, line);
            }
            lastRecvTime = System.currentTimeMillis();
            Log.d(TAG, "onRecvMsg: lastRecvTime更新");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void sendMsg(final String toString) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String toSend = toString;
                printWriter.println(toSend);
                printWriter.flush();
                Log.d(TAG, "run: send success:" + toSend);
            }
        }).start();
    }

    public boolean closeSocket() {
        stopHeartBeatTimer();
        try {
            if (printWriter != null) {
                printWriter.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (mThreadPool != null) {
                mThreadPool.shutdown();
                mThreadPool = null;
            }
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean initSocket(final String ip, final int port) {
        try {
            if (mSocket == null) {
                //取得TLS协议的SSLContext实例
                sslContext = SSLContext.getInstance("TLS");
                //取得BKS类型的本地密钥库实例，这里特别注意：手机只支持BKS密钥库，不支持Java默认的JKS密钥库
                clientKeyStore = KeyStore.getInstance("BKS");
                //初始化
                clientKeyStore.load(
                        mContext.getResources().getAssets().open(KEYSTOREPATH_CLIENT),
                        KEYSTOREPASSWORD.toCharArray());
                trustKeyStore = KeyStore.getInstance("BKS");
                trustKeyStore.load(mContext.getResources().getAssets()
                        .open(KEYSTOREPATH_TRUST), KEYSTOREPASSWORD.toCharArray());
                //获得X509密钥库管理实例
                keyManagerFactory = KeyManagerFactory.getInstance("X509");
                keyManagerFactory.init(clientKeyStore, KEYSTOREPASSWORD.toCharArray());
                trustManagerFactory = TrustManagerFactory
                        .getInstance("X509");
                trustManagerFactory.init(trustKeyStore);
                //初始化SSLContext实例
                sslContext.init(keyManagerFactory.getKeyManagers(),
                        trustManagerFactory.getTrustManagers(), null);

                Log.i("System.out", "SSLContext初始化完毕...");
                //以下两步获得SSLSocket实例
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                mSocket = (SSLSocket) sslSocketFactory.createSocket(ip, port);

                mSocket.setKeepAlive(true);
                mSocket.setReuseAddress(true);
                mSocket.setTcpNoDelay(true);
            }
            inputStream = mSocket.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            outputStream = mSocket.getOutputStream();
            printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
            Log.d(TAG, "initSocket: true");
            return true;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "initSocket: false");
        return false;
    }

}