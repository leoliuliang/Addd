package com.mxkj.h264mediacodecdemo.audio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.mxkj.h264mediacodecdemo.R;
import com.mxkj.h264mediacodecdemo.utils.AssetsUtil;
import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;

/**
 * 音频剪辑
 * */
public class ClipAudioActivity extends AppCompatActivity {
    MusicProcess musicProcess;
    MusicMixtureProcess musicMixtureProcess;

    private String sdcardPath1 = "music.mp3";
    private String sdcardPath2 = "input.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_audio);
        new AssetsUtil(ClipAudioActivity.this).assetsToSdcard(sdcardPath1);
        new AssetsUtil(ClipAudioActivity.this).assetsToSdcard(sdcardPath2);
        musicProcess = new MusicProcess();
        musicMixtureProcess = new MusicMixtureProcess();
    }

    public void clip(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                musicProcess.clip(Constant.filePath2+sdcardPath1,10*1000*1000,15*1000*1000);
            }
        }).start();
    }

    public void mixure(View view) {
        //将一个视频的aac提取出来 和 另一个音频混合
        new Thread(new Runnable() {
            @Override
            public void run() {
                String aacPath = new File(Constant.filePath2, sdcardPath1).getAbsolutePath();
                String videoAAPath = new File(Constant.filePath2, sdcardPath2).getAbsolutePath();

                String outPathPcm = new File(Constant.filePath2, "mix.mp3").getAbsolutePath();

                try {
                    musicMixtureProcess.mixAudioTrack(videoAAPath, aacPath,
                                outPathPcm, 5 * 1000 * 1000, 15 * 1000 * 1000,
                                70,//0 - 100
                                50);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

    }

    public void douyindapian(View view) {
        startActivity(new Intent(this,MixAudioVideoActivity.class));
    }
}