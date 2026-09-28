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
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BracketColorizationProviderTest {
    @Test fun nestedPairsAreSortedAndStickyLinesAreQueriedSeparately() {
        val text = Content("{\n[\n()\n]\n}")
        val requested = ArrayList<Pair<Int, Int>>()
        val pairs = listOf(PairedBracket(0, 9, 0), PairedBracket(2, 7, 1), PairedBracket(4, 5, 2))
        val provider = BracketColorizationProvider().apply {
            bracketsProvider = object : BracketsProvider {
                override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? = null
                override fun queryPairedBracketsForRange(text: Content, leftRange: Long, rightRange: Long): List<PairedBracket> {
                    requested.add(IntPair.getFirst(leftRange) to IntPair.getFirst(rightRange))
                    return pairs // Include duplicates/ancestors to verify filtering and deduplication.
                }
            }
        }
        val scheme = EditorColorScheme()
        val patches = provider.buildPatches(text, scheme, 2, 2, intArrayOf(0, 0, 2)).getPatches()
        assertEquals(listOf(2 to 2, 0 to 0), requested)
        assertEquals(listOf(0, 2, 2), patches.map { it.startLine })
        assertEquals(listOf(0, 0, 1), patches.map { it.startColumn })
        assertEquals(patches.sorted(), patches)
        assertEquals(3, patches.size)
        requested.clear()
        val all = provider.buildPatches(text, scheme, 0, 4)
        assertEquals(6, all.getPatches().size)
        assertEquals(2, all.getPatchesOnLine(2).size)
        assertEquals(all.getPatches().sorted(), all.getPatches())
    }
}
