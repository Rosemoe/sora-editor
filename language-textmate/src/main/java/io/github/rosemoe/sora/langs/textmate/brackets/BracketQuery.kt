/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

/**
 * Collect the pairs whose opening or closing delimiter overlaps `[start, end]`.
 *
 * Every node stores relative lengths, so the walk only ever adds lengths together: a subtree that
 * cannot touch the range is rejected by one comparison, and lists that are completely off-screen
 * are never flattened.
 */
internal fun BracketNode.query(start: TextOffset, end: TextOffset): List<BracketPair> =
    BracketQuery(start, end).collect(this)

private class BracketQuery(private val start: TextOffset, private val end: TextOffset) {

    private val result = ArrayList<BracketPair>()

    /**
     * Number of colorized openers seen on the current path, per bracket type. This is what tells
     * `[ { [ } ]` and `[ [ ] ]` apart when the color of a pair is chosen.
     */
    private val colorizedOpeners = HashMap<Int, Int>()

    fun collect(root: BracketNode): List<BracketPair> {
        visit(root, TextOffset.ZERO, 0)
        return result
    }

    private fun visit(node: BracketNode, offset: TextOffset, level: Int) {
        if (!touches(offset, node.length)) return
        when (node) {
            is ListNode -> visitList(node, offset, level)
            is PairNode -> visitPair(node, offset, level)
            is UnexpectedNode -> result.add(
                BracketPair(offset, node.bracket.text.length, offset, 0, level - 1, 0, true, false)
            )

            is TextNode, is Delimiter -> Unit
        }
    }

    private fun visitList(node: ListNode, offset: TextOffset, level: Int) {
        var childOffset = offset
        for (child in node.children) {
            if (childOffset > end) break
            visit(child, childOffset, level)
            childOffset += child.length
        }
    }

    private fun visitPair(node: PairNode, offset: TextOffset, level: Int) {
        val id = node.open.openingId!!
        val equalTypeLevel = colorizedOpeners[id] ?: 0
        val bodyStart = offset + node.open.length
        val closeStart = bodyStart + (node.body?.length ?: TextOffset.ZERO)
        val close = node.close

        val openTouches = touches(offset, node.open.length)
        val closeTouches = close != null && touches(closeStart, close.length)
        if (openTouches || closeTouches) {
            result.add(
                BracketPair(
                    offset,
                    node.open.text.length,
                    closeStart,
                    close?.text?.length ?: 0,
                    level,
                    equalTypeLevel,
                    close == null,
                    node.colorized
                )
            )
        }

        val increment = if (node.colorized) 1 else 0
        colorizedOpeners[id] = equalTypeLevel + increment
        node.body?.let { visit(it, bodyStart, level + increment) }
        colorizedOpeners[id] = equalTypeLevel
    }

    /** Whether a node of [length] starting at [offset] overlaps `[start, end]`. */
    private fun touches(offset: TextOffset, length: TextOffset): Boolean =
        offset <= end && offset + length >= start
}
