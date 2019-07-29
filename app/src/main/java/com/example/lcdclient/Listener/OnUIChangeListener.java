package com.example.lcdclient.Listener;

public interface OnUIChangeListener {
    void onChange(int msgCode,String msg);
    void onError(int errorCode);
}
