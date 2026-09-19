/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

/**
 * Walks an old tree in document order while the parser advances through the new document.
 *
 * [read] answers "is there a reusable node starting exactly at this offset". Offsets are computed
 * on the way down, so a subtree that cannot contain the offset is rejected by a single length
 * comparison instead of being searched from the root again.
 */
internal class BracketTreeReader(root: BracketNode) {

    // The old tree is walked on every keystroke, so the depth-first stack is kept as two parallel
    // arrays: pushing a child must not allocate one stack entry per node.
    private var stackNodes = arrayOfNulls<BracketNode>(8)
    private var stackOffsets = LongArray(8)
    private var stackSize = 1

    init {
        stackNodes[0] = root
        stackOffsets[0] = TextOffset.ZERO.packed
    }

    /**
     * Offset of the node the reader would visit next, or `null` when the tree is exhausted. The
     * parser uses it to keep bounded text chunks aligned with the old tree.
     */
    val nextOffset: TextOffset?
        get() = if (stackSize == 0) null else TextOffset(stackOffsets[stackSize - 1])

    /**
     * Return the next node starting exactly at [offset] that [accept] allows, or `null` when the
     * tree has nothing to offer before that offset.
     */
    fun read(offset: TextOffset, accept: (BracketNode) -> Boolean): BracketNode? {
        while (stackSize != 0) {
            val top = stackSize - 1
            val node = stackNodes[top]!!
            val nodeOffset = TextOffset(stackOffsets[top])
            if (nodeOffset > offset) return null
            stackNodes[--stackSize] = null
            if (nodeOffset + node.length <= offset) continue
            if (nodeOffset == offset && accept(node)) return node

            // Push the children so that the first one ends up on top of the stack. The stack is
            // popped from the end, so they are written back to front.
            val children = node.children
            val required = stackSize + children.size
            if (required > stackNodes.size) {
                val capacity = maxOf(required, stackNodes.size * 2)
                stackNodes = stackNodes.copyOf(capacity)
                stackOffsets = stackOffsets.copyOf(capacity)
            }
            var childOffset = nodeOffset
            for (index in children.indices) {
                val child = children[index]
                val target = stackSize + children.lastIndex - index
                stackNodes[target] = child
                stackOffsets[target] = childOffset.packed
                childOffset += child.length
            }
            stackSize += children.size
        }
        return null
    }
}
