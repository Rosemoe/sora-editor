/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.lang.styling

import io.github.rosemoe.sora.lang.styling.color.EditorColor
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Container that keeps track of text highlight ranges (line/column pairs) and
 * updates them according to document insert/delete operations.
 */
class HighlightTextContainer {

    class HighlightText(
        var startLine: Int,
        var startColumn: Int,
        var endLine: Int,
        var endColumn: Int,
        val color: ResolvableColor = EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND)
    ) {
        internal fun hasLength(): Boolean {
            return startLine < endLine || (startLine == endLine && startColumn < endColumn)
        }
    }

    private val highlights = mutableListOf<HighlightText>()

    fun isEmpty(): Boolean = highlights.isEmpty()

    fun clear() {
        highlights.clear()
    }

    fun add(highlight: HighlightText) {
        require(comparePositions(highlight.startLine, highlight.startColumn, highlight.endLine, highlight.endColumn) <= 0) {
            "Highlight start must not be after its end"
        }
        val index = getInsertionPoint(highlight)
        highlights.add(index, highlight)
    }

    fun addAll(items: Collection<HighlightText>) {
        for (highlight in items) {
            add(highlight)
        }
    }

    fun remove(target: HighlightText) {
        val index = highlights.indexOfFirst { it === target }
        if (index >= 0) {
            highlights.removeAt(index)
        }
    }

    fun asList(): List<HighlightText> = highlights

    fun getForLine(line: Int): List<HighlightText> {
        if (highlights.isEmpty()) {
            return emptyList()
        }
        val result = ArrayList<HighlightText>()
        for (highlight in highlights) {
            if (highlight.endLine < line) {
                continue
            }
            if (highlight.startLine > line) {
                break
            }
            if (highlight.coversLine(line)) {
                result.add(highlight)
            }
        }
        return result
    }

    fun getLineNumbers(): IntArray {
        if (highlights.isEmpty()) {
            return IntArray(0)
        }
        val lines = sortedSetOf<Int>()
        for (highlight in highlights) {
            if (!highlight.hasLength()) {
                continue
            }
            var line = highlight.startLine
            while (line <= highlight.endLine) {
                if (highlight.coversLine(line)) {
                    lines.add(line)
                }
                line++
            }
        }
        return lines.toIntArray()
    }

    fun updateOnInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        if (highlights.isEmpty() || isNoOp(startLine, startColumn, endLine, endColumn)) {
            return
        }
        for (highlight in highlights) {
            if (!highlight.hasLength()) {
                continue
            }
            if (comparePositions(startLine, startColumn, highlight.startLine, highlight.startColumn) < 0) {
                val newStart = shiftForInsertion(
                    highlight.startLine,
                    highlight.startColumn,
                    startLine,
                    startColumn,
                    endLine,
                    endColumn
                )
                highlight.startLine = newStart.first
                highlight.startColumn = newStart.second
            }

            if (comparePositions(startLine, startColumn, highlight.endLine, highlight.endColumn) < 0) {
                val newEnd = shiftForInsertion(
                    highlight.endLine,
                    highlight.endColumn,
                    startLine,
                    startColumn,
                    endLine,
                    endColumn
                )
                highlight.endLine = newEnd.first
                highlight.endColumn = newEnd.second
            }
        }
        highlights.sortWith(highlightComparator)
    }

    fun updateOnDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        if (highlights.isEmpty() || isNoOp(startLine, startColumn, endLine, endColumn)) {
            return
        }
        val iterator = highlights.listIterator()
        while (iterator.hasNext()) {
            val highlight = iterator.next()
            if (!highlight.hasLength()) {
                iterator.remove()
                continue
            }

            if (comparePositions(highlight.endLine, highlight.endColumn, startLine, startColumn) <= 0) {
                continue
            }

            if (comparePositions(highlight.startLine, highlight.startColumn, endLine, endColumn) >= 0) {
                shiftForDeletion(highlight, startLine, startColumn, endLine, endColumn)
                continue
            }

            val startsBefore = comparePositions(highlight.startLine, highlight.startColumn, startLine, startColumn) < 0
            val endsAfter = comparePositions(highlight.endLine, highlight.endColumn, endLine, endColumn) > 0

            when {
                !startsBefore && !endsAfter -> {
                    iterator.remove()
                }
                startsBefore && !endsAfter -> {
                    highlight.endLine = startLine
                    highlight.endColumn = startColumn
                    if (!highlight.hasLength()) {
                        iterator.remove()
                    }
                }
                !startsBefore && endsAfter -> {
                    highlight.startLine = startLine
                    highlight.startColumn = startColumn
                    val newEnd = shiftPositionAfterDeletion(
                        highlight.endLine, highlight.endColumn,
                        startLine, startColumn, endLine, endColumn
                    )
                    highlight.endLine = newEnd.first
                    highlight.endColumn = newEnd.second
                    if (!highlight.hasLength()) {
                        iterator.remove()
                    }
                }
                startsBefore && endsAfter -> {
                    val newEnd = shiftPositionAfterDeletion(
                        highlight.endLine, highlight.endColumn,
                        startLine, startColumn, endLine, endColumn
                    )
                    highlight.endLine = newEnd.first
                    highlight.endColumn = newEnd.second
                }
            }
        }
        highlights.sortWith(highlightComparator)
    }

    private fun shiftForDeletion(
        highlight: HighlightText,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ) {
        val newStart = shiftPositionAfterDeletion(
            highlight.startLine,
            highlight.startColumn,
            startLine,
            startColumn,
            endLine,
            endColumn
        )
        val newEnd = shiftPositionAfterDeletion(
            highlight.endLine,
            highlight.endColumn,
            startLine,
            startColumn,
            endLine,
            endColumn
        )
        highlight.startLine = newStart.first
        highlight.startColumn = newStart.second
        highlight.endLine = newEnd.first
        highlight.endColumn = newEnd.second
    }

    private fun HighlightText.coversLine(line: Int): Boolean {
        if (!hasLength()) return false
        if (line !in startLine..endLine) return false
        if (startLine == endLine) {
            return true
        }
        if (line == endLine) {
            return endColumn > 0
        }
        return true
    }

    private fun getInsertionPoint(highlight: HighlightText): Int {
        val index = highlights.binarySearch(highlight, highlightComparator)
        return if (index >= 0) index else -(index + 1)
    }

    private fun shiftForInsertion(
        line: Int,
        column: Int,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ): Pair<Int, Int> {
        if (comparePositions(line, column, startLine, startColumn) < 0) {
            return line to column
        }
        val lineDelta = endLine - startLine
        if (line == startLine) {
            return if (lineDelta == 0) {
                line to column + (endColumn - startColumn)
            } else {
                val columnOffset = column - startColumn
                endLine to endColumn + columnOffset
            }
        }
        return line + lineDelta to column
    }

    private fun shiftPositionAfterDeletion(
        line: Int,
        column: Int,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ): Pair<Int, Int> {
        val lineDelta = endLine - startLine
        if (lineDelta == 0) {
            if (line == startLine) {
                return line to column - (endColumn - startColumn)
            }
            return line to column
        }
        if (line > endLine) {
            return line - lineDelta to column
        }
        val columnOffset = column - endColumn
        return startLine to startColumn + columnOffset
    }

    private fun isNoOp(
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int
    ): Boolean {
        return startLine == endLine && startColumn == endColumn
    }

    companion object {
        private val highlightComparator = Comparator<HighlightText> { o1, o2 ->
            val startCompare = comparePositions(
                o1.startLine,
                o1.startColumn,
                o2.startLine,
                o2.startColumn
            )
            if (startCompare != 0) {
                return@Comparator startCompare
            }
            comparePositions(o1.endLine, o1.endColumn, o2.endLine, o2.endColumn)
        }

        private fun comparePositions(
            line1: Int,
            column1: Int,
            line2: Int,
            column2: Int
        ): Int {
            if (line1 != line2) {
                return line1 - line2
            }
            return column1 - column2
        }
    }
}
