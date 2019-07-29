package com.example.lcdclient.Fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lcdclient.Activity.MainActivity;
import com.example.lcdclient.Config;
import com.example.lcdclient.Listener.OnUIChangeListener;
import com.example.lcdclient.R;
import com.example.lcdclient.socket.SocketManager;


/**
 * A simple {@link Fragment} subclass.
 */
public class CommuStateFragment extends Fragment implements View.OnClickListener{
    private Button linkButton;
    private TextView linkState;
    private SocketManager socketManager;
    public CommuStateFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_commu_state, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        linkState = getActivity().findViewById(R.id.link_state);
        linkButton = getActivity().findViewById(R.id.link_button);
        linkButton.setOnClickListener(this);
        socketManager=SocketManager.getInstance(getActivity());
        if(!socketManager.isTcpLink()){
            UIChange(false);
        }else {
            UIChange(true);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.link_button:
                if(!socketManager.isTcpLink()){
                    socketManager.retryTcpConnection();
                    socketManager.getTcpSocket().setOnUIChangeListener(new OnUIChangeListener() {
                        @Override
                        public void onChange(int msgCode, String msg) {
                            switch (msgCode){
                                case Config.MsgCode.LINK_SUCCESS:
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            UIChange(true);
                                            if(socketManager.getLastAccount()!=null&&socketManager.getLastPassword()!=null){
                                                socketManager.Login(socketManager.getLastAccount(),socketManager.getLastPassword());
                                            }else {
                                                MainActivity mainActivity =(MainActivity)getActivity();
                                                LoginFragment loginFragment=new LoginFragment();
                                                Bundle bundle=new Bundle();
                                                bundle.putString("serverIP",socketManager.getLastServerIP());
                                                loginFragment.setArguments(bundle);
                                                mainActivity.newFragment(loginFragment,"login");
                                            }
                                        }
                                    });
                            }
                        }

                        @Override
                        public void onError(int errorCode) {
                            switch (errorCode){
                                case Config.ErrorCode.PING_TIME_OUT:
                                    UIChange(false);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    Toast.makeText(getActivity(),"开始重新连接",Toast.LENGTH_SHORT).show();
                }
                break;
                default:
                    break;
        }
    }

    private void UIChange(boolean isLink){
        if(isLink){
            linkState.setText("已连接");
            linkState.setTextColor(this.getResources().getColor(R.color.blue));
            linkButton.setBackgroundResource(R.drawable.bt_shape_gray);
        }else {
            linkState.setText("未连接");
            linkState.setTextColor(this.getResources().getColor(R.color.red));
            linkButton.setBackgroundResource(R.drawable.bt_shape);
        }
    }
}

