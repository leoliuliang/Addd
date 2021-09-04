package com.mxkj.h264mediacodecdemo.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;


import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 作者：created by 刘亮 on 2021/8/30 17:15
 * 两个音频合成
 *
 * 16位双声道 pcm， 数据两字节，左声道：低8位 高8位 ； 右声道：低8位 高8位
 * 地址递增，低八位高八位？
 *
 * 音乐源文件是双通道的，用其他音频不保证也能行
 *
 */
public class MusicMixtureProcess {

    private static float normalizeVolume(int volume){
        return volume/100f * 1;
    }

    /**
     * pcm1Path 音乐1路径
     * pcm2Path 音乐2路径
     * toPath 输出路径
     * volume 音乐1的音量
     * volume2 音乐2的音量
     * */
    public static void mixPcm(String pcm1Path,String pcm2Path,String toPath,int volume,int volume2) throws Exception {
        float vol1 = normalizeVolume(volume);
        float vol2 = normalizeVolume(volume2);

        //一次多读取一点 2k, 将buffer1写入buffer3, 再将buffer2写入buffer3, 输出buffer3就是混合后的声音
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];
        //待输出数据
        byte[] buffer3 = new byte[2048];

        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);

        FileOutputStream fileOutputStream = new FileOutputStream(toPath);
        short temp1,temp2;
        //两个short变量相加大于short
        int tempOut;
        //音频是否结束
        boolean end1 = false, end2 = false;

        while (!end1 || !end2){
            if (!end1){
                //判断第一个音频 是否读完
                end1 = (is1.read(buffer1) == -1);
                //将音乐1 的pcm数据写到 输出缓冲区buffer3
                System.arraycopy(buffer1,0,buffer3,0,buffer1.length);
            }

            if (!end2){
                //第二个音频是否读完
                end2 = (is2.read(buffer2) == -1);

                //因为一个声音值（比如：E3）是2个字节，所以加2 跳到下个字节
                for (int i = 0; i < buffer2.length; i+=2) {
                    //算出声音的值, 将高8位与低8位交换（第二个字节左移8位再与第一个字节或运算）
                   temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);
                   temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                   //两个声音值相加
                    tempOut = (int) (temp1*vol1+ temp2*vol2);
                    //设置最大最小值, 因为两个字节是65535，声音是波，波的最高点65535/2 = 32767 ，最低点 -32768
                    if (tempOut > 32767){
                        tempOut = 32767;
                    }else if (tempOut < -32768){
                        tempOut = -32768;
                    }

                    //加出来的tempOut 需要再转换一下再输出，低8位在前，高8位在后；不用考虑通道数，因为输入就是顺序输入的。
                    buffer3[i] = (byte) (tempOut & 0xff);
                    buffer3[i+1] = (byte) ((tempOut >>> 8) & 0xff);
                }
                fileOutputStream.write(buffer3);
            }
        }
        is1.close();
        is2.close();
        fileOutputStream.close();
    }

    public   void mixAudioTrack(final String videoInput,
                                final String audioInput,
                                final String output,
                                final Integer startTimeUs, final Integer endTimeUs,
                                int videoVolume,//视频声音大小
                                int aacVolume//音频声音大小
    ) throws  Exception {
        final File videoPcmFile = new File(Constant.filePath2, "video.pcm");
        final File musicPcmFile = new File(Constant.filePath2, "music.pcm");

        decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);
        decodeToPCM(audioInput, musicPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

        final File mixPcmFile = new File(Constant.filePath2, "mix.pcm");

        mixPcm(videoPcmFile.getAbsolutePath(), musicPcmFile.getAbsolutePath(), mixPcmFile.getAbsolutePath(), videoVolume, aacVolume);

        new PcmToWavUtil(44100,  AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixPcmFile.getAbsolutePath()
                , output);
    }

    @SuppressLint("WrongConstant")
    public void decodeToPCM(String musicPath,String outPath, int startTime, int endTime){
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

            File pcmFile = new File(outPath);
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

                   /* //输出文件 方便查看
                    ByteUtil.writeContent(contents,"out.pcm");*/

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

          /*  // pcm 数据转换为mp3封装格式
            File wavFile = new File(Constant.filePath2, "output.mp3");
            new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,2,AudioFormat.ENCODING_PCM_16BIT)
                    .pcmToWav(pcmFile.getAbsolutePath(),wavFile.getAbsolutePath());
            */

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
