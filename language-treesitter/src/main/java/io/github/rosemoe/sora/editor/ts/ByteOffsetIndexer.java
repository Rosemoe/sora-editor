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
package io.github.rosemoe.sora.editor.ts;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Indexer;

/**
 * Indexer for byte offsets. Note that the result index are byte offsets in UTF-16.
 *
 * @author Rosemoe
 */
public class ByteOffsetIndexer implements Indexer {

    @Override
    public int getCharIndex(int line, int column) {
        return getCharPosition(line, column).index;
    }

    @Override
    public int getCharLine(int index) {
        return getCharPosition(index).line;
    }

    @Override
    public int getCharColumn(int index) {
        return getCharPosition(index).column;
    }

    @NonNull
    @Override
    public CharPosition getCharPosition(int index) {
        var pos = new CharPosition();
        getCharPosition(index, pos);
        return pos;
    }

    @NonNull
    @Override
    public CharPosition getCharPosition(int line, int column) {
        var pos = new CharPosition();
        getCharPosition(line, column, pos);
        return pos;
    }

    @Override
    public void getCharPosition(int index, @NonNull CharPosition dest) {
        //TODO
    }

    @Override
    public void getCharPosition(int line, int column, @NonNull CharPosition dest) {
        //TODO
    }
}
