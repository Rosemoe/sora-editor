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
package io.github.rosemoe.sora.langs.textmate.folding

import android.util.SparseIntArray
import java.util.Stack

class FoldingRegions(startIndexes: SparseIntArray, endIndexes: SparseIntArray) {
    private val startIndexes: SparseIntArray
    private val endIndexes: SparseIntArray
    private var parentsComputed: Boolean

    init {
        if (startIndexes.size() != endIndexes.size() || startIndexes.size() > IndentRange.MAX_FOLDING_REGIONS) {
            throw Exception("invalid startIndexes or endIndexes size")
        }
        this.startIndexes = startIndexes
        this.endIndexes = endIndexes
        this.parentsComputed = false
    }

    fun length(): Int {
        return startIndexes.size()
    }

    fun getStartLineNumber(index: Int): Int {
        return startIndexes.get(index) and IndentRange.MAX_LINE_NUMBER
    }

    fun getEndLineNumber(index: Int): Int {
        return this.endIndexes.get(index) and IndentRange.MAX_LINE_NUMBER
    }


    fun toRegion(index: Int): FoldingRegion {
        return FoldingRegion(this, index)
    }

    private fun isInsideLast(
        parentIndexes: Stack<Int>,
        startLineNumber: Int,
        endLineNumber: Int
    ): Boolean {
        val index: Int = parentIndexes[parentIndexes.size - 1]
        return this.getStartLineNumber(index) <= startLineNumber && this.getEndLineNumber(index) >= endLineNumber
    }

    @Throws(Exception::class)
    private fun ensureParentIndices() {
        if (!this.parentsComputed) {
            this.parentsComputed = true
            val parentIndexes = Stack<Int>()
            var i = 0
            val len = this.startIndexes.size()
            while (i < len) {
                val startLineNumber = this.startIndexes.get(i)
                val endLineNumber = this.endIndexes.get(i)
                if (startLineNumber > IndentRange.MAX_LINE_NUMBER || endLineNumber > IndentRange.MAX_LINE_NUMBER) {
                    throw Exception("startLineNumber or endLineNumber must not exceed " + IndentRange.MAX_LINE_NUMBER)
                }
                while (!parentIndexes.isEmpty() && !isInsideLast(
                        parentIndexes,
                        startLineNumber,
                        endLineNumber
                    )
                ) {
                    parentIndexes.pop()
                }
                val parentIndex = if (parentIndexes.isNotEmpty()) parentIndexes[parentIndexes.size - 1] else -1
                parentIndexes.push(i)
                this.startIndexes.put(i, startLineNumber + ((parentIndex and 0xFF) shl 24))
                this.endIndexes.put(i, endLineNumber + ((parentIndex and 0xFF00) shl 16))
                i++
            }
        }
    }

    @Throws(Exception::class)
    fun getParentIndex(index: Int): Int {
        ensureParentIndices()
        val parent =
            ((this.startIndexes.get(index) and IndentRange.MASK_INDENT) ushr 24) +
                    ((this.endIndexes.get(index) and IndentRange.MASK_INDENT) ushr 16)
        if (parent == IndentRange.MAX_FOLDING_REGIONS) {
            return -1
        }
        return parent
    }
}
