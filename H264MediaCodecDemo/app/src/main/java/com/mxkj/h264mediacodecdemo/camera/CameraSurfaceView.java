package com.mxkj.h264mediacodecdemo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 作者：created by 刘亮 on 2021/8/27 14:28
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    Camera mCamera;
    Camera.Size previewSize;
    byte[] bytes;
    private volatile boolean isCaptrue;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void startCaptrue(){
        isCaptrue = true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isCaptrue){
            portraitData2Raw(bytes);
            captrue(data);
            isCaptrue = false;
        }
        mCamera.addCallbackBuffer(bytes);
    }

    //    画面旋转90度  rtmp 推流  软解推流
    private void portraitData2Raw(byte[] data) {
        int width = previewSize.width;
        int height =previewSize.height;
//        旋转y
        int y_len = width * height;
//u   y/4   v  y/4
        int uvHeight = height/2;

        int k = 0;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
//                存值  k++  0          取值  width * i + j
                bytes[k++] = data[width * i + j];
            }
        }
//        旋转uv

        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                bytes[k++] = data[y_len + width * i + j];
                bytes[k++] = data[y_len + width * i + j + 1];
            }
        }
    }

    int index;
    private void captrue(byte[] bytes) {
        String fileName = "IMG_"+String.valueOf(index++)+".jpg";
        File file = new File(Constant.filePath2, fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                //拿到yuv原始数据
                YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, previewSize.width, previewSize.height, null);
                //图像压缩, 将NV21格式图片，以质量70压缩成JEPG，并得到jpeg数据流
                yuvImage.compressToJpeg(new Rect(0,0,yuvImage.getWidth(),yuvImage.getHeight()),70,outputStream);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }


    private void startPreview() {
        //打開摄像头
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        //获取参数
        Camera.Parameters parameters = mCamera.getParameters();
        //得到预览尺寸
        previewSize = parameters.getPreviewSize();
        try {
            //绑定后渲染
            mCamera.setPreviewDisplay(getHolder());
            //旋转90度预览正常，因为硬件 摄像头是横着的摆放的
            mCamera.setDisplayOrientation(90);

            //android摄像头的格式是NV21, 这里需要将预览数据放入缓冲区，缓冲区大小是总像素的1.5倍
            bytes = new byte[previewSize.width * previewSize.height * 3 / 2];
            mCamera.addCallbackBuffer(bytes);
            mCamera.setPreviewCallbackWithBuffer(this);

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
