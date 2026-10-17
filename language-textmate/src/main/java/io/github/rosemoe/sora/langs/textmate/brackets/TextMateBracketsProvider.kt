/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration

/**
 * Publishes the bracket tree built from TextMate tokenization.
 *
 * The analyzer thread is the only writer. It parses after its shadow edits and tokenization have
 * caught up, so it never describes a revision the UI cannot see. Readers take a snapshot and accept
 * it only while the document version and text length still match.
 */
class TextMateBracketsProvider(
    private val content: Content,
    private val spans: Spans,
    configuration: LanguageConfiguration
) : BracketsProvider {

    private data class Snapshot(val root: BracketNode, val version: Long, val textLength: Int)

    private val tokens = BracketTokens(configuration)
    private val changes = TokenChanges(content, spans)
    // The tree both parses of one edit start from. The provisional parse must not become the base
    // of the token-aware parse that follows it, or the same edit would be applied twice.
    private var tokenRoot: BracketNode? = null

    @Volatile
    private var snapshot: Snapshot? = null

    /** Whether the language configuration declares any usable bracket pair. */
    val isSupported: Boolean
        get() = !tokens.isEmpty

    /** Subtrees taken over by the last parse. */
    internal var reusedNodes = 0
        private set

    /** Characters inspected by the last parse. */
    internal var scannedCharacters = 0
        private set

    internal val root: BracketNode?
        get() = snapshot?.root

    /**
     * Build the first tree. Empty spans give immediate, approximate colors before tokenization.
     * Keep this snapshot visible until updateSpans() corrects it using the completed token pass.
     */
    fun initialize(documentVersion: Long) {
        parse(null, documentVersion)
        tokenRoot = snapshot?.root
    }

    /** Report that the analyzer replaced the spans of `[startLine, endLine]`. */
    fun updateSpans(startLine: Int, endLine: Int, documentVersion: Long) {
        val edit = changes.finish(startLine, endLine)
        if (edit != null) {
            parse(edit, documentVersion, tokenRoot)
        } else {
            // Only the token categories of already known lines changed, so the tree is untouched.
            if (snapshot?.version != documentVersion) {
                snapshot = snapshot?.copy(version = documentVersion)
            }
            reusedNodes = 0
            scannedCharacters = 0
        }
        tokenRoot = snapshot?.root
    }

    /** Report the spans the analyzer produced for one line. */
    fun updateLineTokens(line: Int, spans: List<Span>) = changes.lineTokenized(line, spans)

    /**
     * Apply a text edit straight away, before the analyzer has published the spans that follow it,
     * so that brackets keep up with typing instead of waiting for tokenization.
     */
    fun update(start: Long, oldEnd: Long, newEnd: Long, documentVersion: Long) {
        val edit = BracketEdit(toOffset(start), toOffset(oldEnd), toOffset(newEnd))
        changes.record(edit)
        // The provisional parse reuses the token categories of the previous revision. That is only
        // sound while the edit stays on one line, because a multi-line edit invalidates the category
        // layout of every line it spans. updateSpans() replaces this tree once tokenization lands.
        val singleLine = edit.start.line == edit.oldEnd.line && edit.start.line == edit.newEnd.line
        if (singleLine) {
            parse(edit, documentVersion, tokenRoot, provisional = true)
        }
    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        if (index !in 0..text.length) return null
        val root = currentRoot(text) ?: return null
        val position = text.indexer.getCharPosition(index)
        // Inclusive endpoints also match a bracket that ends exactly at the cursor, which is what a
        // selection needs for multi-character brackets.
        val cursor = TextOffset.of(position.line, position.column)
        return root.query(cursor, cursor).firstOrNull { !it.invalid }?.toEditorPair(text)
    }

    override fun queryPairedBracketsForRange(
        text: Content,
        leftRange: Long,
        rightRange: Long
    ): List<PairedBracket> =
        currentRoot(text)?.query(toOffset(leftRange), toOffset(rightRange))
            .orEmpty()
            .filter { !it.invalid && it.colorized }
            .map { it.toEditorPair(text) }

    override fun isReadyFor(text: Content): Boolean = currentRoot(text) != null

    private fun parse(
        edit: BracketEdit?, version: Long,
        oldRoot: BracketNode? = null, provisional: Boolean = false
    ) {
        // Only a provisional parse passes the edit down: it is what makes the tokenizer map the
        // span columns of the previous revision onto the current text.
        val tokenizerEdit = if (provisional) edit else null
        BracketTokenizer(content, spans, tokens, tokenizerEdit).use { tokenizer ->
            val parser = BracketParser(
                tokenizer,
                oldRoot = oldRoot,
                edit = edit
            )
            snapshot = Snapshot(parser.parse(), version, content.length)
            reusedNodes = parser.reusedNodes
            scannedCharacters = tokenizer.scannedCharacters
        }
    }

    /**
     * A snapshot is usable only while it describes the revision the caller is looking at. Comparing
     * the text length as well rejects equal-length edits that were never published.
     */
    private fun currentRoot(text: Content): BracketNode? =
        snapshot?.takeIf { it.version == text.documentVersion && it.textLength == text.length }?.root

    private fun BracketPair.toEditorPair(text: Content) = PairedBracket(
        text.getCharIndex(start.line, start.column),
        openLength,
        text.getCharIndex(closeStart.line, closeStart.column),
        closeLength,
        level
    )

    private companion object {

        /** Ranges handed in by the editor are packed the same way as [TextOffset]. */
        fun toOffset(packed: Long) = TextOffset.of(IntPair.getFirst(packed), IntPair.getSecond(packed))
    }
}
