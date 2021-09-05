package com.mxkj.rtmpliving.rtmpbilibli;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Bundle;
import android.view.Surface;

import com.mxkj.rtmpliving.task.LiveTaskManager;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 编码层
 * */
public class VideoCodec extends Thread {
    //录屏工具类
    private MediaProjection mediaProjection;
    //虚拟的画布
    private VirtualDisplay virtualDisplay;
    //传输层的引用
    private ScreenLive screenLive;

    private MediaCodec mediaCodec;
    private boolean isLiving;
    //    每一帧编码时间
    private long timeStamp;
    //    开始时间
    private long startTime;

    public VideoCodec(ScreenLive screenLive){
        this.screenLive = screenLive;
    }

    @Override
    public void run() {
        super.run();
        isLiving = true;
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//        根据dsp芯片的不同，不一定能主动编码出I帧，画面背景不变时可能不会编码出I帧，需要手动触发I帧
        while (isLiving) {
            if (System.currentTimeMillis() - timeStamp >= 2000) {
//              当前帧编码如果超过2秒，通知 dsp 芯片触发I帧
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mediaCodec.setParameters(params);

                timeStamp = System.currentTimeMillis();
            }
            int index = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
            if (index >= 0) {
                if (startTime == 0) {
//                  rtmp协议 里是毫秒
                    startTime = bufferInfo.presentationTimeUs / 1000;
                }
                ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
                MediaFormat mediaFormat= mediaCodec.getOutputFormat(index);
                byte[] outData = new byte[bufferInfo.size];
                buffer.get(outData);
                //封装成javabean
                RTMPPackage rtmpPackage = new RTMPPackage(outData, (bufferInfo.presentationTimeUs / 1000) - startTime);
                rtmpPackage.setType(RTMPPackage.RTMP_PACKET_TYPE_VIDEO);
                screenLive.addPackage(rtmpPackage);

                mediaCodec.releaseOutputBuffer(index, false);
            }
        }
        isLiving = false;
        mediaCodec.stop();
        mediaCodec.release();
        mediaCodec = null;
        virtualDisplay.release();
        virtualDisplay = null;
        mediaProjection.stop();
        mediaProjection = null;
        startTime = 0;
    }

    //初始化编码、开是编码
    public void startLive(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;

        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 720, 1280);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        format.setInteger(MediaFormat.KEY_BIT_RATE, 400_000);
//      直播帧率比较低，I帧间隔短，背景切换不频繁一秒15帧够了
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(format, null, null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "screen-codec",
                    720, 1280, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface, null, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
        LiveTaskManager.getInstance().execute(this);
    }
}
