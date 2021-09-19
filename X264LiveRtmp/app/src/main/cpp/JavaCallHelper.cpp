//
// Created by 72127106 on 2021/9/17.
//

#include "JavaCallHelper.h"

//回调java层方法
JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj): javaVM(_javaVM),env(_env) {
    //将_jobj 保存为全局的成员变量
    jobj =  _env->NewGlobalRef(_jobj);
    //反射java方法
    jclass jclazz = env->GetObjectClass(jobj);
    jmid_postData = env->GetMethodID(jclazz,"postData","([B)V");
}

void JavaCallHelper::postH264(char *data, int length, int thread) {
    jbyteArray array = env->NewByteArray(length);
    env->SetByteArrayRegion(array, 0, length, reinterpret_cast<const jbyte *>(data));

    if (thread == THREAD_CHILD){
        JNIEnv *jniEnv;
        if (javaVM->AttachCurrentThread(&jniEnv, 0) != JNI_OK){
            return;
        }
        jniEnv->CallVoidMethod(jobj,jmid_postData,array);
        javaVM->DetachCurrentThread();
    } else{
        env->CallVoidMethod(jobj,jmid_postData,array);
    }
}

JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(jobj);
    jobj = 0;

}

