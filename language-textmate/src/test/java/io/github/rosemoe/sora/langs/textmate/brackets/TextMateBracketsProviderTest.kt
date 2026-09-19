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

import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TextMateBracketsProviderTest {
    private val config = configuration("""{"brackets":[["(",")"],["[","]"],["{","}"]]}""")
    private fun configuration(json: String) = LanguageConfiguration.load(json.reader())!!
    private fun spans(text: Content): Spans = MappedSpans.Builder().apply {
        for (line in 0 until text.lineCount) add(line, SpanFactory.obtainNoExt(0, 0))
    }.build()
    private fun parse(text: Content, old: BracketNode? = null, edit: BracketEdit? = null,
        configuration: LanguageConfiguration = config, styles: Spans = spans(text)): BracketNode =
        BracketTokenizer(text, styles, BracketTokens(configuration)).use { BracketParser(it, old, edit).parse() }
    private fun BracketNode.all() = query(TextOffset.ZERO, TextOffset.MAX)
    private fun TextOffset.toPacked() = IntPair.pack(line, column)
    private fun end(text: Content) = TextOffset.of(text.lineCount - 1, text.getColumnCount(text.lineCount - 1))
    private fun at(text: Content, offset: Int): TextOffset {
        val pos = text.indexer.getCharPosition(offset)
        return TextOffset.of(pos.line, pos.column.coerceAtMost(text.getColumnCount(pos.line)))
    }
    private fun assertTree(node: BracketNode) {
        if (node is ListNode) {
            assertTrue(node.children.size in 2..3)
            assertTrue(node.children.all { it.height == node.height - 1 })
        }
        if (node.children.isNotEmpty()) {
            assertEquals(node.length, node.children.fold(TextOffset.ZERO) { offset, child -> offset + child.length })
            node.children.forEach(::assertTree)
        }
    }

    @Test fun resultsMatchActualVSCodeParser() {
        val json = javaClass.classLoader!!.getResourceAsStream("vscode-bracket-oracle.json")!!
            .bufferedReader().use { com.google.gson.JsonParser.parseReader(it).asJsonArray }
        for (case in json) {
            val source = case.asJsonObject.get("text").asString
            val expected = case.asJsonObject.getAsJsonArray("pairs").map { row ->
                val values = row.asJsonArray
                BracketPair(position(values[0].asInt, values[1].asInt), values[2].asInt,
                    position(values[3].asInt, values[4].asInt), values[5].asInt,
                    values[6].asInt, values[7].asInt, values[8].asBoolean,
                    // Invalid closers are not colorized; incomplete openers retain their pair flag.
                    !values[8].asBoolean || source.lineSequence().elementAt(values[0].asInt)[values[1].asInt] in "([{"
                )
            }
            assertEquals(source, expected, parse(Content(source)).all())
        }
    }

    @Test fun nestingAndSameTypeDepthAreIndependent() {
        val pairs = parse(Content("{[({})]} trailing")).all()
        assertEquals(listOf(0, 1, 2, 3), pairs.map { it.level })
        assertEquals(listOf(0, 0, 0, 1), pairs.map { it.levelOfEqualBracketType })
        assertTrue(pairs.none { it.invalid })
        assertEquals(TextOffset.of(0, 17), parse(Content("{[({})]} trailing")).length)
    }

    @Test fun malformedPairsRecoverAtAncestorCloser() {
        val pairs = parse(Content("{(}{}")).all()
        assertEquals(listOf(0, 1, 3), pairs.map { it.start.column })
        assertEquals(listOf(false, true, false), pairs.map { it.invalid })
        assertEquals(listOf(0, 1, 0), pairs.map { it.level })
        val old = parse(Content("(})"))
        val updated = parse(Content("{(})"), old, BracketEdit(TextOffset.ZERO, TextOffset.ZERO, TextOffset.of(0, 1)))
        assertEquals(parse(Content("{(})")).all(), updated.all())
        assertEquals(listOf(false, true, true), updated.all().map { it.invalid })
    }

    @Test fun randomizedEditsMatchFullParseAndLeaveOldSnapshotsUntouched() {
        val random = Random(748891)
        val text = Content("{\n[()] text\r\n}\n".repeat(40))
        var root = parse(text)
        repeat(500) { iteration ->
            val previous = root.all()
            val previousRoot = root
            val offset = random.nextInt(text.length + 1)
            val start = at(text, offset)
            // Content positions normalize CRLF's internal offset, so edit by line/column.
            val oldEnd: TextOffset
            val newEnd: TextOffset
            if (random.nextBoolean() && offset < text.length) {
                oldEnd = at(text, minOf(text.length, offset + random.nextInt(1, 15)))
                text.delete(start.line, start.column, oldEnd.line, oldEnd.column)
                newEnd = start
            } else {
                oldEnd = start
                val inserted = listOf("(", ")", "[", "]", "{", "}", "abc", "\n", "\r\n", "([)]")[random.nextInt(10)]
                val startIndex = text.getCharIndex(start.line, start.column)
                text.insert(start.line, start.column, inserted)
                newEnd = at(text, startIndex + inserted.length)
            }
            root = parse(text, root, BracketEdit(start, oldEnd, newEnd))
            assertEquals("edit $iteration", parse(text).all(), root.all())
            assertEquals(end(text), root.length)
            assertEquals(previous, previousRoot.all())
            assertTree(root)
        }
    }

    @Test fun localEditSkipsLargeUnchangedSubtreesAndTrailingText() {
        for (source in listOf("{} ".repeat(100_000), "x".repeat(300_000))) {
            val text = Content(source)
            val old = parse(text)
            val start = TextOffset.of(0, 150_001)
            text.insert(0, start.column, "x")
            BracketTokenizer(text, spans(text), BracketTokens(config)).use { tokenizer ->
                val parser = BracketParser(tokenizer, old, BracketEdit(start, start, TextOffset.of(0, start.column + 1)))
                val updated = parser.parse()
                assertEquals(parse(text).all(), updated.all())
                assertTrue("scanned ${tokenizer.scannedCharacters}", tokenizer.scannedCharacters < 2048)
                assertTrue(parser.reusedNodes in 1..100)
                assertTree(updated)
            }
        }
    }

    @Test fun movingSubtreeAcrossNestingLimitMatchesFreshParse() {
        val text = Content("(".repeat(151) + ")".repeat(151))
        val root = parse(text)
        text.insert(0, 0, "(")
        val updated = parse(text, root, BracketEdit(TextOffset.ZERO, TextOffset.ZERO, TextOffset.of(0, 1)))
        assertEquals(parse(text).all(), updated.all())
        assertTree(updated)
    }

    @Test fun wordBracketsRespectBoundariesAndTokenSegments() {
        val configuration = configuration("""{"brackets":[["begin","end"],["if","end"]]}""")
        val text = Content("BEGIN if end END ending beginner")
        assertEquals(2, parse(text, configuration = configuration).all().count { !it.invalid })
        val old = parse(Content("begin end"), configuration = configuration)
        assertEquals(parse(Content("begin ending"), configuration = configuration).all(),
            parse(Content("begin ending"), old, BracketEdit(TextOffset.of(0, 9), TextOffset.of(0, 9), TextOffset.of(0, 12)), configuration).all())
        val styles = MappedSpans.Builder().apply {
            add(0, SpanFactory.obtainNoExt(0, 0))
            add(0, SpanFactory.obtainNoExt(3, 0).apply { extra = StandardTokenType.Comment })
        }.build()
        assertTrue(parse(Content("begin end"), configuration = configuration, styles = styles).all().isEmpty())
    }

    @Test fun explicitEmptyColorsKeepMatchingButDisableColorization() {
        val configuration = configuration("""{"brackets":[["(",")"]],"colorizedBracketPairs":[]}""")
        val text = Content("()")
        val provider = TextMateBracketsProvider(text, spans(text), configuration)
        provider.initialize(text.documentVersion)
        assertNotNull(provider.getPairedBracketAt(text, 0))
        assertTrue(provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).isEmpty())
    }

    @Test fun rangeQueriesDoNotReusePartialCursorResults() {
        val text = Content("{[()]}")
        val provider = TextMateBracketsProvider(text, spans(text), config)
        provider.initialize(text.documentVersion)
        assertNotNull(provider.getPairedBracketAt(text, 3))
        assertEquals(3, provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).size)
        val closing = provider.queryPairedBracketsForRange(text, TextOffset.of(0, 5).toPacked(), TextOffset.of(0, 6).toPacked())
        assertTrue(closing.any { it.leftIndex == 0 && it.rightIndex == 5 })
    }

    @Test fun tokenOnlyUpdateInvalidatesBracketsAndRestoresThem() {
        val text = Content("{}\n[()]\n{}")
        val styles = spans(text)
        val provider = TextMateBracketsProvider(text, styles, config)
        provider.initialize(text.documentVersion)
        styles.modify().setSpansOnLine(1, listOf(SpanFactory.obtainNoExt(0, 0).apply { extra = StandardTokenType.Comment }))
        provider.updateSpans(1, 1, text.documentVersion)
        assertEquals(parse(text, styles = styles).all(), provider.root!!.all())
        assertEquals(2, provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).size)
        styles.modify().setSpansOnLine(1, listOf(SpanFactory.obtainNoExt(0, 0)))
        provider.updateSpans(1, 1, text.documentVersion)
        assertEquals(4, provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).size)
    }

    @Test fun unpublishedRevisionIsNeverQueriedEvenForSameLengthEdits() {
        val text = Content("{}()")
        val shadow = text.copyText(false)
        val provider = TextMateBracketsProvider(shadow, spans(shadow), config)
        provider.initialize(text.documentVersion)
        text.delete(0, 1)
        assertNull(provider.getPairedBracketAt(text, 0))
        shadow.delete(0, 1)
        provider.update(0, 1, 0, text.documentVersion)
        provider.updateSpans(0, 0, text.documentVersion)
        assertNotNull(provider.getPairedBracketAt(text, 1))
        text.insert(0, 0, "[")
        assertTrue(provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).isEmpty())
    }
    @Test fun compoundReplacementDoesNotPublishTheIntermediateDeletion() {
        val text = Content("{}()")
        val shadow = text.copyText(false)
        val provider = TextMateBracketsProvider(shadow, spans(shadow), config)
        provider.initialize(text.documentVersion)
        text.replace(0, 1, "[")
        shadow.delete(0, 1)
        provider.update(0, 1, 0, text.documentVersion)
        provider.updateSpans(0, 0, text.documentVersion)
        assertTrue(provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).isEmpty())
        shadow.insert(0, 0, "[")
        provider.update(0, 0, 1, text.documentVersion)
        provider.updateSpans(0, 0, text.documentVersion)
        assertEquals(1, provider.queryPairedBracketsForRange(text, 0, end(text).toPacked()).size)
    }

    @Test fun adjacentCodeSpansCanContainOneWordBracket() {
        val text = Content("begin end")
        val configuration = configuration("""{"brackets":[["begin","end"]]}""")
        val styles = MappedSpans.Builder().apply {
            add(0, SpanFactory.obtainNoExt(0, 0))
            add(0, SpanFactory.obtainNoExt(2, 1))
        }.build()
        assertEquals(1, parse(text, configuration = configuration, styles = styles).all().size)
    }

}
