//
// Created by 72127106 on 2021/9/16.
//

#include "VideoChannel.h"
#include "x264/armeabi-v7a/include/x264.h"
#include <cstring>
#include "log.h"

VideoChannel::VideoChannel() {

}


void VideoChannel::setVideoEncInfo(int width, int height, int fps, int bitrate) {
    //实例化x264
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;

    ySize = width * height;
    uvSize = ySize / 4;

    if (videoCodec){
        //如果之前已经创建了x264编码器，先关闭
        x264_encoder_close(videoCodec);
        videoCodec = 0;
    }

    //定义参数，x264_param_t等价于mediaformat
    x264_param_t param;

    //参数赋值

    x264_param_default_preset(&param,"ultrafast","zerolatency");//编码质量

    param.i_level_idc = 32;//编码等级

    param.i_csp = X264_CSP_I420;//显示格式，I420

    param.i_width = width;
    param.i_height = height;

    param.i_bframe = 0;//B帧

    param.rc.i_rc_method = X264_RC_ABR;//设置码率算法, X264_RC_ABR取折中

    param.rc.i_bitrate = bitrate / 1024;//设置码率

    //x264存fps为了最节省内存，直接存时间肯定不是最省内存的，x264 通过分子分母来设置帧率fps，每一帧间隔时间(fps)=1s(分子)/帧率(分母), 这样可做到完美储存帧率
    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.i_timebase_den = param.i_fps_num;//分母
    param.i_timebase_num = param.i_fps_den;//分子

    param.b_vfr_input = 0;//用fps而不是时间戳来计算帧间距离

    param.i_keyint_max = fps * 2;//I帧间隔

    param.b_repeat_headers = 1;//是否复制sps和pps放到每个关键帧的前面，该参数设置是让每个关键帧（I帧）都附带sps和pps.

    param.i_threads = 1;//多线程

    x264_param_apply_profile(&param,"baseline");//编码等级

    videoCodec = x264_encoder_open(&param);//打开编码器

    pic_in = new x264_picture_t;//实例化容器
    x264_picture_alloc(pic_in,X264_CSP_I420,width,height);//设置容器大小
}

void VideoChannel::encodeData(int8_t *data) {
    //将yuv数据放入x264_picture_t容器，
    memcpy(pic_in->img.plane[0],data,ySize);//y的数据放入容器
    for(int i =0; i < uvSize; ++i){
        //间隔一个字节去一个数据
        //v的数据
        *(pic_in->img.plane[1]+i) = *(data + ySize + i * 2 + 1);
        //u的数据
        *(pic_in->img.plane[2]+i) = *(data + ySize + i * 2);
    }

    //编码出输出在pp_nal里的NALU单元个数，NALU（两个I帧之间的数据）
    int pi_nal;
    //编码出的数据, h264
    x264_nal_t *pp_nals;
    //编码出的参数 ,等价于Mediacodec 的 bufferInfo
    x264_picture_t  pic_out;

    LOGE("videoCodec: %d ",videoCodec);

    x264_encoder_encode(videoCodec, &pp_nals, &pi_nal, pic_in, &pic_out); //编码成h264

    uint8_t  sps[100];
    uint8_t  pps[100];
    int sps_len, pps_len;
//    LOGE("编码出的帧数 %d ",pi_nal);
    if (pi_nal > 0){
        for (int i = 0; i < pi_nal; ++i) {
//            LOGE("输出索引：%d  输出长度：%d ", i , pi_nal);
//            javaCallHelper->postH264(reinterpret_cast<char *>(pp_nals[i].p_payload), pp_nals[i].i_payload);//回调h264数据给java

            //sps 和 pps一起发送出去
            if (pp_nals[i].i_type == NAL_SPS){
                sps_len = pp_nals[i].i_payload - 4;
                memcpy(sps,pp_nals[i].p_payload+4, sps_len);
            } else if(pp_nals[i].i_type == NAL_PPS){
                pps_len = pp_nals[i].i_payload - 4;
                memcpy(pps,pp_nals[i].p_payload+4, pps_len);
                sendSpsPps(sps,pps,sps_len,pps_len);
            }else{
                sendFrame(pp_nals[i].i_type, pp_nals[i].i_payload,pp_nals[i].p_payload);
            }
        }
    }

    return;
}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {

    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 13 + sps_len + 3 + pps_len;
    RTMPPacket_Alloc(packet, bodysize);
    int i = 0;
    //固定头
    packet->m_body[i++] = 0x17;
    //类型
    packet->m_body[i++] = 0x00;
    //composition time 0x000000
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    //版本
    packet->m_body[i++] = 0x01;
    //编码规格
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;

    //整个sps
    packet->m_body[i++] = 0xE1;
    //sps长度
    packet->m_body[i++] = (sps_len >> 8) & 0xff;
    packet->m_body[i++] = sps_len & 0xff;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    //pps
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = (pps_len >> 8) & 0xff;
    packet->m_body[i++] = (pps_len) & 0xff;
    memcpy(&packet->m_body[i], pps, pps_len);


    //视频
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodysize;
    //随意分配一个管道（尽量避开rtmp.c中使用的）
    packet->m_nChannel = 10;
    //sps pps没有时间戳
    packet->m_nTimeStamp = 0;
    //不使用绝对时间
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    //回调给传输层 native-lib的callback就有数据了
    if (this->callback) {
        this->callback(packet);
    }
}

void VideoChannel::setVideoCallback(VideoChannel::VideoCallback callback) {
    this->callback = callback;
}

void VideoChannel::sendFrame(int type, int payload, uint8_t *p_payload) {
    //去掉 00 00 00 01 / 00 00 01
    if (p_payload[2] == 0x00){
        payload -= 4;
        p_payload += 4;
    } else if(p_payload[2] == 0x01){
        payload -= 3;
        p_payload += 3;
    }
    RTMPPacket *packet = new RTMPPacket;
    int bodysize = 9 + payload;
    RTMPPacket_Alloc(packet, bodysize);
    RTMPPacket_Reset(packet);
//    int type = payload[0] & 0x1f;
    packet->m_body[0] = 0x27;
    //关键帧
    if (type == NAL_SLICE_IDR) {
        LOGE("关键帧");
        packet->m_body[0] = 0x17;
    }
    //类型
    packet->m_body[1] = 0x01;
    //时间戳
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    //数据长度 int 4个字节 相当于把int转成4个字节的byte数组
    packet->m_body[5] = (payload >> 24) & 0xff;
    packet->m_body[6] = (payload >> 16) & 0xff;
    packet->m_body[7] = (payload >> 8) & 0xff;
    packet->m_body[8] = (payload) & 0xff;

    //图片数据
    memcpy(&packet->m_body[9],p_payload,  payload);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodysize;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel = 0x10;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    callback(packet);
}

VideoChannel::~VideoChannel() {
  if (videoCodec){
      x264_encoder_close(videoCodec);
      videoCodec = 0 ;
  }
}


















