package com.mxkj.h264mediacodecdemo.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class ByteUtil {

    public static void mkdir(){
        File file = new File(Environment.getExternalStorageDirectory() + File.separator + Constant.filePath).getAbsoluteFile();
        if (!file.exists()){
            file.mkdir();
        }
    }

    /**
     * 以字符串的方式写入
     * */
    public static String writeContent(byte[] array,String fileName){
        mkdir();

        char[] HEX_CHAR_TABLE = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b:array){
            stringBuilder.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            stringBuilder.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        Log.i("--->","writeContent: "+stringBuilder.toString());
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(Environment.getExternalStorageDirectory()+ File.separator+Constant.filePath+File.separator+fileName,true);
            fileWriter.write(stringBuilder.toString());
            fileWriter.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (fileWriter != null){
                    fileWriter.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 以文件的方式写入
     * */
    public static void writeBytes(byte[] array,String fileName){
        mkdir();

        FileOutputStream writer = null;
        try {
            writer= new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + Constant.filePath + File.separator + fileName, true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (writer != null){
                    writer.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
