/*
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
 */
package io.github.rosemoe.sora.langs.textmate.brackets.ast

import java.util.BitSet

internal object ASTObjectPool {

    // Thread-local pools to avoid synchronization
    private val arrayListPool = initThreadLocal { ArrayListPool<Any>() }
    private val bitSetPool = initThreadLocal { BitSetPool() }

    @Suppress("UNCHECKED_CAST")
    fun <T> obtainArrayList(): ArrayList<T> {
        return arrayListPool.get()!!.obtain() as ArrayList<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> recycleArrayList(list: ArrayList<T>) {
        arrayListPool.get()!!.recycle(list as ArrayList<Any>)
    }

    inline fun <T,R> withArrayList(block: (ArrayList<T>) -> R): R {
        val list = obtainArrayList<T>()
        try {
            return block(list)
        } finally {
            recycleArrayList(list)
        }
    }

    fun obtainBitSet(): BitSet {
        return bitSetPool.get()!!.obtain()
    }

    fun recycleBitSet(bitSet: BitSet) {
        bitSetPool.get()!!.recycle(bitSet)
    }

    inline fun <R> withBitSet(block: (BitSet) -> R): R {
        val bitSet = obtainBitSet()
        try {
            return block(bitSet)
        } finally {
            recycleBitSet(bitSet)
        }
    }
}

private class ArrayListPool<T> {
    private val pool = ArrayList<ArrayList<T>>(MAX_POOL_SIZE)

    @Suppress("UNCHECKED_CAST")
    fun obtain(): ArrayList<T> {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.lastIndex)
        } else {
            ArrayList(DEFAULT_CAPACITY)
        }
    }

    fun recycle(list: ArrayList<T>) {
        if (pool.size < MAX_POOL_SIZE) {
            list.clear()
            // Trim capacity if too large to avoid holding too much memory
            if (list.capacity() > MAX_CAPACITY_TO_KEEP) {
                return  // Don't pool large lists
            }
            pool.add(list)
        }
    }

    private fun ArrayList<*>.capacity(): Int {
        // Estimate capacity (actual capacity is not directly accessible)
        return this.size
    }

    companion object {
        private const val MAX_POOL_SIZE = 256
        private const val DEFAULT_CAPACITY = 16
        private const val MAX_CAPACITY_TO_KEEP = 256
    }
}

private class BitSetPool {
    private val pool = ArrayList<BitSet>(MAX_POOL_SIZE)

    fun obtain(): BitSet {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.lastIndex)
        } else {
            BitSet(DEFAULT_SIZE)
        }
    }

    fun recycle(bitSet: BitSet) {
        if (pool.size < MAX_POOL_SIZE) {
            bitSet.clear()
            // Don't pool very large BitSets
            if (bitSet.size() > MAX_SIZE_TO_KEEP) {
                return
            }
            pool.add(bitSet)
        }
    }

    companion object {
        private const val MAX_POOL_SIZE = 256
        private const val DEFAULT_SIZE = 64  // Most bracket sets are small
        private const val MAX_SIZE_TO_KEEP = 512
    }
}

inline fun <T> initThreadLocal(crossinline R: () -> T): ThreadLocal<T> {
    return object : ThreadLocal<T>() {
        override fun get(): T? = R()
    }
}
