package com.example.opengles;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 渲染黑色背景
 */
public class BackgroundRender implements GLSurfaceView.Renderer {
    float a  = 0f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //第一步，清除之前存留的数据 , 类似 canvas.restore;
        //传入argb值表示清空后的颜色值
        GLES20.glClearColor(a, 0.0f,0.0f,1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
    }

    /**
     * 类似view的onDraw，onDraw是16ms刷新一次，
     * onDrawFrame，可以手动刷新
     * */
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0f, 0.0f,0.0f,0.0f);
    }
}
