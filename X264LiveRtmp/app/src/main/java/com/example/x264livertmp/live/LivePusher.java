package com.example.x264livertmp.live;

import android.app.Activity;
import android.util.Log;
import android.view.SurfaceHolder;

import com.example.x264livertmp.FileUtils;
import com.example.x264livertmp.live.channel.AudioChannel;
import com.example.x264livertmp.live.channel.VideoChannel;

/**
 * 推流层，主要和native打交道，native实现x264软编码，rtmp推流
 *
 * 1.初始化x264 ，native_init , native_setVideoEncInfo
 * 2.开始连接服务器 ，
 * 3.设置配置参数，独立的一步
 * 4.编码一帧
 *
 * */
public class LivePusher {


    static {
        System.loadLibrary("native-lib");
    }

    private AudioChannel audioChannel;


    public LivePusher(Activity activity) {
        native_init();

//        audioChannel = new AudioChannel();
    }

//    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
//        videoChannel.setPreviewDisplay(surfaceHolder);
//    }

//    public void switchCamera() {
//        videoChannel.switchCamera();
//    }
    private void onPrepare(boolean isConnect) {
        //通知UI
    }
    public void startLive(String path) {
        native_start(path);

//        audioChannel.startLive();
    }

    public void stopLive(){
//        native_stop();

//        audioChannel.stopLive();

    }

//    jni回调java层的方法
    private void postData(byte[] data) {
        Log.i("rtmp", "postData: "+data.length);
        FileUtils.writeBytes(data);
        FileUtils.writeContent(data);
    }

    public void sendAudio(byte[] buffer, int len) {
        nativeSendAudio(buffer, len);
    }
    public native void native_init();

    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);

    public native void native_start(String path);

    public native int initAudioEnc(int sampleRate, int channels);

    public native void native_pushVideo(byte[] data);

    public native void native_release();

    private native void nativeSendAudio(byte[] buffer, int len);
}
