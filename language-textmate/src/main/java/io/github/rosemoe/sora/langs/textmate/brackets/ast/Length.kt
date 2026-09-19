/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets.ast

import kotlin.math.max

@JvmInline
value class Length internal constructor(val value: Long) : Comparable<Length> {

    val lineCount: Int
        get() = ((value ushr LINE_SHIFT) and LINE_MASK).toInt()

    val columnCount: Int
        get() = (value and COLUMN_MASK).toInt()

        fun isZero(): Boolean = value == 0L

        fun isNonZero(): Boolean = value != 0L

        operator fun plus(other: Length): Length {
        val newLineCount = lineCount + other.lineCount
        val newColumnCount = if (other.lineCount == 0) {
            columnCount + other.columnCount
        } else {
            other.columnCount
        }
        return of(newLineCount, newColumnCount)
    }


        fun diffNonNegative(other: Length): Length {
        if (this >= other) {
            return ZERO
        }
        return if (lineCount == other.lineCount) {
            of(0, other.columnCount - columnCount)
        } else {
            of(other.lineCount - lineCount, other.columnCount)
        }
    }


    override fun compareTo(other: Length): Int {
        val lineDiff = lineCount - other.lineCount
        if (lineDiff != 0) return lineDiff
        return columnCount - other.columnCount
    }

    override fun toString(): String {
        return if (lineCount == 0) {
            "Length(col=$columnCount)"
        } else {
            "Length(lines=$lineCount, col=$columnCount)"
        }
    }

    companion object {
        private const val LINE_BITS = 26
        private const val COLUMN_BITS = 26
        private const val LINE_SHIFT = COLUMN_BITS
        private const val LINE_MASK = (1L shl LINE_BITS) - 1L
        private const val COLUMN_MASK = (1L shl COLUMN_BITS) - 1L

        val ZERO = Length(0L)

                fun of(lineCount: Int, columnCount: Int): Length {
            require(lineCount >= 0) { "lineCount must be >= 0: $lineCount" }
            require(columnCount >= 0) { "columnCount must be >= 0: $columnCount" }
            require(lineCount <= LINE_MASK.toInt()) { "lineCount too large: $lineCount" }
            require(columnCount <= COLUMN_MASK.toInt()) { "columnCount too large: $columnCount" }

            val value = ((lineCount.toLong() and LINE_MASK) shl LINE_SHIFT) or
                       (columnCount.toLong() and COLUMN_MASK)
            return Length(value)
        }

                fun ofColumn(columnCount: Int): Length = of(0, columnCount)

                fun ofLines(lineCount: Int): Length = of(lineCount, 0)
    }
}

@JvmInline
value class Position internal constructor(val value: Long) : Comparable<Position> {

    val line: Int
        get() = ((value ushr LINE_SHIFT) and LINE_MASK).toInt()

    val column: Int
        get() = (value and COLUMN_MASK).toInt()

        operator fun plus(length: Length): Position {
        val newLine = line + length.lineCount
        val newColumn = if (length.lineCount == 0) {
            column + length.columnCount
        } else {
            length.columnCount
        }
        return of(newLine, newColumn)
    }

        operator fun minus(other: Position): Length {
        require(this >= other) { "Cannot subtract larger position from smaller" }

        if (line == other.line) {
            return Length.ofColumn(column - other.column)
        }

        return Length.of(line - other.line, column)
    }

    override fun compareTo(other: Position): Int {
        val lineDiff = line - other.line
        if (lineDiff != 0) return lineDiff
        return column - other.column
    }

        internal inline fun toLength(): Length = Length(value)

    override fun toString(): String = "Position(line=$line, col=$column)"

    companion object {
        private const val LINE_BITS = 26
        private const val COLUMN_BITS = 26
        private const val LINE_SHIFT = COLUMN_BITS
        private const val LINE_MASK = (1L shl LINE_BITS) - 1L
        private const val COLUMN_MASK = (1L shl COLUMN_BITS) - 1L

        val ZERO = Position(0L)

        fun of(line: Int, column: Int): Position {
            require(line >= 0) { "line must be >= 0: $line" }
            require(column >= 0) { "column must be >= 0: $column" }
            require(line <= LINE_MASK.toInt()) { "line too large: $line" }
            require(column <= COLUMN_MASK.toInt()) { "column too large: $column" }

            val value = ((line.toLong() and LINE_MASK) shl LINE_SHIFT) or
                       (column.toLong() and COLUMN_MASK)
            return Position(value)
        }

                internal inline fun fromLength(length: Length): Position = Position(length.value)
    }
}
