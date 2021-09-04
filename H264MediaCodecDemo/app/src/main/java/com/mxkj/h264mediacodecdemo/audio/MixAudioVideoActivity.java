package com.mxkj.h264mediacodecdemo.audio;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.jaygoo.widget.RangeSeekBar;
import com.mxkj.h264mediacodecdemo.R;
import com.mxkj.h264mediacodecdemo.utils.Constant;

import java.io.File;

public class MixAudioVideoActivity  extends AppCompatActivity {
    VideoView videoView;
    RangeSeekBar rangeSeekBar;
    SeekBar musicSeekBar;
    SeekBar voiceSeekBar;
    int musicVolume=0;
    int voiceVolume=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mix_audio_video);
        videoView = findViewById(R.id.videoView);
        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        musicSeekBar = findViewById(R.id.musicSeekBar);
        voiceSeekBar = findViewById(R.id.voiceSeekBar);
        musicSeekBar.setMax(100);
        voiceSeekBar.setMax(100);
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        voiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                voiceVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    Runnable runnable;
    int duration = 0;
    @Override
    protected void onResume() {
        super.onResume();

        startPlay(new File(Constant.filePath2, "input.mp4").getAbsolutePath());
    }
    private void startPlay(String path) {
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.height = 675;
        layoutParams.width = 1285;
        videoView.setLayoutParams(layoutParams);
        videoView.setVideoPath(path);

        videoView.start();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                duration = mp.getDuration() / 1000;
                mp.setLooping(true);
                rangeSeekBar.setRange(0, duration);
                rangeSeekBar.setValue(0, duration);
                rangeSeekBar.setEnabled(true);
                rangeSeekBar.requestLayout();
                rangeSeekBar.setOnRangeChangedListener(new RangeSeekBar.OnRangeChangedListener() {
                    @Override
                    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
                        videoView.seekTo((int) min * 1000);
                    }
                });
                final Handler handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (videoView.getCurrentPosition() >= rangeSeekBar.getCurrentRange()[1] * 1000) {
                            videoView.seekTo((int) rangeSeekBar.getCurrentRange()[0] * 1000);
                        }
                        handler.postDelayed(runnable, 1000);
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        });
    }
    public void music(View view) {
        String cacheDir =  Constant.filePath2;
        final File videoFile = new File(cacheDir, "input.mp4");
        final File audioFile = new File(cacheDir, "music.mp3");
        final File outputFile = new File(cacheDir, "output.mp4");
        new Thread(){
            @Override
            public void run() {
                try {
                    new VideoMusicMixtureProcess().mixAudioTrack(
                            videoFile.getAbsolutePath(),
                            audioFile.getAbsolutePath(),
                            outputFile.getAbsolutePath(),
                            (int) (rangeSeekBar.getCurrentRange()[0] * 1000* 1000),
                            (int) (rangeSeekBar.getCurrentRange()[1] * 1000* 1000),
                            voiceVolume,
                            musicVolume);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startPlay(new File(Constant.filePath2, "output.mp4").getAbsolutePath());
                        Toast.makeText(MixAudioVideoActivity.this, "剪辑完毕", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }.start();
    }

}