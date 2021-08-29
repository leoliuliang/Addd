package com.example.kbitplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class FileHandle {

    private static final String TAG = "MediaDecode";

    private List<String> fileNames;
    private String mFolder;
    private int index = 0;
    private int count = 0;
    private final AssetManager mAssetsManager;

    private FileHandle(Context context) {
        this.mAssetsManager = context.getResources().getAssets();
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setFolder(int width, int height) throws IOException {
        setFolder(width + "x" + height);
    }

    public void setFolder(String folder) throws IOException {
        this.mFolder = folder;
        String[] list = mAssetsManager.list(mFolder);
        assert list != null;
        this.index = 0;
        this.count = 0;
        this.fileNames = Arrays.asList(list);
    }

    public byte[] read() throws IOException {
        String fileName = getNextFileName();
        Log.i(TAG, "读取文件:" + fileName);
        //无内容或播放完毕
        if (fileName == null) return null;

        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = this.mAssetsManager.open(this.mFolder.concat("/").concat(fileName));
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1)
                outputStream.write(buffer, 0, len);
            outputStream.flush();
            return outputStream.toByteArray();
        } finally {
            if (inputStream != null)
                inputStream.close();
            if (outputStream != null)
                outputStream.close();
        }
    }

    private String getNextFileName() {
        if (++this.count > 3) {
            return null;
        }
        @SuppressLint("DefaultLocale")
        String format = String.format("%05d-", ++index);

        if (this.fileNames.isEmpty()) return null;

        for (int i = 0; i < this.fileNames.size(); i++) {
            String fileName = this.fileNames.get(i);
            if (fileName.startsWith(format)) {
                this.count = 0;
                return fileName;
            }
        }
        this.index = 0;
        return getNextFileName();
    }

    public static FileHandle newInstance(Context context) {
        return new FileHandle(context);
    }
}
