package com.mxkj.h264mediacodecdemo.touping;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CodecLiveH265 extends Thread {

    private MediaCodec mediaCodec;
    private SocketLive socketLive;
    private MediaProjection mediaProjection;
    private int width = 720;
    private int height = 1280;
    private VirtualDisplay virtualDisplay;
    //数值来源于h265文档
    private static final int NAL_I = 19;
    private static final int NAL_VPS = 32;

    private byte[] vps_sps_pps_buf;

    public CodecLiveH265(SocketLive socketLive,MediaProjection mediaProjection){
        this.socketLive = socketLive;
        this.mediaProjection = mediaProjection;
    }

    public void startLive(){
        try {
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            //设置码率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,width*height);
            //告诉dsp芯片每秒钟编码出20帧
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,20);
            //设置I帧间隔1秒钟（场景不变的情况下）, 这里如果场景变换而此时还不足1秒也会编码出I帧
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);

            mediaCodec = MediaCodec.createEncoderByType("video/hevc");

            mediaCodec.configure(mediaFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();

            //需要将此surface与mediaProjection进行关联, 此处是一个虚拟的surface，只是有来包裹数据
            virtualDisplay = mediaProjection.createVirtualDisplay("-display",width,height,1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,surface,null,null);


        } catch (IOException e) {
            e.printStackTrace();
        }

        start();
    }

    @Override
    public void run() {
        super.run();
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true){
            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outIndex >= 0){
                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
//                byte[] bytes = new byte[bufferInfo.size];
                dealFrame(bufferInfo,byteBuffer);
            }

        }
    }

    private void dealFrame(MediaCodec.BufferInfo bufferInfo, ByteBuffer byteBuffer) {
        int offset = 4;
        //与上0x7E是取出中间6位，再右移一位将最后 一个0移除, 就算出了中间6位
        int type = (byteBuffer.get(offset) & 0x7E) >> 1;

        if (type == NAL_VPS){
            //如果是vps，就缓存起来
            vps_sps_pps_buf = new byte[bufferInfo.size];
            //把mediaCodec解码出来的数据缓存到全局变量，此时不需要发出去, 再后面编码I帧时 加到 I帧前面即可
            byteBuffer.get(vps_sps_pps_buf);
        }else if(type == NAL_I){
            //取出I帧 发送I帧数据
            byte[] byte_I = new byte[bufferInfo.size];
            byteBuffer.get(byte_I);
            //newBuf 里 要包含 vps、pps + I帧
            byte[] newBuf = new byte[vps_sps_pps_buf.length + byte_I.length];

            //先放vps pps
            System.arraycopy(vps_sps_pps_buf,0,newBuf,0,vps_sps_pps_buf.length);
            //再放 I帧
            System.arraycopy(byte_I,0,newBuf,vps_sps_pps_buf.length,byte_I.length);

            //通过socket推送出去
            this.socketLive.sendData(newBuf);
        }else{
            //其他帧，P帧 直接发送出去
            byte[] byte_P = new byte[bufferInfo.size];
            byteBuffer.get(byte_P);
            this.socketLive.sendData(byte_P);
        }
    }
}
