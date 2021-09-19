package com.example.x264livertmp.cameraxpush;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.x264livertmp.R;
import com.example.x264livertmp.live.LivePusher;
import com.example.x264livertmp.live.channel.AudioChannel;


public class MainActivity extends AppCompatActivity {

    private String url = "rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_28079658_29481297&key=ac1028dd3255c4cfb76513a3e1508c8b";

    private LivePusher livePusher;
    private TextureView textureView;

    VideoChanel videoChanel;
    AudioChannel audioChannel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camerax);
        checkPermission();
        textureView = findViewById(R.id.textureView);
        livePusher = new LivePusher(this);
        //        链接B站
        livePusher.startLive(url);
        videoChanel = new VideoChanel(this, textureView, livePusher);
        audioChannel = new AudioChannel(44100, 2, livePusher);
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            }, 1);

        }
        return false;
    }

    public void switchCamera(View view) {

    }

    public void startLive(View view) {
        videoChanel.startLive();
        audioChannel.startLive();
    }

    public void stopLive(View view) {
        livePusher.stopLive();
        videoChanel.stopLive();
        audioChannel.stopLive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        livePusher.native_release();
    }
}
