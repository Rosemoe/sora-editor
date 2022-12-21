/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryError
import com.itsaky.androidide.treesitter.TSQueryMatch
import com.itsaky.androidide.treesitter.TSQueryPredicateStep
import io.github.rosemoe.sora.editor.ts.predicate.PredicateResult
import io.github.rosemoe.sora.editor.ts.predicate.TsClientPredicateStep
import io.github.rosemoe.sora.editor.ts.predicate.TsPredicate
import io.github.rosemoe.sora.editor.ts.predicate.builtin.MatchPredicate
import java.io.Closeable

/**
 * Language specification for tree-sitter highlighter. This specification covers language code
 * parsing, highlighting captures and local variable tracking descriptions.
 *
 * Note that you must use ASCII characters in your scm sources. Otherwise, an [IllegalArgumentException] is
 * thrown.
 * Be careful that this should be closed to avoid native memory leaks.
 * Note that, client predicates are applied only to highlighting scm source.
 *
 * @author Rosemoe
 * @param language The tree-sitter language instance to be used for parsing
 * @param highlightScmSource The scm source code for highlighting tree nodes
 * @param codeBlocksScmSource The scm source for capturing code blocks.
 *                          All captured nodes are considered to be a code block.
 *                          Capture named with '.marked' suffix will have its last terminal node's start position as its scope end
 * @param bracketsScmSource The scm source for capturing brackets. Capture named 'open' and 'close' are used to compute bracket pairs
 * @param localsScmSource The scm source code for tracking local variables
 * @param localsCaptureSpec Custom specification for locals scm file
 * @param predicates Client custom predicate implementations
 */
class TsLanguageSpec(
    val language: TSLanguage,
    highlightScmSource: String,
    codeBlocksScmSource: String = "",
    bracketsScmSource: String = "",
    localsScmSource: String = "",
    localsCaptureSpec: LocalsCaptureSpec = LocalsCaptureSpec.DEFAULT,
    val predicates: List<TsPredicate> = listOf(MatchPredicate)
) : Closeable {

    /**
     * The generated scm source code for querying
     */
    val querySource = localsScmSource + "\n" + highlightScmSource

    /**
     * Offset of highlighting scm source code in [querySource]
     */
    val highlightScmOffset = localsScmSource.encodeToByteArray().size + 1

    /**
     * The actual [TSQuery] object
     */
    val tsQuery = TSQuery(language, querySource)

    /**
     * The first index of highlighting pattern
     */
    val highlightPatternOffset: Int

    /**
     * Indices of variable definition patterns
     */
    val localsDefinitionIndices = mutableListOf<Int>()

    /**
     * Indices of variable reference patterns
     */
    val localsReferenceIndices = mutableListOf<Int>()

    /**
     * Indices of variable scope patterns
     */
    val localsScopeIndices = mutableListOf<Int>()

    /**
     * Indices of variable definition-value patterns. Currently unused in analysis.
     */
    val localsDefinitionValueIndices = mutableListOf<Int>()

    /**
     * Predicates for patterns
     */
    val patternPredicates = mutableListOf<List<TsClientPredicateStep>>()

    val blocksQuery = TSQuery(language, codeBlocksScmSource)

    val bracketsQuery = TSQuery(language, bracketsScmSource)

    /**
     * Close flag
     */
    var closed = false
        private set

    init {
        querySource.forEach {
            if (it > 0xFF.toChar()) {
                throw IllegalArgumentException("use non-ASCII characters in scm source is unexpected")
            }
        }
        if (tsQuery.errorType != TSQueryError.None) {
            val region = if (tsQuery.errorOffset < highlightScmOffset) "locals" else "highlight"
            val offset = if (tsQuery.errorOffset < highlightScmOffset) tsQuery.errorOffset else tsQuery.errorOffset - highlightScmOffset
            throw IllegalArgumentException("bad scm sources: error ${tsQuery.errorType.name} occurs in $region range at offset $offset")
        }
        var highlightOffset = 0
        for (i in 0 until tsQuery.patternCount) {
            patternPredicates.add(tsQuery.getPredicatesForPattern(i).map {
                when (it.type) {
                    TSQueryPredicateStep.Type.String -> TsClientPredicateStep(it.type, tsQuery.getStringValueForId(it.valueId))
                    TSQueryPredicateStep.Type.Capture -> TsClientPredicateStep(it.type, tsQuery.getCaptureNameForId(it.valueId))
                    else -> TsClientPredicateStep(it.type, "")
                }
            })
        }
        for (i in 0 until tsQuery.captureCount) {
            if (tsQuery.getStartByteForPattern(i) < highlightScmOffset) {
                highlightOffset ++
                // Only locals in localsScm are taken down
                val name = tsQuery.getCaptureNameForId(i)
                if (localsCaptureSpec.isDefinitionCapture(name)) {
                    localsDefinitionIndices.add(i)
                } else if (localsCaptureSpec.isReferenceCapture(name)) {
                    localsReferenceIndices.add(i)
                } else if (localsCaptureSpec.isScopeCapture(name)) {
                    localsScopeIndices.add(i)
                } else if (localsCaptureSpec.isDefinitionValueCapture(name)) {
                    localsDefinitionValueIndices.add(i)
                }
            }
        }
        highlightPatternOffset = highlightOffset
    }

    fun doPredicate(text: CharSequence, match: TSQueryMatch) : Boolean {
        val description = patternPredicates[match.patternIndex]
        for (predicate in predicates) {
           when (predicate.doPredicate(tsQuery, text, match, description)) {
               PredicateResult.ACCEPT -> return true
               PredicateResult.REJECT -> return false
               else -> {}
           }
        }
        return true
    }

    override fun close() {
        tsQuery.close()
        blocksQuery.close()
        bracketsQuery.close()
        closed = true
    }

}