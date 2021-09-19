package com.example.x264livertmp.cameraxpush;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import com.example.x264livertmp.live.LivePusher;
import com.example.x264livertmp.live.channel.ImageUtil;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 作者：created by 刘亮 on 2021/9/18 11:10
 */
public class VideoChanel implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer{
    private static final String TAG = "VideoChanel";
    int width = 480;
    int height = 640;
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private TextureView textureView;
    private LivePusher livePusher;


    private ReentrantLock lock = new ReentrantLock();
    private byte[] y;
    private byte[] u;
    private byte[] v;
    private MediaCodec mediaCodec;
    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;
    private boolean isLiving;



    public VideoChanel(LifecycleOwner lifecycleOwner, TextureView textureView, LivePusher livePusher) {
        this.textureView = textureView;
        this.livePusher = livePusher;
        //子线程中回调

        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        CameraX.bindToLifecycle(lifecycleOwner, getPreView(),getAnalysis());
    }

    private Preview getPreView() {
        PreviewConfig previewConfig = new PreviewConfig.Builder().setTargetResolution(new Size(width, height)).setLensFacing(currentFacing).build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        return preview;
    }

    private ImageAnalysis getAnalysis() {
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(width, height))
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);
        return imageAnalysis;
    }

    public void startLive() {
        isLiving = true;
    }

    public void stopLive() {
        isLiving = false;
    }

    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        if (!isLiving) {
            return;
        }
        lock.lock();

        ImageProxy.PlaneProxy[] planes =  image.getPlanes();
        // 重复使用同一批byte数组，减少gc频率
        if (y == null) {
//            初始化y v  u
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
            livePusher.native_setVideoEncInfo(image.getHeight(), image.getWidth(), 10, 640_000);
        }

        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
            int stride = planes[0].getRowStride();
            Size size = new Size(image.getWidth(), image.getHeight());
            int width = size.getHeight();
            int heigth = image.getWidth();
            //Log.i(TAG, "analyze: "+width+"  heigth "+heigth);
            if (nv21 == null) {
                nv21 = new byte[heigth * width * 3 / 2];
                nv21_rotated = new byte[heigth * width * 3 / 2];
            }
            ImageUtil.yuvToNv21(y, u, v, nv21, heigth, width);
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, heigth, width);
            this.livePusher.native_pushVideo(nv21_rotated);
        }

        lock.unlock();
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView.getSurfaceTexture() != surfaceTexture) {
            if (textureView.isAvailable()) {
                // 当切换摄像头时，会报错
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);
                parent.requestLayout();
            }
            textureView.setSurfaceTexture(surfaceTexture);
        }
    }
}
