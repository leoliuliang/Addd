package com.mxkj.h264mediacodecdemo.audio;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
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
 * 音视频合成
 * 1.通过提取器MediaExtractor从音乐、视频 的封装格式中解码提取出pcm
 * 2.将两个pcm 通过操作流的方式 写到一个byte[] 然后输出, 完成合并pcm
 * 3.将合并后的mix.pcm 再封装为 wav格式，得到 mix.wav
 * 4.初始化一个新的视频封装容器MediaMuxer， 给新的容器添加两条新轨道（音频、视频），
 * 然后将mix.wav 编码数据aac 写入新的音频轨道，和视频编码数据 h264 写入新的视频轨道
 * 5.释放mediaExtractor 、mediaEncoder 、mediaMuxer
 *
 * 将pcm转换为wav封装格式是为了方便直接取出编码数据，
 * 编码数据（压缩数据）可以直接从封装格式MediaExtractor中获取，
 * 如果直接用原始数据pcm也可以，只不过要自己写算法，很麻烦.
 *
 * 16位双声道 pcm， 数据两字节，左声道：低8位 高8位 ； 右声道：低8位 高8位
 * 地址递增，低八位高八位？
 *
 * music.mp3音乐源文件是双通道的，用其他音频不保证也能行
 *
 */
public class VideoMusicMixtureProcess {
    private static  int TIMEOUT = 1000;

    private static float normalizeVolume(int volume){
        return volume/100f * 1;
    }

    private int getMaxBufferSize(MediaFormat audioFormat){
        int maxBufferSize = 0;
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)){
            maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }else{
            maxBufferSize = 100 * 1000;
        }

        return maxBufferSize;
    }

    private void mixVideoAndMusic(String videoInput, String audioInput, String outPut, Integer startTimeUs, Integer endTimeUs) throws IOException {

        //初始化一个视频封装容器
        MediaMuxer mediaMuxer = new MediaMuxer(outPut, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        /*-------------视频轨道  start-------------------------------------------------------------*/
        //先取出视频里的aac
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(videoInput);
        //视频轨道索引
        int videoTrack = selectTrack(mediaExtractor, false);
        //根据索引取出视频配置信息
        MediaFormat videoFormat = mediaExtractor.getTrackFormat(videoTrack);
        //添加一个轨道，此时还是空轨道，没有数据
        mediaMuxer.addTrack(videoFormat);
        /*-------------视频轨道  end------------------*/

        /*-------------音频轨道  start-----------------------------------------------------------------*/
        int audio_index = selectTrack(mediaExtractor, true);
        // 视频中音频轨道   应该取自于原视频的音频参数
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audio_index);
        int  audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        //        添加一个空的轨道  轨道格式取自 视频文件，跟视频所有信息一样
        int muxerAudioIndex = mediaMuxer.addTrack(audioFormat);
        //音频轨道开辟好了  输出开始工作
        mediaMuxer.start();

        MediaExtractor audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(audioInput);
        int audioTrack = selectTrack(audioExtractor, true);
        audioExtractor.selectTrack(audioTrack);
        MediaFormat pcmTrackFormat = audioExtractor.getTrackFormat(audioTrack);
        /*-------------音频轨道  end---------------*/

        int maxBufferSize = getMaxBufferSize(pcmTrackFormat);


        /*-------------编码配置  start-----------------------------------------------------------------*/

        //创建最后生成所需要的配置信息类MediaFormat
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        //音频编码器
        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);

        int audioBitRate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        //编码设置什么 解码的时候就可以拿到什么
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE,audioBitRate);
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);//音质等级，值越大音质越高
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,maxBufferSize);
        encoder.configure(encodeFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();
        /*-------------编码配置  end----------------------*/

        /*-------------音频添加  start----------------------*/
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        boolean encodeDone = false;
        while (!encodeDone){
            int inputBufferIndex = encoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0){
                //获取pts
                long pts = audioExtractor.getSampleTime();
                if (pts < 0){
                    //pts小于0 ，来到了文件末尾，通知编码器不用编码了
                    encoder.queueInputBuffer(inputBufferIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }else{
                    int flags = audioExtractor.getSampleFlags();

                    //將音频pcm数据读到byteBuffer里，以后都这样read，不再像刚开始一样用分隔符去判断
                    int size = audioExtractor.readSampleData(byteBuffer, 0);

                    //系統的，dsp芯片里的byteBuffer不要轻易直接去用, 会出问题, 放到一个新的byteBuffer去处理
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(byteBuffer);
                    inputBuffer.position(0);

                    //通知dsp编码
                    encoder.queueInputBuffer(inputBufferIndex,0,size,pts,flags);
                    //告诉audioExtractor读完这一帧
                    audioExtractor.advance();
                }
            }
            //获取编码完的数据
            int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT);
            while (outputIndex>=0){
                //跳出外循环
                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                    encodeDone = true;
                    break;
                }
                //取出编码的数据
                ByteBuffer encoderOutputBuffer = encoder.getOutputBuffer(outputIndex);
                //往mediaMuxer这个空轨道里写入混音后的数据
                mediaMuxer.writeSampleData(muxerAudioIndex,encoderOutputBuffer,bufferInfo);

                //释放
                encoderOutputBuffer.clear();
                encoder.releaseOutputBuffer(outputIndex,false);
                outputIndex = encoder.dequeueOutputBuffer(bufferInfo,TIMEOUT);

            }
        }
        /*-------------音频添加  end----------------------*/

        //音频添加好后，取消选中音频轨道
        if (audioTrack >= 0) {
            mediaExtractor.unselectTrack(audioTrack);
        }

        /*-------------视频添加  在封装容器中添加视频轨道信息 start----------------------*/
        //选择视频轨道
        mediaExtractor.selectTrack(videoTrack);
        mediaExtractor.seekTo(startTimeUs,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        byteBuffer = ByteBuffer.allocateDirect(maxBufferSize);
        while (true){
            long sampleTimeUs = mediaExtractor.getSampleTime();
            if (sampleTimeUs == -1){
                break;
            }
            if (sampleTimeUs < startTimeUs){
                //把开始剪辑时间startTimeUs之前的数据丢掉，加快处理速度
                mediaExtractor.advance();
                continue;
            }
            if (endTimeUs != null && sampleTimeUs > endTimeUs){
                break;
            }
            //pts 被重新赋值
            bufferInfo.presentationTimeUs = sampleTimeUs - startTimeUs;
            bufferInfo.flags = mediaExtractor.getSampleFlags();
            //直接从封装视频里读取压缩数据h264
            bufferInfo.size = mediaExtractor.readSampleData(byteBuffer, 0);

            if (bufferInfo.size < 0){
                break;
            }
            //h264 写到轨道mediaMuxer
            mediaMuxer.writeSampleData(videoTrack,byteBuffer,bufferInfo);
            mediaExtractor.advance();
        }

        try{
            audioExtractor.release();
            mediaExtractor.release();
            encoder.stop();
            encoder.release();
            mediaMuxer.release();
        }catch (Exception e){

        }

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

        final File mixPcmFile = new File(Constant.filePath2, "混合后的.pcm");

        mixPcm(videoPcmFile.getAbsolutePath(), musicPcmFile.getAbsolutePath(), mixPcmFile.getAbsolutePath(), videoVolume, aacVolume);

        File wavFile = new File(Constant.filePath2, "混合后的.wav");
        new PcmToWavUtil(44100,  AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixPcmFile.getAbsolutePath()
                , wavFile.getAbsolutePath());
        Log.i("--->", "mixAudioTrack: 转换完毕");

        mixVideoAndMusic(videoInput,wavFile.getAbsolutePath(),output,startTimeUs,endTimeUs);
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
            int audioTrack = selectTrack(mediaExtractor,true);

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
    private int selectTrack(MediaExtractor mediaExtractor , boolean audio) {
        //获取轨道数
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            //直接从封装格式里获取 MediaFormat
            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (audio){
                if (mime.startsWith("audio/")){
                    return i;
                }
            }else{
                if (mime.startsWith("video/")){
                    return i;
                }
            }

        }
        return -5;
    }
}
