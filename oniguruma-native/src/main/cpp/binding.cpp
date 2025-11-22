//
// Created by Rosemoe on 2025/11/8.
//

#include "oniguruma.h"
#include "jni.h"
#include <string>
#include <mutex>

OnigErrorInfo lastOnigError;

struct OnigCachedResult {
    jlong cacheKey{};
    int lastSearchPosition{};
    OnigRegion *region{};
};

struct OnigCachedRegex {
    bool hasGAnchor{};
    OnigRegex regex{};
    OnigCachedResult lastResult{};
    std::mutex cacheLock{};
};

bool checkGAnchor(const unsigned char *data, jsize length) {
    for (int i = 0; i < length - 1; i++) {
        if (data[i] == '\\' && data[i + 1] == 'G') {
            return true;
        }
    }
    return false;
}

void updateCache(OnigCachedRegex *regex, OnigCachedResult result) {
    if (regex->hasGAnchor) {
        return;
    }
    std::lock_guard<std::mutex> _{regex->cacheLock};
    if (regex->lastResult.region != nullptr) {
        onig_region_free(regex->lastResult.region, 1);
        regex->lastResult.region = nullptr;
    }
    regex->lastResult = result;
}

bool
searchCached(OnigCachedRegex *regex, jlong cacheKey, const unsigned char *data, int start, int end,
             OnigRegion *outRegion) {
    // First check if the cache is suitable
    if (cacheKey != 0 && !regex->hasGAnchor && regex->lastResult.cacheKey == cacheKey) {
        std::lock_guard<std::mutex> _{regex->cacheLock};
        auto &lastResult = regex->lastResult;
        if (lastResult.cacheKey == cacheKey && lastResult.lastSearchPosition <= start) {
            // Last search result is no match
            if (lastResult.region == nullptr) {
                return false;
            }
            // Suitable non-empty last match
            if (lastResult.region->num_regs > 0 &&
                    std::max(0, lastResult.region->beg[0]) >= start &&
                    std::max(0, lastResult.region->end[0]) <= end) {
                onig_region_copy(outRegion, lastResult.region);
                return true;
            }
        }
    }

    // Do the search
    OnigRegion *region = onig_region_new();
    int status = onig_search(regex->regex, data, data + end, data + start, data + end, region,
                             ONIG_OPTION_DEFAULT);

    // no match or error
    if (status == ONIG_MISMATCH || status < 0) {
        onig_region_free(region, 1);
        updateCache(regex, {cacheKey, start, nullptr});
        return false;
    }

    updateCache(regex, {cacheKey, start, region});
    onig_region_copy(outRegion, region);
    return true;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_rosemoe_oniguruma_OnigNative_nCreateRegex(JNIEnv *env, jclass clazz,
                                                         jbyteArray pattern,
                                                     jboolean ignore_case) {
    auto size = env->GetArrayLength(pattern);
    auto content = env->GetByteArrayElements(pattern, nullptr);
    if (content == nullptr) {
        return 0;
    }

    OnigRegex regex;
    auto buffer = reinterpret_cast<unsigned char *>(content);
    bool hasGAnchor = checkGAnchor(buffer, size);
    OnigOptionType type = ignore_case ? ONIG_OPTION_IGNORECASE : 0;
    type |= ONIG_OPTION_CAPTURE_GROUP;

    auto result = onig_new(&regex, buffer, buffer + size, type, ONIG_ENCODING_UTF8,
                           ONIG_SYNTAX_DEFAULT, &lastOnigError);
    env->ReleaseByteArrayElements(pattern, content, JNI_ABORT);

    if (result != ONIG_NORMAL) {
        return 0;
    }

    auto cachedRegex = new OnigCachedRegex{hasGAnchor, regex};
    return reinterpret_cast<jlong>(cachedRegex);
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_io_github_rosemoe_oniguruma_OnigNative_nRegexSearch(JNIEnv *env, jclass clazz,
                                                         jlong native_ptr,
                                                        jlong cache_key, jbyteArray str, jint start,
                                                        jint end) {
    auto regex = reinterpret_cast<OnigCachedRegex *>(native_ptr);

    auto content = env->GetByteArrayElements(str, nullptr);
    if (content == nullptr) {
        return nullptr;
    }

    OnigRegion *region = onig_region_new();

    auto buffer = reinterpret_cast<unsigned char *>(content);
    bool success = searchCached(regex, cache_key, buffer, start, end, region);
    env->ReleaseByteArrayElements(str, content, JNI_ABORT);

    if (!success) {
        onig_region_free(region, 1);
        return nullptr;
    }

    auto result = env->NewIntArray(region->num_regs * 2);
    auto indices = env->GetIntArrayElements(result, nullptr);
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
        auto regex = reinterpret_cast<OnigCachedRegex *>(native_ptr);
        // Delete any old search cache
        updateCache(regex, {0, 0, nullptr});
        onig_free(regex->regex);
        delete regex;
    }
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_io_github_rosemoe_oniguruma_OnigNative_nRegexSearchBatch(JNIEnv *env, jclass clazz,
                                                             jlongArray native_ptrs,
                                                             jlong cache_key, jbyteArray str,
                                                             jint start, jint end) {
    auto nativePtrs = env->GetLongArrayElements(native_ptrs, nullptr);
    if (nativePtrs == nullptr) {
        return nullptr;
    }

    auto strData = env->GetByteArrayElements(str, nullptr);
    if (strData == nullptr) {
        env->ReleaseLongArrayElements(native_ptrs, nativePtrs, JNI_ABORT);
        return nullptr;
    }
    auto buffer = reinterpret_cast<unsigned char *>(strData);

    OnigRegion *region = onig_region_new();
    OnigRegion *resultRegion = onig_region_new();

    auto regexCount = env->GetArrayLength(native_ptrs);
    int bestIndex = -1;
    int bestLocation = -1;
    for (int i = 0; i < regexCount; i++) {
        auto regex = reinterpret_cast<OnigCachedRegex *>(nativePtrs[i]);

        onig_region_clear(region);
        bool success = searchCached(regex, cache_key, buffer, start, end, region);

        if (success && region->num_regs > 0) {
            // matched here
            int location = std::max(0, region->beg[0]);

            if (bestIndex == -1 || location < bestLocation) {
                bestIndex = i;
                bestLocation = location;
                std::swap(region, resultRegion);
            }

            if (location == start) {
                break;
            }
        }
    }

    env->ReleaseLongArrayElements(native_ptrs, nativePtrs, JNI_ABORT);
    env->ReleaseByteArrayElements(str, strData, JNI_ABORT);
    onig_region_free(region, 1);

    if (bestIndex == -1) {
        onig_region_free(resultRegion, 1);
        return nullptr;
    }

    auto result = env->NewIntArray(resultRegion->num_regs * 2 + 1);
    auto indices = env->GetIntArrayElements(result, nullptr);
    for (int i = 0; i < resultRegion->num_regs; i++) {
        indices[i * 2] = resultRegion->beg[i];
        indices[i * 2 + 1] = resultRegion->end[i];
    }
    indices[resultRegion->num_regs * 2] = bestIndex;
    onig_region_free(resultRegion, 1);
    env->ReleaseIntArrayElements(result, indices, JNI_COMMIT);
    return result;
}
