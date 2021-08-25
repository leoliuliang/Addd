package com.mxkj.h264mediacodecdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.mxkj.h264mediacodecdemo.utils.PermissionUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionUtil.checkPermission(this);
    }

    public void H264Encoder(View view) {
        startActivity(new Intent(this,H264EncoderActivity.class));
    }
}