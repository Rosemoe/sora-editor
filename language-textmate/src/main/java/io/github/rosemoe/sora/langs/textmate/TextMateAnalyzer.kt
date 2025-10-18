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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager.LineTokenizeResult
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.OnlineBracketsMatcher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete.SyncIdentifiers
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.langs.textmate.folding.FoldingHelper
import io.github.rosemoe.sora.langs.textmate.folding.IndentRange
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentLine
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.ArrayList
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.EncodedTokenAttributes
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult
import org.eclipse.tm4e.core.internal.oniguruma.OnigString
import org.eclipse.tm4e.core.internal.theme.FontStyle
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.time.Duration
import java.util.Collections

class TextMateAnalyzer(
    private val language: TextMateLanguage,
    private val grammar: IGrammar,
    private val configuration: LanguageConfiguration?,  /* GrammarRegistry grammarRegistry,*/
//private final GrammarRegistry grammarRegistry;
    private val themeRegistry: ThemeRegistry
) : AsyncIncrementalAnalyzeManager<MyState, Span>(), FoldingHelper,
    ThemeRegistry.ThemeChangeListener {
    private var theme: Theme? = themeRegistry.currentThemeModel.theme

    private var cachedRegExp: OnigRegExp? = null
    private var foldingOffside = false
    private var bracketsProvider: BracketsProvider? = null
    @JvmField
    val syncIdentifiers = SyncIdentifiers()


    init {
        themeRegistry.addListener(this)

        if (configuration != null) {
            val pairs = configuration.brackets
            if (!pairs.isNullOrEmpty()) {
                var size = pairs.size
                for (pair in pairs) {
                    if (pair.open.length != 1 || pair.close.length != 1) {
                        size--
                    }
                }
                val pairArr = CharArray(size * 2)
                var i = 0
                for (pair in pairs) {
                    if (pair.open.length != 1 || pair.close.length != 1) {
                        continue
                    }
                    pairArr[i * 2] = pair.open[0]
                    pairArr[i * 2 + 1] = pair.close[0]
                    i++
                }
                bracketsProvider = OnlineBracketsMatcher(pairArr, 100000)
            }
        }
        createFoldingExp()
    }

    private fun createFoldingExp() {
        val markers = configuration?.folding ?: return
        foldingOffside = markers.offSide
        cachedRegExp = OnigRegExp("(" + markers.markersStart + ")|(?:" + markers.markersEnd + ")")
    }

    override fun getInitialState(): MyState? {
        return null
    }

    override fun stateEquals(state: MyState?, another: MyState?): Boolean {
        if (state == null && another == null) {
            return true
        }
        if (state != null && another != null) {
            return state.tokenizeState == another.tokenizeState
        }
        return false
    }

    override fun getIndentFor(line: Int): Int {
        return getState(line).state?.indent ?: 0
    }

    override fun getResultFor(line: Int): OnigResult? {
        return getState(line).state?.foldingCache
    }

    override fun computeBlocks(
        text: Content,
        delegate: CodeBlockAnalyzeDelegate
    ): MutableList<CodeBlock?> {
        val list = ArrayList<CodeBlock>()
        analyzeCodeBlocks(text, list, delegate)
        if (delegate.isNotCancelled) {
            withReceiver { r: StyleReceiver ->
                r.updateBracketProvider(
                    this,
                    bracketsProvider
                )
            }
        }
        return list
    }

    fun analyzeCodeBlocks(
        model: Content,
        blocks: ArrayList<CodeBlock>,
        delegate: CodeBlockAnalyzeDelegate
    ) {
        if (cachedRegExp == null) {
            return
        }
        try {
            val foldingRegions = IndentRange.computeRanges(
                model,
                language.tabSize,
                foldingOffside,
                this,
                cachedRegExp,
                delegate
            )
            blocks.ensureCapacity(foldingRegions.length())
            var i = 0
            while (i < foldingRegions.length() && delegate.isNotCancelled) {
                val startLine = foldingRegions.getStartLineNumber(i)
                val endLine = foldingRegions.getEndLineNumber(i)
                if (startLine != endLine) {
                    val codeBlock = CodeBlock()
                    codeBlock.toBottomOfEndLine = true
                    codeBlock.startLine = startLine
                    codeBlock.endLine = endLine

                    // It's safe here to use raw data because the Content is only held by this thread
                    val length = model.getColumnCount(startLine)
                    val chars = model.getLine(startLine).backingCharArray

                    codeBlock.startColumn = IndentRange.computeStartColumn(chars, length, language.tabSize)
                    codeBlock.endColumn = codeBlock.startColumn
                    blocks.add(codeBlock)
                }
                i++
            }
            Collections.sort(blocks, CodeBlock.COMPARATOR_END)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        getManagedStyles().isIndentCountMode = true
    }

    @SuppressLint("NewApi")
    @Synchronized
    override fun tokenizeLine(
        lineC: CharSequence,
        state: MyState?,
        lineIndex: Int
    ): LineTokenizeResult<MyState, Span> {
        val line = if (lineC is ContentLine) lineC.toStringWithNewline() else lineC.toString()
        val tokens = arrayListOf<Span>()
        val lineTokens = grammar.tokenizeLine2(
            line,
            state?.tokenizeState,
            Duration.ofSeconds(2)
        )
        val tokensLength = lineTokens.tokens.size / 2
        val identifiers = arrayListOf<String>()
        for (i in 0..<tokensLength) {
            val startIndex = line.offsetByCodePoints(0, lineTokens.tokens[2 * i])
            if (i == 0 && startIndex != 0) {
                tokens.add(SpanFactory.obtain(0, EditorColorScheme.TEXT_NORMAL.toLong()))
            }
            val metadata = lineTokens.tokens[2 * i + 1]
            val foreground = EncodedTokenAttributes.getForeground(metadata)
            val fontStyle = EncodedTokenAttributes.getFontStyle(metadata)
            val tokenType = EncodedTokenAttributes.getTokenType(metadata)
            if (language.createIdentifiers) {
                if (tokenType == StandardTokenType.Other) {
                    val end = if (i + 1 == tokensLength) {
                        lineC.length
                    } else {
                        line.offsetByCodePoints(0, lineTokens.tokens[2 * (i + 1)])
                    }
                    if (end > startIndex && MyCharacter.isJavaIdentifierStart(line[startIndex])) {
                        val flag = line.substring(startIndex + 1, end).none { !MyCharacter.isJavaIdentifierPart(it) }
                        if (flag) {
                            identifiers.add(line.substring(startIndex, end))
                        }
                    }
                }
            }
            val span = SpanFactory.obtain(
                startIndex,
                TextStyle.makeStyle(
                    foreground + 255,
                    0,
                    fontStyle and FontStyle.Bold != 0,
                    fontStyle and FontStyle.Italic != 0,
                    false
                )
            ).apply { extra = tokenType }

            if (fontStyle and FontStyle.Underline != 0) {
                val color = theme?.getColor(foreground)
                if (color != null) {
                    span.setUnderlineColor(Color.parseColor(color))
                }
            }
            tokens.add(span)
        }
        val foldingCache = cachedRegExp?.search(OnigString.of(line), 0)
        val indentLevel = IndentRange.computeIndentLevel(
            (lineC as ContentLine).backingCharArray,
            line.length - 1,
            language.tabSize
        )
        val state = MyState(lineTokens.getRuleStack(), foldingCache, indentLevel, identifiers)
        return LineTokenizeResult<MyState, Span>(state, null, tokens)
    }

    override fun onAddState(state: MyState) {
        super.onAddState(state)
        if (language.createIdentifiers) {
            for (identifier in state.identifiers) {
                syncIdentifiers.identifierIncrease(identifier)
            }
        }
    }

    override fun onAbandonState(state: MyState) {
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
        themeRegistry.removeListener(this)
    }

    override fun generateSpansForLine(tokens: LineTokenizeResult<MyState, Span>): List<Span>? {
        return null
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        this.theme = newTheme.theme
    }
}
