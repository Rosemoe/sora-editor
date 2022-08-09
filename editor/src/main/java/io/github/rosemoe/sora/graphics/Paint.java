/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;

import io.github.rosemoe.sora.text.CharArrayWrapper;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.UnicodeIterator;
import io.github.rosemoe.sora.util.TemporaryCharBuffer;

public class Paint extends android.graphics.Paint {

    private float spaceWidth;
    private float tabWidth;

    private SingleCharacterWidths widths;

    public Paint() {
        super();
        widths = new SingleCharacterWidths(1);
        spaceWidth = measureText(" ");
        tabWidth = measureText("\t");
    }

    public void onAttributeUpdate() {
        spaceWidth = measureText(" ");
        tabWidth = measureText("\t");
        widths.clearCache();
    }

    public float getSpaceWidth() {
        return spaceWidth;
    }

    public void setTypefaceWrapped(Typeface typeface) {
        super.setTypeface(typeface);
        onAttributeUpdate();
    }

    public void setTextSizeWrapped(float textSize) {
        super.setTextSize(textSize);
        onAttributeUpdate();
    }

    public void setFontFeatureSettingsWrapped(String settings) {
        super.setFontFeatureSettings(settings);
        onAttributeUpdate();
    }

    @Override
    public void setLetterSpacing(float letterSpacing) {
        super.setLetterSpacing(letterSpacing);
        onAttributeUpdate();
    }

    @SuppressLint("NewApi")
    public float myGetTextRunAdvances(@NonNull char[] chars, int index, int count, int contextIndex, int contextCount, boolean isRtl, @Nullable float[] advances, int advancesIndex, boolean fast) {
        if (fast) {
            var width = 0f;
            for (int i = 0;i < count;i++) {
                char ch = chars[i + index];
                float charWidth;
                if (Character.isHighSurrogate(ch) && i + 1 < count && Character.isLowSurrogate(chars[index + i + 1])) {
                    charWidth = widths.measureCodePoint(Character.toCodePoint(ch, chars[index + i + 1]), this);
                    if (advances != null) {
                        advances[advancesIndex + i] = charWidth;
                        advances[advancesIndex + i + 1] = 0f;
                    }
                    i++;
                } else {
                    charWidth = (ch == '\t') ? tabWidth : widths.measureChar(ch, this);
                    if (advances != null) {
                        advances[advancesIndex + i] = charWidth;
                    }
                }
                width += charWidth;
            }
            return width;
        } else {
            return getTextRunAdvances(chars, index, count, contextIndex, contextCount, isRtl, advances, advancesIndex);
        }
    }

    /**
     * Get the advance of text with the context positions related to shaping the characters
     */
    @SuppressLint("NewApi")
    public float measureTextRunAdvance(char[] text, int start, int end, int contextStart, int contextEnd, boolean fast) {
        return myGetTextRunAdvances(text, start, end - start, contextStart, contextEnd - contextStart, false, null, 0, fast);
    }

    /**
     * Find offset for a certain advance returned by {@link #measureTextRunAdvance(char[], int, int, int, int, boolean)}
     */
    public int findOffsetByRunAdvance(ContentLine text, int start, int end, float advance, boolean useCache, boolean fast) {
        if (text.widthCache != null && useCache) {
            var cache = text.widthCache;
            var offset = start;
            var currAdvance = 0f;
            for (; offset < end && currAdvance < advance; offset++) {
                currAdvance += cache[offset];
            }
            if (currAdvance > advance) {
                offset--;
            }
            return Math.max(offset, start);
        }
        if (fast) {
            var width = 0f;
            for (int i = start;i < end;i++) {
                char ch = text.value[i];
                float charWidth;
                int j = i;
                if (Character.isHighSurrogate(ch) && i + 1 < end && Character.isLowSurrogate(text.value[i + 1])) {
                    charWidth = widths.measureCodePoint(Character.toCodePoint(ch, text.value[i + 1]), this);
                    i++;
                } else {
                    charWidth = (ch == '\t') ? tabWidth : widths.measureChar(ch, this);
                }
                width += charWidth;
                if (width > advance) {
                    return Math.max(start, j - 1);
                }
            }
            return end;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getOffsetForAdvance(text, start, end, start, end, false, advance);
        } else {
            return start + breakText(text.value, start, end - start, advance, null);
        }
    }

}
