package com.mxkj.h264mediacodecdemo.touping.play;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.mxkj.h264mediacodecdemo.R;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 投屏播放端
 * */
public class ToupingPlayerActivity extends AppCompatActivity implements SocketLive.SocketCallback{
    private static final String TAG = "ToupingPlayerActivity";
    Surface surface ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touping_player);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                initSocket();
                initDecoder(surface);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

    }

    MediaCodec mediaCodec;
    public void initDecoder(Surface surface) {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            final MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 720, 1280);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1280);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(format,
                    surface,
                    null, 0);
            mediaCodec.start();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
    private void initSocket() {
        SocketLive screenLive = new SocketLive(this,11000);
        screenLive.start();
    }

    @Override
    public void callBack(byte[] data) {
        Log.i(TAG, "接收到消息: " + data.length);
        int index = mediaCodec.dequeueInputBuffer(100000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            inputBuffer.put(data, 0, data.length);
//       通知dsp芯片帮忙解码
            mediaCodec.queueInputBuffer(index,
                    0, data.length, System.currentTimeMillis(), 0);
        }

//        获取数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);

        while (outputBufferIndex > 0) {
            mediaCodec.releaseOutputBuffer(outputBufferIndex, true
            );
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
    }
}