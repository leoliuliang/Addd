package com.example.kbitplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.MediaSizeCallback {

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initData() {
        try {
            this.mMediaPlayer = MediaPlayer.newInstance(this);
            this.mMediaPlayer.setMediaSizeCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        this.mSurfaceView = findViewById(R.id.main_atv_sv);
        SurfaceHolder mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (this.mMediaPlayer != null) {
            try {
                mMediaPlayer.setFolder(800, 400);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            this.mMediaPlayer.play(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (this.mMediaPlayer != null)
            this.mMediaPlayer.disPlay();
    }

    public void setSize(int width, int height) {
        ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        mSurfaceView.setLayoutParams(layoutParams);
    }

    @Override
    public void sizeChanged(int width, int height) {
        setSize(width, height);
    }
}
