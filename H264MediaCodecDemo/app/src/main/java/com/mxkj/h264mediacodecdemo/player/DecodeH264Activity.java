package com.mxkj.h264mediacodecdemo.player;

import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;


import com.mxkj.h264mediacodecdemo.R;
import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;

public class DecodeH264Activity extends AppCompatActivity {

    private H264Player h264Player;
    private H264Player2Image h264Player2Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode_h264);
        
        initSurface();

    }

    private void initSurface() {
        SurfaceView surfaceView = findViewById(R.id.preview);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                final String path = new File(Environment.getExternalStorageDirectory()+ File.separator+ Constant.filePath, "out.h264").getAbsolutePath();
                h264Player = new H264Player(DecodeH264Activity.this,holder.getSurface(),path);
                h264Player.play();

                h264Player2Image = new H264Player2Image(DecodeH264Activity.this,holder.getSurface(),path);
                h264Player2Image.play();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }
}