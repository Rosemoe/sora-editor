/*******************************************************************************
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.languageconfiguration.model

import com.squareup.moshi.JsonClass
import io.github.dingyi222666.regex.Regex

@JsonClass(generateAdapter = false)
data class LanguageConfiguration(
    /**
     * The language's comment settings.
     */
    val comments: CommentRule? = null,
    /**
     * The language's brackets.
     * This configuration implicitly affects pressing Enter around these brackets.
     */
    val brackets: List<CharacterPair>? = null,
    /**
     * The language's word definition.
     * If the language supports Unicode identifiers (e.g. JavaScript), it is preferable
     * to provide a word definition that uses exclusion of known separators.
     * e.g.: A regex that matches anything except known separators (and dot is allowed to occur in a floating point number):
     *   /(-?\d*\.\d\w*)|([^\`\~\!\@\#\%\^\&\*\(\)\-\=\+\[\{\]\}\\\|\;\:\'\"\,\.\<\>\/\?\s]+)/g
     */
    val wordPattern: Regex? = null,
    /**
     * The language's indentation settings.
     */
    val indentationRules: IndentationRule? = null,
    /**
     * The language's rules to be evaluated when pressing Enter.
     */
    val onEnterRules: List<OnEnterRule> = listOf(),
    /**
     * The language's auto closing pairs. The 'close' character is automatically inserted with the
     * 'open' character is typed. If not set, the configured brackets will be used.
     */
    val autoClosingPairs: List<AutoClosingPairConditional>? = null,
    /**
     * The language's surrounding pairs. When the 'open' character is typed on a selection, the
     * selected string is surrounded by the open and close characters. If not set, the autoclosing pairs
     * settings will be used.
     */
    val surroundingPairs: List<BaseAutoClosingPair>? = null,
    /**
     * Defines a list of bracket pairs that are colorized depending on their nesting level.
     * If not set, the configured brackets will be used.
     */
    val colorizedBracketPairs: List<CharacterPair>? = null,
    /**
     * Defines what characters must be after the cursor for bracket or quote autoclosing to occur when using the \'languageDefined\' autoclosing setting.
     *
     * This is typically the set of characters which can not start an expression, such as whitespace, closing brackets, non-unary operators, etc.
     */
    val autoCloseBefore: String? = null,

    /**
     * The language's folding rules.
     */
    val folding: FoldingRules? = null
)

typealias CharacterPair = Pair<String, String>
