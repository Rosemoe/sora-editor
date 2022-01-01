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
package io.github.rosemoe.sora.langs.textmate.folding;

import android.util.SparseIntArray;

import io.github.rosemoe.sora.text.Content;

public class RangesCollector {
    private final SparseIntArray _startIndexes;
    private final SparseIntArray _endIndexes;
    private final SparseIntArray _indentOccurrences;
    private final int _foldingRangesLimit;
    private int _length;
    private final int tabSize;
    public RangesCollector(int foldingRangesLimit, int tabSize) {
        this.tabSize = tabSize;
        this._startIndexes = new SparseIntArray();
        this._endIndexes = new SparseIntArray();
        this._indentOccurrences = new SparseIntArray();
        this._length = 0;
        this._foldingRangesLimit = foldingRangesLimit;
    }

    public void insertFirst(int startLineNumber, int endLineNumber, int indent) {
        if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
            return;
        }
        int index = this._length;
        this._startIndexes.put(index, startLineNumber);
        this._endIndexes.put(index, endLineNumber);
        this._length++;
        if (indent < 1000) {
            this._indentOccurrences.put(indent, (this._indentOccurrences.get(indent)) + 1);
        }
    }

    public FoldingRegions toIndentRanges(Content model) throws Exception {
        if (this._length <= this._foldingRangesLimit) {
            // reverse and create arrays of the exact length
            SparseIntArray startIndexes = new SparseIntArray(this._length);
            SparseIntArray endIndexes = new SparseIntArray(this._length);
            for (int i = this._length - 1, k = 0; i >= 0; i--, k++) {
                startIndexes.put(k, this._startIndexes.get(i));
                endIndexes.put(k, this._endIndexes.get(i));
            }
            return new FoldingRegions(startIndexes, endIndexes);
        } else {
            int entries = 0;
            int maxIndent = this._indentOccurrences.size();
            for (int i = 0; i < this._indentOccurrences.size(); i++) {
                int n = this._indentOccurrences.get(i);
                if (n != 0) {
                    if (n + entries > this._foldingRangesLimit) {
                        maxIndent = i;
                        break;
                    }
                    entries += n;
                }
            }
            // reverse and create arrays of the exact length
            SparseIntArray startIndexes = new SparseIntArray(this._foldingRangesLimit);
            SparseIntArray endIndexes = new SparseIntArray(this._foldingRangesLimit);
            for (int i = this._length - 1, k = 0; i >= 0; i--) {
                int startIndex = this._startIndexes.get(i);
                String lineContent = model.getLineString(startIndex);
                int indent = IndentRange.computeIndentLevel(lineContent, tabSize);
                if (indent < maxIndent || (indent == maxIndent && entries++ < this._foldingRangesLimit)) {
                    startIndexes.put(k, startIndex);
                    endIndexes.put(k, this._endIndexes.get(i));
                    k++;
                }
            }
            return new FoldingRegions(startIndexes, endIndexes);
        }

    }
}
