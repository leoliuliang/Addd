#include <jni.h>
#include <string>
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"---->",__VA_ARGS__)

extern "C"{
#include  "librtmp/rtmp.h"
}

typedef  struct {
    RTMP *rtmp;
    int16_t sps_len;
    int8_t *sps;
//
    int16_t pps_len;
    int8_t *pps;
} Live;

Live *live = NULL;

//传递第一帧 ,找出sps和pps缓存到Live结构体里     00 00 00 01 67 64 00 28ACB402201E3CBCA41408681B4284D4  0000000168  EE 06 F2 C0
void prepareVideo(int8_t *data, int len, Live *live) {
    for (int i = 0; i < len; i++) {
//        防止越界
        if (i + 4 < len) {
            //判断分隔符
            if (data[i] == 0x00 && data[i + 1] == 0x00
                && data[i + 2] == 0x00
                && data[i + 3] == 0x01) {
                if (data[i + 4]  == 0x68) {
                    //反向推理，得到sps长度；编码68是pps，pps前面4位是分隔符，上面判断i+4是68，所以此时i-4就是前面的长度就是sps的长度
                    live->sps_len = i - 4;
//                    new一个数组，大小是sps_len, 用来放sps数据
                    live->sps = static_cast<int8_t *>(malloc(live->sps_len));
//                    sps解析出来了放到live.sps ，+4过滤掉第一个分隔符，rtmp协议不需要分隔符
                    memcpy(live->sps, data + 4, live->sps_len);

//                    解析pps ，总长度-（4 + sps长度）- 4 = pps_len
                    live->pps_len = len - (4 + live->sps_len) - 4;
//                    实例化PPS 的数组
                    live->pps = static_cast<int8_t *>(malloc(live->pps_len));
//                    rtmp  协议
                    memcpy(live->pps, data + 4 + live->sps_len + 4, live->pps_len);

                    LOGI("sps:%d pps:%d", live->sps_len, live->pps_len);
                    break;
                }
            }
        }
    }
}

//创建rtmp协议包，主要对sps与pps的协议封装
RTMPPacket *createVideoPackage(Live *live) {
    //第一帧sps pps 协议里的整体长度，16是rtmp协议规定的
    int body_size = 16 + live->sps_len + live->pps_len;
    //sps pps得packet
    RTMPPacket *packet = (RTMPPacket *)malloc(sizeof(RTMPPacket));
    //实列化rtmp数据包
    RTMPPacket_Alloc(packet,body_size);

    //下面组装rtmp协议，十六进制都是rtmp协议规定好的
    //开始组装 第一帧sps与pps的格式
    int i = 0;
    packet->m_body[i++] = 0x17;
    //AVC sequence header 设置为0x00
    packet->m_body[i++] = 0x00;
    //CompositionTime
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    //AVC sequence header
    packet->m_body[i++] = 0x01;//版本

    //sps格式
    packet->m_body[i++] = live->sps[1]; //profile：编码等级 如baseline、main、 high
    packet->m_body[i++] = live->sps[2]; //profile_compatibility 兼容性
    packet->m_body[i++] = live->sps[3]; //profile level
    packet->m_body[i++] = 0xFF;//NALU长度，已经给你规定好了
    packet->m_body[i++] = 0xE1; //sps个数

    //sps长度用2个字节表示的，
    //所以先取出 高八位
    packet->m_body[i++] = (live->sps_len >> 8) & 0xff;
    //再取低八位
    packet->m_body[i++] = live->sps_len & 0xff;
    //将sps数据放到body
    memcpy(&packet->m_body[i],live->sps,live->sps_len);
    i += live->sps_len;

    //开始 pps 拼接
    packet->m_body[i++] = 0x01; //pps number
    //pps length
    packet->m_body[i++] = (live->pps_len >> 8) & 0xff;
    packet->m_body[i++] = live->pps_len & 0xff;
    //拷贝pps内容
    memcpy(&packet->m_body[i], live->pps, live->pps_len);
    //sps 与 pps 拼接完成

    //给packet设置其他属性
    //视频类型
    packet->m_packetType =  RTMP_PACKET_TYPE_VIDEO;

    packet->m_nBodySize = body_size;
//    视频 04
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;

    return packet;
}

//关键帧 和 非关键帧的协议格式
RTMPPacket *createVideoPackage(int8_t *buf, int len, const long tms, Live *live) {
    buf += 4;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    //9个字节是数据前面需要加上的信息，rtmp协议规定好的
    int body_size = len + 9;
//初始化RTMP内部的body数组
    RTMPPacket_Alloc(packet, body_size);

    if (buf[0] == 0x65) {
        packet->m_body[0] = 0x17;
        LOGI("发送关键帧 data");
    } else{
        packet->m_body[0] = 0x27;
        LOGI("发送非关键帧 data");
    }
//    固定的大小
    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    //长度
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;

    //copy数据
    memcpy(&packet->m_body[9], buf, len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = tms;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;

    return packet;
}

int sendPacket(RTMPPacket *packet){
    int r = RTMP_SendPacket(live->rtmp,packet,1);
    RTMPPacket_Free(packet);
    free(packet);
    return r;
}

int sendVideo(int8_t *buf, int len, long tms) {
    int ret = 0;
    //第一帧sps+pps 缓存起来
    if (buf[4] == 0x67) {
        //缓存sps 和pps 到全局遍历 不需要推流
        if (live && (!live->pps || !live->sps)) {
            prepareVideo(buf, len, live);
        }
        return ret;
    }

    //第二帧 I帧
    if (buf[4] == 0x65){
        //i帧之前先发送sps pps
        RTMPPacket *packet = createVideoPackage(live);
        sendPacket(packet);
    }
    //再发送I帧
    RTMPPacket *packet2 = createVideoPackage(buf,len,tms,live);
    ret = sendPacket(packet2);

    return ret;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_mxkj_rtmpliving_rtmpbilibli_ScreenLive_sendData(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                         jint len, jlong tms) {
    int ret;
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    ret = sendVideo(data, len, tms);
    env->ReleaseByteArrayElements(data_, data, 0);
    return ret;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mxkj_rtmpliving_rtmpbilibli_ScreenLive_connect(JNIEnv *env, jobject thiz, jstring url_) {

    const char *url = env->GetStringUTFChars(url_, 0);
//    链接服务器失败， 重试几次
    int ret;
    do {
//        实例化
        live = (Live*)malloc(sizeof(Live));
        memset(live, 0, sizeof(Live));

        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        LOGI("connect %s", url);
        if (!(ret = RTMP_SetupURL(live->rtmp, (char*)url))) break;
        RTMP_EnableWrite(live->rtmp);
        LOGI("RTMP_Connect");
        if (!(ret = RTMP_Connect(live->rtmp, 0))) break;
        LOGI("RTMP_ConnectStream ");
        if (!(ret = RTMP_ConnectStream(live->rtmp, 0))) break;
        LOGI("connect success");
    } while (0);

    if (!ret && live) {
        free(live);
        live = nullptr;
    }
    env->ReleaseStringUTFChars(url_, url);
    return ret;
}
