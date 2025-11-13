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
import com.itsaky.androidide.treesitter.string.UTF16String
import io.github.rosemoe.sora.lang.brackets.CachedBracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import kotlin.math.max

class TsBracketPairs(
    private val safeTree: SafeTsTree,
    private val localString: UTF16String,
    private val languageSpec: TsLanguageSpec
) : CachedBracketsProvider() {

    private val cachedBracketPairs = mutableListOf<BracketPair>()

    companion object {
        const val OPEN_NAME = "editor.brackets.open"
        const val CLOSE_NAME = "editor.brackets.close"
        const val BRACKET_PAIR_COLORIZATION_LIMIT = 6000 * 300
    }

    override fun computePairedBracketAt(text: Content, index: Int): PairedBracket? {
        if (languageSpec.bracketsQuery.canAccess() && languageSpec.bracketsQuery.patternCount > 0) {
            TSQueryCursor.create().use { cursor ->
                cursor.setByteRange(max(0, index - 1) * 2, index * 2 + 1)

                return safeTree.accessTree { tree ->
                    if (tree.closed) return@accessTree null
                    val rootNode = tree.rootNode
                    if (!rootNode.canAccess() || rootNode.hasChanges()) {
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
        return null
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
        if (!query.canAccess() || query.patternCount == 0) {
            return null
        }
        val leftIndex = text.safeCharIndex(
            IntPair.getFirst(leftRange),
            IntPair.getSecond(leftRange)
        )
        val rightIndex = leftIndex.coerceAtLeast(
            text.safeCharIndex(
                IntPair.getFirst(rightRange),
                IntPair.getSecond(rightRange)
            )
        )

        if (cachedBracketPairs.isEmpty()) {
            return emptyList()
        }

        val bracketEnds = ArrayDeque<Int>()
        val result = mutableListOf<PairedBracket>()

        for (pair in cachedBracketPairs) {
            // Remove brackets that close before this one opens
            while (bracketEnds.isNotEmpty() && bracketEnds.last() <= pair.openStart) {
                bracketEnds.removeLast()
            }

            val depth = bracketEnds.size
            bracketEnds.addLast(pair.closeEnd)

            val openIndex = pair.openStart / 2
            val closeIndex = pair.closeStart / 2
            val openLength = (pair.openEnd - pair.openStart) / 2
            val closeLength = (pair.closeEnd - pair.closeStart) / 2

            // Only include brackets visible in the range
            if (closeIndex + closeLength > leftIndex && openIndex < rightIndex) {
                result.add(
                    PairedBracket(
                        openIndex,
                        openLength,
                        closeIndex,
                        closeLength,
                        depth
                    )
                )
            }
        }

        return if (result.isEmpty()) emptyList() else result
    }

    internal fun computeBracketPairs() = safeTree.accessTree { tree ->
        if (tree.closed) {
            return@accessTree
        }
        val rootNode = tree.rootNode
        if (!rootNode.canAccess() || rootNode.hasChanges()) {
            return@accessTree
        }
        val query = languageSpec.bracketsQuery
        if (!query.canAccess() || query.patternCount == 0) {
            return@accessTree
        }

        TSQueryCursor.create().use { cursor ->
            cursor.exec(query, rootNode)

            var match = cursor.nextMatch()
            while (match != null) {
                if (languageSpec.bracketsPredicator.doPredicate(
                        languageSpec.predicates,
                        localString,
                        match
                    )
                ) {
                    var openNode: TSNode? = null
                    var closeNode: TSNode? = null

                    for (capture in match.captures) {
                        val captureName = query.getCaptureNameForId(capture.index)
                        when (captureName) {
                            OPEN_NAME -> openNode = capture.node
                            CLOSE_NAME -> closeNode = capture.node
                        }
                    }

                    if (openNode != null && closeNode != null) {
                        cachedBracketPairs.add(
                            BracketPair(
                                openNode.startByte,
                                openNode.endByte,
                                closeNode.startByte,
                                closeNode.endByte
                            )
                        )
                    }
                }
                match = cursor.nextMatch()
            }
        }

        cachedBracketPairs.sortBy { it.openStart }
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


    // Collect all bracket pairs
    data class BracketPair(
        val openStart: Int,
        val openEnd: Int,
        val closeStart: Int,
        val closeEnd: Int
    )
}
