package com.example.opengles.soul;

import android.content.Context;

import com.example.opengles.R;
import com.example.opengles.filter.AbstractFboFilter;

public class SplitFilter extends AbstractFboFilter {
    public SplitFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.split3_screen);
    }

    public int onDraw(int texture  ) {
        // fbo-> new texture  美颜 ps    ps   ps ---》美颜  步骤   ps
        super.onDraw(texture );
        return frameTextures[0];
    }
}