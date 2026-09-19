/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package io.github.rosemoe.sora.langs.textmate.brackets

import io.github.rosemoe.sora.lang.styling.EmptyReader
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.text.Content
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType

/**
 * Scans the document and yields one [BracketNode] at a time: a [Delimiter] for a bracket token, or
 * a [TextNode] for a bounded run of ordinary text.
 *
 * The scanner is used by the analyzer thread only, and reads the span list of the tokenization pass
 * that is currently being published. Only spans typed [StandardTokenType.Other] may contain
 * brackets, so anything inside a comment or a string is skipped.
 */
internal class BracketTokenizer(
    private val content: Content,
    spans: Spans,
    private val brackets: BracketTokens,
    private val edit: BracketEdit? = null
) : AutoCloseable {

    // The initial scan treats all text as code, without allocating a fallback span per token.
    private val reader = if (spans.lineCount == 0) EmptyReader.getInstance() else spans.read()
    private val documentEnd = TextOffset.of(
        content.lineCount - 1,
        content.getColumnCount(content.lineCount - 1)
    )

    private var cached: BracketNode? = null

    /**
     * Optional upper bound for the next text token, set by the parser so that text chunks stay
     * aligned with reusable subtrees of the previous tree.
     */
    var textLimit: TextOffset? = null

    private var loadedLine = -1
    private var segment = 0
    private var spanCount = 0

    /** Current scan position. */
    var offset = TextOffset.ZERO
        private set

    /** How many characters were inspected as bracket candidates. Reported by tests. */
    var scannedCharacters = 0
        private set

    /** Move past [length] without producing a node. */
    fun skip(length: TextOffset) {
        offset += length
        cached = null
        textLimit = null
    }

    /** Consume and return the next node, or `null` at the end of the document. */
    fun read(): BracketNode? = peek()?.also { skip(it.length) }

    /**
     * Return the next node without consuming it.
     *
     * Text is emitted in chunks of at most [MAX_TEXT_CHUNK] characters, and a line break always ends
     * a chunk, so callers can rely on nodes being reasonably small.
     */
    fun peek(): BracketNode? {
        cached?.let { return it }
        if (offset >= documentEnd) return null

        val start = offset
        val limit = textLimit
        var cursor = start
        var scanned = 0
        while (cursor < documentEnd && scanned < MAX_TEXT_CHUNK && (limit == null || cursor < limit)) {
            val line = cursor.line
            val column = cursor.column
            val text = content.getLine(line)
            if (column == text.length) {
                // The line break itself is a text chunk.
                cursor = TextOffset.of(line + 1)
                break
            }
            if (loadedLine != line) {
                reader.moveToLine(line)
                loadedLine = line
                segment = 0
                spanCount = reader.spanCount
            }
            // Skips, including reused subtrees, can jump forward inside the current line.
            while (segment + 1 < spanCount && spanColumn(segment + 1) <= column) segment++
            val type = tokenTypeAt(segment)
            while (segment + 1 < spanCount && tokenTypeAt(segment + 1) == type) segment++
            val segmentEnd = if (segment + 1 < spanCount) {
                spanColumn(segment + 1).coerceAtMost(text.length)
            } else {
                text.length
            }

            if (type != StandardTokenType.Other) {
                val count = minOf(
                    segmentEnd - column,
                    MAX_TEXT_CHUNK - scanned,
                    textLimit?.takeIf { it.line == line }?.let { it.column - column } ?: Int.MAX_VALUE
                )
                cursor = TextOffset.of(line, column + count)
                scanned += count
                continue
            }

            val scanEnd = minOf(
                segmentEnd, column + MAX_TEXT_CHUNK - scanned,
                textLimit?.takeIf { it.line == line }?.column ?: Int.MAX_VALUE
            )
            // Jump straight to the next character that can start a delimiter; everything before it
            // is plain text that needs no per-character inspection.
            val candidate = brackets.nextCandidate(text, column, scanEnd)
            val skipped = candidate - column
            scannedCharacters += skipped
            if (candidate >= scanEnd) {
                cursor = TextOffset.of(line, scanEnd)
                scanned += scanEnd - column
                continue
            }
            cursor = TextOffset.of(line, candidate)
            scanned += skipped
            // at() inspects the candidate character itself.
            scannedCharacters++
            val bracket = brackets.at(text, candidate, segmentEnd)
            if (bracket != null) {
                cached = if (cursor == start) bracket else TextNode(start.lengthTo(cursor))
                return cached
            }
            // Not a bracket after all, so continue just past it.
            cursor = TextOffset.of(line, candidate + 1)
            scanned++
        }
        return TextNode(start.lengthTo(cursor)).also { cached = it }
    }

    override fun close() {
        reader.moveToLine(-1)
    }

    private fun tokenTypeAt(segment: Int): Int =
        if (spanCount == 0) StandardTokenType.Other
        else reader.getSpanAt(segment).extra as? Int ?: StandardTokenType.Other

    /**
     * Column of a span boundary, mapped through the pending edit.
     *
     * A provisional parse reuses the token categories of the previous revision, whose span columns
     * still describe the old text. Only a single-line edit shifts those columns, and it shifts them
     * exactly the way it moved the text; a multi-line edit retokenizes the affected lines anyway,
     * so its boundaries are left alone.
     */
    private fun spanColumn(segment: Int): Int {
        val column = reader.getSpanAt(segment).column
        val edit = edit ?: return column
        if (loadedLine != edit.start.line
            || edit.start.line != edit.oldEnd.line
            || edit.start.line != edit.newEnd.line
        ) {
            return column
        }
        val mapped = when {
            column <= edit.start.column -> column
            column < edit.oldEnd.column -> edit.newEnd.column
            else -> column + edit.newEnd.column - edit.oldEnd.column
        }
        return mapped.coerceIn(0, content.getLine(loadedLine).length)
    }

    private companion object {
        /** Longest text run produced by one [peek], so reuse windows stay small. */
        const val MAX_TEXT_CHUNK = 256
    }
}
