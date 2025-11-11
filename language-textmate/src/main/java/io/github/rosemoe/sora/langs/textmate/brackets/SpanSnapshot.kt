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
package io.github.rosemoe.sora.langs.textmate.brackets

import io.github.rosemoe.sora.lang.styling.Spans
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

/** Thread-safe snapshot of [Spans] rows used by the bracket lexer. */
internal class SpanSnapshot(
    private val spans: Spans
) {

    @Volatile
    private var storage = ConcurrentHashMap<Int, LineData>()

    @Volatile
    private var lineCount = 0

    fun rebuildAll() {
        copyRange(0, spans.lineCount - 1)
    }

    fun updateRange(startLine: Int, endLine: Int) {
        val totalLines = spans.lineCount
        if (totalLines <= 0) {
            clear()
            return
        }

        copyRange(startLine, endLine)
    }

    fun clear() {
        storage.clear()
        lineCount = 0
    }

    fun lineCount(): Int = lineCount

    fun line(index: Int): LineData {
        val count = lineCount
        if (index !in 0..<count) {
            return LineData.EMPTY
        }
        return storage[index] ?: LineData.EMPTY
    }

    fun adjustOnInsert(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        if (storage.isEmpty() || startLine >= lineCount) {
            return
        }

        if (startLine == endLine) {
            shiftColumnsOnSingleLineInsert(startLine, startColumn, endColumn)
        } else {
            shiftColumnsOnMultiLineInsert(startLine, startColumn, endLine, endColumn)
        }
    }

    fun adjustOnDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        if (storage.isEmpty() || startLine >= lineCount) {
            return
        }

        if (startLine == endLine) {
            shiftColumnsOnSingleLineDelete(startLine, startColumn, endColumn)
        } else {
            shiftColumnsOnMultiLineDelete(startLine, startColumn, endLine, endColumn)
        }
    }

    private fun copyRange(startLine: Int, endLine: Int) {
        val totalLines = spans.lineCount
        if (totalLines <= 0) {
            storage.clear()
            lineCount = 0
            return
        }
        val range = normalizeRange(totalLines, startLine, endLine) ?: run {
            lineCount = totalLines
            return
        }
        val reader = spans.read()
        try {
            for (line in range) {
                reader.moveToLine(line)
                storage[line] = captureLine(reader)
            }
        } finally {
            reader.moveToLine(-1)
        }
        lineCount = totalLines
    }

    private fun captureLine(reader: Spans.Reader): LineData {
        val spanCount = reader.spanCount
        if (spanCount <= 0) {
            return LineData.EMPTY
        }
        val columns = IntArray(spanCount)
        val tokenTypes = IntArray(spanCount)
        for (i in 0 until spanCount) {
            val span = reader.getSpanAt(i)
            columns[i] = span.column
            val extra = span.extra
            tokenTypes[i] = extra as? Int ?: LineData.UNKNOWN_TOKEN_TYPE
        }
        return LineData(columns, tokenTypes, synthetic = false)
    }

    private fun shiftColumnsOnSingleLineInsert(line: Int, startCol: Int, endCol: Int) {
        val lineData = storage[line] ?: return
        if (lineData.spanCount == 0) return

        val delta = endCol - startCol
        val newColumns = IntArray(lineData.spanCount)

        for (i in 0 until lineData.spanCount) {
            val col = lineData.columns[i]
            newColumns[i] = if (col >= startCol) col + delta else col
        }

        storage[line] = LineData(newColumns, lineData.tokenTypes, synthetic = true)
    }

    private fun shiftColumnsOnSingleLineDelete(line: Int, startCol: Int, endCol: Int) {
        val lineData = storage[line] ?: return
        if (lineData.spanCount == 0) return

        val delta = endCol - startCol
        val newColumns = mutableListOf<Int>()
        val newTokenTypes = mutableListOf<Int>()

        for (i in 0 until lineData.spanCount) {
            val col = lineData.columns[i]
            when {
                col < startCol -> {
                    // Before deletion range, keep as is
                    newColumns.add(col)
                    newTokenTypes.add(lineData.tokenTypes[i])
                }
                col >= endCol -> {
                    // After deletion range, shift left
                    newColumns.add(col - delta)
                    newTokenTypes.add(lineData.tokenTypes[i])
                }
                // else: within deletion range, skip this span
            }
        }

        // Ensure at least one span at column 0 if list is empty
        if (newColumns.isEmpty() || newColumns[0] != 0) {
            newColumns.add(0, 0)
            newTokenTypes.add(0, LineData.UNKNOWN_TOKEN_TYPE)
        }

        storage[line] = LineData(newColumns.toIntArray(), newTokenTypes.toIntArray(), synthetic = true)
    }

    private fun shiftColumnsOnMultiLineInsert(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val linesToInsert = endLine - startLine

        val startLineData = storage[startLine] ?: LineData.EMPTY
        var extendedTokenType = LineData.UNKNOWN_TOKEN_TYPE

        for (i in startLineData.spanCount - 1 downTo 0) {
            if (startLineData.columns[i] <= startColumn) {
                extendedTokenType = startLineData.tokenTypes[i]
                break
            }
        }

        for (line in lineCount - 1 downTo startLine + 1) {
            storage[line]?.let { data ->
                storage[line + linesToInsert] = data
            }
        }

        for (i in 0 until linesToInsert) {
            storage[startLine + 1 + i] = LineData(intArrayOf(0), intArrayOf(extendedTokenType), synthetic = true)
        }

        lineCount += linesToInsert

        val startData = storage[startLine] ?: return
        val newStartColumns = mutableListOf<Int>()
        val newStartTokens = mutableListOf<Int>()
        val newEndColumns = mutableListOf<Int>()
        val newEndTokens = mutableListOf<Int>()

        var foundSplit = false
        for (i in 0 until startData.spanCount) {
            val col = startData.columns[i]
            if (col < startColumn) {
                newStartColumns.add(col)
                newStartTokens.add(startData.tokenTypes[i])
            } else {
                if (!foundSplit) {
                    newEndColumns.add(endColumn)
                    newEndTokens.add(startData.tokenTypes[i])
                    foundSplit = true
                } else {
                    newEndColumns.add(col - startColumn + endColumn)
                    newEndTokens.add(startData.tokenTypes[i])
                }
            }
        }

        if (newStartColumns.isNotEmpty()) {
            storage[startLine] = LineData(newStartColumns.toIntArray(), newStartTokens.toIntArray(), synthetic = true)
        }

        if (newEndColumns.isNotEmpty()) {
            storage[endLine] = LineData(newEndColumns.toIntArray(), newEndTokens.toIntArray(), synthetic = true)
        }
    }

    private fun shiftColumnsOnMultiLineDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val linesToRemove = endLine - startLine

        val startData = storage[startLine] ?: LineData.EMPTY
        val endData = storage[endLine] ?: LineData.EMPTY

        val newColumns = mutableListOf<Int>()
        val newTokens = mutableListOf<Int>()

        for (i in 0 until startData.spanCount) {
            val col = startData.columns[i]
            if (col < startColumn) {
                newColumns.add(col)
                newTokens.add(startData.tokenTypes[i])
            }
        }

        for (i in 0 until endData.spanCount) {
            val col = endData.columns[i]
            if (col >= endColumn) {
                newColumns.add(col - endColumn + startColumn)
                newTokens.add(endData.tokenTypes[i])
            }
        }

        if (newColumns.isEmpty() || newColumns[0] != 0) {
            newColumns.add(0, 0)
            newTokens.add(0, LineData.UNKNOWN_TOKEN_TYPE)
        }

        storage[startLine] = LineData(newColumns.toIntArray(), newTokens.toIntArray(), synthetic = true)

        for (line in startLine + 1..endLine) {
            storage.remove(line)
        }

        for (line in endLine + 1 until lineCount) {
            storage[line]?.let { data ->
                storage[line - linesToRemove] = data
                storage.remove(line)
            }
        }

        lineCount = max(0, lineCount - linesToRemove)
    }

    private fun normalizeRange(totalLines: Int, startLine: Int, endLine: Int): IntRange? {
        if (totalLines == 0) {
            return null
        }
        val minLine = max(0, min(startLine, endLine))
        val maxLine = min(totalLines - 1, max(startLine, endLine))
        if (maxLine < minLine) {
            return null
        }
        return minLine..maxLine
    }

    internal class LineData(
        val columns: IntArray,
        val tokenTypes: IntArray,
        val synthetic: Boolean
    ) {
        val spanCount: Int
            get() = columns.size

        companion object {
            const val UNKNOWN_TOKEN_TYPE = StandardTokenType.Other
            val EMPTY = LineData(IntArray(0), IntArray(0), synthetic = true)
        }
    }
}
