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

import io.github.rosemoe.sora.lang.brackets.CachedBracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content

/**
 * Bracket pair provider backed by the `brackets` tree-sitter query of [languageSpec].
 *
 * The query must capture the opening bracket as [OPEN_NAME] and its partner as [CLOSE_NAME].
 * Pairs are computed on demand and cached by [CachedBracketsProvider].
 *
 * @author Rosemoe
 */
class TsBracketPairs @JvmOverloads constructor(
    private val safeTree: SafeTsTree,
    private val languageSpec: TsLanguageSpec,
    private val documentVersion: Long = -1
) : CachedBracketsProvider() {

    override fun isReadyFor(text: Content): Boolean =
        (documentVersion == -1L || documentVersion == text.documentVersion) &&
            safeTree.accessTree { !it.closed }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? =
        if (isReadyFor(text)) super.getPairedBracketAt(text, index) else null

    override fun queryPairedBracketsForRange(text: Content, leftRange: Long, rightRange: Long): List<PairedBracket>? =
        if (isReadyFor(text)) super.queryPairedBracketsForRange(text, leftRange, rightRange) else null

    companion object {
        const val OPEN_NAME = "editor.brackets.open"
        const val CLOSE_NAME = "editor.brackets.close"

        /** Documents larger than this skip bracket colorization to bound the query cost. */
        const val BRACKET_PAIR_COLORIZATION_LIMIT = 60000 * 300
    }

    /** Find the pair whose bracket encloses [index]. */
    override fun computePairedBracketAt(text: Content, index: Int): PairedBracket? =
        findBracketPairAt(safeTree, languageSpec, text, index)

    /**
     * Find every pair overlapping `[leftRange, rightRange]`.
     *
     * @return `null` when the language has no bracket query at all
     */
    override fun computePairedBracketsForRange(
        text: Content,
        leftRange: Long,
        rightRange: Long
    ): List<PairedBracket>? {
        if (text.length > BRACKET_PAIR_COLORIZATION_LIMIT) return emptyList()
        if (languageSpec.bracketsQuery.patternCount == 0) return null
        return collectBracketPairsInRange(safeTree, languageSpec, text, leftRange, rightRange)
    }
}
