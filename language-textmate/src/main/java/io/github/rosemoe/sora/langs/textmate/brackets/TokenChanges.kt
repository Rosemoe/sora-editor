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

import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.text.Content
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType

/**
 * Works out which region of a text edit actually changed the token category of a line.
 *
 * A retokenized line is not necessarily a changed line: restyling plain code, or changing only a
 * syntax color, leaves brackets untouched. Treating every retokenized line as changed would defeat
 * subtree reuse on long lines, so the runs of code versus non-code are compared instead.
 *
 * Every method runs on the analyzer thread, before the analyzer replaces the previous spans of a
 * line, because the old spans are needed for the comparison.
 */
internal class TokenChanges(private val content: Content, private val spans: Spans) {

    /** Start column of a run of columns sharing one token category. */
    private data class Run(val start: Int, val code: Boolean)

    private var edit: BracketEdit? = null
    private var firstLine = emptyList<Run>()
    private var lastLine = emptyList<Run>()
    private var changedStart: TextOffset? = null
    private var changedEnd = TextOffset.ZERO
    private var observedLines = false

    /** Record a text edit. Its span update must be reported before the next edit arrives. */
    fun record(edit: BracketEdit) {
        check(this.edit == null) { "Each shadow edit must be followed by its span update" }
        this.edit = edit
        firstLine = oldLine(edit.start.line)
        lastLine = if (edit.oldEnd.line == edit.start.line) firstLine else oldLine(edit.oldEnd.line)
    }

    /** Report the spans the analyzer produced for [line]. */
    fun lineTokenized(line: Int, newSpans: List<Span>) {
        observedLines = true
        val current = runsOf(newSpans)
        val length = content.getColumnCount(line)
        val edit = edit
        if (edit == null || line < edit.start.line || line > edit.newEnd.line) {
            compareRuns(line, current, oldLine(line), 0, length, 0)
        } else {
            if (line == edit.start.line) {
                compareRuns(line, current, firstLine, 0, edit.start.column, 0)
            }
            if (line == edit.newEnd.line) {
                compareRuns(
                    line, current, lastLine,
                    edit.newEnd.column, length, edit.oldEnd.column - edit.newEnd.column
                )
            }
        }
    }

    /** Combine the pending text edit with the observed token changes and consume both. */
    fun finish(startLine: Int, endLine: Int): BracketEdit? {
        if (!observedLines) {
            // A token-only change, reported outside the analyzer's per-line callback.
            changedStart = TextOffset.of(startLine.coerceIn(0, content.lineCount - 1))
            changedEnd = if (endLine < content.lineCount - 1) {
                TextOffset.of(endLine + 1)
            } else {
                TextOffset.of(content.lineCount - 1, content.getColumnCount(content.lineCount - 1))
            }
        }
        val textEdit = edit
        val start = changedStart
        val combined = when {
            start == null -> textEdit
            textEdit == null -> BracketEdit(start, changedEnd, changedEnd)
            else -> {
                val end = maxOf(changedEnd, textEdit.newEnd)
                BracketEdit(minOf(start, textEdit.start), textEdit.oldPosition(end), end)
            }
        }
        reset()
        return combined
    }

    private fun reset() {
        edit = null
        firstLine = emptyList()
        lastLine = emptyList()
        changedStart = null
        changedEnd = TextOffset.ZERO
        observedLines = false
    }

    /** Split a line's spans into runs of equal token category. */
    private fun runsOf(spans: List<Span>): List<Run> {
        val result = ArrayList<Run>()
        for (span in spans) {
            val code = span.extra == null || span.extra == StandardTokenType.Other
            if (result.lastOrNull()?.code != code) result.add(Run(span.column, code))
        }
        return result.ifEmpty { listOf(Run(0, true)) }
    }

    private fun oldLine(line: Int): List<Run> = spans.read().let { reader ->
        try {
            runsOf(reader.getSpansOnLine(line))
        } finally {
            reader.moveToLine(-1)
        }
    }

    /**
     * Walk the old and the new run boundaries of one line together and record the columns where the
     * token category differs.
     *
     * [columnShift] maps a column of the new document to the matching column of the old one. It is
     * non-zero only on the last line of an edit that changed the character count.
     */
    private fun compareRuns(
        line: Int,
        newRuns: List<Run>,
        oldRuns: List<Run>,
        from: Int,
        to: Int,
        columnShift: Int
    ) {
        var column = from
        var newIndex = 0
        var oldIndex = 0
        while (column < to) {
            while (newIndex + 1 < newRuns.size && newRuns[newIndex + 1].start <= column) newIndex++
            while (oldIndex + 1 < oldRuns.size && oldRuns[oldIndex + 1].start <= column + columnShift) {
                oldIndex++
            }
            val boundary = minOf(
                to,
                newRuns.getOrNull(newIndex + 1)?.start ?: to,
                oldRuns.getOrNull(oldIndex + 1)?.let { it.start - columnShift } ?: to
            )
            if (newRuns[newIndex].code != oldRuns[oldIndex].code) {
                val diffStart = TextOffset.of(line, column)
                changedStart = minOf(changedStart ?: diffStart, diffStart)
                changedEnd = maxOf(changedEnd, TextOffset.of(line, boundary))
            }
            column = boundary
        }
    }
}
