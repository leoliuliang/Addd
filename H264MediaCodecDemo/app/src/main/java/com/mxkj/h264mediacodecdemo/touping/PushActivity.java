package com.mxkj.h264mediacodecdemo.touping;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.mxkj.h264mediacodecdemo.R;

/**
 * 投屏push端
* */
public class PushActivity extends AppCompatActivity {
    private MediaProjectionManager mediaProjectionManager;
    SocketLive socketLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);
        this.mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent,100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 100 || resultCode != RESULT_OK) return;
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null)return;
        socketLive = new SocketLive(11000);
        socketLive.start(mediaProjection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketLive.close();
    }
}