package com.example.opengles.filter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 打开摄像头
 * */
public class CameraRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private CameraHelper cameraHelper;
    private CameraView cameraView;
    private SurfaceTexture mCameraTexure;
    private int [] textures;
    private ScreenFilter screenFilter;
    float[] mtx = new float[16];

    public CameraRender(CameraView cameraView){
        this.cameraView = cameraView;
        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
        //实列化CameraHelper就可以打开摄像头
        cameraHelper = new CameraHelper(lifecycleOwner,this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        textures = new int[1];
        //预览数据更opengl关联上了，让SurfaceTexture与GPU共享一个数据源
        mCameraTexure.attachToGLContext(textures[0]);

        //监听摄像头数据的回调
        mCameraTexure.setOnFrameAvailableListener(this);

        //初始化滤镜类，在有数据后给ScreenFilter里的顶点、片元程序
        screenFilter = new ScreenFilter(cameraView.getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //预览有改变，重置宽高
        screenFilter.setSize(width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //在这里将摄像头数据给到ScreenFilter，实现滤镜效果

        //更新摄像头数据
        mCameraTexure.updateTexImage();
        //得到摄像头矩阵
        mCameraTexure.getTransformMatrix(mtx);

        screenFilter.setTransformMatrix(mtx);
        screenFilter.onDraw(textures[0]);
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        //摄像头预览到的数据在这里
        mCameraTexure = output.getSurfaceTexture();
    }


    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //当有数据过来时回调的方法

        //摄像头有数据回来，执行requestRender，回调到onDrawFrame里
        cameraView.requestRender();

    }
}
