//
// Created by admin on 2018/6/12.
//
#include <jni.h>
#include "Wden.h"

extern "C"
{
JNIEXPORT void JNICALL
Java_com_example_admin_keygen_activity_SampleActivity_wden(JNIEnv *env, jobject instance, jint scale,
                                                           jint dbn, jstring srcfile_, jstring dstfile_) {

    const char *srcfile = env->GetStringUTFChars(srcfile_, 0);
    const char *dstfile = env->GetStringUTFChars(dstfile_, 0);

    // TODO
    Wden *wdenInstance = new Wden();
    wdenInstance->wden(scale,dbn,srcfile,dstfile);

    env->ReleaseStringUTFChars(srcfile_, srcfile);
    env->ReleaseStringUTFChars(dstfile_, dstfile);
}

JNIEXPORT void JNICALL
Java_com_example_admin_keygen_thread_KeyGenThread_wden(JNIEnv *env, jobject instance, jint scale,
                                                       jint dbn, jstring srcfile_, jstring dstfile_) {

    const char *srcfile = env->GetStringUTFChars(srcfile_, 0);
    const char *dstfile = env->GetStringUTFChars(dstfile_, 0);

    // TODO
    Wden *wdenInstance = new Wden();
    wdenInstance->wden(scale,dbn,srcfile,dstfile);

    env->ReleaseStringUTFChars(srcfile_, srcfile);
    env->ReleaseStringUTFChars(dstfile_, dstfile);
}

}
