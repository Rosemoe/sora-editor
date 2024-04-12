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

package io.github.rosemoe.sora.langs.monarch.languageconfiguration.model

import io.github.dingyi222666.regex.Regex

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
    val onEnterRules: List<OnEnterRule>? = null,
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
