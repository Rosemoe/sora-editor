/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets.ast

internal class NodeReader(node: ASTNode) {

    // Stack of nodes being traversed (from root to current)
    private val nextNodes = ArrayList<ASTNode>()

    // Stack of offsets for each node in nextNodes
    private val offsets = ArrayList<Length>()

    // Stack of child indices - idxs[i] is the index of nextNodes[i+1] within nextNodes[i]'s children
    private val idxs = ArrayList<Int>()

    // Last offset queried (for monotonicity check)
    private var lastOffset = Length.ZERO

    init {
        nextNodes.add(node)
        offsets.add(Length.ZERO)
    }

    fun readLongestNodeAt(
        offset: Length,
        predicate: (node: ASTNode) -> Boolean
    ): ASTNode? {
        if (offset < lastOffset) {
            throw IllegalArgumentException("Invalid offset: $offset < $lastOffset")
        }
        lastOffset = offset

        // Find the longest node of all those that are closest to the current offset
        while (true) {
            val curNode = nextNodes.lastOrNull()
            if (curNode == null) {
                return null
            }
            val curNodeOffset = offsets.last()

            when {
                // The next best node is not here yet
                // The reader must advance before a cached node is hit
                offset < curNodeOffset -> {
                    return null
                }

                // The reader is ahead of the current node
                curNodeOffset < offset -> {
                    // The reader is after the end of the current node
                    if (curNodeOffset + curNode.length <= offset) {
                        nextNodeAfterCurrent()
                    } else {
                        // The reader is somewhere in the current node
                        val nextChildIdx = getNextChildIdx(curNode)
                        if (nextChildIdx != -1) {
                            // Go to the first child and repeat
                            val child = getChild(curNode, nextChildIdx)!!
                            nextNodes.add(child)
                            offsets.add(curNodeOffset)
                            idxs.add(nextChildIdx)
                        } else {
                            // We don't have children
                            nextNodeAfterCurrent()
                        }
                    }
                }

                // offset == curNodeOffset
                else -> {
                    if (predicate(curNode)) {
                        nextNodeAfterCurrent()
                        return curNode
                    } else {
                        // Look for shorter node
                        val nextChildIdx = getNextChildIdx(curNode)
                        if (nextChildIdx == -1) {
                            // There is no shorter node. Advance so the same stale node is not
                            // reconsidered immediately (matching VSCode's implementation).
                            nextNodeAfterCurrent()
                            return null
                        } else {
                            // Descend into first child & repeat
                            val child = getChild(curNode, nextChildIdx)!!
                            nextNodes.add(child)
                            offsets.add(curNodeOffset)
                            idxs.add(nextChildIdx)
                        }
                    }
                }
            }
        }
    }

    private fun nextNodeAfterCurrent() {
        while (true) {
            val currentOffset = offsets.lastOrNull()
            val currentNode = nextNodes.lastOrNull()

            nextNodes.removeLastOrNull()
            offsets.removeLastOrNull()

            if (idxs.isEmpty()) {
                // We just popped the root node, there is no next node
                break
            }

            // Parent is not null, because idxs is not empty
            val parent = nextNodes.last()
            val lastIdx = idxs.last()
            val nextChildIdx = getNextChildIdx(parent, lastIdx)

            if (nextChildIdx != -1) {
                val child = getChild(parent, nextChildIdx)!!
                val nextOffset = currentOffset!! + currentNode!!.length
                nextNodes.add(child)
                offsets.add(nextOffset)
                idxs[idxs.size - 1] = nextChildIdx
                break
            } else {
                idxs.removeLastOrNull()
            }
            // We fully consumed the parent
            // Current node is now parent, so call nextNodeAfterCurrent again
        }
    }

    private fun getNextChildIdx(node: ASTNode, curIdx: Int = -1): Int {
        val nextIdx = curIdx + 1
        if (nextIdx >= node.childCount) {
            return -1
        }

        return nextIdx
    }

    private fun getChild(node: ASTNode, idx: Int): ASTNode? {
        return try {
            node.getChild(idx)
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }
}
