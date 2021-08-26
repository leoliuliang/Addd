package com.mxkj.h264mediacodecdemo.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetsUtil {
    private Context mContext;

    public AssetsUtil(Context context){
        this.mContext = context;
    }

    public void assetsToSdcard(final String assetsName){
//        final String toPath = new File(Environment.getExternalStorageDirectory()+ File.separator+"aaaAssets", assetsName).getAbsolutePath();
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + Constant.filePath).getAbsoluteFile();
        if (!file.exists()){
            file.mkdir();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    copyAssets(assetsName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
//        return toPath;
    }


    private void copyAssets(String assetsName) throws IOException {
//        AssetFileDescriptor assetFileDescriptor = mContext.getAssets().openFd(assetsName);
//        FileChannel from = new FileInputStream(assetFileDescriptor.getFileDescriptor()).getChannel();
//        FileChannel to = new FileOutputStream(path).getChannel();
//        from.transferTo(assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength(), to);

        try
        {
            boolean sdCardExist = Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED );
            if ( !sdCardExist )
                return;

            File dirFile = new File( Environment.getExternalStorageDirectory() + File.separator+ Constant.filePath );
            if(!dirFile.exists())
                dirFile.mkdir();

            File file = new File( Environment.getExternalStorageDirectory() + File.separator+Constant.filePath+File.separator+assetsName);
            if(file.exists())
                return;

            InputStream ins = mContext.getAssets().open( assetsName );
            FileOutputStream fos = new FileOutputStream( file );
            byte[] buffer = new byte[1024];
            int l = 0;
            while( ( l = ins.read( buffer ) ) != -1 )
            {
                fos.write( buffer, 0, l );
            }
            fos.flush();
            fos.close();
            ins.close();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


}
