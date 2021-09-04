package com.mxkj.h264mediacodecdemo.video;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.widget.Toast;

import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 作者：created by 刘亮 on 2021/9/4 13:53
 *
 * 视频合成
 *
 */
public class VideoProcess {
    public VideoProcess(final Activity activity){
        final String videoPath2 = new File(Constant.filePath2, "input.mp4").getAbsolutePath();
        final String videoPath = new File(Constant.filePath2, "input2.mp4").getAbsolutePath();
        final String outpath = new File(Constant.filePath2, "mixOutPath.mp4").getAbsolutePath();

        new Thread(){
            @Override
            public void run() {
                try {
                    appendVideo(videoPath,videoPath2,outpath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "视频合成完成", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }.start();

    }

    private static void appendVideo(String videoPath,String videoPath2,String outPath) throws IOException {

        //新建一个视频封装容器（可以理解为 轨道管理类）
        MediaMuxer mediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        //第一个视频的提取器
        MediaExtractor mediaExtractor1 = new MediaExtractor();
        mediaExtractor1.setDataSource(videoPath);
        //第二个视频的提取器
        MediaExtractor mediaExtractor2 = new MediaExtractor();
        mediaExtractor2.setDataSource(videoPath2);

        //两个视频一共4个轨道

        //新的视频、音频轨道索引，轨道长度是两个视频总时长
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;

        //第一个原视频、音频轨道索引
        int sourceVideoTrack1 = -1;
        int sourceAudioTrack1 = -1;

        //第二个原视频、音频轨道索引
        int sourceVideoTrack2 = -1;
        int sourceAudioTrack2 = -1;

        //第一个视频总时长
        long file1_duration_audio = 0L;
        long file1_duration_video = 0L;


        //遍历第一个视频文件的轨道，找出视频轨道索引
        for (int index =0; index < mediaExtractor1.getTrackCount(); index++){
            MediaFormat format = mediaExtractor1.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")){
                file1_duration_video = format.getLong(MediaFormat.KEY_DURATION);
                sourceVideoTrack1 = index;
                //新增新的 空视频轨道
                videoTrackIndex = mediaMuxer.addTrack(format);

            }else if (mime.startsWith("audio/")){
                file1_duration_audio = format.getLong(MediaFormat.KEY_DURATION);
                sourceAudioTrack1 = index;
                //新增新的 空音频轨道
                audioTrackIndex = mediaMuxer.addTrack(format);
            }
        }


        //第二个视频
        for (int index =0; index < mediaExtractor2.getTrackCount(); index++){
            MediaFormat format = mediaExtractor2.getTrackFormat(index);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")){
                sourceVideoTrack2= index;
            }else if (mime.startsWith("audio/")){
                sourceAudioTrack2 = index;
            }
        }

        //开始工作
        mediaMuxer.start();

        //选中第一个视频轨道
        mediaExtractor1.selectTrack(sourceVideoTrack1);

        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        while (true){
            int sampleSize = mediaExtractor1.readSampleData(buffer, 0);
            if (sampleSize < 0){
                break;
            }
//            byte[] data = new byte[buffer.remaining()];
//            buffer.get(data);
//            ByteUtil.writeBytes(data,"input1.h264");
//            ByteUtil.writeContent(data,"input1.txt");

            info.offset = 0;
            info.presentationTimeUs = mediaExtractor1.getSampleTime();
            info.flags = mediaExtractor1.getSampleFlags();
            info.size = sampleSize;

            mediaMuxer.writeSampleData(videoTrackIndex,buffer,info);
            mediaExtractor1.advance();

        }

        //视频操作完成，开始操作音频
        mediaExtractor1.unselectTrack(sourceVideoTrack1);
        mediaExtractor1.selectTrack(sourceAudioTrack1);

        //避免脏数据，重新初始化相关参数
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500*1024);

        while (true) {
            int sampleSize = mediaExtractor1.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                break;
            }
            info.offset = 0;
            info.presentationTimeUs = mediaExtractor1.getSampleTime();
            info.flags = mediaExtractor1.getSampleFlags();
            info.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex,buffer,info);
            mediaExtractor1.advance();
        }
        mediaExtractor1.release();

        //第三路
        mediaExtractor2.selectTrack(sourceVideoTrack2);

        //避免脏数据，重新初始化相关参数
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500*1024);

        while (true) {
            int sampleSize = mediaExtractor2.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                break;
            }
            info.offset = 0;
            info.presentationTimeUs = mediaExtractor2.getSampleTime() + file1_duration_video;
            info.flags = mediaExtractor2.getSampleFlags();
            info.size = sampleSize;
            mediaMuxer.writeSampleData(videoTrackIndex,buffer,info);
            mediaExtractor2.advance();
        }

        //第4路
        mediaExtractor2.unselectTrack(sourceVideoTrack2);
        mediaExtractor2.selectTrack(sourceAudioTrack2);

        //避免脏数据，重新初始化相关参数
        info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        buffer = ByteBuffer.allocate(500*1024);

        while (true) {
            int sampleSize = mediaExtractor2.readSampleData(buffer, 0);
            if (sampleSize < 0) {
                break;
            }
            info.offset = 0;
            info.presentationTimeUs = mediaExtractor2.getSampleTime() + file1_duration_audio;
            info.flags = mediaExtractor2.getSampleFlags();
            info.size = sampleSize;
            mediaMuxer.writeSampleData(audioTrackIndex,buffer,info);
            mediaExtractor2.advance();
        }


        mediaExtractor2.release();
        mediaMuxer.stop();
        mediaMuxer.release();
    }
}
