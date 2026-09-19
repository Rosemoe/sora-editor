/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package io.github.rosemoe.sora.langs.textmate.brackets

import kotlin.math.abs

/**
 * Persistent concatenation of bracket trees.
 *
 * Nodes are never mutated, so joining two trees copies only the outer spine of the taller one and
 * shares everything else. Heights are kept in the 2-3 range, which bounds the spine length.
 */

/** Join [left] and [right] into 2..3 nodes of the same height. */
private fun join(left: BracketNode, right: BracketNode): List<BracketNode> {
    if (left.height == right.height) return listOf(left, right)
    val children = if (left.height > right.height) {
        left.children.dropLast(1) + join(left.children.last(), right)
    } else {
        join(left, right.children.first()) + right.children.drop(1)
    }
    return if (children.size <= 3) listOf(ListNode(children))
    else listOf(ListNode(children.take(2)), ListNode(children.drop(2)))
}

/** Concatenate two trees, possibly wrapping the result in a new root. */
internal fun concat(left: BracketNode, right: BracketNode): BracketNode {
    val nodes = join(left, right)
    return if (nodes.size == 1) nodes[0] else ListNode(nodes)
}

/** Build a balanced subtree out of same-height nodes, grouping 2 or 3 at a time. */
private fun balanceEqualHeight(nodes: List<BracketNode>): BracketNode {
    var level = nodes
    while (level.size > 1) {
        val parents = ArrayList<BracketNode>((level.size + 1) / 2)
        var index = 0
        while (index < level.size) {
            // Prefer a group of 3 when one node would otherwise be left over.
            val count = if (level.size - index == 3) 3 else 2
            parents.add(ListNode(level.subList(index, index + count).toList()))
            index += count
        }
        level = parents
    }
    return level[0]
}

/**
 * Balance a sequence of parsed items into a single tree, or `null` when it is empty.
 *
 * Equal-height runs are grouped first, then joined pairwise preferring the two trees with the
 * closest heights. This keeps the result balanced without a global sort.
 */
internal fun balance(nodes: List<BracketNode>): BracketNode? {
    if (nodes.isEmpty()) return null
    val runs = ArrayList<BracketNode>()
    var index = 0
    while (index < nodes.size) {
        val start = index++
        while (index < nodes.size && nodes[index].height == nodes[start].height) index++
        runs.add(balanceEqualHeight(nodes.subList(start, index)))
    }
    var first = runs[0]
    var second = runs.getOrNull(1) ?: return first
    for (next in runs.drop(2)) {
        if (abs(first.height - second.height) <= abs(second.height - next.height)) {
            first = concat(first, second)
            second = next
        } else {
            second = concat(second, next)
        }
    }
    return concat(first, second)
}
