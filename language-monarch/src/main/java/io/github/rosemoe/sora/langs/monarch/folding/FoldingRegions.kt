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

class FoldingRegions(
    private val _startIndexes: SparseIntArray,
    private val _endIndexes: SparseIntArray
) {

    companion object {
        const val MAX_FOLDING_REGIONS = 8192
        const val MAX_LINE_NUMBER = 0x00FFFFFF
        const val MASK_INDENT = 0xFF000000.toInt()
    }

    private var _parentsComputed = false

    init {
        require(_startIndexes.size() == _endIndexes.size()) {
            "Invalid startIndexes or endIndexes size"
        }
        require(_startIndexes.size() <= MAX_FOLDING_REGIONS) {
            "startIndexes or endIndexes size exceeds $MAX_FOLDING_REGIONS"
        }
    }

    val indices: IntRange
        get() = IntRange(0, length)

    val length: Int
        get() = _startIndexes.size()

    fun getStartLineNumber(index: Int): Int {
        return _startIndexes[index] and MAX_LINE_NUMBER
    }

    fun getEndLineNumber(index: Int): Int {
        return _endIndexes[index] and MAX_LINE_NUMBER
    }

    fun toRegion(index: Int): FoldingRegion = FoldingRegion(this, index)

    private fun isInsideLast(
        parentIndexes: ArrayDeque<Int>,
        startLineNumber: Int,
        endLineNumber: Int
    ): Boolean {
        val index = parentIndexes[parentIndexes.size - 1]
        return getStartLineNumber(index) <= startLineNumber && getEndLineNumber(index) >= endLineNumber
    }

    private fun ensureParentIndices() {
        if (!_parentsComputed) {
            _parentsComputed = true
            val parentIndexes = ArrayDeque<Int>()
            for (i in 0 until _startIndexes.size()) {
                val startLineNumber = _startIndexes[i]
                val endLineNumber = _endIndexes[i]
                require(startLineNumber <= MAX_LINE_NUMBER && endLineNumber <= MAX_LINE_NUMBER) {
                    "startLineNumber or endLineNumber must not exceed $MAX_LINE_NUMBER"
                }
                while (parentIndexes.isNotEmpty() && !isInsideLast(
                        parentIndexes,
                        startLineNumber,
                        endLineNumber
                    )
                ) {
                    parentIndexes.removeFirst()
                }
                val parentIndex =
                    if (parentIndexes.isNotEmpty()) parentIndexes[parentIndexes.size - 1] else -1
                parentIndexes.addFirst(i)
                _startIndexes.put(i, startLineNumber + ((parentIndex and 0xFF) shl 24))
                _endIndexes.put(i, endLineNumber + ((parentIndex and 0xFF00) shl 16))
            }
        }
    }

    fun getParentIndex(index: Int): Int {
        ensureParentIndices()
        val parent =
            ((_startIndexes[index] and MASK_INDENT) ushr 24) + ((_endIndexes[index] and MASK_INDENT) ushr 16)
        return if (parent == MAX_FOLDING_REGIONS) -1 else parent
    }
}
