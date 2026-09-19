/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

/**
 * A `(line, column)` pair packed into a single long.
 *
 * The bracket tree measures everything in this shape: a node stores an absolute document position,
 * and each child stores a *relative* length from its parent's start. Both use the same arithmetic,
 * so a subtree can be skipped by adding its length and two positions can be compared directly.
 *
 * Both fields are non-negative, which makes the natural long order identical to the lexicographic
 * `(line, column)` order.
 */
@JvmInline
internal value class TextOffset(val packed: Long) : Comparable<TextOffset> {

    val line: Int
        get() = (packed ushr 32).toInt()

    val column: Int
        get() = packed.toInt()

    /**
     * Move this offset forward by the relative [length].
     */
    operator fun plus(length: TextOffset): TextOffset = of(
        line + length.line,
        if (length.line == 0) column + length.column else length.column
    )

    /**
     * Relative length from this offset up to [other]. The result is a length, not a position.
     */
    fun lengthTo(other: TextOffset): TextOffset = of(
        other.line - line,
        if (line == other.line) other.column - column else other.column
    )

    override fun compareTo(other: TextOffset) = packed.compareTo(other.packed)

    override fun toString() = "($line, $column)"

    companion object {
        val ZERO = of(0)

        /** Larger than any offset reachable from a document, for open-ended range queries. */
        val MAX = of(Int.MAX_VALUE)

        fun of(line: Int, column: Int = 0) = TextOffset(
            (line.toLong() shl 32) or column.toLong()
        )
    }
}
