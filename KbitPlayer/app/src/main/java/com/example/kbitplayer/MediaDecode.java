package com.example.kbitplayer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaDecode {

    private static final String TAG = "MediaDecode";
    private boolean isQuit = true;
    private MediaCodec mMediaCodec;
    private MediaFormat mVideoFormat;
    private final MediaCodec.BufferInfo info;

    public void initMedia(String mime, int width, int height) throws IOException {
        this.mMediaCodec = MediaCodec.createDecoderByType(mime);
        this.mVideoFormat = MediaFormat.createVideoFormat(mime, width, height);
//        this.mVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
//        this.mVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 8000_000);
//        this.mVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    }

    private MediaDecode() {
        this.info = new MediaCodec.BufferInfo();
        try {
            initMedia(MediaFormat.MIMETYPE_VIDEO_AVC, 800, 400);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public void start(Surface surface) {
        this.isQuit = false;

        this.mMediaCodec.configure(mVideoFormat, surface, null, 0);
        this.mMediaCodec.start();
    }
    public static String bytesToPrintHex(@NonNull byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
            hexString.append(" ").append(hexChars[j * 2]).append(hexChars[j * 2 + 1]);
        }
        return hexString.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void queueBuffer(byte[] data) {
        int startFrameIndex = 0;
        int totalSize = data.length;
        Log.d(TAG, "queueBuffer: "+bytesToPrintHex(data));

        try {
//            while (!this.isQuit) {
                if (data.length == 0 || startFrameIndex >= totalSize) return;

                int nextFrameIndex = findFrame(data, startFrameIndex + 1);
                if (nextFrameIndex < 0) return;
                Log.d(TAG, "queueBuffer: "+bytesToPrintHex(data));
//                while (!this.isQuit) {
                    int inIndex = this.mMediaCodec.dequeueInputBuffer(10000);
                    if (inIndex < 0) return;
                    ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
//                    ByteBuffer inputBuffer = this.mMediaCodec.getInputBuffer(inIndex);
                    ByteBuffer inputBuffer = inputBuffers[inIndex];
                    inputBuffer.clear();
                    inputBuffer.put(data, 0, data.length);

                    this.mMediaCodec.queueInputBuffer(inIndex, 0, data.length, System.currentTimeMillis(), 0);

                    startFrameIndex = nextFrameIndex;
//                    break;
//                }

                int outIndex = this.mMediaCodec.dequeueOutputBuffer(this.info, 10000);
                while (outIndex > 0) {
                    try {
                        Thread.sleep(1000 / 25);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mMediaCodec.releaseOutputBuffer(outIndex, true);
                    outIndex = mMediaCodec.dequeueOutputBuffer(info, 0);
                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int findFrame(byte[] data, int startFrameIndex) {
        for (int i = startFrameIndex; i < data.length - 4; i++) {
            if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x00 && data[i + 3] == 0x01 || data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x01) {
                return i;
            }
        }
        return -1;
    }

    public void reset() {
        this.isQuit = true;
    }

    public void stop(){
        if (this.mMediaCodec != null){
            this.mMediaCodec.stop();
            this.mMediaCodec.release();

            Log.i(TAG, "停止播放");
        }
    }

    public static MediaDecode newInstance(){
        return new MediaDecode();
    }
}
