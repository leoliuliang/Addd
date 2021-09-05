package com.mxkj.rtmpliving.rtmpbilibli;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 推送层，录屏数据不断往外推送
 * */
public class ScreenLive extends Thread {
    //队列，出参入参
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();

    //是否正在执行
    private boolean isLiving;

    private String url;

    private MediaProjection mediaProjection;

    static {
        System.loadLibrary("rtmp-lib");
    }

    //将录屏数据添加进来
    public void addPackage(RTMPPackage rtmpPackage){
        if (!isLiving){
            return;
        }
        //生产，往队列里添加
        queue.add(rtmpPackage);
    }

    //开始推送
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        start();
    }

    @Override
    public void run() {
        if (!connect(url)) {
            Log.i("ScreenLive", "run: ----------->推送失败");
            return;
        }

        VideoCodec videoCodec = new VideoCodec(this);
        videoCodec.startLive(mediaProjection);

        isLiving = true;
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                //消费，从队列取出数据
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i("ScreenLive", "run: ----------->推送 "+ rtmpPackage.getBuffer().length);

                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length , rtmpPackage.getTms());
            }
        }
    }

    private native boolean sendData(byte[] data, int len, long tms);
//
    public native boolean connect(String url);

}
