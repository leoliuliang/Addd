

extern "C"{
#include "librtmp/rtmp.h"
}

#include <jni.h>
#include <string>
#include <pthread.h>
#include "VideoChannel.h"
#include "AudioChannel.h"
#include "safe_queue.h"
#include "log.h"
#include "JavaCallHelper.h"


AudioChannel *audioChannel = 0;
VideoChannel *videoChannel = 0;
int isStart = 0;
//记录子线程的对象
pthread_t  pid;
//阻塞式队列
SafeQueue<RTMPPacket> packets;

uint32_t start_time;
RTMP *rtmp = 0;

//推流标志位
int readyPushing = 0;

JavaCallHelper *helper = 0;

//拿到java虚拟机引用，native线程需要用到将数据callback给java层
JavaVM *javaVM = 0;

void callback(RTMPPacket *packet){
    if(packet){
        //队列阻塞太多数据就丢掉，防止OOM
        if (packets.size() > 50) {
            packets.clear();
        }
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        //packet不为空就放入队列
        packets.push(reinterpret_cast<RTMPPacket &&>(packet));
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved){
    javaVM = vm;
    LOGE("保存虚拟机的引用");
    return JNI_VERSION_1_4;
}

void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}

void *start(void *args) {
    char *url = static_cast<char *>(args);
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp创建失败");
            break;
        }
        RTMP_Init(rtmp);
        //设置超时时间 5s
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp设置地址失败:%s", url);
            break;
        }
        //开启输出模式
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, 0);
        if (!ret) {
            LOGE("rtmp连接地址失败:%s", url);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);

        LOGE("rtmp连接成功----------->:%s", url);
        if (!ret) {
            LOGE("rtmp连接流失败:%s", url);
            break;
        }

        //准备好了 可以开始推流了
        readyPushing = 1;
        //记录一个开始推流的时间
        start_time = RTMP_GetTime();
        packets.setWork(1);
        RTMPPacket *packet = 0;

        RTMPPacket *audioHeader =audioChannel->getAudioConfig();
        callback(audioHeader);
        //循环从队列取包 然后发送
        while (isStart) {
            packets.pop(reinterpret_cast<RTMPPacket &>(packet));
            if (!isStart) {
                break;
            }
            if (!packet) {
                continue;
            }
            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            //发送包 1:加入队列发送
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("发送数据失败");
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_x264livertmp_live_LivePusher_native_1init(JNIEnv *env, jobject thiz) {
    videoChannel = new VideoChannel;

    helper = new JavaCallHelper(javaVM,env,thiz);

    //设置回调
    videoChannel->setVideoCallback(callback);

    //进行绑定，让后在VideoChannel.cpp里就可以快乐的使用javaCallHelper了
    videoChannel->javaCallHelper = helper;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_x264livertmp_live_LivePusher_native_1setVideoEncInfo(JNIEnv *env, jobject thiz,
                                                                      jint width, jint height,
                                                                      jint fps, jint bitrate) {
    //实例化编码工具
    if (videoChannel){
        videoChannel->setVideoEncInfo(width,height,fps,bitrate);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_x264livertmp_live_LivePusher_native_1start(JNIEnv *env, jobject thiz,
                                                            jstring path_) {
    //链接rtmp服务器，需要在子线程
    if (isStart){
        //直播标志，如果已经开始直播就return
        return;
    }

    const char *path = env->GetStringUTFChars(path_,0);
    char *url = new char[strlen(path)+1];
    strcpy(url,path);

    isStart = 1;

    //开子线程链接b站服务器
    pthread_create(&pid,0,start,url);


    env->ReleaseStringUTFChars(path_,path);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_x264livertmp_live_LivePusher_native_1pushVideo(JNIEnv *env, jobject thiz,
                                                                jbyteArray data_) {
//    LOGE("!videoChannel: %d ,!readyPushing: %d",!videoChannel,!readyPushing);
    if(!videoChannel || !readyPushing){
        return;
    }


    jbyte *data = env->GetByteArrayElements(data_,NULL);
    videoChannel->encodeData(data);
    env->ReleaseByteArrayElements(data_,data,0);

}

extern "C" JNIEXPORT void JNICALL
Java_com_example_x264livertmp_live_LivePusher_native_1release(JNIEnv *env, jobject thiz) {

    if(rtmp){
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    if(videoChannel){
        delete (videoChannel);
        videoChannel  =0;
    }
    if(helper){
        delete (helper);
        helper = 0;
    }
}

extern "C" JNIEXPORT jint JNICALL Java_com_example_x264livertmp_live_LivePusher_initAudioEnc(JNIEnv *env, jobject thiz,
                                                           jint sample_rate, jint channels) {
    //初始化音频faac编码器，
    audioChannel = new AudioChannel;

    audioChannel->setCallback(callback);
    audioChannel->openCodec(sample_rate, channels);

    return audioChannel->getInputByteNum();

}
extern "C" JNIEXPORT void JNICALL
Java_com_example_x264livertmp_live_LivePusher_nativeSendAudio(JNIEnv *env, jobject thiz,
                                                              jbyteArray buffer, jint len) {

    LOGE("!videoChannel: %d ,!readyPushing: %d",!videoChannel,!readyPushing);
    if (!audioChannel || !readyPushing) {
        return;
    }
    //C层的字节数组
    jbyte *data=env->GetByteArrayElements(buffer, 0);

    //data为原始数据pcm数据
    audioChannel->encode(reinterpret_cast<int32_t *>(data), len);
    //释放掉
    env->ReleaseByteArrayElements(buffer, data, 0);
}