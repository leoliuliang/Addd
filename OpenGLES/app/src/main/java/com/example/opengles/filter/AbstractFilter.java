package com.example.opengles.filter;

import android.content.Context;
import android.opengl.GLES20;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class AbstractFilter{

    //屏幕4个顶点坐标，铺满
    float[] VERTEX = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };
    //纹理坐标系，
    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    //gpu顶点坐标缓存区
    FloatBuffer vertexBuffer;
    // 纹理坐标
    FloatBuffer textureBuffer;
    public int program;
    private int vPosition;
    private   int vCoord;
    private   int vTexture;
    protected int mWidth;
    protected int mHeight;

    public AbstractFilter(Context context, int vertexShaderId, int fragmentShaderId){
        vertexBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);

        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);

        //读取出的这段字符串就是顶点程序，需要将这段字符串变为程序需要经过，编译、链接、运行
        String vertexShader = OpenGLUtils.readRawTextFile(context,vertexShaderId);
        //片元着色程序
        String fragShader = OpenGLUtils.readRawTextFile(context, fragmentShaderId);

        //加载程序
        program = OpenGLUtils.loadProgram(vertexShader, fragShader);
        //得到gpu里顶点变量vPosition的地址
        vPosition = GLES20.glGetAttribLocation(program, "vPosition");
        //接收纹理坐标，接收采样器采样图片的坐标
        vCoord = GLES20.glGetAttribLocation(program, "vCoord");//1
        //采样点的坐标
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");

    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }


    //传过来的摄像头数据，在这里给gpu的变量vPosition、vCoord等赋值渲染
    public int onDraw(int texture){
        //告诉gl要渲染的宽高、
        GLES20.glViewport(0,0,mWidth,mHeight);
        //使用程序
        GLES20.glUseProgram(program);

        vertexBuffer.position(0);//从索引为0的地方开始读

        /**
         *         int indx, 顶点程序地址
         *         int size, 顶点数量，VERTEX里每个顶点是x、y两个值
         *         int type, 顶点类型
         *         boolean normalized, 标准化，true自动纠正VERTEX里的错误值
         *         int stride, 大小，3个顶点 * vec4（4个字节）
         *         java.nio.Buffer ptr , 顶点数据
         **/
        GLES20.glVertexAttribPointer(vPosition,2,GLES20.GL_FLOAT,false,0,vertexBuffer);
        //使生效vPosition
        GLES20.glEnableVertexAttribArray(vPosition);


        textureBuffer.position(0);//从索引为0的地方开始读
        GLES20.glVertexAttribPointer(vCoord,2,GLES20.GL_FLOAT,false,0,textureBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);
        /** ---------到此确定了形状----------------------*/

        //激活图层
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        //根据gpu里的缓冲区texture 绑定一个采样器
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);

        //使生效vTexture
        GLES20.glUniform1i(vTexture,0);

        beforeDraw();

        //通知渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        return texture;
    }

    public void beforeDraw() {}

    public void release(){
        GLES20.glDeleteProgram(program);
    }
}
