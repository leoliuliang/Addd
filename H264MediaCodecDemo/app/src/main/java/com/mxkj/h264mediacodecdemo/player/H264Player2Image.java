package com.mxkj.h264mediacodecdemo.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;


import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 将视频原始数据（dsp芯片bytBuffer数据）输出图片到sdcard
 */
public class H264Player2Image implements Runnable {

    private String path;
//    private Surface surface;
    private Context context;
    //硬解，走dsp芯片
    private MediaCodec mediaCodec;
    int width = 368;
    int height = 384;

    public H264Player2Image(Context context, Surface surface, String path){
        this.context = context;
//        this.surface = surface;
        this.path = path;
        try {
             mediaCodec = MediaCodec.createDecoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,15);
            mediaCodec.configure(mediaFormat,null,null,0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play(){
        mediaCodec.start();
        new Thread(this).start();
    }

    @Override
    public void run() {
        try{
            decodeH264();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private byte[] decodeH264() {
        byte[] bytes =null;
        try {
           bytes = getBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //开始分隔符
        int startIndex = 0;
        //结束分隔符
        int nextFrameStart;

        int totalSize = bytes.length;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        //dsp芯片有16个缓冲队列
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        while (true){
            if (startIndex >= totalSize){
                break;
            }

            //找分隔符, +2是跳过前面的sps和pps
            nextFrameStart = findByFrame(bytes,startIndex+2);

            //查到可用的缓冲队列的索引
            int inIndex = mediaCodec.dequeueInputBuffer(10000);
            if (inIndex >= 0){
                //拿到可用的byteBuffer
                ByteBuffer inputBuffer = inputBuffers[inIndex];
                inputBuffer.clear();
                //放入一段NALU单元到byteBuffer , 就是两个分隔符之间的数据
                inputBuffer.put(bytes,startIndex,nextFrameStart-startIndex);
                //然后就通知dsp解码了，索引很重要，传入可用的队列的索引，意味着用这块空间去解码，因为dsp的所有16个队列都是暴露出来给所有app的，所以一定要找到对应的索引
                mediaCodec.queueInputBuffer(inIndex,0,nextFrameStart-startIndex,0,0);
                //解码完一帧之后，上一帧的结束分隔符就是下一帧的开始分隔符，所有重新赋值
                startIndex = nextFrameStart;
            }else{
                //可能dsp芯片太忙，没有拿到可用的缓冲队列, 那就continue等待
                continue;
            }

            //解码完成后，需要自己去buffer里查询，返回解码好的数据的索引
            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outIndex >= 0){

                try {
                    //先粗鲁的sleep，应该是按照pts来的，这个时间是=解码时间+渲染时间+等待时间
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outIndex);
                outputBuffer.limit(bufferInfo.size);
                //dsp芯片的byteBuffer数据不能直接使用，需要放到另一个容器（byte[]）里
                byte[] ba = new byte[outputBuffer.remaining()];
                outputBuffer.get(ba);

                outputImage(ba);

                //解码成功, 直接释放就行了，不用自己去渲染，传true，表示上面config已经将surface和mediacodec绑定好了，mediaCodec.configure(mediaFormat,surface,null,0);
                mediaCodec.releaseOutputBuffer(outIndex,false);
            }else{
                Log.e("--->","没有解码成功");
            }
        }

        return bytes;
    }

    int i =0;
    //将视频原始yuv数据 输出图片
    private void outputImage(byte[] ba){
        YuvImage yuvImage = new YuvImage(ba, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baso = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0,0,width,height),100,baso);
        byte[] jdata = baso.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

        if (bmp != null){
            if (i % 5 == 0){
                File file = new File(Environment.getExternalStorageDirectory() + File.separator + Constant.filePath + File.separator, "impp.png");
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    bmp.compress(Bitmap.CompressFormat.JPEG,80,bos);
                    bos.flush();
                    bos.close();
                    baso.flush();
                    baso.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            i++;
        }
    }

    private int findByFrame(byte[] bytes, int startIndex) {
        int j =0;
        int totalSize = bytes.length;
        for (int i = startIndex; i < totalSize; i++) {
            if (bytes[i]==0x00 && bytes[i+1]==0x00 && bytes[i+2]==0x00 && bytes[i+3]==0x01){
                return i;
            }
        }

        return -1;
    }


    private byte[] getBytes(String path) throws Exception {
        DataInputStream is = new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf,0,size)) != -1){
            bos.write(buf,0,len);
        }
        buf = bos.toByteArray();
        return buf;
    }
}
