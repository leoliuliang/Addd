package com.example.opengles.filter;

import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.lifecycle.LifecycleOwner;

public class CameraHelper  {
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private Preview.OnPreviewOutputUpdateListener listener;

    public CameraHelper(LifecycleOwner lifecycleOwner,Preview.OnPreviewOutputUpdateListener listener,int type){
        if (type==3){
            currentFacing = CameraX.LensFacing.FRONT;
        }
        this.listener = listener;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        CameraX.bindToLifecycle(lifecycleOwner,getPreview());
    }


    private Preview getPreview() {
        //分辨率并不是最终分辨率，CameraX会根据设备的支持情况，结合你的参数，设置一个最为接近的合适分辨率
        PreviewConfig config = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(currentFacing)
                .build();

        Preview preview = new Preview(config);
        //设置摄像头数据回调监听才能拿到摄像头数据
        preview.setOnPreviewOutputUpdateListener(listener);
        return preview;
    }
}
