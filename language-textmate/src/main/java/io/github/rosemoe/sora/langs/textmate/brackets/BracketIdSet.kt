/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
