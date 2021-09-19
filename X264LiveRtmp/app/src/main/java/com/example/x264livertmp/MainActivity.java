package com.example.x264livertmp;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.example.x264livertmp.live.LivePusher;
import com.example.x264livertmp.live.channel.VideoChannel;

public class MainActivity extends AppCompatActivity {

    private LivePusher livePusher;
    private VideoChannel videoChannel;

//    private String url = "rtmp://47.106.114.67/live/test";
    private String url = "rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_28079658_29481297&key=bfc85cf1c7af443a5fa024b83291855c&schedule=rtmp&pflag=9";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        checkPermission();
        livePusher = new LivePusher(this);
        videoChannel = new VideoChannel(livePusher,this,800, 480, 800_000, 10, Camera.CameraInfo.CAMERA_FACING_BACK);
        //  设置摄像头预览的界面
        videoChannel.setPreviewDisplay(surfaceView.getHolder());

    }
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);
        }
        return false;
    }

    public void switchCamera(View view) {
        videoChannel.switchCamera();
    }

    public void startLive(View view) {
        livePusher.startLive(url);
        videoChannel.startLive();
    }

    public void stopLive(View view) {
        livePusher.stopLive();
        videoChannel.stopLive();
    }

    public void toggleCamera(View view) {
    }
}