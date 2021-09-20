package com.example.opengles.filter;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * 滤镜相机
 *
 * 设置gl监听
 *
 * opengl所有的代码都是运行在gl线程的
 *
 * */
public class CameraView extends GLSurfaceView {
    CameraRender cameraRender;
    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);//配置opengl版本
        cameraRender = new CameraRender(this);
        setRenderer(cameraRender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
