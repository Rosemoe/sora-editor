/*
 *    CodeEditor - the awesome code editor for Android
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
package io.github.rosemoe.sora.langs.textmate.folding;

import android.util.SparseIntArray;

public class FoldingRegions {
    private final SparseIntArray _startIndexes;
    private final SparseIntArray _endIndexes;

    public FoldingRegions(SparseIntArray startIndexes, SparseIntArray endIndexes) throws Exception {
        if (startIndexes.size() != endIndexes.size() || startIndexes.size() > IndentRange.MAX_FOLDING_REGIONS) {
            throw new Exception("invalid startIndexes or endIndexes size");
        }
        this._startIndexes = startIndexes;
        this._endIndexes = endIndexes;
    }

    public int length() {
        return this._startIndexes.size();
    }

    public int getStartLineNumber(int index) {
        return this._startIndexes.get(index) & IndentRange.MAX_LINE_NUMBER;
    }

    public int getEndLineNumber(int index) {
        return this._endIndexes.get(index) & IndentRange.MAX_LINE_NUMBER;
    }


    public FoldingRegion toRegion(int index) {
        return new FoldingRegion(this, index);
    }

}
