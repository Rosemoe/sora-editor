/*******************************************************************************
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

package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSNode
import com.itsaky.androidide.treesitter.TSQueryCursor
import com.itsaky.androidide.treesitter.TSQueryMatch
import com.itsaky.androidide.treesitter.TSTreeCursor
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import kotlin.math.max

/**
 * Collect every bracket pair that overlaps a document range.
 *
 * Tree-sitter can only restrict a query by byte range, so a slightly larger window is queried and
 * the resulting nodes are trimmed back to the range the caller asked for.
 */
internal fun collectBracketPairsInRange(
    safeTree: SafeTsTree,
    languageSpec: TsLanguageSpec,
    text: Content,
    leftRange: Long,
    rightRange: Long
): List<PairedBracket>? {
    val window = BracketQueryWindow.of(text, leftRange, rightRange) ?: return emptyList()
    val pairs = safeTree.accessTree { tree ->
        if (tree.closed) return@accessTree null
        val rootNode = tree.rootNode
        if (!rootNode.canAccess()) return@accessTree null
        queryBracketPairs(languageSpec, text, rootNode, window)
    } ?: return null
    return window.trim(pairs)
}

/**
 * Byte window to query, together with the char range the caller actually asked for.
 *
 * The window is padded by two lines on each side because a bracket pair may start or end just
 * outside the requested range.
 */
private class BracketQueryWindow(
    val leftIndex: Int,
    val rightIndex: Int,
    val startByte: Int,
    val endByte: Int
) {

    /** Keep only pairs that overlap the requested char range. */
    fun trim(pairs: List<PairedBracket>): List<PairedBracket> = pairs.filter {
        it.leftIndex < rightIndex && it.rightIndex + it.rightLength > leftIndex
    }

    companion object {

        private const val LINE_PADDING = 2

        fun of(text: Content, leftRange: Long, rightRange: Long): BracketQueryWindow? {
            val lineCount = text.lineCount
            if (lineCount == 0) return null

            val leftLine = IntPair.getFirst(leftRange)
            val leftColumn = IntPair.getSecond(leftRange)
            val rightLine = IntPair.getFirst(rightRange)
            val rightColumn = IntPair.getSecond(rightRange)

            val leftIndex = text.safeCharIndex(leftLine, leftColumn)
            val rightIndex = leftIndex.coerceAtLeast(text.safeCharIndex(rightLine, rightColumn))

            val startLine = (leftLine - LINE_PADDING).coerceAtLeast(0)
            val endLine = (rightLine + LINE_PADDING).coerceAtMost(lineCount - 1)
            val startIndex = text.safeCharIndex(startLine, 0)
            val endIndex = text.safeCharIndex(endLine, text.getColumnCount(endLine))

            val startByte = max(0, startIndex * 2)
            val endByte = max(startByte, endIndex * 2)
            return BracketQueryWindow(leftIndex, rightIndex, startByte, endByte)
        }
    }
}

private fun queryBracketPairs(
    languageSpec: TsLanguageSpec,
    text: Content,
    rootNode: TSNode,
    window: BracketQueryWindow
): List<PairedBracket> {
    val result = ArrayList<PairedBracket>()
    val treeCursor = TSTreeCursor.create(rootNode)
    try {
        TSQueryCursor.create().use { cursor ->
            cursor.isAllowChangedNodes = true
            cursor.setByteRange(window.startByte, window.endByte)
            cursor.exec(languageSpec.bracketsQuery, rootNode)
            var match = cursor.nextMatch()
            while (match != null) {
                bracketPairOf(match, languageSpec, text, window, treeCursor, rootNode)?.let(result::add)
                match = cursor.nextMatch()
            }
        }
    } finally {
        treeCursor.close()
    }
    result.sortBy { it.leftIndex }
    return result
}

/** Convert one query match into a pair, or `null` when it is unusable or outside the window. */
private fun bracketPairOf(
    match: TSQueryMatch,
    languageSpec: TsLanguageSpec,
    text: Content,
    window: BracketQueryWindow,
    treeCursor: TSTreeCursor,
    rootNode: TSNode
): PairedBracket? {
    if (!languageSpec.bracketsPredicator.doPredicate(languageSpec.predicates, text, match)) return null

    var openNode: TSNode? = null
    var closeNode: TSNode? = null
    for (capture in match.captures) {
        when (languageSpec.bracketsQuery.getCaptureNameForId(capture.index)) {
            TsBracketPairs.OPEN_NAME -> openNode = capture.node
            TsBracketPairs.CLOSE_NAME -> closeNode = capture.node
        }
    }
    val open = openNode ?: return null
    val close = closeNode ?: return null

    val openIndex = open.startByte / 2
    val openLength = (open.endByte - open.startByte) / 2
    val closeIndex = close.startByte / 2
    val closeLength = (close.endByte - close.startByte) / 2
    if (closeIndex + closeLength <= window.leftIndex) return null
    if (openIndex >= window.rightIndex) return null

    return PairedBracket(
        openIndex,
        openLength,
        closeIndex,
        closeLength,
        bracketDepth(treeCursor, rootNode, open, close)
    )
}
