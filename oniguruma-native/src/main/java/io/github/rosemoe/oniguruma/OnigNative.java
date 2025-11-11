/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.oniguruma;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Oniguruma native function bridge.
 * Use with carefulness. Dirty pointer may cause a direct crash of your app.
 *
 * @author Rosemoe
 */
public class OnigNative {


    static {
        System.loadLibrary("oniguruma-binding");
    }

    /**
     * Create a new OnigRegex
     *
     * @param pattern    Pattern string
     * @param ignoreCase Ignore case when matching
     * @return The pointer of newly created regex, or null if it fails
     */
    public static long newRegex(String pattern, boolean ignoreCase) {
        Objects.requireNonNull(pattern, "pattern can not be null");
        return newRegex(pattern.getBytes(StandardCharsets.UTF_8), ignoreCase);
    }

    /**
     * Create a new OnigRegex
     *
     * @param pattern    UTF-8 Bytes of the pattern string
     * @param ignoreCase Ignore case when matching
     * @return The pointer of newly created regex, or null if it fails
     */
    public static long newRegex(byte[] pattern, boolean ignoreCase) {
        Objects.requireNonNull(pattern, "pattern can not be null");
        return nCreateRegex(pattern, ignoreCase);
    }

    /**
     * Release a OnigRegex previously created by {@link #newRegex}
     *
     * @param nativePtr Native pointer. Passing null pointer will have no effect.
     */
    public static native void releaseRegex(long nativePtr);

    /**
     * Search using the given OnigRegex.
     *
     * @param nativePointer OnigRegex pointer from {@link #newRegex}
     * @param cacheKey  The cache key for the source string
     * @param str       String to be search in
     * @param start     Start position in the string (inclusive)
     * @param end       End position in the string (exclusive)
     * @return Ranges if the match is successful. Each range is represented as two integer start and end.
     * Null if the match failed.
     */
    public static int[] regexSearch(long nativePointer, long cacheKey, byte[] str, int start, int end) {
        Objects.requireNonNull(str, "string can not be null");
        if (start > end || start < 0 || end > str.length) {
            throw new IndexOutOfBoundsException("start:" + start + " end:" + end + " str.length:" + str.length);
        }
        return nRegexSearch(nativePointer, cacheKey, str, start, end);
    }


    /**
     * Search using the given OnigRegex list
     *
     * @param nativePointers OnigRegex pointers from {@link #newRegex}
     * @param cacheKey   The cache key for the source string
     * @param str        String to be search in
     * @param start      Start position in the string (inclusive)
     * @param end        End position in the string (exclusive)
     * @return Ranges if the match is successful. Each range is represented as two integer start and end.
     * Null if the match failed.
     */
    public static int[] regexSearchBatch(long[] nativePointers, long cacheKey, byte[] str, int start, int end) {
        if (nativePointers == null || str == null) {
            throw new NullPointerException("pointers or string is null");
        }
        if (start > end || start < 0 || end > str.length) {
            throw new IndexOutOfBoundsException("start:" + start + " end:" + end + " str.length:" + str.length);
        }
        return nRegexSearchBatch(nativePointers, cacheKey, str, start, end);
    }

    private static native long nCreateRegex(byte[] pattern, boolean ignoreCase);

    private static native int[] nRegexSearch(long nativePtr, long cacheKey, byte[] str, int start, int end);

    private static native int[] nRegexSearchBatch(long[] nativePtrs, long cacheKey, byte[] str, int start, int end);

}
