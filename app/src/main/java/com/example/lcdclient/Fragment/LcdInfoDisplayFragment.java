package com.example.lcdclient.Fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.lcdclient.Activity.ContentSettingActivity;
import com.example.lcdclient.Activity.MainActivity;
import com.example.lcdclient.Activity.NowDisplayActivity;
import com.example.lcdclient.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class LcdInfoDisplayFragment extends Fragment implements View.OnClickListener{


    public LcdInfoDisplayFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lcd_info_display, container, false);
        Log.d("LcdInfoDisplayFragment", "onCreateView: ");
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().findViewById(R.id.now_display).setOnClickListener(this);
        getActivity().findViewById(R.id.content_setting).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()){
            case R.id.now_display:
                intent.setClass(getActivity(), NowDisplayActivity.class);
                startActivity(intent);
                break;
            case R.id.content_setting:
                MainActivity activity=(MainActivity)getActivity();
                if(activity.isLogin()){
                    intent.setClass(getActivity(), ContentSettingActivity.class);
                    startActivity(intent);
                }else {
                    Toast.makeText(getActivity(),"还没有登陆",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
}
