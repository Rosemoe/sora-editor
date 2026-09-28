/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.util.Locale

/**
 * Lookup table for the delimiters declared by a language configuration.
 *
 * Tokens are indexed by their lowercased text. A closer keeps the set of every bracket type it can
 * terminate, because several openers may share one closer: `{"brackets":[["begin","end"],["if","end"]]}`
 * makes `end` close both.
 */
internal class BracketTokens(configuration: LanguageConfiguration) {

    private val byFirstChar: Map<Char, List<Delimiter>>

    /**
     * True for every character that can start a delimiter. Scanning text with this table skips runs
     * of plain characters in bulk, instead of testing each one against [byFirstChar]. One bit per
     * code unit of the basic multilingual plane is a cheap price for that.
     */
    private val delimiterStarts = BooleanArray(Char.MAX_VALUE.code + 1)

    val isEmpty: Boolean
        get() = byFirstChar.isEmpty()

    init {
        val ordinary = configuration.brackets.orEmpty()
        // A missing colorizedBracketPairs means "colorize the ordinary pairs except angle brackets";
        // an explicitly empty list means "colorize nothing".
        val colorized = configuration.colorizedBracketPairs
            ?: ordinary.filterNot { it.open == "<" && it.close == ">" }
        val pairs = (ordinary + colorized).filter { it.open.isNotEmpty() && it.close.isNotEmpty() }

        val openIds = pairs.map { it.open.lowercase(Locale.ROOT) }.distinct().withIndex()
            .associate { it.value to it.index }

        val tokens = LinkedHashMap<String, Delimiter>()
        for ((text, id) in openIds) {
            tokens[text] = Delimiter(text, id, BracketIdSet.EMPTY + id)
        }
        for ((text, group) in pairs.groupBy { it.close.lowercase(Locale.ROOT) }) {
            val ids = group.fold(BracketIdSet.EMPTY) { bits, pair ->
                bits + openIds.getValue(pair.open.lowercase(Locale.ROOT))
            }
            val colorizedIds = colorized.filter { it.close.equals(text, true) }
                .fold(BracketIdSet.EMPTY) { bits, pair ->
                    openIds[pair.open.lowercase(Locale.ROOT)]?.let { bits + it } ?: bits
                }
            tokens[text] = Delimiter(text, null, ids, colorizedIds)
        }

        // Longest token first, so "begin" wins over a hypothetical "b".
        byFirstChar = tokens.values.groupBy { it.text[0] }
            .mapValues { (_, values) -> values.sortedByDescending { it.text.length } }
        byFirstChar.keys.forEach { delimiterStarts[it.code] = true }
    }

    /**
     * Find the delimiter starting at [column] that does not extend past [end], or `null`.
     */
    fun at(text: CharSequence, column: Int, end: Int): Delimiter? =
        byFirstChar[text[column].lowercaseChar()]?.firstOrNull { matches(text, column, end, it) }

    /**
     * Index of the first character at or after [column] that could start a delimiter, or [end] when
     * there is none before it.
     */
    fun nextCandidate(text: CharSequence, column: Int, end: Int): Int {
        var index = column
        while (index < end && !delimiterStarts[text[index].lowercaseChar().code]) index++
        return index
    }

    private fun matches(text: CharSequence, column: Int, end: Int, token: Delimiter): Boolean {
        val length = token.text.length
        if (column + length > end) return false
        if (!text.regionMatches(column, token.text, 0, length, true)) return false
        // Word brackets must not match inside a longer identifier.
        if (isWordEdge(token.text.first()) && column > 0 && isWord(text[column - 1])) return false
        if (isWordEdge(token.text.last()) && column + length < text.length && isWord(text[column + length])) {
            return false
        }
        return true
    }

    private fun isWord(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_'

    private fun isWordEdge(c: Char) = isWord(c) || c == ' '
}
