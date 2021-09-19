//
// Created by 72127106 on 2021/9/18.
//

#include "AudioChannel.h"
#include <cstring>
#include <malloc.h>

AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {

}

void AudioChannel::openCodec(int sampleRate, int channels) {
    unsigned long inputSamples;

    /**
     * 实例化faac编码器
     *
     unsigned long   nSampleRate,        // 采样率，单位是bps
     unsigned long   nChannels,          // 声道，1为单声道，2为双声道
     unsigned long   &nInputSamples,     // 传引用，得到每次调用编码时所应接收的原始数据长度
     unsigned long   &nMaxOutputBytes    // 传引用，得到每次调用编码时生成的AAC数据的最大长度
     */
    codec = faacEncOpen(sampleRate, channels, &inputSamples, &maxOutputBytes);

    //输入容器真正大小
    inputByteNum = inputSamples * 2;

    //实例化 输出的容器
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));

    //参数
    faacEncConfigurationPtr  configurationPtr = faacEncGetCurrentConfiguration(codec);

    configurationPtr->mpegVersion = MPEG4;//编码格式
    configurationPtr->aacObjectType = LOW;//编码等级
    configurationPtr->outputFormat = 0;//输出aac裸流数据
    configurationPtr->inputFormat = FAAC_INPUT_16BIT;//采样位数
    //将参数配置生效
    faacEncSetConfiguration(codec,configurationPtr);

}

void AudioChannel::encode(int32_t *data, int len) {
    //编码，将pcm数据编码成aac数据
    int bytelen = faacEncEncode(codec,data,len,outputBuffer,maxOutputBytes);

    if (bytelen > 0){
        RTMPPacket *packet = new RTMPPacket;
        //开始拼装packet数据
        RTMPPacket_Alloc(packet,bytelen +2);
        packet->m_body[0] = 0xAF;
        packet->m_body[1] = 0x01;

        memcpy(&packet->m_body[2], outputBuffer,bytelen);

        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = bytelen + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        callback(packet);

    }
}

RTMPPacket *AudioChannel::getAudioConfig() {

        // 视频帧的sps pps
        u_char *buf;
        u_long len;
        //头帧的内容   {0x12 0x08}
        faacEncGetDecoderSpecificInfo(codec, &buf, &len);
        //头帧的  rtmpdump  实时录制  实时给时间戳
        RTMPPacket *packet = new RTMPPacket;
        RTMPPacket_Alloc(packet, len + 2);

        packet->m_body[0] = 0xAF;
        packet->m_body[1] = 0x00;
        memcpy(&packet->m_body[2], buf, len);

        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = len + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        return packet;
}
