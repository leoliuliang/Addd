package com.example.opengles.filter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.FrameLayout;

import com.example.opengles.R;

/**
 * 滤镜相机
 * */
public class FilterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        FrameLayout frameLayout = findViewById(R.id.cameraView);
        CameraView cameraView = new CameraView(this,null, 0);
        frameLayout.addView(cameraView);
    }
}