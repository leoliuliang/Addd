//
// Created by 72127106 on 2021/9/17.
//

#ifndef X264LIVERTMP_JAVACALLHELPER_H
#define X264LIVERTMP_JAVACALLHELPER_H

#include "jni.h"

//标记线程，因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_jobj);
    ~JavaCallHelper();
    void postH264(char *data, int length, int thread = THREAD_MAIN);

public:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject  jobj;
    jmethodID jmid_postData;
};


#endif //X264LIVERTMP_JAVACALLHELPER_H
