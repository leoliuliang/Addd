package com.mxkj.h264mediacodecdemo.camera;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;

import androidx.appcompat.app.AppCompatActivity;

import com.mxkj.h264mediacodecdemo.R;
import com.mxkj.h264mediacodecdemo.utils.ByteUtil;
import com.mxkj.h264mediacodecdemo.utils.YuvUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 对Camera2操作
 * */
public class Camera2Activity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera2Helper.Camera2Listener{
    private TextureView textureView;
    private Camera2Helper camera2Helper;
    private MediaCodec mediaCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        textureView = findViewById(R.id.texture_preview);
        textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        //初始化摄像头
        initCamera();
    }

    private void initCamera() {
        camera2Helper = new Camera2Helper(this);
        camera2Helper.start(textureView);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;

    @Override
    public void onPreview(byte[] y, byte[] u, byte[] v, Size previewSize, int stride) {
        //需要将yuv转为yuv420

        if (nv21 == null){
            nv21 = new byte[stride * previewSize.getHeight() * 3/2];
            nv21_rotated = new byte[stride * previewSize.getHeight() * 3/2];
        }

        if (mediaCodec == null){
            initCodec(previewSize);
        }


        YuvUtils.yuvToNv21(y,u,v,nv21,stride,previewSize.getHeight());
        YuvUtils.nv21_rotate_to_90(nv21,nv21_rotated,stride,previewSize.getHeight());
        byte[] temp = YuvUtils.nv21toNV12(nv21_rotated);

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int inIndex = mediaCodec.dequeueInputBuffer(100000);
        if (inIndex >= 0){
            ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
            byteBuffer.clear();
            byteBuffer.put(temp,0,temp.length);
            mediaCodec.queueInputBuffer(inIndex,0,temp.length,0,0);
        }
        int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,100000);
        if (outIndex >= 0){
            ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
            byte[] ba = new byte[byteBuffer.remaining()];
            byteBuffer.get(ba);
            ByteUtil.writeContent(ba,"camera2.txt");
            ByteUtil.writeBytes(ba,"camera2.h264");
            mediaCodec.releaseOutputBuffer(outIndex,false);
        }
    }


    private void initCodec(Size size){
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat videoFormat = MediaFormat.createVideoFormat("video/avc", size.getHeight(), size.getWidth());
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
}