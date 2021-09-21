package com.example.opengles.soul;

import android.content.Context;
import android.opengl.GLES20;

import com.example.opengles.R;
import com.example.opengles.filter.AbstractFboFilter;

public class BeautyFilter extends AbstractFboFilter {

//句柄
    private int width;
    private int height;
//    片元
    public BeautyFilter(Context context ) {
        super(context, R.raw.base_vert, R.raw.beauty_fragment2);

        width = GLES20.glGetUniformLocation(program, "width");
        height = GLES20.glGetUniformLocation(program, "height");
    }

    @Override
    public void beforeDraw() {
        super.beforeDraw();
        GLES20.glUniform1i(width, mWidth);
        GLES20.glUniform1i(height,mHeight);
    }


}
