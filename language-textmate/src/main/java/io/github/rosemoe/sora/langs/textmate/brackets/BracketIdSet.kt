/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

import java.math.BigInteger

/**
 * A set of bracket type ids, stored as a bit mask.
 *
 * Two sets are used across the tree: a delimiter carries the ids it can open or close, and every
 * node carries the ids of openers it is still missing. Intersecting the two tells the parser whether
 * a closer can terminate the current node.
 */
@JvmInline
internal value class BracketIdSet(private val bits: BigInteger) {

    val isEmpty: Boolean
        get() = bits.signum() == 0

    /** Whether the two sets share at least one id. */
    fun intersects(other: BracketIdSet): Boolean = bits.and(other.bits).signum() != 0

    operator fun plus(id: Int): BracketIdSet = BracketIdSet(bits.setBit(id))

    operator fun plus(other: BracketIdSet): BracketIdSet = BracketIdSet(bits.or(other.bits))

    companion object {
        val EMPTY = BracketIdSet(BigInteger.ZERO)
    }
}
