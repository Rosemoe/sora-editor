//
// Created by Rosemoe on 2025/11/8.
//

#include "oniguruma.h"
#include "jni.h"
#include <string>

OnigErrorInfo lastOnigError;

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_rosemoe_oniguruma_OnigNative_newRegex(JNIEnv *env, jclass clazz, jbyteArray pattern,
                                                     jboolean ignore_case) {
    jboolean isCopy;
    auto size = env->GetArrayLength(pattern);
    auto content = env->GetByteArrayElements(pattern, &isCopy);
    if (content == nullptr) {
        return 0;
    }

    OnigRegex regex;
    auto buffer = reinterpret_cast<unsigned char *>(content);
    OnigOptionType type = ignore_case ? ONIG_OPTION_IGNORECASE : 0;
    type |= ONIG_OPTION_CAPTURE_GROUP;

    auto result = onig_new(&regex, buffer, buffer + size, type, ONIG_ENCODING_UTF8,
                           ONIG_SYNTAX_DEFAULT, &lastOnigError);
    env->ReleaseByteArrayElements(pattern, content, JNI_ABORT);

    if (result != ONIG_NORMAL) {
        return 0;
    }
    return reinterpret_cast<jlong>(regex);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_io_github_rosemoe_oniguruma_OnigNative_regexSearch(JNIEnv *env, jclass clazz, jlong native_ptr,
                                                        jbyteArray str, jint start, jint end) {
    auto regex = reinterpret_cast<OnigRegex>(native_ptr);

    jboolean isCopy;
    auto content = env->GetByteArrayElements(str, &isCopy);
    if (content == nullptr) {
        return 0;
    }

    OnigRegion *region = onig_region_new();

    auto buffer = reinterpret_cast<unsigned char *>(content);
    int status = onig_search(regex, buffer, buffer + end, buffer + start, buffer + end, region,
                             ONIG_OPTION_DEFAULT);
    env->ReleaseByteArrayElements(str, content, JNI_ABORT);

    if (status == ONIG_MISMATCH || status < 0) {
        onig_region_free(region, 1);
        return nullptr;
    }

    auto result = env->NewIntArray(region->num_regs * 2);
    auto indices = env->GetIntArrayElements(result, &isCopy);
    for (int i = 0; i < region->num_regs; i++) {
        indices[i * 2] = region->beg[i];
        indices[i * 2 + 1] = region->end[i];
    }
    onig_region_free(region, 1);
    env->ReleaseIntArrayElements(result, indices, JNI_COMMIT);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_rosemoe_oniguruma_OnigNative_releaseRegex(JNIEnv *env, jclass clazz,
                                                         jlong native_ptr) {
    if (native_ptr != 0L) {
        auto regex = reinterpret_cast<OnigRegex>(native_ptr);
        onig_free(regex);
    }
}