package com.mxkj.gifframework;

import android.graphics.Bitmap;

public class GifHandler {
    /**
     * 需要持有native的引用，这个引用在native是指针类型，指针类型大小都是字节，所以用java层用long接收
     * */
    long gifHandler;

    static {
        System.loadLibrary("native-lib");
    }

    private GifHandler(long gifHandler){
        this.gifHandler = gifHandler;
    }

    /**
     *  第一步，开始加载gif文件
     * */
    public static GifHandler load(String path){
        long nativeObj = loadGif(path);
        GifHandler gifHandler = new GifHandler(nativeObj);
        return gifHandler;
    }

    public static native long loadGif(String path);

    public int getWidth(){
        return getWidth(gifHandler);
    }
    public int getHeight(){
        return getHeight(gifHandler);
    }

    /**
     * 第二步，获取宽、高
     * gifHandler就相当于java和native交互的纽带
     * */
    public static native int getWidth(long gifHandler);
    public static native int getHeight(long gifHandler);

    public int updateFrame(Bitmap bitmap){
        return updateFrame(gifHandler,bitmap);
    }
    /**
     *第三步，渲染图片到bitmap
     * */
    public static native int updateFrame(long gifHandler, Bitmap bitmap);

}
