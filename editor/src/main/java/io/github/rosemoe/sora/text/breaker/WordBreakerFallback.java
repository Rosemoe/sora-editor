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
package io.github.rosemoe.sora.text.breaker;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.util.MyCharacter;

public class WordBreakerFallback implements WordBreaker {

    private final char[] text;
    private final int len;

    public WordBreakerFallback(@NonNull ContentLine text) {
        this.text = text.getBackingCharArray();
        this.len = text.length();
    }

    @Override
    public int getOptimizedBreakPoint(int start, int end) {
        if (MyCharacter.isAlpha(text[end - 1]) &&
                end < len && (MyCharacter.isAlpha(text[end]) || text[end] == '-')) {
            int wordStart = end - 1;
            while (wordStart > start && MyCharacter.isAlpha(text[wordStart - 1])) {
                wordStart--;
            }
            if (wordStart > start) {
                end = wordStart;
            }
        }
        return end;
    }
}
