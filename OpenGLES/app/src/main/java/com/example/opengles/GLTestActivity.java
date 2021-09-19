package com.example.opengles;

import androidx.appcompat.app.AppCompatActivity;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

public class GLTestActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_g_l_test);
        initView();
    }

    private void initView(){
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);

        //glSurfaceView.setRenderer(new BackgroundRender());
        glSurfaceView.setRenderer(new TriangleRender());

        /*渲染方式，RENDERMODE_WHEN_DIRTY表示被动渲染，只有在调用requestRender或者onResume等方法时才会进行渲染，效率比较高。
        * RENDERMODE_CONTINUOUSLY表示持续渲染 */
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        //调用requetRender就会回调Render里的onDrawFrame()
        glSurfaceView.requestRender();
    }
}