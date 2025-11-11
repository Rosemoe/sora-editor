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

import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.util.IntPair
import java.util.ArrayDeque

/**
 * Lightweight bracket pair description used by the matcher and rainbow rendering.
 * Instances are pooled to reduce churn during rapid updates.
 */
class BracketPair private constructor() {

    var leftPosition: Long = 0
        private set

    var rightPosition: Long = 0
        private set

    var leftLength: Int = 0
        private set

    var rightLength: Int = 0
        private set

    var bracketId: Int = -1
        private set

    var level: Int = 0
        private set

    val leftLine: Int
        get() = IntPair.getFirst(leftPosition)

    val leftColumn: Int
        get() = IntPair.getSecond(leftPosition)

    val rightLine: Int
        get() = IntPair.getFirst(rightPosition)

    val rightColumn: Int
        get() = IntPair.getSecond(rightPosition)

    fun isInPosition(position: CharPosition): Boolean {
        if (position.line < leftLine) {
            return false
        }
        if (position.line == leftLine && position.column < leftColumn) {
            return false
        }

        if (position.line > rightLine) {
            return false
        }
        if (position.line == rightLine && position.column > rightColumn) {
            return false
        }

        return true
    }

    /** Returns true if the position falls within the opening bracket span. */
    fun openingBracketContainsPosition(line: Int, column: Int): Boolean {
        return line == leftLine && column >= leftColumn && column <= leftColumn + leftLength
    }

    /** Returns true if the position falls within the closing bracket span. */
    fun closingBracketContainsPosition(line: Int, column: Int): Boolean {
        return line == rightLine && column >= rightColumn && column <= rightColumn + rightLength
    }

    /** Returns true if the position is inside the pair's overall range. */
    fun rangeContainsPosition(line: Int, column: Int): Boolean {
        if (line < leftLine) return false
        if (line == leftLine && column < leftColumn) return false

        val endColumn = rightColumn + rightLength
        if (line > rightLine) return false
        if (line == rightLine && column >= endColumn) return false

        return true
    }

    /** True when the position is between the brackets but not on them. */
    fun rangeStrictlyContainsPosition(line: Int, column: Int): Boolean {
        if (line < leftLine) return false
        if (line == leftLine && column <= leftColumn + leftLength) return false

        if (line > rightLine) return false
        if (line == rightLine && column >= rightColumn) return false

        return true
    }

    private fun populate(
        leftPosition: Long,
        rightPosition: Long,
        leftLength: Int,
        rightLength: Int,
        bracketId: Int,
        level: Int
    ) {
        this.leftPosition = leftPosition
        this.rightPosition = rightPosition
        this.leftLength = leftLength
        this.rightLength = rightLength
        this.bracketId = bracketId
        this.level = level
    }

    private fun reset() {
        leftPosition = 0
        rightPosition = 0
        leftLength = 0
        rightLength = 0
        bracketId = -1
        level = 0
    }

    override fun toString(): String {
        return "BracketPair(leftPosition=$leftPosition, rightPosition=$rightPosition, leftLength=$leftLength, rightLength=$rightLength, bracketId=$bracketId, level=$level, leftLine=$leftLine, leftColumn=$leftColumn, rightLine=$rightLine, rightColumn=$rightColumn)"
    }


    companion object {
        private const val MAX_POOL_SIZE = 10_000
        private val pool = ArrayDeque<BracketPair>(MAX_POOL_SIZE)

        @JvmStatic
        fun obtain(
            leftPosition: Long,
            rightPosition: Long,
            leftLength: Int,
            rightLength: Int,
            bracketId: Int,
            level: Int
        ): BracketPair {
            require(leftLength > 0) { "leftLength must be > 0 (was $leftLength)" }
            require(rightLength > 0) { "rightLength must be > 0 (was $rightLength)" }
            require(bracketId >= 0) { "bracketId must be >= 0 (was $bracketId)" }

            val pair = synchronized(pool) {
                if (pool.isEmpty()) BracketPair() else pool.removeLast()
            }
            pair.populate(leftPosition, rightPosition, leftLength, rightLength, bracketId, level)
            return pair
        }

        @JvmStatic
        fun recycle(pair: BracketPair) {
            synchronized(pool) {
                if (pool.size < MAX_POOL_SIZE) {
                    pair.reset()
                    pool.addLast(pair)
                }
            }
        }

        @JvmStatic
        fun clearPool() {
            synchronized(pool) {
                pool.clear()
            }
        }
    }
}
