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
import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryCursor
import com.itsaky.androidide.treesitter.TSQueryMatch
import com.itsaky.androidide.treesitter.TSTreeCursor
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.CachedBracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import kotlin.math.max
import kotlin.math.min

class TsBracketPairs(
    private val safeTree: SafeTsTree,
    private val languageSpec: TsLanguageSpec
) : CachedBracketsProvider() {

    companion object {
        const val OPEN_NAME = "editor.brackets.open"
        const val CLOSE_NAME = "editor.brackets.close"
        const val BRACKET_PAIR_COLORIZATION_LIMIT = 60000 * 300
    }

    override fun computePairedBracketAt(text: Content, index: Int): PairedBracket? {
        if (!languageSpec.bracketsQuery.canAccess() || languageSpec.bracketsQuery.patternCount < 1) {
            return null
        }
        return TSQueryCursor.create().use { cursor ->
            cursor.isAllowChangedNodes = true
            cursor.setByteRange(max(0, index - 1) * 2, index * 2 + 1)

            safeTree.accessTree { tree ->
                if (tree.closed) return@accessTree null
                val rootNode = tree.rootNode
                if (!rootNode.canAccess()) {
                    return@accessTree null
                }
                cursor.exec(languageSpec.bracketsQuery, rootNode)
                var match = cursor.nextMatch()
                var matched = false
                val pos = IntArray(4)
                while (match != null && !matched) {
                    if (languageSpec.bracketsPredicator.doPredicate(
                            languageSpec.predicates,
                            text,
                            match
                        )
                    ) {
                        pos.fill(-1)
                        for (capture in match.captures) {
                            val captureName =
                                languageSpec.bracketsQuery.getCaptureNameForId(capture.index)
                            if (captureName == OPEN_NAME || captureName == CLOSE_NAME) {
                                val node = capture.node
                                if (index >= node.startByte / 2 && index <= node.endByte / 2) {
                                    matched = true
                                }
                                if (captureName == OPEN_NAME) {
                                    pos[0] = node.startByte
                                    pos[1] = node.endByte
                                } else {
                                    pos[2] = node.startByte
                                    pos[3] = node.endByte
                                }
                            }
                        }
                        if (matched && pos[0] != -1 && pos[2] != -1) {
                            return@accessTree PairedBracket(
                                pos[0] / 2,
                                (pos[1] - pos[0]) / 2,
                                pos[2] / 2,
                                (pos[3] - pos[2]) / 2
                            )
                        }
                    }
                    match = cursor.nextMatch()
                }
                null
            }

        }
    }

    override fun computePairedBracketsForRange(
        text: Content,
        leftRange: Long,
        rightRange: Long
    ): List<PairedBracket>? {

        if (text.length > BRACKET_PAIR_COLORIZATION_LIMIT) {
            return emptyList()
        }

        val query = languageSpec.bracketsQuery
        if (query.patternCount == 0) {
            return null
        }

        val window = buildQueryWindow(text, leftRange, rightRange) ?: return emptyList()

        val pairs = safeTree.accessTree { tree ->
            if (tree.closed) {
                return@accessTree null
            }
            val rootNode = tree.rootNode
            if (!rootNode.canAccess()) {
                return@accessTree null
            }
            collectBracketPairs(rootNode, text, query, window)
        } ?: return null
        return filterWindowPairs(pairs, window)
    }

    private data class QueryWindow(
        val leftIndex: Int,
        val rightIndex: Int,
        val startByte: Int,
        val endByte: Int,
        val rangeStartLine: Int,
        val rangeEndLine: Int
    )

    private fun buildQueryWindow(text: Content, leftRange: Long, rightRange: Long): QueryWindow? {
        val lineCount = text.lineCount
        if (lineCount == 0) {
            return null
        }

        val leftLine = IntPair.getFirst(leftRange)
        val leftColumn = IntPair.getSecond(leftRange)
        val rightLine = IntPair.getFirst(rightRange)
        val rightColumn = IntPair.getSecond(rightRange)

        val leftIndex = text.safeCharIndex(leftLine, leftColumn)
        val rightIndex = leftIndex.coerceAtLeast(text.safeCharIndex(rightLine, rightColumn))

        val startLine = (leftLine - 2).coerceAtLeast(0)
        val endLine = (rightLine + 2).coerceAtMost(lineCount - 1)
        val startIndex = text.safeCharIndex(startLine, 0)
        val endIndex = text.safeCharIndex(endLine, text.getColumnCount(endLine))
        val startByte = max(0, startIndex * 2)
        val endByte = max(startByte, endIndex * 2)

        return QueryWindow(leftIndex, rightIndex, startByte, endByte, leftLine, rightLine)
    }

    private fun filterWindowPairs(
        pairs: List<PairedBracket>,
        window: QueryWindow
    ): List<PairedBracket> {
        if (pairs.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<PairedBracket>()
        for (pair in pairs) {
            val rightEnd = pair.rightIndex + pair.rightLength
            if (pair.leftIndex < window.rightIndex && rightEnd > window.leftIndex) {
                result.add(pair)
            }
        }
        if (result.isEmpty()) {
            return emptyList()
        }
        return result
    }


    private fun collectBracketPairs(
        rootNode: TSNode,
        text: Content,
        query: TSQuery,
        window: QueryWindow
    ): List<PairedBracket> {
        val treeCursor = TSTreeCursor.create(rootNode)
        val result = mutableListOf<PairedBracket>()
        try {
            TSQueryCursor.create().use { cursor ->
                cursor.isAllowChangedNodes = true
                cursor.setByteRange(window.startByte, window.endByte)
                cursor.exec(query, rootNode)
                var match = cursor.nextMatch()
                while (match != null) {
                    processMatch(match, text, query, window, treeCursor, rootNode)?.let {
                        result.add(it)
                    }
                    match = cursor.nextMatch()
                }
            }
        } finally {
            treeCursor.close()
        }

        if (result.isEmpty()) {
            return emptyList()
        }
        result.sortBy { it.leftIndex }
        return result
    }

    private fun processMatch(
        match: TSQueryMatch,
        text: Content,
        query: TSQuery,
        window: QueryWindow,
        treeCursor: TSTreeCursor,
        rootNode: TSNode
    ): PairedBracket? {
        if (!languageSpec.bracketsPredicator.doPredicate(
                languageSpec.predicates,
                text,
                match
            )
        ) {
            return null
        }

        var openNode: TSNode? = null
        var closeNode: TSNode? = null

        for (capture in match.captures) {
            val captureName = query.getCaptureNameForId(capture.index)
            when (captureName) {
                OPEN_NAME -> openNode = capture.node
                CLOSE_NAME -> closeNode = capture.node
            }
        }

        if (openNode == null || closeNode == null) {
            return null
        }

        return buildPairedBracket(openNode, closeNode, window, treeCursor, rootNode)
    }

    private fun buildPairedBracket(
        openNode: TSNode,
        closeNode: TSNode,
        window: QueryWindow,
        treeCursor: TSTreeCursor,
        rootNode: TSNode
    ): PairedBracket? {
        val openIndex = openNode.startByte / 2
        val closeIndex = closeNode.startByte / 2
        val openLength = (openNode.endByte - openNode.startByte) / 2
        val closeLength = (closeNode.endByte - closeNode.startByte) / 2

        if (closeIndex + closeLength <= window.leftIndex || openIndex >= window.rightIndex) {
            return null
        }

        val depth = computeBracketDepth(treeCursor, rootNode, openNode, closeNode)

        return PairedBracket(
            openIndex,
            openLength,
            closeIndex,
            closeLength,
            depth
        )
    }

    private fun computeBracketDepth(
        treeCursor: TSTreeCursor,
        rootNode: TSNode,
        openNode: TSNode,
        closeNode: TSNode
    ): Int {
        val openDepth = treeCursor.depthForRange(rootNode, openNode.startByte, openNode.endByte)
        val closeDepth = treeCursor.depthForRange(rootNode, closeNode.startByte, closeNode.endByte)
        return min(openDepth, closeDepth)
    }

    private fun TSTreeCursor.depthForRange(rootNode: TSNode, startByte: Int, endByte: Int): Int {
        reset(rootNode)
        if (gotoNodeEnclosingRange(startByte, endByte, false)) {
            return depth
        }
        return depth
    }

    private fun TSTreeCursor.gotoNodeEnclosingRange(
        startByte: Int,
        endByte: Int,
        requireLarger: Boolean
    ): Boolean {
        var ascending = false
        val queryLength = (endByte - startByte).coerceAtLeast(0)

        while (true) {
            val node = currentNode
            val rangeStart = node.startByte
            val rangeEnd = node.endByte

            if (startByte == endByte) {
                if (rangeStart > startByte && gotoPreviousSibling()) {
                    continue
                }
            } else if (rangeEnd == startByte && gotoNextSibling()) {
                continue
            }

            val encloses = rangeStart <= startByte &&
                rangeEnd >= endByte &&
                (!requireLarger || rangeEnd - rangeStart > queryLength)

            if (!encloses) {
                ascending = true
                if (!gotoParent()) {
                    return false
                }
                continue
            }

            if (ascending) {
                return true
            }

            if (gotoFirstChildForByte(startByte) >= 0) {
                continue
            }

            return true
        }
    }

    private fun Content.safeCharIndex(line: Int, column: Int): Int {
        val lineCount = this.lineCount
        if (line < 0) {
            return 0
        }
        if (line >= lineCount) {
            return length
        }
        val safeColumn = when {
            column < 0 -> 0
            column > this.getColumnCount(line) -> this.getColumnCount(line)
            else -> column
        }
        return try {
            indexer.getCharIndex(line, safeColumn)
        } catch (_: IndexOutOfBoundsException) {
            length
        }
    }
}
