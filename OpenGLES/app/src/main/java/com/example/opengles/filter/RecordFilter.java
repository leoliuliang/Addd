package com.example.opengles.filter;

import android.content.Context;

import com.example.opengles.R;


public class RecordFilter extends AbstractFilter{
    public RecordFilter(Context context){
        super(context, R.raw.base_vert, R.raw.base_frag);
    }

}
