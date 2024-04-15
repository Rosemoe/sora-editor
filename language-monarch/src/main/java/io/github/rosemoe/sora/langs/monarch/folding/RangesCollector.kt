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
package io.github.rosemoe.sora.langs.monarch.folding

import android.util.SparseIntArray
import io.github.rosemoe.sora.text.Content

class RangesCollector {
    //this.tabSize = tabSize;
    private val _startIndexes = SparseIntArray()
    private val _endIndexes = SparseIntArray()

    //private final SparseIntArray _indentOccurrences;
    private var _length = 0

    //private final int tabSize;
    init {
        //this._indentOccurrences = new SparseIntArray();
    }

    fun insertFirst(startLineNumber: Int, endLineNumber: Int, indent: Int) {
        if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
            return
        }
        val index = this._length
        _startIndexes.put(index, startLineNumber)
        _endIndexes.put(index, endLineNumber)
        _length++
        /*if (indent < 1000) {
            this._indentOccurrences.put(indent, (this._indentOccurrences.get(indent)) + 1);
        }*/
    }

    @Throws(Exception::class)
    fun toIndentRanges(model: Content): FoldingRegions {
        return FoldingRegions(_startIndexes, _endIndexes)
        /*
        // reverse and create arrays of the exact length
        SparseIntArray startIndexes = new SparseIntArray(this._length);
        SparseIntArray endIndexes = new SparseIntArray(this._length);
        for (int i = this._length - 1, k = 0; i >= 0; i--, k++) {
            startIndexes.put(k, this._startIndexes.get(i));
            endIndexes.put(k, this._endIndexes.get(i));
        }
        return new FoldingRegions(startIndexes, endIndexes);*/
    }
}
