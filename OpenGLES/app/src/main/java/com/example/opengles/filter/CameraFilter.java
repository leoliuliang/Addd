package com.example.opengles.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.example.opengles.R;

public class CameraFilter extends AbstractFboFilter {
    private   int vMatrix;
    private float[] mtx;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vert, R.raw.camera_frag3);

        //变换矩阵， 需要将原本的vCoord（01,11,00,10） 与矩阵相乘
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");
    }

    @Override
    public void beforeDraw() {
        super.beforeDraw();

        //生效vMatrix
        GLES20.glUniformMatrix4fv(vMatrix,1,false,mtx,0);

    }

    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }

}
