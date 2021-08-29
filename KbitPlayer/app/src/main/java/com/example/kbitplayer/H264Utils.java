package com.example.kbitplayer;

import android.util.Log;

public class H264Utils {

    private static final String TAG = "H264Utils";

    public static int getWidth(byte[] data) {
        int size = getSize(data, 83);
        Log.i(TAG, "宽:" + size);
        if (size > 0) return size;
        return 800;
    }

    public static int getHeight(byte[] data) {
        int size = getSize(data, 92);
        Log.i(TAG, "高:" + size);
        if (size > 0) return size;
        return 480;
    }

    private static int getSize(byte[] data, int startBit) {
        int spsIndex = -1;
        boolean pps = false;
        for (int i = 0; i < data.length; i++) {
            if (i + 4 < data.length) {
                if (data[i] == 0x00
                        && data[i + 1] == 0x00
                        && data[i + 2] == 0x00
                        && data[i + 3] == 0x01
                        && data[i + 4] == 0x67) {
                    spsIndex = i;
                    continue;
                }
                if (data[i] == 0x00
                        && data[i + 1] == 0x00
                        && data[i + 2] == 0x00
                        && data[i + 3] == 0x01
                        && data[i + 4] == 0x68) {
                    pps = true;
                    break;
                }
            }
        }

        if (spsIndex < 0 || !pps) return -1;

        int nZeroNum = 0;
        while (startBit < data.length * 8) {
            if ((data[startBit / 8] & (0x80 >> (startBit % 8))) != 0) {
                break;
            }
            nZeroNum++;
            startBit++;
        }
        startBit++;
        int dwRet = 0;
        for (int j = 0; j < nZeroNum; j++) {
            dwRet <<= 1;
            if ((data[startBit / 8] & (0x80 >> (startBit % 8))) != 0) {
                dwRet += 1;
            }
            startBit++;
        }
        int i1 = (1 << nZeroNum) - 1 + dwRet;
        return (i1 + 1) * 16;
    }
}
