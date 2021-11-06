/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
 *    Copyright (C) 2020-2021  Rosemoe
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
package io.github.rosemoe.sora.graphics;

import android.graphics.Paint;

import java.util.Arrays;

import io.github.rosemoe.sora.text.TextUtils;

/**
 * Cache to measure text quickly,which is very useful when text is long
 * <p>
 * It is not thread-safe.
 *
 * @author Rosemoe
 */
public class FontCache {

    private final float[] cache;
    public final float[] widths;
    public final char[] buffer;

    public FontCache() {
        cache = new float[65536];
        buffer = new char[5];
        widths = new float[10];
    }

    /**
     * Clear caches of font
     */
    public void clearCache() {
        Arrays.fill(cache, 0);
    }

    /**
     * Measure a single character
     */
    public float measureChar(char ch, Paint p) {
        float width = cache[ch];
        if (width == 0) {
            buffer[0] = ch;
            width = p.measureText(buffer, 0, 1);
            cache[ch] = width;
        }
        return width;
    }

    /*
     * Measure text
     */
    public float measureText(char[] chars, int start, int end, Paint p) {
        float width = 0f;
        for (int i = start; i < end; i++) {
            char ch = chars[i];
            if (TextUtils.isEmoji(ch)) {
                if (i + 4 <= end) {
                    p.getTextWidths(chars, i, 4, widths);
                    if (widths[0] > 0 && widths[1] == 0 && widths[2] == 0 && widths[3] == 0) {
                        i += 3;
                        width += widths[0];
                        continue;
                    }
                }
                int commitEnd = Math.min(end, i + 2);
                int len = commitEnd - i;
                if (len >= 0) {
                    System.arraycopy(chars, i, buffer, 0, len);
                }
                width += p.measureText(buffer, 0, len);
                i += len - 1;
            } else {
                width += measureChar(ch, p);
            }
        }
        return width;
    }

    /**
     * Measure text
     */
    public float measureText(CharSequence str, int start, int end, Paint p) {
        float width = 0f;
        for (int i = start; i < end; i++) {
            char ch = str.charAt(i);
            if (TextUtils.isEmoji(ch)) {
                if (i + 4 <= end) {
                    p.getTextWidths(str, i, i + 4, widths);
                    if (widths[0] > 0 && widths[1] == 0 && widths[2] == 0 && widths[3] == 0) {
                        i += 3;
                        width += widths[0];
                        continue;
                    }
                }
                int commitEnd = Math.min(end, i + 2);
                int len = commitEnd - i;
                for (int j = 0; j < len; j++) {
                    buffer[j] = str.charAt(i + j);
                }
                width += p.measureText(buffer, 0, len);
                i += len - 1;
            } else {
                width += measureChar(ch, p);
            }
        }
        return width;
    }

}
