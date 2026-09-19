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
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.brackets.ast.ASTNode
import io.github.rosemoe.sora.langs.textmate.brackets.ast.BracketAST
import io.github.rosemoe.sora.langs.textmate.brackets.ast.BracketPairAST
import io.github.rosemoe.sora.langs.textmate.brackets.ast.InvalidBracketAST
import io.github.rosemoe.sora.langs.textmate.brackets.ast.ListAST
import io.github.rosemoe.sora.langs.textmate.brackets.ast.TextAST
import io.github.rosemoe.sora.langs.textmate.brackets.ast.TwoThreeListAST
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.FileResolver
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.IntPair
import org.eclipse.tm4e.core.registry.IThemeSource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test for TextMateBracketsProvider using real TextMate style parsing.
 * Uses proper AnalyzeManager/StyleReceiver mechanism.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TextMateBracketsProviderTest {

    private lateinit var content: Content
    private lateinit var language: TextMateLanguage
    private lateinit var analyzer: AnalyzeManager
    private lateinit var bracketsProvider: TextMateBracketsProvider
    private lateinit var testReceiver: TestStyleReceiver

    @Before
    fun setup() {
        // Initialize FileProviderRegistry with test file resolver
        FileProviderRegistry.getInstance().addFileProvider(TestFileResolver())

        // Initialize registries
        val grammarRegistry = GrammarRegistry.getInstance()
        val themeRegistry = ThemeRegistry.getInstance()

        // Register Java language
        grammarRegistry.loadGrammars(
            languages {
                language("java") {
                    grammar = "java.tmLanguage.json"
                    defaultScopeName()
                    languageConfiguration = "java-language-configuration.json"
                }
            }
        )


        // Load Darcula theme
        val themeSource = object : IThemeSource {
            private fun getInputStream() = getResourceStream("darcula.json")
            override fun getFilePath() = "darcula.json"
            override fun getReader(): Reader = getInputStream().bufferedReader()
        }
        themeRegistry.setTheme(ThemeModel(themeSource, "darcula"))

        // Load sample code from file
        val sampleCode =
            javaClass.classLoader!!.getResourceAsStream("sample.txt").bufferedReader()
                .use { it.readText() }

        // Create content
        content = Content(sampleCode)

        // Create language
        language =
            TextMateLanguage.create("source.java", grammarRegistry, themeRegistry, false)

        // Get analyzer
        analyzer = language.analyzeManager

        // Create test receiver
        testReceiver = TestStyleReceiver()
    }

    /**
     * Test 1: Full parse and cache bracket pairs as complete result
     */
    @Test
    fun testFullParse() {
        println("=== Test 1: Full Parse ===")

        // Set receiver and trigger initial analysis
        analyzer.setReceiver(testReceiver)
        analyzer.reset(ContentReference(content), Bundle())

        // Wait for analysis to complete
        val completed = testReceiver.waitForStyles(30, TimeUnit.SECONDS)
        if (!completed) {
            throw AssertionError("Analysis timeout")
        }

        println("Analysis completed, styles received")

        println("3 ${testReceiver.currentBracketsProvider}")
        // Get bracket provider
        bracketsProvider = testReceiver.currentBracketsProvider as TextMateBracketsProvider

        // Print AST structure
        println("\nComplete AST Tree:")
        val ast = getActiveAST(bracketsProvider)
        println(ASTDebugHelper.summarizeFull(ast))

        // Query all bracket pairs
        val allPairs =
            bracketsProvider.queryPairedBracketsForRange(
                content,
                IntPair.pack(0, 0),
                content.indexer.getCharPosition(content.lastIndex).toIntPair()
            )
        println("\nTotal bracket pairs found: ${allPairs?.size ?: 0}")

        allPairs?.forEachIndexed { index, pair ->
            println("Pair $index: left=${pair.leftIndex}:${pair.leftLength} right=${pair.rightIndex}:${pair.rightLength} level=${pair.level}")
        }

        println("\n=== Test 1 Complete ===\n")
    }

    /**
     * Test 2: Repeatedly delete and insert '}' at line 95, test 10 times
     */
    @Test
    fun testRepeatedEditLine95() {
        println("=== Test 2: Repeated Edit at Line 95 ===")

        // Initial analysis
        analyzer.setReceiver(testReceiver)
        analyzer.reset(ContentReference(content), Bundle())
        testReceiver.waitForStyles(30, TimeUnit.SECONDS)

        bracketsProvider = testReceiver.currentBracketsProvider as TextMateBracketsProvider

        // Get initial bracket pairs as baseline
        val initialPairs =
            bracketsProvider.queryPairedBracketsForRange(
                content,
                IntPair.pack(0, 0),
                content.indexer.getCharPosition(content.lastIndex).toIntPair()
            )
        println("Initial bracket pairs: ${initialPairs?.size ?: 0}")

        // Repeat 10 times
        repeat(10) { iteration ->
            println("\n--- Iteration ${iteration + 1} ---")

            // Find '}' at line 95 (line index 94)
            val line95 = content.getLine(94)
            val line95Text = line95.toString()
            val braceIndex = line95Text.lastIndexOf('}')
            if (braceIndex < 0) {
                throw AssertionError("Cannot find '}' at line 95")
            }

            // Delete '}'
            val deleteStart = CharPosition(94, braceIndex)
            val deleteEnd = CharPosition(94, braceIndex + 1)
            content.delete(
                deleteStart.line,
                deleteStart.column,
                deleteEnd.line,
                deleteEnd.column
            )

            // Notify analyzer
            testReceiver.resetLatch()
            analyzer.delete(deleteStart, deleteEnd, "}")

            // Wait for analysis
            testReceiver.waitForStyles(10, TimeUnit.SECONDS)
            println("After delete: content length = ${content.length}")

            // Re-insert '}'
            val insertPos = CharPosition(94, braceIndex)
            content.insert(insertPos.line, insertPos.column, "}")

            // Notify analyzer
            testReceiver.resetLatch()
            analyzer.insert(insertPos, CharPosition(94, braceIndex + 1), "}")

            // Wait for analysis
            testReceiver.waitForStyles(10, TimeUnit.SECONDS)
            println("After insert: content length = ${content.length}")

            // Verify bracket pairs match initial state
            val currentPairs =
                bracketsProvider.queryPairedBracketsForRange(
                    content,
                    IntPair.pack(0, 0),
                    content.indexer.getCharPosition(content.lastIndex).toIntPair()
                )
            val matches = (currentPairs?.size == initialPairs?.size)
            assert(currentPairs != initialPairs) {
                buildString {
                    appendLine("Bracket pairs match initial: $matches (current=${currentPairs?.size}, initial=${initialPairs?.size})")

                    appendLine("WARNING: Bracket pair count mismatch!")
                    appendLine("Current AST:")
                    appendLine(ASTDebugHelper.summarizeFull(getActiveAST(bracketsProvider)))
                }
            }
        }

        println("\n=== Test 2 Complete ===\n")
    }

    /**
     * Test 3: Delete '}' at line 95, insert newlines/spaces, then re-insert '}'
     * Tests error recovery and bracket level correctness
     */
    @Test
    fun testErrorRecoveryLine95() {
        println("=== Test 3: Error Recovery at Line 95 ===")

        // Initial analysis
        analyzer.setReceiver(testReceiver)
        analyzer.reset(ContentReference(content), Bundle())
        testReceiver.waitForStyles(30, TimeUnit.SECONDS)

        bracketsProvider = testReceiver.currentBracketsProvider as TextMateBracketsProvider

        val initialPairs =
            bracketsProvider.queryPairedBracketsForRange(
                content,
                0,
                content.length.toLong()
            )
        println("Initial bracket pairs: ${initialPairs?.size ?: 0}")
        println("Initial AST:")
        println(ASTDebugHelper.summarizeDetailed(getActiveAST(bracketsProvider)))

        // Find and delete '}' at line 95
        val line95 = content.getLine(94)
        val line95Text = line95.toString()
        val braceIndex = line95Text.lastIndexOf('}')

        val deleteStart = CharPosition(94, braceIndex)
        val deleteEnd = CharPosition(94, braceIndex + 1)
        content.delete(
            deleteStart.line,
            deleteStart.column,
            deleteEnd.line,
            deleteEnd.column
        )

        testReceiver.resetLatch()
        analyzer.delete(deleteStart, deleteEnd, "}")
        testReceiver.waitForStyles(10, TimeUnit.SECONDS)

        println("\nAfter deleting '}' at line 95:")
        val afterDeletePairs =
            bracketsProvider.queryPairedBracketsForRange(
                content,
                IntPair.pack(0, 0),
                content.indexer.getCharPosition(content.lastIndex).toIntPair()
            )
        println("Bracket pairs: ${afterDeletePairs?.size ?: 0}")
        println("AST after delete:")
        println(ASTDebugHelper.summarizeDetailed(getActiveAST(bracketsProvider)))

        // Insert newlines and spaces, one at a time (each insertion is a newline + spaces)
        val insertions = listOf("\n    ", "\n    ", "\n    ", "\n    ")
        var currentLine = 94
        var currentCol = braceIndex

        insertions.forEachIndexed { index, text ->
            println("\n--- Insertion ${index + 1}: \"${text.replace("\n", "\\n")}\" ---")

            val insertStart = CharPosition(currentLine, currentCol)
            content.insert(currentLine, currentCol, text)

            // Calculate end position after insertion
            val newlineCount = text.count { it == '\n' }
            val insertEnd = if (newlineCount > 0) {
                val lastLineLength = text.length - text.lastIndexOf('\n') - 1
                CharPosition(currentLine + newlineCount, lastLineLength)
            } else {
                CharPosition(currentLine, currentCol + text.length)
            }

            testReceiver.resetLatch()
            analyzer.insert(insertStart, insertEnd, text)
            testReceiver.waitForStyles(10, TimeUnit.SECONDS)

            // Update position for next insertion
            currentLine = insertEnd.line
            currentCol = insertEnd.column

            val pairs =
                bracketsProvider.queryPairedBracketsForRange(
                    content,
                    IntPair.pack(0, 0),
                    content.indexer.getCharPosition(content.lastIndex).toIntPair()
                )
            println("Bracket pairs: ${pairs?.size ?: 0}")
            println("Bracket levels are ${if (checkBracketLevelsValid(pairs)) "valid" else "INVALID"}")
        }

        // Re-insert '}' at current position
        println("\n--- Re-inserting '}' ---")
        val reinsertStart = CharPosition(currentLine, currentCol)
        content.insert(currentLine, currentCol, "}")

        testReceiver.resetLatch()
        analyzer.insert(reinsertStart, CharPosition(currentLine, currentCol + 1), "}")
        testReceiver.waitForStyles(10, TimeUnit.SECONDS)

        println("After re-inserting '}':")
        val finalPairs =
            bracketsProvider.queryPairedBracketsForRange(
                content,
                IntPair.pack(0, 0),
                content.indexer.getCharPosition(content.lastIndex).toIntPair()
            )
        println("Bracket pairs: ${finalPairs?.size ?: 0}")

        assert(checkBracketLevelsValid(finalPairs)) {
            buildString {
                appendLine("Bracket levels are ${if (checkBracketLevelsValid(finalPairs)) "valid" else "INVALID"}")
                appendLine("Final AST:")
                appendLine(ASTDebugHelper.summarizeDetailed(getActiveAST(bracketsProvider)))
            }
        }

        println("Bracket levels are ${if (checkBracketLevelsValid(finalPairs)) "valid" else "INVALID"}")
        println("Final AST:")
        println(ASTDebugHelper.summarizeDetailed(getActiveAST(bracketsProvider)))

        println("\n=== Test 3 Complete ===\n")
    }

    /**
     * Check if bracket levels are valid
     */
    private fun checkBracketLevelsValid(pairs: List<io.github.rosemoe.sora.lang.brackets.PairedBracket>?): Boolean {
        if (pairs.isNullOrEmpty()) return true
        val levelGroups = pairs.groupBy { it.level }
        return levelGroups.all { (level, brackets) ->
            brackets.all { it.level == level }
        }
    }

    /**
     * Get active AST using reflection
     */
    private fun getActiveAST(provider: TextMateBracketsProvider): ASTNode? {
        return try {
            val field = provider.javaClass.getDeclaredField("astManager")
            field.isAccessible = true
            val astManager = field.get(provider)
            val getActiveASTMethod = astManager.javaClass.getDeclaredMethod("getActiveAST")
            getActiveASTMethod.invoke(astManager) as? ASTNode
        } catch (e: Exception) {
            println("Failed to get active AST: ${e.message}")
            null
        }
    }

    /**
     * Get resource stream from test resources
     */
    private fun getResourceStream(name: String): InputStream {
        return javaClass.classLoader!!.getResourceAsStream(name)
            ?: throw IllegalStateException("Resource $name not found")
    }

    /**
     * Test StyleReceiver implementation
     */
    private class TestStyleReceiver : StyleReceiver {
        private var latch = CountDownLatch(1)
        var currentStyles: Styles? = null
        var currentBracketsProvider: BracketsProvider? = null

        override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?) {
            currentStyles = styles
            if (currentBracketsProvider != null) {
                latch.countDown()
            }
        }

        override fun setStyles(
            sourceManager: AnalyzeManager,
            styles: Styles?,
            action: Runnable?
        ) {
            action?.run()
            setStyles(sourceManager, styles)
        }

        override fun updateStyles(
            sourceManager: AnalyzeManager,
            styles: Styles,
            range: StyleUpdateRange
        ) {
            currentStyles = styles
            if (currentBracketsProvider != null) {
                latch.countDown()
            }
        }

        override fun setDiagnostics(
            sourceManager: AnalyzeManager,
            diagnostics: DiagnosticsContainer?
        ) {
            // Not used in this test
        }

        override fun updateBracketProvider(
            sourceManager: AnalyzeManager,
            provider: BracketsProvider?
        ) {
            val last = currentBracketsProvider
            currentBracketsProvider = provider
            println("2 $provider")
            if (last == null && provider != null) {
                latch.countDown()
            }
        }

        fun waitForStyles(timeout: Long, unit: TimeUnit): Boolean {
            return latch.await(timeout, unit)
        }

        fun resetLatch() {
            latch = CountDownLatch(1)
        }
    }

    /**
     * Test file resolver for loading test resources
     */
    private class TestFileResolver : FileResolver {

        override fun resolveStreamByPath(path: String?): InputStream? {
            return this.javaClass.classLoader.getResourceAsStream("$path")
        }
    }
}

/**
 * Helper object for AST debugging
 */
object ASTDebugHelper {
    private const val DEFAULT_MAX_DEPTH = 3
    private const val DEFAULT_CHILD_SAMPLE = 4
    private const val DETAILED_MAX_DEPTH = 8
    private const val DETAILED_CHILD_SAMPLE = 10

    fun summarize(
        node: ASTNode?,
        maxDepth: Int = DEFAULT_MAX_DEPTH,
        childSample: Int = DEFAULT_CHILD_SAMPLE
    ): String {
        if (node == null) {
            return "<null>"
        }
        val builder = StringBuilder()
        appendNode(node, builder, 0, maxDepth, childSample)
        return builder.toString()
    }

    fun summarizeDetailed(node: ASTNode?): String {
        return summarize(node, DETAILED_MAX_DEPTH, DETAILED_CHILD_SAMPLE)
    }

    fun summarizeFull(node: ASTNode?): String {
        return summarize(node, maxDepth = Int.MAX_VALUE, childSample = Int.MAX_VALUE)
    }

    private fun appendNode(
        node: ASTNode,
        builder: StringBuilder,
        depth: Int,
        maxDepth: Int,
        childSample: Int
    ) {
        when (node) {
            is TextAST -> builder.append("Text(len=").append(node.length).append(")")
            is BracketAST -> builder.append("Bracket(id=").append(node.bracketId)
                .append(", open=").append(node.isOpening).append(", len=").append(node.length)
                .append(")")

            is BracketPairAST -> builder.append("Pair(id=").append(node.bracketId)
                .append(", closed=").append(node.isClosed)
                .append(", len=").append(node.length).append(")")

            is ListAST -> builder.append("List(children=").append(node.childCount)
                .append(", len=").append(node.length).append(")")

            is TwoThreeListAST -> builder.append("TwoThree(children=").append(node.childCount)
                .append(", len=").append(node.length).append(")")

            is InvalidBracketAST -> builder.append("Invalid(id=").append(node.bracketId)
                .append(", len=").append(node.length).append(")")

            else -> builder.append(node.javaClass.simpleName).append("(len=")
                .append(node.length)
                .append(")")
        }

        if (node.childCount == 0) {
            return
        }
        if (depth >= maxDepth) {
            builder.append(" -> ...")
            return
        }

        builder.append(" [")
        val limit = node.childCount.coerceAtMost(childSample)
        for (i in 0 until limit) {
            if (i > 0) {
                builder.append(", ")
            }
            appendNode(node.getChild(i), builder, depth + 1, maxDepth, childSample)
        }
        if (node.childCount > limit) {
            builder.append(", +").append(node.childCount - limit).append(" more")
        }
        builder.append("]")
    }
}
