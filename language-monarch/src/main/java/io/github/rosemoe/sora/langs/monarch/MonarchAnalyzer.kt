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

import android.os.Bundle
import io.github.dingyi222666.monarch.types.ITokenizationSupport
import io.github.dingyi222666.regex.GlobalRegexLib
import io.github.dingyi222666.regex.MatchResult
import io.github.dingyi222666.regex.Regex
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager.LineTokenizeResult
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.OnlineBracketsMatcher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete.SyncIdentifiers
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.langs.monarch.folding.FoldingHelper
import io.github.rosemoe.sora.langs.monarch.folding.IndentRange
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration
import io.github.rosemoe.sora.langs.monarch.registery.ThemeChangeListener
import io.github.rosemoe.sora.langs.monarch.registery.ThemeRegistry
import io.github.rosemoe.sora.langs.monarch.registery.model.ThemeModel
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.ArrayList

class MonarchAnalyzer(
    private val language: MonarchLanguage,
    private val tokenization: ITokenizationSupport,
    private val languageConfiguration: LanguageConfiguration? = null,
) : AsyncIncrementalAnalyzeManager<MonarchState, Span>(), FoldingHelper,
    ThemeChangeListener {

    private var cachedFoldingRegExp: Regex? = null
    private var foldingOffside = false
    private var bracketsProvider: BracketsProvider? = null
    private val syncIdentifiers = SyncIdentifiers()
    private var theme = ThemeRegistry.currentTheme


    init {
        if (!ThemeRegistry.hasListener(this)) {
            ThemeRegistry.addListener(this)
        }

        if (languageConfiguration != null) {
            val pairs = languageConfiguration.brackets
            if (!pairs.isNullOrEmpty()) {
                val filteredPairs = pairs.filter { it.first.length == 1 && it.second.length == 1 }
                val pairArr = CharArray(filteredPairs.size * 2) { index ->
                    val pairIndex = index / 2
                    when (index % 2) {
                        0 -> filteredPairs[pairIndex].first[0]
                        else -> filteredPairs[pairIndex].second[0]
                    }
                }
                bracketsProvider = OnlineBracketsMatcher(pairArr, 100000)
            }

            createFoldingExp()
        }

    }

    private fun createFoldingExp() {
        if (languageConfiguration == null) {
            return
        }
        val markers = languageConfiguration.folding ?: return
        foldingOffside = markers.offSide == true
        cachedFoldingRegExp =
            GlobalRegexLib.compile("(" + markers.markers?.start + ")|(?:" + markers.markers?.end + ")")
    }


    override fun getInitialState(): MonarchState {
        return MonarchState(
            tokenizeState = tokenization.getInitialState(),
            foldingCache = MatchResult("", IntRange.EMPTY, emptyArray()),
            indent = 0,
            identifiers = emptyList()
        )
    }

    override fun stateEquals(state: MonarchState?, another: MonarchState?): Boolean {
        if (state == null && another == null) {
            return true
        }
        if (state != null && another != null) {
            return state.tokenizeState == another.tokenizeState
        }
        return false
    }

    override fun getIndentFor(line: Int): Int {
        return getState(line).state.indent
    }

    override fun getResultFor(line: Int): MatchResult {
        return getState(line).state.foldingCache
    }


    override fun computeBlocks(
        text: Content,
        delegate: CodeBlockAnalyzeDelegate
    ): MutableList<CodeBlock> {
        val list = ArrayList<CodeBlock>()
        analyzeCodeBlocks(text, list, delegate)
        if (delegate.isNotCancelled) {
            withReceiver {
                it.updateBracketProvider(
                    this,
                    bracketsProvider
                )
            }
        }
        return list
    }

    private fun analyzeCodeBlocks(
        model: Content,
        blocks: ArrayList<CodeBlock>,
        delegate: CodeBlockAnalyzeDelegate
    ) {
        val cachedFoldingRegExp = cachedFoldingRegExp ?: return

        runCatching {
            val foldingRegions = IndentRange.computeRanges(
                model, language.tabSize, foldingOffside,
                this, cachedFoldingRegExp, delegate
            )
            blocks.ensureCapacity(foldingRegions.length)

            for (i in foldingRegions.indices) {
                if (delegate.isCancelled) {
                    break
                }

                val foldingStartLine = foldingRegions.getStartLineNumber(i)
                val foldingEndLine = foldingRegions.getEndLineNumber(i)

                if (foldingStartLine == foldingEndLine) {
                    continue
                }

                val codeBlock = CodeBlock().apply {
                    toBottomOfEndLine = true
                    startLine = foldingStartLine
                    endLine = foldingEndLine
                }

                // It's safe here to use raw data because the Content is only held by this thread
                val length = model.getColumnCount(foldingStartLine)
                val chars = model.getLine(foldingStartLine).backingCharArray

                codeBlock.startColumn =
                    IndentRange.computeStartColumn(
                        chars,
                        length,
                        language.tabSize
                    )
                codeBlock.endColumn = codeBlock.startColumn
                blocks.add(codeBlock)

            }

        }.onFailure {
            it.printStackTrace()
        }
        managedStyles.isIndentCountMode = true
    }


    override fun tokenizeLine(
        line: CharSequence?,
        state: MonarchState?,
        lineIndex: Int
    ): LineTokenizeResult<MonarchState, Span> {
        TODO("Not yet implemented")
    }

    override fun onAddState(state: MonarchState) {
        super.onAddState(state)
        if (language.createIdentifiers) {
            for (identifier in state.identifiers) {
                syncIdentifiers.identifierIncrease(identifier)
            }
        }
    }

    override fun onAbandonState(state: MonarchState) {
        super.onAbandonState(state)
        if (language.createIdentifiers) {
            for (identifier in state.identifiers) {
                syncIdentifiers.identifierDecrease(identifier)
            }
        }
    }

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        super.reset(content, extraArguments)
        syncIdentifiers.clear()
    }

    override fun destroy() {
        super.destroy()
        ThemeRegistry.removeListener(this)
    }

    override fun generateSpansForLine(tokens: LineTokenizeResult<MonarchState, Span>): List<Span>? {
        return null
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        this.theme = newTheme
    }

}
