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
import com.itsaky.androidide.treesitter.TSTreeCursor
import kotlin.math.min

/**
 * Nesting depth of a bracket pair, taken as the shallower of its two brackets.
 *
 * A pair can sit at a different depth from each side when the tree is not perfectly nested, and the
 * shallower side is what the colorization expects.
 */
internal fun bracketDepth(
    treeCursor: TSTreeCursor,
    rootNode: TSNode,
    openNode: TSNode,
    closeNode: TSNode
): Int = min(
    depthEnclosing(treeCursor, rootNode, openNode),
    depthEnclosing(treeCursor, rootNode, closeNode)
)

/**
 * Move [treeCursor] onto the deepest node enclosing [node] and report the depth it landed at.
 */
private fun depthEnclosing(treeCursor: TSTreeCursor, rootNode: TSNode, node: TSNode): Int {
    treeCursor.reset(rootNode)
    treeCursor.gotoNodeEnclosingRange(node.startByte, node.endByte, false)
    return treeCursor.depth
}

/**
 * Position the cursor on the deepest node enclosing `[startByte, endByte]`.
 *
 * Adapted from tree-sitter's `ts_tree_cursor_goto_first_child_for_byte` traversal. When
 * [requireLarger] is set, a node exactly as large as the query does not count as enclosing.
 *
 * @return whether such a node was found
 */
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
            // Empty range: step back over a sibling that starts after the query.
            if (rangeStart > startByte && gotoPreviousSibling()) continue
        } else if (rangeEnd == startByte && gotoNextSibling()) {
            continue
        }

        val encloses = rangeStart <= startByte &&
            rangeEnd >= endByte &&
            (!requireLarger || rangeEnd - rangeStart > queryLength)

        if (!encloses) {
            ascending = true
            if (!gotoParent()) return false
            continue
        }

        if (ascending) return true
        if (gotoFirstChildForByte(startByte) >= 0) continue
        return true
    }
}
