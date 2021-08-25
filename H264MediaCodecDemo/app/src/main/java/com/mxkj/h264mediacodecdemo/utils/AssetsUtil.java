package com.mxkj.h264mediacodecdemo.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class AssetsUtil {
    private Context mContext;

    public AssetsUtil(Context context){
        this.mContext = context;
    }

    public String assetsToSdcard(final String assetsName){
        final String toPath = new File(Environment.getExternalStorageDirectory()+ File.separator+Constant.filePath, assetsName).getAbsolutePath();
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + Constant.filePath).getAbsoluteFile();
        if (!file.exists()){
            file.mkdir();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    copyAssets(assetsName, toPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return toPath;
    }


    private void copyAssets(String assetsName, String path) throws IOException {
        AssetFileDescriptor assetFileDescriptor = mContext.getAssets().openFd(assetsName);
        FileChannel from = new FileInputStream(assetFileDescriptor.getFileDescriptor()).getChannel();
        FileChannel to = new FileOutputStream(path).getChannel();
        from.transferTo(assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength(), to);
    }


}
