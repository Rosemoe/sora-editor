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

import com.itsaky.androidide.treesitter.TSInputEdit
import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSNode
import com.itsaky.androidide.treesitter.TSTree
import java.util.concurrent.locks.ReentrantLock

/**
 * Safe accessor for [TSTree] instances. single [TSTree] is not thread-safe. This class adds lock
 * to make thread-safe access to the tree.
 *
 * @author Rosemoe
 */
class SafeTsTree(private val tree: TSTree) : AutoCloseable {

    private val lock = ReentrantLock()

    /**
     * Close the original [TSTree]
     */
    override fun close() {
        accessTree { tree.close() }
    }

    /**
     * Make access to the tree. All access to the tree must be in the param [block].
     * The [TreeAccessor] object will be invalid immediately after the given [block] is executed.
     *
     * @return the result for executing the given [block]
     */
    fun <R> accessTree(block: (tree: TreeAccessor) -> R): R {
        lock.lock()
        try {
            val accessor = TreeAccessor()
            val result = block(accessor)
            accessor.accessible = false
            return result
        } finally {
            lock.unlock()
        }
    }

    /**
     * Make access to the tree if the tree is not currently closed.
     *
     * @see accessTree
     * @return whether the given [block] is executed
     */
    fun accessTreeIfAvailable(block: (tree: TreeAccessor) -> Unit): Boolean {
        if (!tree.canAccess()) {
            return false
        }
        return accessTree {
            if (!it.closed) {
                block(it)
                true
            } else {
                false
            }
        }
    }

    /**
     * Accessor for [TSTree]
     */
    inner class TreeAccessor(internal var accessible: Boolean = true) {

        /**
         * Check if the [TreeAccessor] can be used and the original tree is not closed.
         */
        private fun checkAccess() {
            if (closed)
                throw IllegalStateException("executing operation on dead accessor")
        }

        /**
         * Root node of the tree. This should not be stored outside the accessing block.
         *
         * @see TSTree.getRootNode
         */
        val rootNode: TSNode
            get() {
                checkAccess()
                return tree.rootNode
            }

        /**
         * Language of the tree
         *
         * @see TSTree.getLanguage
         */
        val language: TSLanguage
            get() {
                checkAccess()
                return tree.language
            }

        /**
         * If the [TreeAccessor] can be used and the original tree is not closed.
         */
        val closed: Boolean
            get() {
                return !accessible || !tree.canAccess()
            }

        /**
         * Copy a new [TSTree]
         */
        fun copy(): TSTree {
            checkAccess()
            return tree.copy()
        }

        /**
         * Edit the [TSTree]
         */
        fun edit(edit: TSInputEdit) {
            checkAccess()
            tree.edit(edit)
        }

    }

}