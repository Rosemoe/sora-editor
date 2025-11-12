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
package io.github.rosemoe.sora.langs.textmate.brackets

import io.github.rosemoe.sora.util.IntPair

/**
 * Packed bracket token representation (line/column/length/id/type) stored inside a single [Long].
 * Bit layout (lowest → highest):
 * ```
 * [0, 9]     : length (10 bits, up to 1023)
 * [10, 21]   : bracket id (12 bits, up to 4095)
 * [22]       : is opening flag (1 bit)
 * [23, 43]   : column (21 bits, up to 2_097_151)
 * [44, 63]   : line (20 bits, up to 1_048_575)
 * ```
 */
@JvmInline
value class BracketToken internal constructor(val data: Long) {

    val line: Int
        get() = ((data ushr LINE_SHIFT) and LINE_MASK.toLong()).toInt()

    val column: Int
        get() = ((data ushr COLUMN_SHIFT) and COLUMN_MASK.toLong()).toInt()

    val length: Int
        get() = ((data ushr LENGTH_SHIFT) and LENGTH_MASK.toLong()).toInt()

    val bracketId: Int
        get() = ((data ushr ID_SHIFT) and ID_MASK.toLong()).toInt()

    val isOpening: Boolean
        get() = ((data ushr OPEN_SHIFT) and 1L) != 0L

    val position: Long
        get() = IntPair.pack(line, column)

    val endColumn: Int
        get() = column + length

    override fun toString(): String {
        return "BracketToken(line=$line, column=$column, length=$length, bracketId=$bracketId, isOpening=$isOpening)"
    }

    companion object {
        private const val LENGTH_BITS = 10
        private const val ID_BITS = 12
        private const val COLUMN_BITS = 21
        private const val LINE_BITS = 20

        private const val LENGTH_SHIFT = 0
        private const val ID_SHIFT = LENGTH_SHIFT + LENGTH_BITS
        private const val OPEN_SHIFT = ID_SHIFT + ID_BITS
        private const val COLUMN_SHIFT = OPEN_SHIFT + 1
        private const val LINE_SHIFT = COLUMN_SHIFT + COLUMN_BITS

        private const val LENGTH_MASK = (1 shl LENGTH_BITS) - 1
        private const val ID_MASK = (1 shl ID_BITS) - 1
        private const val COLUMN_MASK = (1 shl COLUMN_BITS) - 1
        private const val LINE_MASK = (1 shl LINE_BITS) - 1

        const val MAX_LINE_INDEX = LINE_MASK
        const val MAX_COLUMN_INDEX = COLUMN_MASK
        const val MAX_LENGTH = LENGTH_MASK
        const val MAX_BRACKET_ID = ID_MASK

        fun create(line: Int, column: Int, length: Int, bracketId: Int, isOpening: Boolean): BracketToken {
            require(line in 0..MAX_LINE_INDEX) { "line out of range: $line" }
            require(column in 0..MAX_COLUMN_INDEX) { "column out of range: $column" }
            require(length in 1..MAX_LENGTH) { "length must be 1..$MAX_LENGTH (was $length)" }
            require(bracketId in 0..MAX_BRACKET_ID) { "bracketId must be 0..$MAX_BRACKET_ID (was $bracketId)" }

            var value = 0L
            value = value or ((line.toLong() and LINE_MASK.toLong()) shl LINE_SHIFT)
            value = value or ((column.toLong() and COLUMN_MASK.toLong()) shl COLUMN_SHIFT)
            value = value or (((if (isOpening) 1L else 0L) and 1L) shl OPEN_SHIFT)
            value = value or ((bracketId.toLong() and ID_MASK.toLong()) shl ID_SHIFT)
            value = value or ((length.toLong() and LENGTH_MASK.toLong()) shl LENGTH_SHIFT)
            return BracketToken(value)
        }
    }
}
