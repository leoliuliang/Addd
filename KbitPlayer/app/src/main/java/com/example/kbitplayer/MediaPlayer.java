package com.example.kbitplayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.RequiresApi;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class MediaPlayer implements Runnable {
    private static final String TAG = "MediaPlayer";
    private Surface mSurface;
    private boolean isStop = false;
    private final MediaDecode mMediaDecode;
    private final FileHandle mFileHandle;
    private LinkedBlockingQueue<Boolean> que;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Context mContext;

    private MediaPlayer(Context context) throws Exception {
        mContext = context;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            throw new Exception(context.getString(R.string.app_error_version));

        this.que = new LinkedBlockingQueue<>();
        this.mMediaDecode = MediaDecode.newInstance();
        this.mFileHandle = FileHandle.newInstance(context instanceof Activity ? context.getApplicationContext() : context);

        new Thread(this).start();
    }

    public void setFolder(int width, int height) throws IOException {
        this.mFileHandle.setFolder(width, height);
    }

    public void setFolder(String folder) throws IOException {
        this.mFileHandle.setFolder(folder);
    }

    public void play(Surface surface) {
        this.mSurface = surface;
        this.isStop = false;
        this.mMediaDecode.start(surface);
        try {
            this.que.put(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        this.isStop = true;
        this.mMediaDecode.stop();
    }

    private String[] getAllFiles(){
        String[]  files = null;
        try {
            files = mContext.getAssets().list("800x480");
            StringBuilder builder = new StringBuilder();
            for(String node : files){
                builder.append(node);
            }
            Log.d(TAG, "getAllFiles: "+builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        while (true) {
            try {
                Boolean take = this.que.take();
                if (!take) {
                    this.mMediaDecode.reset();
                    this.mMediaDecode.stop();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            String[] fileNames = getAllFiles();
            int fileCount = fileNames.length;
            int index = 0;
            int size = 0;
            while (fileCount-- > 0 && !this.isStop) {
                byte[] read = new byte[1 << 18];
//                try {
//                    read = this.mFileHandle.read();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    break;
//                }
//                if (read == null) break;
//
//                final int width = H264Utils.getWidth(read);
//                final int height = H264Utils.getHeight(read);
//
//                mHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mMediaSizeCallback != null)
//                            mMediaSizeCallback.sizeChanged(width, height);
//                    }
//                });
//
//                try {
//                    mMediaDecode.initMedia(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    break;
//                }
                try {
                    String fileName = fileNames[index++];
                    InputStream inputStream = mContext.getAssets().open("800x480/"+fileName);
                    if(inputStream == null){
                        continue;
                    }
                    size = inputStream.read(read);
                    inputStream.close();
                    Log.d(TAG, "decodeReadTask: read size "+size);
                    if(size <= 0){
                        index++;
                        Log.d(TAG, "decodeReadTask: read size failed, filename "+fileName);
                        continue;
                    }
                    this.mMediaDecode.queueBuffer(read);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void disPlay() {
        this.isStop = true;
        this.que.clear();
        this.mFileHandle.setIndex(0);
        if (this.mMediaDecode != null)
            this.mMediaDecode.reset();
    }

    public static MediaPlayer newInstance(Context context) throws Exception {
        return new MediaPlayer(context);
    }

    private MediaSizeCallback mMediaSizeCallback;

    public void setMediaSizeCallback(MediaSizeCallback mediaSizeCallback) {
        this.mMediaSizeCallback = mediaSizeCallback;
    }

    public interface MediaSizeCallback {
        void sizeChanged(int width, int height);
    }
}
