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
            var itr = new UnicodeIterator(new CharArrayWrapper(chars, index, count));
            char[] candidates = null;
            int offset = 0;
            var width = 0f;
            while (itr.hasNext()) {
                int codePoint = itr.nextCodePoint();
                if (GraphicCharacter.couldBeEmojiPart(codePoint)) {
                    candidates = appendCodePoint(candidates, offset, codePoint);
                    offset += Character.charCount(codePoint);
                } else {
                    if (offset != 0) {
                        width += getTextRunAdvances(candidates, 0, offset, 0, offset, isRtl, advances, advances != null ? advancesIndex + itr.getStartIndex() - offset : 0);
                        offset = 0;
                    }
                    float textWidth;
                    var flag = true;
                    if (codePoint == '\t') {
                        textWidth = tabWidth;
                    } else if (Character.charCount(codePoint) == 1) {
                        textWidth = widths.measureChar((char) codePoint, this);
                    } else {
                        flag = false;
                        var start = itr.getStartIndex();
                        var count2 = itr.getEndIndex() - start;
                        textWidth = getTextRunAdvances(chars, index + start, count2, index + start, count2, isRtl, advances, advances != null ? advancesIndex + itr.getStartIndex() - offset : 0);
                    }
                    width += textWidth;
                    if (flag && advances != null) {
                        advances[advancesIndex + itr.getStartIndex()] = textWidth;
                    }
                }
            }
            if (offset != 0) {
                width += getTextRunAdvances(candidates, 0, offset, 0, offset, isRtl, advances, advances != null ? advancesIndex + itr.getStartIndex() - offset : 0);
            }
            return width;
        } else {
            return getTextRunAdvances(chars, index, count, contextIndex, contextCount, isRtl, advances, advancesIndex);
        }
    }

    private static char[] appendCodePoint(char[] chars, int offset, int codePoint) {
        if (chars == null) {
            chars = TemporaryCharBuffer.obtain(16);
        }
        Character.toChars(codePoint, chars, offset);
        return chars;
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
            // TODO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getOffsetForAdvance(text, start, end, start, end, false, advance);
        } else {
            return start + breakText(text.value, start, end - start, advance, null);
        }
    }

}
