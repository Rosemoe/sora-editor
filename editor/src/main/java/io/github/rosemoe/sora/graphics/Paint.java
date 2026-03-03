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

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.GetChars;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.text.FunctionCharacters;
import io.github.rosemoe.sora.util.TemporaryCharBuffer;

public class Paint extends android.graphics.Paint {

    private float spaceWidth;
    private boolean renderFunctionCharacters;

    public Paint() {
        this(false);
    }

    public Paint(boolean renderFunctionCharacters) {
        super();
        this.renderFunctionCharacters = renderFunctionCharacters;
        spaceWidth = measureText(" ");
    }

    public void setRenderFunctionCharacters(boolean renderFunctionCharacters) {
        this.renderFunctionCharacters = renderFunctionCharacters;
    }

    public boolean isRenderFunctionCharacters() {
        return renderFunctionCharacters;
    }

    public void onAttributeUpdate() {
        spaceWidth = measureText(" ");
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
    public float myGetTextRunAdvances(@NonNull GetChars chars, int index, int count, int contextIndex, int contextCount, boolean isRtl, @Nullable float[] advances, int advancesIndex) {
        var buffer = TemporaryCharBuffer.obtain(contextCount);
        chars.getChars(contextIndex, contextIndex + contextCount, buffer, 0);
        float advance = getTextRunAdvances(buffer, index - contextIndex, count, 0, contextCount, isRtl, advances, advancesIndex);
        if (renderFunctionCharacters) {
            for (int i = 0; i < count; i++) {
                char ch = chars.charAt(index + i);
                if (FunctionCharacters.isEditorFunctionChar(ch)) {
                    float width = measureText(FunctionCharacters.getNameForFunctionCharacter(ch));
                    if (advances != null) {
                        advance -= advances[advancesIndex + i];
                        advances[advancesIndex + i] = width;
                    } else {
                        advance -= measureText(Character.toString(ch));
                    }
                    advance += width;
                }
            }
        }
        TemporaryCharBuffer.recycle(buffer);
        return advance;
    }

    /**
     * Get the advance of text with the context positions related to shaping the characters
     */
    public float measureTextRunAdvance(GetChars text, int start, int end, int contextStart, int contextEnd, boolean isRtl) {
        return myGetTextRunAdvances(text, start, end - start, contextStart, contextEnd - contextStart, isRtl, null, 0);
    }

    /**
     * Find offset for a certain advance returned by {@link #measureTextRunAdvance(GetChars, int, int, int, int, boolean)}
     */
    public int findOffsetByRunAdvance(GetChars text, int start, int end,
                                      int contextStart, int contextEnd, boolean isRtl,
                                      float advance) {
        if (renderFunctionCharacters) {
            int lastEnd = start;
            float current = 0f;
            for (int i = start;i < end;i++) {
                char ch = text.charAt(i);
                if (FunctionCharacters.isEditorFunctionChar(ch)) {
                    int result = lastEnd == i ? i : breakTextImpl(text, lastEnd, i, contextStart, contextEnd, isRtl, advance - current);
                    if (result < i) {
                        return result;
                    }
                    current += measureTextRunAdvance(text, lastEnd, i, contextStart, contextEnd, isRtl);
                    current += measureText(FunctionCharacters.getNameForFunctionCharacter(ch));
                    if (current >= advance) {
                        return i;
                    }
                    lastEnd = i + 1;
                }
            }
            if (lastEnd < end) {
                return breakTextImpl(text, lastEnd, end, contextStart, contextEnd, isRtl, advance - current);
            }
            return end;
        } else {
            return breakTextImpl(text, start, end, contextStart, contextEnd, isRtl, advance);
        }
    }

    private int breakTextImpl(GetChars text, int start, int end, int contextStart, int contextEnd, boolean isRtl, float advance) {
        var buffer = TemporaryCharBuffer.obtain(contextEnd - contextStart);
        text.getChars(contextStart, contextEnd, buffer, 0);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return contextStart + getOffsetForAdvance(buffer, start - contextStart, end - contextStart, 0, contextEnd - contextStart, isRtl, advance);
            } else {
                return start + breakText(buffer, start - contextStart, end - start, advance, null);
            }
        } finally {
            TemporaryCharBuffer.recycle(buffer);
        }
    }

}
