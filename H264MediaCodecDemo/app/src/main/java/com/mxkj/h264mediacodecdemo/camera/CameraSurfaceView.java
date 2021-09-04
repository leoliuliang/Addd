package com.mxkj.h264mediacodecdemo.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.mxkj.h264mediacodecdemo.utils.ByteUtil;
import com.mxkj.h264mediacodecdemo.utils.Constant;
import com.mxkj.h264mediacodecdemo.utils.YuvUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 作者：created by 刘亮 on 2021/8/27 14:28
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    Camera mCamera;
    Camera.Size previewSize;
    byte[] bytes;
    byte[] nv12;
    private volatile boolean isCaptrue;
    private volatile boolean isVideo;
    MediaCodec mediaCodec;

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);

    }

    public void startCaptrue(){
        isCaptrue = true;
    }
    public void startVideo(){
        isVideo = true;
    }

    private void initCodec(){
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, previewSize.height, previewSize.width);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,15);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,4000_000);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2);

            mediaCodec.configure(videoFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isCaptrue){
//            portraitData2Raw(data);
//            YuvUtils.portraitData2Raw(data,bytes,previewSize.width,previewSize.height);
            YuvUtils.nv21_rotate_to_90(data,nv12,previewSize.width,previewSize.height);
            captrue(nv12);
            isCaptrue = false;
        }

        if (isVideo){
            video(data);
        }

        mCamera.addCallbackBuffer(bytes);
    }

    //    画面旋转90度  rtmp 推流  软解推流
    private void portraitData2Raw(byte[] data) {
        int width = previewSize.width;
        int height =previewSize.height;
//        旋转y
        int y_len = width * height;
//u   y/4   v  y/4
        int uvHeight = height/2;

        int k = 0;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
//                存值  k++  0          取值  width * i + j
                bytes[k++] = data[width * i + j];
            }
        }
//        旋转uv

        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                bytes[k++] = data[y_len + width * i + j];
                bytes[k++] = data[y_len + width * i + j + 1];
            }
        }
    }


    //录像输出h264
    private void video(byte[] data) {
        // 1. 旋转90度
//        portraitData2Raw(bytes);
        YuvUtils.nv21_rotate_to_90(data,nv12,previewSize.width,previewSize.height);
        // 2. nv21转nv12(yuv420), 因为mediaCodec不支持nv21，只支持nv12.
        byte[] temp = YuvUtils.nv21toNV12(nv12);

        //3. 放入数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int inputIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inputIndex >= 0){
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inputIndex);
            byteBuffer.clear();
            byteBuffer.put(temp,0,temp.length);
            mediaCodec.queueInputBuffer(inputIndex,0,temp.length,0,0);
        }

        //4. 取出编码好得数据, ba就是编码好的h264数据
        int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
        if (outIndex >= 0){
            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
            //remaining是取出有效数据，不包括无效的0000这种数据，size包括了所有
            byte[] ba = new byte[byteBuffer.remaining()];
            byteBuffer.get(ba);

            ByteUtil.writeBytes(ba,"vip27.h264");
            ByteUtil.writeContent(ba,"vip27.txt");

            mediaCodec.releaseOutputBuffer(outIndex,false);
        }
    }


    //拍照
    private void captrue(byte[] bytes) {
        int index = 0;
        String fileName = "IMG_"+String.valueOf(index++)+".jpg";
        File file = new File(Constant.filePath2, fileName);

        if (!file.exists()){
            try {
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                //拿到yuv原始数据 ，这里经过旋转后拿到的数据width就是height, 高就是宽，交换传值，不然输出的照片是花屏的
                YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, previewSize.height, previewSize.width, null);
                //图像压缩, 将NV21格式图片，以质量70压缩成JEPG，并得到jpeg数据流
                yuvImage.compressToJpeg(new Rect(0,0,yuvImage.getWidth(),yuvImage.getHeight()),70,outputStream);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }else{
            file.delete();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();

        initCodec();

    }


    private void startPreview() {
        //打開摄像头
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        //获取参数
        Camera.Parameters parameters = mCamera.getParameters();
        //得到预览尺寸
        previewSize = parameters.getPreviewSize();
        try {
            //绑定后渲染
            mCamera.setPreviewDisplay(getHolder());
            //旋转90度预览正常，因为硬件 摄像头是横着的摆放的
            mCamera.setDisplayOrientation(90);

            //android摄像头的格式是NV21, 这里需要将预览数据放入缓冲区，缓冲区大小是总像素的1.5倍
            bytes = new byte[previewSize.width * previewSize.height * 3 / 2];
            nv12 = new byte[previewSize.width * previewSize.height * 3 / 2];
            mCamera.addCallbackBuffer(bytes);
            mCamera.setPreviewCallbackWithBuffer(this);

            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

}
