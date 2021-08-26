#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" JNIEXPORT void JNICALL
Java_com_mxkj_h264mediacodecdemo_MainActivity_nativeTojava(JNIEnv* env,jobject this_ ) {
    //native反射，回调java方法
    jclass activityClass = env->GetObjectClass(this_);
    jmethodID jmethodId = env->GetMethodID(activityClass,"callBack","(I)V");
    env->CallVoidMethod(this_,jmethodId,555);
}

jstring dynamicRegister1(JNIEnv* env,jobject this_){
    std::string hello = "实现了动态注册";
    return env->NewStringUTF(hello.c_str());
}

static const JNINativeMethod gMethods[] = {
        {
            "dynamicRegister1",
            "()Ljava/lang/String;",
                (jstring*)dynamicRegister1
        }
};

//实现动态注册
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *unused) {
    __android_log_print(ANDROID_LOG_INFO,"native","Jni_OnLoad");

    JNIEnv *env = NULL;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK){
        return -1;
    }
    jclass clazz = env->FindClass("com/mxkj/h264mediacodecdemo/MainActivity");
    //sizeof(gMethods)/sizeof(gMethods[0]) 动态注册的个数
    env->RegisterNatives(clazz,gMethods,sizeof(gMethods)/sizeof(gMethods[0]));

    return JNI_VERSION_1_4;
}