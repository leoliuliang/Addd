package com.mxkj.h264mediacodecdemo.camera;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mxkj.h264mediacodecdemo.R;

/**
* 用Camera1 拍照，数据是经过处理后的，拍照保存后也是竖着的，不是横着的
* */
public class CameraActivity extends AppCompatActivity {

    private CameraSurfaceView cameraSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraSurfaceView = findViewById(R.id.cameraSurfaceView);

    }


    public void cameraCapture(View view) {
        cameraSurfaceView.startCaptrue();
    }

    public void video(View view) {
        cameraSurfaceView.startVideo();
    }
}