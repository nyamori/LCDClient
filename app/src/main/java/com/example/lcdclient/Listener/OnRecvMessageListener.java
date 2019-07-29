package com.example.lcdclient.Listener;

public interface OnRecvMessageListener {
        void onRecvMessage(String message);
        void onError(int errorCode);
}
