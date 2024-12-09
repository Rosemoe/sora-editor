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

package io.github.rosemoe.sora.langs.monarch

import io.github.dingyi222666.monarch.types.StandardTokenType
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.AutoClosingPairConditional
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.BaseAutoClosingPair
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import java.util.Arrays
import java.util.Locale

class MonarchSymbolPairMatch(
    private val language: MonarchLanguage,
) : SymbolPairMatch(DefaultSymbolPairs()) {


    var enabled = true
        set(value) {
            if (!value) {
                removeAllPairs()
            } else {
                updatePair()
            }
            field = value
        }

    init {
        updatePair()
    }


    fun updatePair() {
        if (!enabled) {
            return
        }

        val languageConfiguration: LanguageConfiguration =
            language.languageConfiguration
                ?: return

        removeAllPairs()

        val surroundingPairs =
            languageConfiguration.surroundingPairs ?: emptyList()

        val autoClosingPairs: List<AutoClosingPairConditional> =
            languageConfiguration.autoClosingPairs?.toMutableList() ?: emptyList()

        val pairs = mutableMapOf<String, SymbolPair>()

        autoClosingPairs.forEach {
            pairs[it.open] = SymbolPair(
                it.open,
                it.close,
                SymbolPairEx(it, true)
            )
        }

        for (surroundingPair in surroundingPairs) {
            val originAutoClosingPair = autoClosingPairs.find {
                it.open == surroundingPair.open && it.close == surroundingPair.close
            } as? AutoClosingPairConditional

            val surroundingPairNotInList = if (surroundingPair is AutoClosingPairConditional) {
                surroundingPair.notIn
            } else emptyList()


            if (originAutoClosingPair == null) {
                pairs[surroundingPair.open] = SymbolPair(
                    surroundingPair.open,
                    surroundingPair.close,
                    SymbolPairEx(surroundingPair, false)
                )
                continue
            }


            pairs.remove(surroundingPair.open)
            val pair = AutoClosingPairConditional(
                surroundingPair.open,
                surroundingPair.close,
                (surroundingPairNotInList + originAutoClosingPair.notIn).distinct(),
                true
            )

            pairs[pair.open] = SymbolPair(
                pair.open,
                pair.close,
                SymbolPairEx(pair, true)
            )
        }

        pairs.forEach {
            putPair(it.key, it.value)
        }

    }

    class SymbolPairEx(
        private val autoClosingPairConditional: BaseAutoClosingPair,
        private val isAutoClosingPair: Boolean
    ) :
        SymbolPair.SymbolPairEx {

        private var excludeTokenTypesArray: IntArray? = null

        private var isSurroundingPair = autoClosingPairConditional.isSurroundingPair

        init {
            run {
                val excludeTokenTypeList =
                    if (autoClosingPairConditional is AutoClosingPairConditional)
                        autoClosingPairConditional.notIn.toMutableList()
                    else emptyList()

                if (excludeTokenTypeList.isEmpty()) {
                    excludeTokenTypesArray = null
                }

                val excludeTokenTypesArray = IntArray(excludeTokenTypeList.size)

                for (i in excludeTokenTypesArray.indices) {
                    val excludeTokenName =
                        excludeTokenTypeList[i].lowercase(Locale.getDefault())

                    var excludeTokenType = StandardTokenType.String

                    when (excludeTokenName) {
                        "comment" -> excludeTokenType = StandardTokenType.Comment
                        "regex" -> excludeTokenType = StandardTokenType.RegEx
                    }
                    excludeTokenTypesArray[i] = excludeTokenType
                }

                Arrays.sort(excludeTokenTypesArray)

                this.excludeTokenTypesArray = excludeTokenTypesArray
            }
        }

        override fun shouldReplace(
            editor: CodeEditor,
            contentLine: ContentLine?,
            leftColumn: Int
        ): Boolean {
            if (editor.cursor.isSelected) {
                return isSurroundingPair
            }

            // No text was selected, so should not complete surrounding pair
            if (!isAutoClosingPair) {
                return false;
            }

            val excludedTokenTypes = excludeTokenTypesArray ?: return true

            val cursor = editor.cursor

            val currentLine = cursor.leftLine
            val currentColumn = cursor.leftColumn

            val spansOnCurrentLine = editor.getSpansForLine(currentLine)

            val currentSpan = binarySearchSpan(spansOnCurrentLine, currentColumn)
            val extra = currentSpan?.extra


            if (extra is Int) {
                val index = Arrays.binarySearch(
                    excludedTokenTypes,
                    extra
                )
                return index < 0
            }

            return true
        }

        private fun checkIndex(index: Int, max: Int): Int {
            return index.coerceIn(0, max)
        }

        private fun binarySearchSpan(spanList: List<Span>, column: Int): Span? {
            var left = 0
            var right = spanList.size - 1
            val size = spanList.size - 1

            while (left <= right) {
                val middle = (left + right) / 2

                val currentSpan = spanList[middle]

                if (currentSpan.column == column) {
                    return currentSpan
                }

                val nextIndex = checkIndex(middle + 1, right)
                val nextSpan = if (nextIndex <= right) spanList[nextIndex] else null
                if (nextSpan != null && nextSpan.column > column) {
                    return currentSpan
                }

                // if (currentSpan.column > column)
                val lastSpan = spanList[checkIndex(middle - 1, size)]

                if (lastSpan.column < column) {
                    return currentSpan
                }

                if (currentSpan.column < column) {
                    left = middle + 1
                } else {
                    right = middle - 1
                }
            }

            return null
        }


        override fun shouldDoAutoSurround(content: Content): Boolean {
            return isSurroundingPair && content.cursor.isSelected
        }
    }


}