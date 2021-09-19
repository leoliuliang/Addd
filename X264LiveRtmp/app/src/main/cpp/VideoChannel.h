//
// Created by 72127106 on 2021/9/16.
//

#ifndef X264LIVERTMP_VIDEOCHANNEL_H
#define X264LIVERTMP_VIDEOCHANNEL_H

#include <jni.h>
#include "inttypes.h"
#include "JavaCallHelper.h"
#include <x264.h>
#include "librtmp/rtmp.h"


class VideoChannel {

    //定义一个回调方法，将编码层的数据packet回调给传输层
    typedef void (*VideoCallback)(RTMPPacket *packet);

public:
    VideoChannel();
    ~VideoChannel();
    //创建x264编码器
    void setVideoEncInfo(int width,int height,int fps,int bitrate);
    //真正开始编码一帧数据，通过这个函数将yuv传给x264进行编码后放入队列
    void encodeData(int8_t *data);

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int len, int pps_len);
    //发送帧 包含关键帧和非关键帧
    void sendFrame(int type, int payload, uint8_t *p_payload);

    void setVideoCallback(VideoCallback callback);

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;

    int ySize;
    int uvSize;

    //x264编码器
    x264_t *videoCodec = 0;

    //码流容器，等价于mediacodec的bytebuffer
    x264_picture_t  *pic_in = 0;

    VideoCallback callback;

public:
    //定义JavaCallHelper，最终需要将X264编码好的数据回调给java层
    JavaCallHelper *javaCallHelper;
};


#endif //X264LIVERTMP_VIDEOCHANNEL_H
