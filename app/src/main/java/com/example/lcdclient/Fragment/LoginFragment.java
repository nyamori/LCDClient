package com.example.lcdclient.Fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.lcdclient.R;
import com.example.lcdclient.socket.SocketManager;


public class LoginFragment extends Fragment {

    private static final String TAG="LoginFragment";

    private String serverIP=null;
    private TextView serverIpView;
    private TextInputLayout account;
    private TextInputLayout password;
    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        if(isAdded()){
            serverIP=getArguments().getString("serverIP");
            if(serverIpView!=null)serverIpView.setText(serverIP);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        serverIpView=(TextView)getActivity().findViewById(R.id.server_ip);
        account=(TextInputLayout)getActivity().findViewById(R.id.user_account);
        password=(TextInputLayout)getActivity().findViewById(R.id.user_password);
        setErrorListener();
        getActivity().findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mAccount=account.getEditText().getText().toString();
                String mPassword = password.getEditText().getText().toString();
                Log.d(TAG, "onClick: 点击了登陆按钮"+mAccount+mAccount.length()+mPassword+mPassword.length());
                getActivity().findViewById(R.id.login_button).setEnabled(false);
                if((mAccount.length()==11)&&(mPassword.length()==6))
                    SocketManager.getInstance(getActivity()).Login(mAccount,mPassword);
            }
        });
    }

    private void setErrorListener() {
        account.setErrorEnabled(true);
        account.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    if ("".equals(charSequence + "")) {
                        // 设置错误提示语为null，即不显示错误提示语
                        account.setError(null);
                    } else if (charSequence.length() > 11) {
                        // 如果输入长度超过6位，则显示错误提示
                        account.setError("长度超过上限！");
                    } else {
                        Integer.parseInt( charSequence+ "");
                        account.setError(null);
                    }
                } catch (Exception e) {
                    // 设置错误提示语为具体的文本
                    account.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        password.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    if ("".equals(charSequence + "")) {
                        // 设置错误提示语为null，即不显示错误提示语
                        password.setError(null);
                    } else if (charSequence.length() > 6) {
                        // 如果输入长度超过6位，则显示错误提示
                        password.setError("密码长度超过上限！");
                    } else {
                        Integer.parseInt(charSequence + "");
                        password.setError(null);
                    }
                } catch (Exception e) {
                    // 设置错误提示语为具体的文本
                    password.setError("输入内容不是数字！");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
}
