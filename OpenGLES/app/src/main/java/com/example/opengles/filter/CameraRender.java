package com.example.opengles.filter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Environment;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import com.example.opengles.soul.SoulFilter;
import com.example.opengles.soul.SplitFilter;

import java.io.File;
import java.io.IOException;

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
    private CameraFilter cameraFilter;
    float[] mtx = new float[16];
    RecordFilter recordFilter;
    private MediaRecorder mRecorder;
    SoulFilter soulFilter;
    SplitFilter splitFilter;
    int type;

    public CameraRender(CameraView cameraView, int type){
        this.cameraView = cameraView;
        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
        //实列化CameraHelper就可以打开摄像头
        cameraHelper = new CameraHelper(lifecycleOwner,this);

        this.type = type;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        textures = new int[1];
        //预览数据更opengl关联上了，让SurfaceTexture与GPU共享一个数据源
        mCameraTexure.attachToGLContext(textures[0]);

        //监听摄像头数据的回调
        mCameraTexure.setOnFrameAvailableListener(this);

        Context context = cameraView.getContext();
        recordFilter = new RecordFilter(context);

        String path = new File(Environment.getExternalStorageDirectory(),
                "input.mp4").getAbsolutePath();

        mRecorder = new MediaRecorder(cameraView.getContext(), path,
                EGL14.eglGetCurrentContext(),
                480, 640);

        //初始化滤镜类，在有数据后给ScreenFilter里的顶点、片元程序
        cameraFilter = new CameraFilter(cameraView.getContext());

        if (type==1) {
            soulFilter = new SoulFilter(cameraView.getContext());
        }else if(type==2){
            splitFilter = new SplitFilter(cameraView.getContext());
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //预览有改变，重置宽高
        cameraFilter.setSize(width,height);
        recordFilter.setSize(width,height);
        if (type==1){
            soulFilter.setSize(width,height);
        }else if (type==2){
            splitFilter.setSize(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //在这里将摄像头数据给到ScreenFilter，实现滤镜效果

        //更新摄像头数据
        mCameraTexure.updateTexImage();
        //得到摄像头矩阵
        mCameraTexure.getTransformMatrix(mtx);

        cameraFilter.setTransformMatrix(mtx);

        //id ，fbo所在的纹理（图层）
        int id = cameraFilter.onDraw(textures[0]);

        if (type==1){
            id = soulFilter.onDraw(id);
        }else if (type==2){
            id = splitFilter.onDraw(id);
        }

        //加载新的顶点程序和片元程序
        id = recordFilter.onDraw(id);

        mRecorder.fireFrame(id,mCameraTexure.getTimestamp());
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

    public void startRecord(float speed) {
        try {
            mRecorder.start(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stopRecord() {
        mRecorder.stop();
    }
}
