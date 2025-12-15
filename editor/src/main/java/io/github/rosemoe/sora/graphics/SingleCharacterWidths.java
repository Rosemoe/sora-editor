/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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

import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.util.Arrays;

import io.github.rosemoe.sora.text.CharArrayWrapper;
import io.github.rosemoe.sora.text.FunctionCharacters;

public class SingleCharacterWidths {

    /**
     * Floating-point precision steps.
     * <p>
     * Introduced to avoid accumulated floating-point errors.
     */
    private final static long PRECISION = 1000L;
    public final float[] widths;
    public final SparseArray<Float> codePointWidths;
    public final char[] buffer;
    private final float[] cache;
    private final int tabWidth;
    private boolean handleFunctionCharacters;

    public SingleCharacterWidths(int tabWidth) {
        cache = new float[65536];
        buffer = new char[10];
        widths = new float[10];
        codePointWidths = new SparseArray<>();
        this.tabWidth = tabWidth;
    }

    public void setHandleFunctionCharacters(boolean handleFunctionCharacters) {
        this.handleFunctionCharacters = handleFunctionCharacters;
    }

    public boolean isHandleFunctionCharacters() {
        return handleFunctionCharacters;
    }

    public static boolean isEmoji(char ch) {
        return ch == 0xd83c || ch == 0xd83d || ch == 0xd83e;
    }

    /**
     * Clear caches of font
     */
    public void clearCache() {
        Arrays.fill(cache, 0);
        codePointWidths.clear();
    }

    /**
     * Measure a single character
     */
    public float measureChar(char ch, @NonNull Paint p) {
        var rate = 1;
        if (ch == '\t') {
            ch = ' ';
            rate = tabWidth;
        }
        float width = cache[ch];
        if (width == 0) {
            buffer[0] = ch;
            width = p.measureText(buffer, 0, 1);
            cache[ch] = width;
        }
        return width * rate;
    }

    /**
     * Measure a single character
     * @param cp Code Point
     */
    public float measureCodePoint(int cp, @NonNull Paint p) {
        if (cp <= 65535) {
            return measureChar((char) cp, p);
        }
        var width = codePointWidths.get(cp);
        if (width == null) {
            var count = Character.toChars(cp, buffer, 0);
            width = p.measureText(buffer, 0, count);
            codePointWidths.put(cp, width);
        }
        return width;
    }

    /*
     * Measure text
     */
    public float measureText(char[] chars, int start, int end, @NonNull Paint p) {
        return measureText(new CharArrayWrapper(chars, chars.length), start, end, p);
    }

    public float measureText(@NonNull CharSequence str, @NonNull Paint p) {
        return measureText(str, 0, str.length(), p);
    }

    /**
     * Measure text
     */
    public float measureText(@NonNull CharSequence str, int start, int end, @NonNull Paint p) {
        long width = 0;
        for (int i = start; i < end; i++) {
            char ch = str.charAt(i);
            if (isEmoji(ch)) {
                if (i + 4 <= end) {
                    p.getTextWidths(str, i, i + 4, widths);
                    if (widths[0] > 0 && widths[1] == 0 && widths[2] == 0 && widths[3] == 0) {
                        i += 3;
                        width += (long) Math.ceil(widths[0] * PRECISION);
                        continue;
                    }
                }
                int commitEnd = Math.min(end, i + 2);
                int len = commitEnd - i;
                for (int j = 0; j < len; j++) {
                    buffer[j] = str.charAt(i + j);
                }
                width += (long) Math.ceil(p.measureText(buffer, 0, len) * PRECISION);
                i += len - 1;
            } else if(isHandleFunctionCharacters() && FunctionCharacters.isEditorFunctionChar(ch)) {
                var name = FunctionCharacters.getNameForFunctionCharacter(ch);
                for (int j = 0;j < name.length();j++) {
                    width += (long) Math.ceil(measureChar(name.charAt(j), p) * PRECISION);
                }
            } else {
                width += (long) Math.ceil(measureChar(ch, p) * PRECISION);
            }
        }
        return (float) width / PRECISION;
    }

}
