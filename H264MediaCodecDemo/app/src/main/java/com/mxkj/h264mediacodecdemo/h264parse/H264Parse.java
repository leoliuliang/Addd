package com.mxkj.h264mediacodecdemo.h264parse;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 手写解析h264，从sps里解析出宽高
* */
public class H264Parse {
    private  String path;

    public H264Parse(String path) {
        this.path = path;
    }

    public String startCodec() {
        byte[] bytes = null;
        try {
            bytes = getBytes(path);
        } catch ( Exception e) {
            e.printStackTrace();
        }
        int totalSize = bytes.length;
        int startIndex = 0;
        while (true) {
            if (totalSize == 0 ||startIndex >= totalSize) {
                break;
            }
            int nextFrameStart = findByFrame(  bytes, startIndex+2 , totalSize);
            byte[] h264 = spliteByte(bytes, startIndex, nextFrameStart - startIndex);
            nStartBit = 4*8;
            int forbidden_zero_bit=u(1, h264);
            int nal_ref_idc       =u(2, h264);
            int nal_unit_type     =u(5, h264);
            switch (nal_unit_type) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 7:
//                    进入sps解码过程
                    return parseSps(h264);

                case 8:
                    break;
            }
            break;
        }

        return null;
    }

    private String parseSps(byte[] h264) {

//            编码等级   Baseline Main Extended High   High 10   High 4:2:2
        int profile_idc = u(8, h264);
//            当constrained_set0_flag值为1的时候，就说明码流应该遵循基线profile(Baseline profile)的所有约束.constrained_set0_flag值为0时，说明码流不一定要遵循基线profile的所有约束。
        int constraint_set0_flag = u(1, h264);//(h264[1] & 0x80)>>7;
        //            当constrained_set1_flag值为1的时候，就说明码流应该遵循主profile(Main profile)的所有约束.constrained_set1_flag值为0时，说明码流不一定要遵
        int constraint_set1_flag = u(1, h264);//(h264[1] & 0x40)>>6;
        //当constrained_set2_flag值为1的时候，就说明码流应该遵循扩展profile(Extended profile)的所有约束.constrained_set2_flag值为0时，说明码流不一定要遵循扩展profile的所有约束。
        int constraint_set2_flag = u(1, h264);//(h264[1] & 0x20)>>5;
//            注意：当constraint_set0_flag,constraint_set1_flag或constraint_set2_flag中不只一个值为1的话，那么码流必须满足所有相应指明的profile约束。
        int constraint_set3_flag = u(1, h264);//(h264[1] & 0x10)>>4;
//            4个零位
        int reserved_zero_4bits = u(4, h264);
//            它指的是码流对应的level级
        int level_idc = u(8, h264);
//            是否是哥伦布编码  0 是 1 不是
        int seq_parameter_set_id = Ue(h264);
        if (profile_idc == 100) {
//                颜色位数
            int chroma_format_idc=Ue(h264);
            int bit_depth_luma_minus8   =Ue(h264);
            int bit_depth_chroma_minus8  =Ue(h264);
            int qpprime_y_zero_transform_bypass_flag=u(1, h264);
            int seq_scaling_matrix_present_flag     =u(1, h264);


        }
        int log2_max_frame_num_minus4=Ue(h264);

        int pic_order_cnt_type       =Ue(h264);

        {
            int log2_max_pic_order_cnt_lsb_minus4=Ue(h264);
        }
        int num_ref_frames                      =Ue(h264);
        int gaps_in_frame_num_value_allowed_flag=u(1,     h264);
        int pic_width_in_mbs_minus1             =Ue(h264);
        int pic_height_in_map_units_minus1      =Ue(h264);
        int width=(pic_width_in_mbs_minus1       +1)*16;
        int height=(pic_height_in_map_units_minus1+1)*16;
        Log.i("---->", "width: "+width+"  height: "+height);
        return width+" * "+height;
    }

    public byte[] spliteByte(byte[] array,int start,int lenght) {
        byte[] newArray = new byte[lenght];
        for (int i = start; i < start + lenght; i++) {
            newArray[i - start] = array[i];
        }
        return newArray;
    }


    private int findByFrame( byte[] bytes, int start, int totalSize) {
        int j = 0;  // Number of chars matched in pattern
        for (int i = start; i < totalSize; i++) {
            if ((bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x00
                    && bytes[i + 3] == 0x01)||(bytes[i] == 0x00 && bytes[i + 1] == 0x00
                    && bytes[i + 2] == 0x01)) {
                return i;
            }
        }
        return -1;  // Not found
    }
    public   byte[] getBytes(String path) throws IOException {
        InputStream is =   new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024*1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        len=is.read(buf, 0, size);
         bos.write(buf, 0, len);
        buf = bos.toByteArray();
        return buf;
    }


    private static int u(int bitIndex, byte[] h264)
    {
        int dwRet = 0;
        for (int i=0; i<bitIndex; i++)
        {
            dwRet <<= 1;
            if ((h264[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                dwRet += 1;
            }
            nStartBit++;
        }
        return dwRet;
    }

    private static int nStartBit = 0;


    private static int Ue(byte[] pBuff)
    {
        int nZeroNum = 0;
        while (nStartBit < pBuff.length * 8)
        {
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                break;
            }
            nZeroNum++;
            nStartBit++;
        }
        nStartBit ++;

        int dwRet = 0;
        for (int i=0; i<nZeroNum; i++)
        {
            dwRet <<= 1;
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                dwRet += 1;
            }
            nStartBit++;
        }
        return (1 << nZeroNum) - 1 + dwRet;
    }

}
