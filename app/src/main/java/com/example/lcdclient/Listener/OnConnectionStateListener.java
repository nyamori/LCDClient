package com.example.lcdclient.Listener;

public interface OnConnectionStateListener {
    void onLink();
    void onError(int errorCode);
}
