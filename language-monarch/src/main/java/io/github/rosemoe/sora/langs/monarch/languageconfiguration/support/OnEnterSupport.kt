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

package io.github.rosemoe.sora.langs.monarch.languageconfiguration.support

import io.github.dingyi222666.regex.Regex
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.CharacterPair
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.OnEnterRule

// See https://github.com/microsoft/vscode/blob/aa31bfc9fd1746626b3efe86f41b9c172d5f4d23/src/vs/editor/common/languages/supports/onEnter.ts#

data class OnEnterSupportOptions(
    val brackets: List<CharacterPair>?,
    val onEnterRules: List<OnEnterRule>?
)

data class ProcessedBracketPair(
    val open: String,
    val close: String,
    val openRegExp: Regex,
    val closeRegExp: Regex
)

class OnEnterSupport(
    options: OnEnterSupportOptions
) {

    private val brackets: List<ProcessedBracketPair>
    private val regExpRules: List<OnEnterRule>

    init {
        val brackets = options.brackets ?: listOf(
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
        this.regExpRules = options.onEnterRules ?: emptyList()
    }

    companion object {
        private fun createOpenBracketRegExp(bracket: String): Regex? {
            TODO("")
        }

        private fun createCloseBracketRegExp(bracket: String): Regex? {
            TODO("")
        }
    }
}