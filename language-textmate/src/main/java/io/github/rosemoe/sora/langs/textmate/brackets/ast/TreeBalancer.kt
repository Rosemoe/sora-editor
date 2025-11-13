/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets.ast

import kotlin.math.abs

internal object TreeBalancer {

        fun balance(nodes: List<ASTNode>): ASTNode {
        return concat23Trees(nodes)
    }

        fun balanceSameHeight(nodes: List<ASTNode>, makeImmutable: Boolean = false): ASTNode {
        return concat23TreesOfSameHeight(nodes, makeImmutable)
    }

    private fun concat23Trees(nodes: List<ASTNode>): ASTNode {
        if (nodes.isEmpty()) return TextAST.EMPTY
        if (nodes.size == 1) return nodes.first()

        var index = 0

        fun readNode(): ASTNode? {
            if (index >= nodes.size) return null
            val start = index
            val height = nodes[start].listHeight
            index++
            while (index < nodes.size && nodes[index].listHeight == height) {
                index++
            }

            val count = index - start
            return if (count >= 2) {
                val slice = if (start == 0 && index == nodes.size) {
                    nodes
                } else {
                    nodes.subList(start, index)
                }
                concat23TreesOfSameHeight(slice, makeImmutable = false)
            } else {
                nodes[start]
            }
        }

        var first = readNode() ?: return TextAST.EMPTY
        var second = readNode() ?: return first

        while (true) {
            val next = readNode() ?: break
            if (heightDiff(first, second) <= heightDiff(second, next)) {
                first = concatTwo(first, second)
                second = next
            } else {
                second = concatTwo(second, next)
            }
        }

        return concatTwo(first, second)
    }

    private fun concat23TreesOfSameHeight(nodes: List<ASTNode>, makeImmutable: Boolean): ASTNode {
        if (nodes.isEmpty()) return TextAST.EMPTY
        if (nodes.size == 1) return nodes.first()

        val items = ArrayList(nodes)
        var length = items.size
        while (length > 3) {
            val newLength = length shr 1
            for (i in 0 until newLength) {
                val j = i shl 1
                val third = if (j + 3 == length) items[j + 2] else null
                items[i] = createListNode(items[j], items[j + 1], third, makeImmutable)
            }
            length = newLength
        }

        return createListNode(
            items[0],
            items[1],
            if (length >= 3) items[2] else null,
            makeImmutable
        )
    }

    private fun heightDiff(a: ASTNode, b: ASTNode): Int {
        return abs(a.listHeight - b.listHeight)
    }

    private fun concatTwo(node1: ASTNode, node2: ASTNode): ASTNode {
        return when {
            node1.listHeight == node2.listHeight -> createListNode(node1, node2, null, false)
            node1.listHeight > node2.listHeight -> append(node1, node2)
            else -> prepend(node2, node1)
        }
    }

    private fun append(tree: ASTNode, node: ASTNode): ASTNode {
        val listNode = tree as? IListNode ?: return createListNode(tree, node, null, false)
        val mutableRoot = listNode.toMutable() as ASTNode

        val parents = ArrayList<IListNode>()
        var current: ASTNode = mutableRoot

        while (node.listHeight != current.listHeight) {
            val curList = current as? IListNode
                ?: throw IllegalStateException("Expected list node, got ${current::class.simpleName}")
            parents.add(curList)
            current = curList.makeLastElementMutable() ?: break
        }

        var carry: ASTNode? = node
        for (i in parents.size - 1 downTo 0) {
            val parent = parents[i]
            if (carry != null) {
                if (parent.childCount >= 3) {
                    val removed = parent.unappendChild()
                    carry = createListNode(removed, carry!!, null, false)
                } else {
                    parent.appendChildOfSameHeight(carry!!)
                    carry = null
                }
            } else {
                parent.handleChildrenChanged()
            }
        }

        return if (carry != null) {
            createListNode(mutableRoot, carry!!, null, false)
        } else {
            mutableRoot
        }
    }

    private fun prepend(tree: ASTNode, node: ASTNode): ASTNode {
        val listNode = tree as? IListNode ?: return createListNode(node, tree, null, false)
        val mutableRoot = listNode.toMutable() as ASTNode

        val parents = ArrayList<IListNode>()
        var current: ASTNode = mutableRoot

        while (node.listHeight != current.listHeight) {
            val curList = current as? IListNode
                ?: throw IllegalStateException("Expected list node, got ${current::class.simpleName}")
            parents.add(curList)
            current = curList.makeFirstElementMutable() ?: break
        }

        var carry: ASTNode? = node
        for (i in parents.size - 1 downTo 0) {
            val parent = parents[i]
            if (carry != null) {
                if (parent.childCount >= 3) {
                    val removed = parent.unprependChild()
                    carry = createListNode(carry!!, removed, null, false)
                } else {
                    parent.prependChildOfSameHeight(carry!!)
                    carry = null
                }
            } else {
                parent.handleChildrenChanged()
            }
        }

        return if (carry != null) {
            createListNode(carry!!, mutableRoot, null, false)
        } else {
            mutableRoot
        }
    }

    private fun createListNode(
        first: ASTNode,
        second: ASTNode,
        third: ASTNode?,
        makeImmutable: Boolean
    ): ASTNode {
        val node = if (third != null) {
            TwoThreeListAST(first, second, third)
        } else {
            TwoThreeListAST(first, second)
        }
        return if (makeImmutable) node.makeImmutable() else node
    }
}
