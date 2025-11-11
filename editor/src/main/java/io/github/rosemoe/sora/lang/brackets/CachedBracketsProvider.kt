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
package io.github.rosemoe.sora.lang.brackets

import android.util.SparseArray
import androidx.annotation.UiThread
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair

/**
 * Brackets provider that caches computed pairs keyed by left index.
 * Clear the cache after edits to keep results consistent.
 *
 */
abstract class CachedBracketsProvider : BracketsProvider {

    /** Cache keyed by left bracket index. */
    private val cache = SparseArray<PairedBracket>(1024)

    @UiThread
    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        val cached = cache.get(index)
        if (cached != null) {
            return cached
        }

        val result = computePairedBracketAt(text, index) ?: return null
        cache.put(result.leftIndex, result)
        return result
    }

    @UiThread
    override fun queryPairedBracketsForRange(
        text: Content,
        leftRange: Long,
        rightRange: Long
    ): List<PairedBracket>? {
        val indexer = text.indexer
        val leftLine = IntPair.getFirst(leftRange)
        val leftColumn = IntPair.getSecond(leftRange)
        val rightLine = IntPair.getFirst(rightRange)
        val rightColumn = IntPair.getSecond(rightRange)
        val leftIndex = indexer.getCharIndex(leftLine, leftColumn)
        val rightIndex = indexer.getCharIndex(rightLine, rightColumn)

        val cachedResults = getRangeFromCache(leftIndex, rightIndex)
        if (cachedResults.isNotEmpty()) {
            return cachedResults
        }

        println(cachedResults)

        val computed = computePairedBracketsForRange(text, leftRange, rightRange) ?: return null
        computed.forEach { cache.put(it.leftIndex, it) }
        return computed
    }

    /**
     * Returns cached pairs whose left indexes are within [leftIndex, rightIndex].
     */
    private fun getRangeFromCache(leftIndex: Int, rightIndex: Int): List<PairedBracket> {
        val result = mutableListOf<PairedBracket>()

        var start = 0
        var end = cache.size() - 1
        var firstIndex = -1

        while (start <= end) {
            val mid = (start + end) / 2
            val key = cache.keyAt(mid)
            if (key >= leftIndex) {
                firstIndex = mid
                end = mid - 1
            } else {
                start = mid + 1
            }
        }

        if (firstIndex == -1) {
            return result
        }

        for (i in firstIndex until cache.size()) {
            val key = cache.keyAt(i)
            if (key > rightIndex) break
            result.add(cache.valueAt(i))
        }

        return result
    }

    /** Clears cached data. */
    @UiThread
    open fun clear() {
        cache.clear()
    }

    /**
     * Computes the bracket pair at the given position when it is not cached.
     */
    @UiThread
    protected abstract fun computePairedBracketAt(text: Content, index: Int): PairedBracket?

    /**
     * Computes all bracket pairs in [leftRange, rightRange] when they are not cached.
     */
    @UiThread
    protected abstract fun computePairedBracketsForRange(
        text: Content,
        leftRange: Long,
        rightRange: Long
    ): List<PairedBracket>?


}
