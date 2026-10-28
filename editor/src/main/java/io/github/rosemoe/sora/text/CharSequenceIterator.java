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
package io.github.rosemoe.sora.text;

import androidx.annotation.NonNull;

import java.text.CharacterIterator;

/**
 * CharacterIterator implementation
 *
 * @author Rosemoe
 */
public class CharSequenceIterator implements CharacterIterator {

    private final CharSequence src;
    private int index;

    public CharSequenceIterator(@NonNull CharSequence source) {
        src = source;
    }

    @Override
    public char first() {
        index = 0;
        return current();
    }

    // next(), previous() and last() follow the CharacterIterator contract - DONE
    // at the ends, index clamped to [begin, end]. previous() used to return the first character
    // again at index 0 instead of DONE, which sends java.text.BreakIterator.preceding() into an
    // endless loop, so word wrap never finished laying out and the editor stayed busy.
    @Override
    public char last() {
        index = Math.max(0, src.length() - 1);
        return current();
    }

    @Override
    public char current() {
        return index >= 0 && index < getEndIndex() ? src.charAt(index) : CharacterIterator.DONE;
    }

    @Override
    public char next() {
        if (index < getEndIndex() - 1) {
            index++;
            return src.charAt(index);
        }
        index = getEndIndex();
        return CharacterIterator.DONE;
    }

    @Override
    public char previous() {
        if (index <= getBeginIndex()) {
            return CharacterIterator.DONE;
        }
        index--;
        return src.charAt(index);
    }

    @Override
    public char setIndex(int i) {
        if (i < getBeginIndex() || i > getEndIndex()) {
            throw new IllegalArgumentException("Invalid index " + i);
        }
        index = i;
        return current();
    }

    @Override
    public int getBeginIndex() {
        return 0;
    }

    @Override
    public int getEndIndex() {
        return src.length();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NonNull
    @Override
    public Object clone() {
        var another = new CharSequenceIterator(src);
        another.index = index;
        return another;
    }

}
