package com.example.opengles;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 渲染三角形
 *
 * 顶点程序：运行在gpu的可执行程序
 *
 * 1.顶点程序确定形状
 * 2.栅格化
 * 3.片元程序上色
 *
 */
public class TriangleRender implements GLSurfaceView.Renderer {
    FloatBuffer vertexBuffer;
    int mProgram;

    //设置的颜色，依次为红绿蓝和透明通道
    float color[] = {1.0f,0.0f,0.0f,1.0f};

    //顶点程序 ，给gpu里的vPosition赋值，直接运行在gpu上的
    private final String vertexShaderCode =
            "attribute vec4 vPosition; "+
                    "void main() {"+
                    "gl_Position = vPosition;"+
                    "}";

    //片元程序，给gpu里的 vColor赋值
    private final String fragmentShaderCode =
            "precision mediump float; "+
                    "uniform vec4 vColor;"+
                    "void main(){"+
                    "   gl_FragColor = vColor;"+
                    "}";

    static float triangleCoordinate[] = {
            0.5f, 0.5f, 0.0f, // top
            -0.5f, -0.5f, 0.0f, // bottom left
            0.5f, -0.5f, 0.0f //bottom right
    };

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //将背景设置为灰色
        GLES20.glClearColor(0.5f,0.5f,0.5f,1.0f);
        //
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(triangleCoordinate.length * 4);
        //gpu 重新整理下内存
        byteBuffer.order(ByteOrder.nativeOrder());
        //到GPU申请内存
        vertexBuffer = byteBuffer.asFloatBuffer();
        //放入顶点给gpu
        vertexBuffer.put(triangleCoordinate);
        //告诉gpu从0的位置开始读
        vertexBuffer.position(0);


        //创建一个顶点程序，shader顶点程序在gpu的地址，在cpu里没用的，要传回给gpu
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        //通过顶点程序地址，可将程序源码传到gpu里的顶点程序中
        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        //编译程序，
        GLES20.glCompileShader(vertexShader);

        //创建片元程序
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShader);

        mProgram = GLES20.glCreateProgram();
        //将程序加到program
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        //链接两个程序为一个program
        GLES20.glLinkProgram(mProgram);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //程序在onSurfaceCreated里已经创建好，需要在这里使用程序
        GLES20.glUseProgram(mProgram);

        //塞数据, 将程序mProgram 关联 到 opengl里的变脸vPosition
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //运行顶点程序往gpu里面写
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        /**
         *         int indx, 顶点程序地址
         *         int size, 顶点数量，三角形3个顶点
         *         int type, 顶点类型
         *         boolean normalized,
         *         int stride, 大小，3个顶点 * vec4（4个字节）
         *         java.nio.Buffer ptr , 顶点数据
         *
         * 经此一步 顶点坐标triangleCoordinate才真正写到顶点程序里 ，vPosition变量就数据了
         * */
        GLES20.glVertexAttribPointer(mPositionHandle,3,GLES20.GL_FLOAT,false,3*4,vertexBuffer);

        //片元着色器
        int vColorHandler = GLES20.glGetUniformLocation(mProgram, "vColor");
        //设置颜色,
        GLES20.glUniform4fv(vColorHandler,1,color,0);

        //绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,3);

        //禁止顶点数组的句柄
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
