package com.mxkj.h264mediacodecdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Surface;

import com.mxkj.h264mediacodecdemo.utils.ByteUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 通过录屏，将录屏数据编码后输出为.h264压缩文件
 * */
public class H264EncoderActivity extends AppCompatActivity {
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    int width = 540;
    int height = 960;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_encoder);

        this.mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent,100);
    }

    private void iniMediaCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            //设置码率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,1200_000);
            //告诉dsp芯片每秒钟编码出15帧
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,15);
            //设置I帧间隔2秒钟（场景不变的情况下）, 这里如果场景变换而此时还不足2秒也会编码出I帧
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);

            //format 配置到mediaCodec
            mediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);

            final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            final Surface surface = mediaCodec.createInputSurface();
            //编码是耗时操作
            new Thread(){
                @Override
                public void run() {
                    //开始编码
                    mediaCodec.start();
                    //mediaCodec提供surface，

                    //需要将此surface与mediaProjection进行关联, 此处是一个虚拟的surface，只是有来包裹数据
                    mediaProjection.createVirtualDisplay("screen-codec",width,height,1,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,surface,null,null);

                    //上面已经将编码数据放到dsp芯片，下面只需呀查询数据就好了
                    while (true){
                        //源源不断的查询编码好的数据，
                        int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
                         if (outIndex >= 0){
                             //查询成功, 此时是还是h264压缩数据
                             ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
                             //dsp芯片的byteBuffer要及时取出，不然会阻塞dsp, 所以写到临时 byte[]里
                             byte[] outData = new byte[bufferInfo.size];
                             byteBuffer.get(outData);

                             //以字符串的方式写到 codex.txt，是不能播放的
                             ByteUtil.writeContent(outData,"codec.txt");

                             //以文件方式写到 codec.h264
                             ByteUtil.writeBytes(outData,"codec.h264");
                         }
                    }
                }
            }.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==100 && resultCode == Activity.RESULT_OK){
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            iniMediaCodec();
        }
    }
}