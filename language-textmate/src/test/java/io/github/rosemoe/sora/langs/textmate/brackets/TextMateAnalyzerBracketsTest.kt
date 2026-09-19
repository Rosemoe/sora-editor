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
package io.github.rosemoe.sora.langs.textmate.brackets

import android.os.Bundle
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.langs.textmate.TextMateAnalyzer
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.Registry
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TextMateAnalyzerBracketsTest {
    private lateinit var language: TextMateLanguage
    private val results = LinkedBlockingQueue<Result>()
    private val styleUpdates = LinkedBlockingQueue<Boolean>()
    @Volatile private var verifyFresh = false
    @Volatile private var provider: TextMateBracketsProvider? = null
    private data class Result(val root: BracketNode, val scanned: Int, val reused: Int, val expected: List<BracketPair>?) {
        val pairs get() = root.query(TextOffset.ZERO, TextOffset.MAX)
    }
    private val configuration = LanguageConfiguration.load(
        """{"brackets":[["{","}"],["[","]"],["(",")"]]}""".reader())!!

    @Before fun setup() {
        // Only the token categories relevant to brackets; no external language or theme assets.
        val grammar = Registry().addGrammar(IGrammarSource.fromString(IGrammarSource.ContentType.JSON, """
            {"scopeName":"source.bracket-test","patterns":[
                {"name":"comment.block","begin":"/\\*","end":"\\*/"},
                {"name":"comment.line","match":"//.*"},
                {"name":"string.quoted.double","begin":"\"","end":"\"",
                 "patterns":[{"match":"\\\\.","name":"constant.character.escape"}]}
            ]}
        """))
        language = object : TextMateLanguage(grammar, configuration, GrammarRegistry(null), ThemeRegistry(), false) {}
        language.setBracketPairColorization(true)
        language.analyzeManager.setReceiver(object : StyleReceiver {
            private var completedBlocks: Any? = null

            override fun updateStyles(source: AnalyzeManager, styles: Styles, range: StyleUpdateRange) {
                // Tokenization now publishes intermediate batches. Compare only settled revisions,
                // identified by the completed block pass, with a fresh full token-aware parse.
                if (completedBlocks != null && styles.blocks !== completedBlocks) setStyles(source, styles)
            }

            override fun setStyles(source: AnalyzeManager, styles: Styles?) {
                if (styles != null) {
                    completedBlocks = styles.blocks
                    provider?.let { p -> p.root?.let {
                        val fresh = if (verifyFresh) BracketTokenizer((source as TextMateAnalyzer).managedContent,
                            styles.spans, BracketTokens(configuration))
                            .use { tokens -> BracketParser(tokens).parse().query(TextOffset.ZERO, TextOffset.MAX) } else null
                        results.put(Result(it, p.scannedCharacters, p.reusedNodes, fresh))
                    } }
                    styleUpdates.put(provider != null)
                }
            }
            override fun setStyles(source: AnalyzeManager, styles: Styles?, action: Runnable?) { action?.run(); setStyles(source, styles) }
            override fun setDiagnostics(source: AnalyzeManager, diagnostics: DiagnosticsContainer?) = Unit
            override fun setInlayHints(source: AnalyzeManager, inlayHints: InlayHintsContainer?) = Unit
            override fun updateBracketProvider(source: AnalyzeManager, provider: BracketsProvider?) {
                this@TextMateAnalyzerBracketsTest.provider = provider as? TextMateBracketsProvider
                // The initial tree is published before tokens and blocks.
                if (provider is TextMateBracketsProvider) completedBlocks = null
            }
        })
    }
    @After fun teardown() { language.destroy() }
    private fun awaitResult(): Result {
        val result = results.poll(30, TimeUnit.SECONDS) ?: error("Analyzer did not publish brackets")
        result.expected?.let { assertEquals("Incremental AST differs from fresh token-aware parse", it, result.pairs) }
        return result
    }
    private fun initialize(text: Content): Result {
        language.analyzeManager.reset(ContentReference(text), Bundle())
        return awaitResult()
    }
    private fun insert(text: Content, index: Int, inserted: String) {
        val start = text.indexer.getCharPosition(index).fromThis()
        text.insert(start.line, start.column, inserted)
        val end = text.indexer.getCharPosition(index + inserted.length).fromThis()
        language.analyzeManager.insert(start, end, inserted)
    }
    private fun delete(text: Content, startIndex: Int, endIndex: Int) {
        val start = text.indexer.getCharPosition(startIndex).fromThis()
        val end = text.indexer.getCharPosition(endIndex).fromThis()
        val removed = text.subSequence(startIndex, endIndex).toString()
        text.delete(start.line, start.column, end.line, end.column)
        language.analyzeManager.delete(start, end, removed)
    }

    @Test fun actualTypingReusesNodesOnLongLinesIncludingPlainText() {
        for (source in listOf("class X { void m() { " + "{} ".repeat(20_000) + "} }", "x".repeat(100_000))) {
            val text = Content(source)
            initialize(text)
            val middle = text.length / 2
            insert(text, middle, "x")
            val changed = awaitResult()
            assertTrue("Actual analyzer scanned ${changed.scanned} characters", changed.scanned < 2048)
            assertTrue("No subtree reuse", changed.reused > 0)
            println("actual typing: length=${text.length}, scanned=${changed.scanned}, reused=${changed.reused}")
            language.analyzeManager.rerun()
            assertEquals(awaitResult().pairs, changed.pairs)
        }
    }

    @Test fun queuedMultilineEditsAndCommentPropagationMatchRerun() {
        verifyFresh = true
        val text = Content("class X {\n" + "void m() { int[] a = new int[1]; }\n".repeat(30) + "}\n")
        val initial = initialize(text)
        insert(text, 0, "/*")
        assertTrue(awaitResult().pairs.isEmpty())
        delete(text, 0, 2)
        assertEquals(initial.pairs, awaitResult().pairs)
        repeat(30) { insert(text, text.length / 2, if (it % 3 == 0) "\n{([])}\n" else "x") }
        var last = awaitResult()
        val end = TextOffset.of(text.lineCount - 1, text.getColumnCount(text.lineCount - 1))
        while (last.root.length != end) last = awaitResult()
        language.analyzeManager.rerun()
        assertEquals(awaitResult().pairs, last.pairs)
        val index = text.toString().indexOf("\n")
        delete(text, index, index + 8)
        val deleted = awaitResult()
        language.analyzeManager.rerun()
        assertEquals(awaitResult().pairs, deleted.pairs)
    }
    @Test fun randomEditsKeepTokenInvalidationInSync() {
        verifyFresh = true
        val text = Content("class X {\n void m() { String s = \"[()]\"; /* { } */ int[] x = new int[2]; }\n}\n")
        initialize(text)
        val random = Random(891748)
        repeat(150) {
            val index = random.nextInt(text.length + 1)
            if (index < text.length && random.nextBoolean()) {
                delete(text, index, minOf(text.length, index + random.nextInt(1, 8)))
            } else {
                val inserted = listOf("/*", "*/", "\"", "//", "\n", "x", "{([])}", " ")
                insert(text, index, inserted[random.nextInt(inserted.size)])
            }
            awaitResult()
        }
    }

    @Test fun disablingThenEnablingDoesNotLeaveAnEditListenerBehind() {
        val text = Content("class X { int[] x; }")
        initialize(text)
        styleUpdates.clear()
        language.setBracketPairColorization(false)
        insert(text, 0, " ")
        assertEquals(false, styleUpdates.poll(30, TimeUnit.SECONDS))
        insert(text, 0, " ")
        assertEquals(false, styleUpdates.poll(30, TimeUnit.SECONDS))
        language.setBracketPairColorization(true)
        assertEquals(2, awaitResult().pairs.count { !it.invalid })
    }

}
