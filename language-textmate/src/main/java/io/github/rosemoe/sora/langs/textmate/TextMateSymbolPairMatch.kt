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
package io.github.rosemoe.sora.langs.textmate

import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.languageconfiguration.internal.model.AutoClosingPairConditional

class TextMateSymbolPairMatch(private val language: TextMateLanguage) :
    SymbolPairMatch(DefaultSymbolPairs()) {
    private var enabled = true

    init {
        updatePair()
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            removeAllPairs()
        } else {
            updatePair()
        }
    }


    fun updatePair() {
        if (!enabled) return
        val languageConfiguration = language.languageConfiguration ?: return

        removeAllPairs()
        val surroundingPairs = languageConfiguration.surroundingPairs
        val autoClosingPairs = languageConfiguration.autoClosingPairs

        // merge
        autoClosingPairs?.forEach { pair ->
            putPair(pair.open, SymbolPair(pair.open, pair.close, SymbolPairEx(pair)))
        }
        surroundingPairs?.forEach { pair ->
            val p = AutoClosingPairConditional(
                pair.open, pair.close,
                surroundingPairFlagWithList
            )
            putPair(p.open, SymbolPair(p.open, p.close, SymbolPairEx(p)))
        }
    }

    internal class SymbolPairEx(pair: AutoClosingPairConditional) : SymbolPair.SymbolPairEx {
        val notInTokenTypeArray: IntArray?

        var isSurroundingPair: Boolean = false

        init {
            val notInList = pair.notIn as? ArrayList<String>?
            notInTokenTypeArray = if (!notInList.isNullOrEmpty()) {
                if (notInList.remove(SURROUNDING_PAIR_FLAG)) {
                    isSurroundingPair = true
                }
                val newTokenTypeArray = IntArray(notInList.size) { i ->
                    val value = notInList[i].lowercase()
                    when (value) {
                        "comment" -> StandardTokenType.Comment
                        "regex" -> StandardTokenType.RegEx
                        else -> StandardTokenType.String
                    }
                }.apply { sort() }
                newTokenTypeArray.takeIf { it.isNotEmpty() }
            } else {
                null
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
            // No text was selected，so should not complete surrounding pair
            if (isSurroundingPair) {
                return false
            }

            if (notInTokenTypeArray == null) {
                return true
            }

            val cursor = editor.cursor

            val currentLine = cursor.leftLine
            val currentColumn = cursor.leftColumn

            val spansOnCurrentLine = editor.getSpansForLine(currentLine)

            val currentSpan = binarySearchSpan(spansOnCurrentLine, currentColumn)
            val extra = currentSpan?.getExtra()

            if (extra is Int) {
                return notInTokenTypeArray.binarySearch(extra) < 0
            }

            return true
        }

        private fun binarySearchSpan(spanList: MutableList<Span>, column: Int): Span? {
            var start = 0
            var end = spanList.lastIndex
            var middle: Int
            val size = spanList.lastIndex

            var currentSpan: Span? = null

            while (start <= end) {
                middle = (start + end) / 2

                currentSpan = spanList[middle]
                if (currentSpan.getColumn() == column) {
                    break
                }

                if (currentSpan.getColumn() < column) {
                    val nextSpan = spanList[Math.clamp((middle + 1).toLong(), 0, size)]

                    if (nextSpan.getColumn() > column) {
                        return currentSpan
                    }

                    start++

                    continue
                }

                // if (currentSpan.column > column)
                val previousSpan = spanList[Math.clamp((middle - 1).toLong(), 0, size)]

                if (previousSpan.getColumn() < column) {
                    return currentSpan
                }

                end--
            }

            return currentSpan
        }

        override fun shouldDoAutoSurround(content: Content): Boolean {
            return isSurroundingPair && content.getCursor().isSelected
        }
    }

    companion object {
        private const val SURROUNDING_PAIR_FLAG = "surroundingPair"
        private val surroundingPairFlagWithList = listOf<String>(SURROUNDING_PAIR_FLAG)
    }
}
