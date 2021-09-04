package com.mxkj.h264mediacodecdemo.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.mxkj.h264mediacodecdemo.utils.ByteUtil;
import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 作者：created by 刘亮 on 2021/8/30 17:15
 *
 * 解码一段封装格式的音乐，
 * 将解码后的aac裁剪出一段输出为原始数据pcm，并转为mp3
 */
public class MusicProcess {
    @SuppressLint("WrongConstant")
    public void clip(String musicPath, int startTime, int endTime){
        if (endTime < startTime){
            return;
        }
        //提取器，相当于解压工具，从mp3源文件提取数据
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(musicPath);
            int audioTrack = selectTrack(mediaExtractor);

            mediaExtractor.selectTrack(audioTrack);
            
            mediaExtractor.seekTo(startTime,MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            //轨道信息，配置信息等都在 MediaFormat
            MediaFormat format = mediaExtractor.getTrackFormat(audioTrack);
            
            int maxBufferSize;
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)){
                maxBufferSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            }else{
                maxBufferSize = 100*1000;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(maxBufferSize);

            MediaCodec mediaCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            //设置解码器信息
            mediaCodec.configure(format,null,null,0);
            mediaCodec.start();

            File pcmFile = new File(Constant.filePath2,"out.pcm");
            FileChannel writeChannel = new FileOutputStream(pcmFile).getChannel();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outIndex = -1;
            while (true){
                int inputIndex = mediaCodec.dequeueInputBuffer(100_000);
                if (inputIndex >= 0){
                    long sampleTime = mediaExtractor.getSampleTime();
                    if (sampleTime == -1){
                        break;
                    }else if (sampleTime < startTime){
                        //从startTime截取，之前的都丢掉
                        mediaExtractor.advance();
                        continue;
                    }else if (sampleTime > endTime){
                        //截取到endTime，
                        break;
                    }
                    //获取到压缩数据
                    bufferInfo.size = mediaExtractor.readSampleData(byteBuffer, 0);
                    bufferInfo.presentationTimeUs = sampleTime;
                    bufferInfo.flags = mediaExtractor.getSampleFlags();

                    //下面放数据 到dsp解码
                    byte[] contents = new byte[byteBuffer.remaining()];
                    byteBuffer.get(contents);

                    //输出文件 方便查看
                    ByteUtil.writeContent(contents,"out.pcm");
                    //解码
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
                    inputBuffer.put(contents);
                    mediaCodec.queueInputBuffer(inputIndex,0,bufferInfo.size,bufferInfo.presentationTimeUs,bufferInfo.flags);

                    //释放上一帧的压缩数据
                    mediaExtractor.advance();
                }
                outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100_000);
                while (outIndex >= 0){
                    ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outIndex);
                    writeChannel.write(outputBuffer);
                    mediaCodec.releaseOutputBuffer(outIndex,false);
                    outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,100_000);
                }
            }
            
            writeChannel.close();
            mediaExtractor.release();
            mediaCodec.stop();
            mediaCodec.release();
            
            // pcm 数据转换为mp3封装格式
            File wavFile = new File(Constant.filePath2, "output.mp3");
            new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,2,AudioFormat.ENCODING_PCM_16BIT)
                    .pcmToWav(pcmFile.getAbsolutePath(),wavFile.getAbsolutePath());

            Log.i("--->", "mixAudioTrack: 转换完毕");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取轨道
     * */
    private int selectTrack(MediaExtractor mediaExtractor) {
        //获取轨道数
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            //直接从封装格式里获取 MediaFormat
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")){
                return i;
            }
        }
        return -1;
    }
}
