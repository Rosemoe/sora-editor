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

package io.github.rosemoe.sora.langs.monarch.languageconfiguration.support

import io.github.dingyi222666.regex.GlobalRegexLib
import io.github.dingyi222666.regex.Regex
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.CharacterPair
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.EnterAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.IndentAction
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.OnEnterRule
import io.github.rosemoe.sora.langs.monarch.utils.escapeRegExpCharacters
import io.github.rosemoe.sora.langs.monarch.utils.matchesPartially
import java.util.regex.Pattern

// See https://github.com/microsoft/vscode/blob/aa31bfc9fd1746626b3efe86f41b9c172d5f4d23/src/vs/editor/common/languages/supports/onEnter.ts#


data class ProcessedBracketPair(
    val open: String,
    val close: String,
    val openRegExp: Regex,
    val closeRegExp: Regex
)

class OnEnterSupport(
    brackets: List<CharacterPair>?,
    onEnterRules: List<OnEnterRule>?
) {

    private val brackets: List<ProcessedBracketPair>
    private val regExpRules: List<OnEnterRule>

    init {
        val brackets = brackets ?: listOf(
            CharacterPair("(", ")"),
            CharacterPair("[", "]"),
            CharacterPair("{", "}")
        )

        val processedBracketPairs = mutableListOf<ProcessedBracketPair>()

        brackets.forEach {
            val openRegExp = createOpenBracketRegExp(it.first)
            val closeRegExp = createCloseBracketRegExp(it.second)
            if (openRegExp != null && closeRegExp != null) {
                processedBracketPairs.add(
                    ProcessedBracketPair(
                        it.first,
                        it.second,
                        openRegExp,
                        closeRegExp
                    )
                )
            }
        }


        this.brackets = processedBracketPairs
        this.regExpRules = onEnterRules ?: emptyList()
    }

    fun onEnter(
        // TODO autoIndent: EditorAutoIndentStrategy,
        previousLineText: String?,
        beforeEnterText: String,
        afterEnterText: String
    ): EnterAction? {
        // (1): `regExpRules`
        // if (autoIndent >= EditorAutoIndentStrategy.Advanced) {
        for ((beforeText, afterTextPattern, previousLinePattern, action) in regExpRules) {
            if (!beforeText.matchesPartially(beforeEnterText)) continue

            if (afterTextPattern != null && !afterTextPattern.matchesPartially(afterEnterText)) continue

            if (previousLinePattern != null && !previousLinePattern.matchesPartially(
                    previousLineText ?: ""
                )
            ) continue

            return action
        }

        // (2): Special indent-outdent
        // if (autoIndent >= EditorAutoIndentStrategy.Brackets) {
        if (beforeEnterText.isNotEmpty() && afterEnterText.isNotEmpty()) {
            for (bracket in brackets) {
                if (bracket.openRegExp.matches(beforeEnterText) && bracket.closeRegExp.matches(
                        afterEnterText
                    )
                ) {
                    return EnterAction(indentAction = IndentAction.IndentOutdent)
                }
            }
        }

        // (3): Open bracket based logic
        // if (autoIndent >= EditorAutoIndentStrategy.Brackets) {
        if (beforeEnterText.isNotEmpty()) {
            for (bracket in brackets) {
                if (bracket.openRegExp.matches(beforeEnterText)) {
                    return EnterAction(IndentAction.Indent)
                }
            }
        }

        return null
    }

    companion object {

        // native pattern is faster that the regexp pattern
        val B_REGEXP = Pattern.compile("\\B") //$NON-NLS-1$

        private fun createOpenBracketRegExp(bracket: String): Regex? {
            val string = StringBuilder(bracket.escapeRegExpCharacters())
            val firstChar = string.first().toString()
            if (!B_REGEXP.matcher(
                    firstChar
                ).find()
            ) {
                string.insert(0, "\\b") //$NON-NLS-1$
            }
            string.append("\\s*$") //$NON-NLS-1$
            return GlobalRegexLib.compile(string)
        }

        private fun createCloseBracketRegExp(bracket: String): Regex? {
            val string = StringBuilder(bracket.escapeRegExpCharacters())
            val firstChar = string.last().toString()
            if (!B_REGEXP.matcher(
                    firstChar
                ).find()
            ) {
                string.insert(0, "\\b") //$NON-NLS-1$
            }
            string.append("\\s*$") //$NON-NLS-1$
            return GlobalRegexLib.compile(string)
        }


    }
}