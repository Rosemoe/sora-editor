/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets.ast

import java.util.BitSet

@JvmInline
internal value class AnchorSet private constructor(private val bits: BitSet) {

    operator fun contains(bracketId: Int): Boolean {
        return bits.get(bracketId)
    }

    operator fun plus(bracketId: Int): AnchorSet {
        return ASTObjectPool.withBitSet { newBits ->
            newBits.or(bits)
            newBits.set(bracketId)
            AnchorSet(newBits.clone() as BitSet)
        }
    }

    operator fun minus(bracketId: Int): AnchorSet {
        return ASTObjectPool.withBitSet { newBits ->
            newBits.or(bits)
            newBits.clear(bracketId)
            AnchorSet(newBits.clone() as BitSet)
        }
    }

    fun union(other: AnchorSet): AnchorSet {
        if (this.isEmpty()) return other
        if (other.isEmpty()) return this

        return ASTObjectPool.withBitSet { newBits ->
            newBits.or(bits)
            newBits.or(other.bits)
            AnchorSet(newBits.clone() as BitSet)
        }
    }

    fun intersects(other: AnchorSet): Boolean {
        return ASTObjectPool.withBitSet { intersection ->
            intersection.or(bits)
            intersection.and(other.bits)
            !intersection.isEmpty
        }
    }

    fun isEmpty(): Boolean = bits.isEmpty

    fun size(): Int = bits.cardinality()

    override fun toString(): String {
        val ids = mutableListOf<Int>()
        var i = bits.nextSetBit(0)
        while (i >= 0) {
            ids.add(i)
            i = bits.nextSetBit(i + 1)
        }
        return "AnchorSet($ids)"
    }

    companion object {
        val EMPTY = AnchorSet(BitSet())

        fun of(bracketId: Int): AnchorSet {
            val bits = BitSet()
            bits.set(bracketId)
            return AnchorSet(bits)
        }

        fun of(vararg bracketIds: Int): AnchorSet {
            val bits = BitSet()
            for (id in bracketIds) {
                bits.set(id)
            }
            return AnchorSet(bits)
        }
    }
}
