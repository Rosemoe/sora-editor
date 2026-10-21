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
 ******************************************************************************/
package io.github.rosemoe.sora.lang.folding

/**
 * Immutable-style mapping between logical lines and the rows that are actually displayed on
 * screen when some [FoldingRegion]s are collapsed.
 *
 * The map stores the merged, sorted and disjoint intervals of hidden lines. It is updated in
 * place by [update], so long living references (layouts, row iterators) always observe the
 * latest folding state.
 *
 * An empty map is the identity mapping, which means that code paths using this class behave
 * exactly like before folding was introduced when nothing is collapsed.
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class FoldedLineMap {

    private var starts = EMPTY
    private var ends = EMPTY
    private var hiddenLineCount = 0
    private var lineCount = 0

    /** Whether nothing is hidden by folding */
    val isEmpty: Boolean
        get() = starts.isEmpty()

    /** Total number of hidden lines */
    val hiddenLines: Int
        get() = hiddenLineCount

    /** Logical line count this map was built for */
    val totalLines: Int
        get() = lineCount

    /** Number of lines that are actually displayed */
    val visibleLineCount: Int
        get() = (lineCount - hiddenLineCount).coerceAtLeast(0)

    /**
     * Rebuild the map for the given collapsed regions.
     *
     * @param regions  Regions that are currently collapsed. Regions that do not hide anything are ignored.
     * @param lineCount Current logical line count of the content
     */
    fun update(regions: Collection<FoldingRegion>, lineCount: Int) {
        this.lineCount = lineCount
        if (regions.isEmpty()) {
            clear()
            return
        }
        val maxIndex = (lineCount - 1).coerceAtLeast(0)
        val intervals = ArrayList<IntArray>(regions.size)
        for (region in regions) {
            if (!region.isFoldable) {
                continue
            }
            val start = (region.startLine + 1).coerceIn(0, maxIndex)
            val end = region.endLine.coerceIn(0, maxIndex)
            if (end < start) {
                continue
            }
            intervals.add(intArrayOf(start, end))
        }
        if (intervals.isEmpty()) {
            clear()
            return
        }
        intervals.sortBy { it[0] }
        val mergedStart = IntArray(intervals.size)
        val mergedEnd = IntArray(intervals.size)
        var count = 0
        for (interval in intervals) {
            val start = interval[0]
            val end = interval[1]
            if (count > 0 && start <= mergedEnd[count - 1] + 1) {
                if (end > mergedEnd[count - 1]) {
                    mergedEnd[count - 1] = end
                }
            } else {
                mergedStart[count] = start
                mergedEnd[count] = end
                count++
            }
        }
        var hidden = 0
        for (i in 0 until count) {
            hidden += mergedEnd[i] - mergedStart[i] + 1
        }
        starts = if (count == mergedStart.size) mergedStart else mergedStart.copyOf(count)
        ends = if (count == mergedEnd.size) mergedEnd else mergedEnd.copyOf(count)
        hiddenLineCount = hidden
    }

    private fun clear() {
        starts = EMPTY
        ends = EMPTY
        hiddenLineCount = 0
    }

    /**
     * Whether the given line is hidden by folding
     */
    fun isHidden(line: Int): Boolean {
        return findInterval(line) >= 0
    }

    /**
     * Number of hidden lines that are placed before the given logical line
     */
    fun hiddenCountBefore(line: Int): Int {
        if (starts.isEmpty() || line <= 0) {
            return 0
        }
        var count = 0
        for (i in starts.indices) {
            val start = starts[i]
            if (start >= line) {
                break
            }
            val end = ends[i]
            count += minOf(end, line - 1) - start + 1
        }
        return count
    }

    /**
     * Get the index of the row that displays the given logical line.
     * Hidden lines are clamped to their fold header, so the result is always a valid row index.
     */
    fun lineToDisplay(line: Int): Int {
        val visible = clampToVisible(line)
        return (visible - hiddenCountBefore(visible)).coerceAtLeast(0)
    }

    /**
     * Get the logical line displayed by the given row index.
     * Out of range values are clamped to the closest visible line.
     */
    fun displayToLine(display: Int): Int {
        if (lineCount <= 0) {
            return 0
        }
        if (starts.isEmpty()) {
            return display.coerceIn(0, lineCount - 1)
        }
        var rest = display.coerceAtLeast(0)
        var base = 0
        for (i in starts.indices) {
            val start = starts[i]
            val end = ends[i]
            val visibleBefore = (start - base).coerceAtLeast(0)
            if (rest < visibleBefore) {
                return (base + rest).coerceAtMost(lineCount - 1)
            }
            rest -= visibleBefore
            base = end + 1
        }
        return (base + rest).coerceIn(0, lineCount - 1)
    }

    /**
     * Map the given line to a visible line. Visible lines are returned as is, hidden lines are
     * mapped to the header of the fold that hides them.
     */
    fun clampToVisible(line: Int): Int {
        val maxIndex = (lineCount - 1).coerceAtLeast(0)
        val index = findInterval(line)
        if (index >= 0) {
            val header = starts[index] - 1
            return if (header >= 0) header else (ends[index] + 1).coerceAtMost(maxIndex)
        }
        return line.coerceIn(0, maxIndex)
    }

    /**
     * Index of the interval that hides the given line or -1 when the line is visible
     */
    private fun findInterval(line: Int): Int {
        if (starts.isEmpty()) {
            return -1
        }
        var left = 0
        var right = starts.size - 1
        while (left <= right) {
            val mid = left + right ushr 1
            val start = starts[mid]
            val end = ends[mid]
            when {
                start > line -> right = mid - 1
                end < line -> left = mid + 1
                else -> return mid
            }
        }
        return -1
    }

    override fun toString(): String {
        return "FoldedLineMap{hidden=$hiddenLineCount, lines=$lineCount}"
    }

    companion object {
        private val EMPTY = IntArray(0)
    }

}
