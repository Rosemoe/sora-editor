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
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import kotlin.math.max

/**
 * Point query for the bracket pair under a cursor.
 *
 * Only the two characters around the cursor are searched, which keeps this cheap even on very large
 * documents. Tree-sitter reports byte offsets, hence the `* 2` scaling of a UTF-16 index.
 */
internal fun findBracketPairAt(
    safeTree: SafeTsTree,
    languageSpec: TsLanguageSpec,
    text: Content,
    index: Int
): PairedBracket? {
    val query = languageSpec.bracketsQuery
    if (!query.canAccess() || query.patternCount < 1) return null
    return TSQueryCursor.create().use { cursor ->
        cursor.isAllowChangedNodes = true
        cursor.setByteRange(max(0, index - 1) * 2, index * 2 + 1)
        safeTree.accessTree { tree ->
            if (tree.closed) return@accessTree null
            val rootNode = tree.rootNode
            if (!rootNode.canAccess()) return@accessTree null
            cursor.exec(query, rootNode)
            firstPairCovering(cursor, languageSpec, text, index)
        }
    }
}

/**
 * The pair of the first match whose brackets cover [index].
 *
 * A covering match with only one capture yields `null` rather than falling through to later
 * matches, so the result is always a pair that really contains the cursor.
 */
private fun firstPairCovering(
    cursor: TSQueryCursor,
    languageSpec: TsLanguageSpec,
    text: Content,
    index: Int
): PairedBracket? {
    var match = cursor.nextMatch()
    while (match != null) {
        if (languageSpec.bracketsPredicator.doPredicate(languageSpec.predicates, text, match)) {
            val captures = bracketCaptures(match, languageSpec)
            if (captures.covers(index)) return captures.toPair()
        }
        match = cursor.nextMatch()
    }
    return null
}

/** The opening and closing bracket captured by one match. Either may be absent. */
private class BracketCaptures(val open: TSNode?, val close: TSNode?) {

    fun covers(index: Int): Boolean = open.covers(index) || close.covers(index)

    fun toPair(): PairedBracket? {
        val opening = open ?: return null
        val closing = close ?: return null
        return PairedBracket(
            opening.startByte / 2,
            (opening.endByte - opening.startByte) / 2,
            closing.startByte / 2,
            (closing.endByte - closing.startByte) / 2
        )
    }

    private fun TSNode?.covers(index: Int): Boolean =
        this != null && index >= startByte / 2 && index <= endByte / 2
}

private fun bracketCaptures(match: TSQueryMatch, languageSpec: TsLanguageSpec): BracketCaptures {
    var open: TSNode? = null
    var close: TSNode? = null
    for (capture in match.captures) {
        when (languageSpec.bracketsQuery.getCaptureNameForId(capture.index)) {
            TsBracketPairs.OPEN_NAME -> open = capture.node
            TsBracketPairs.CLOSE_NAME -> close = capture.node
        }
    }
    return BracketCaptures(open, close)
}
