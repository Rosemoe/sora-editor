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

import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.CachedBracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.langs.textmate.brackets.ast.BracketASTManager
import io.github.rosemoe.sora.langs.textmate.brackets.ast.BracketMatcherAST
import io.github.rosemoe.sora.langs.textmate.brackets.ast.BracketTokenizer
import io.github.rosemoe.sora.langs.textmate.brackets.ast.EditCombiner
import io.github.rosemoe.sora.langs.textmate.brackets.ast.EditInfo
import io.github.rosemoe.sora.text.Content
import org.eclipse.tm4e.languageconfiguration.internal.model.CharacterPair
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import kotlin.math.max

/**
 * TextMate implementation of [BracketsProvider] backed by the lexer/index/matcher stack.
 * Thread-safe for concurrent analyzer (writes) and UI (reads) access.
 *
 * Uses Token AST strategy:
 * - Token AST: Updated after tokenization (accurate, uses fresh snapshot)
 */
class TextMateBracketsProvider(
    content: Content,
    spans: Spans,
    configuration: LanguageConfiguration?
) : CachedBracketsProvider() {

    private val bracketPairs: List<CharacterPair> = extractBracketPairs(configuration)
    private val hasBrackets = bracketPairs.isNotEmpty()

    // Core components
    private val snapshot = SpanSnapshot(spans)
    private val lexer = BracketLexer(content, snapshot, bracketPairs)
    private val astManager = BracketASTManager()
    private val matcher = BracketMatcherAST { astManager.getActiveAST() }

    // Edit tracking
    private val editTracker = EditTracker()

    /**
     * Whether this provider has usable bracket data.
     */
    val isSupported: Boolean
        get() = hasBrackets

    /**
     * Current version of the underlying index. Useful for cache invalidation.
     */
    val version: Long
        get() = astManager.currentVersion()

    /**
     * Performs a full lex/update pass. Must be called on the analyzer thread once spans are ready.
     */
    fun initialize() {
        if (!hasBrackets) return
        snapshot.rebuildAll()
        val tokenizer = BracketTokenizer(lexer, 0, BracketToken.MAX_LINE_INDEX)
        astManager.rebuildTokenAST(tokenizer)
        matcher.invalidateCache()
        super.clear()
    }

    /**
     * Propagates incremental span updates.
     *
     * Updates the Token AST with accurate bracket information now that tokens are available.
     */
    fun notifySpansChanged(startLine: Int, endLine: Int) {
        if (!hasBrackets) return
        snapshot.updateRange(startLine, endLine)

        if (editTracker.hasPendingTokenEdits()) {
            try {
                val edits = editTracker.consumeTokenEdits()
                // Guard against empty edit lists (e.g., all edits collapsed to no-ops)
                if (edits.isEmpty()) {
                    return
                }
                val tokenizer = BracketTokenizer(lexer, 0, BracketToken.MAX_LINE_INDEX)
                astManager.updateTokenASTWithEdits(edits, tokenizer)
            } catch (e: Exception) {
                System.err.println("TextMateBracketsProvider: updateTokenAST failed, falling back to rebuild")
                e.printStackTrace()
                val tokenizer = BracketTokenizer(lexer, 0, BracketToken.MAX_LINE_INDEX)
                astManager.rebuildTokenAST(tokenizer)
            }
        } else {
            // Fallback to full rebuild when no edit info is available
            val tokenizer = BracketTokenizer(lexer, 0, BracketToken.MAX_LINE_INDEX)
            astManager.rebuildTokenAST(tokenizer)
        }
        super.clear()
    }

    /**
     * Called when content is inserted. Records edit information for incremental update.
     *
     * Queues the edit for Token AST (accurate) update.
     *
     * @param startLine Line where insertion starts (0-based)
     * @param startColumn Column where insertion starts (0-based)
     * @param insertedText The exact sequence inserted at the position
     */
    fun onContentInsert(startLine: Int, startColumn: Int, insertedText: CharSequence) {
        if (!hasBrackets || insertedText.isEmpty()) return

        val (newLineCount, newColumnCount) = measureTextLength(insertedText)

        val endLine = startLine + newLineCount
        val endColumn = if (newLineCount == 0) startColumn + newColumnCount else newColumnCount

        val edit = EditInfo(
            startLine = startLine,
            startColumn = startColumn,
            oldLineCount = 0,
            oldColumnCount = 0,
            newLineCount = newLineCount,
            newColumnCount = newColumnCount
        )

        editTracker.addEdit(edit)

        // Update snapshot with immediate mapping after recording the old-coordinate edit
        snapshot.adjustOnInsert(startLine, startColumn, endLine, endColumn)
        super.clear()
    }

    /**
     * Called when content is deleted. Records edit information for incremental update.
     *
     * Queues the edit for Token AST (accurate) update.
     *
     * @param startLine Line where deletion starts (0-based)
     * @param startColumn Column where deletion starts (0-based)
     * @param endLine Line where deletion ends (0-based, before deletion)
     * @param endColumn Column where deletion ends (0-based, before deletion)
     */
    fun onContentDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        if (!hasBrackets) return

        // Calculate the length of deleted content
        val oldLineCount: Int
        val oldColumnCount: Int

        if (startLine == endLine) {
            // Single line deletion
            oldLineCount = 0
            oldColumnCount = endColumn - startColumn
        } else {
            // Multi-line deletion
            oldLineCount = endLine - startLine
            oldColumnCount = endColumn
        }

        val edit = EditInfo(
            startLine = startLine,
            startColumn = startColumn,
            oldLineCount = oldLineCount,
            oldColumnCount = oldColumnCount,
            newLineCount = 0,
            newColumnCount = 0
        )

        editTracker.addEdit(edit)

        // Update snapshot after enqueuing the edit so coordinates stay anchored to the pre-edit state
        snapshot.adjustOnDelete(startLine, startColumn, endLine, endColumn)
        super.clear()
    }

    /**
     * Clears all indexed data. Safe to call when TextMate analyzer tears down.
     */
    override fun clear() {
        super.clear()
        astManager.clear()
        snapshot.clear()
        matcher.invalidateCache()
    }


    override fun computePairedBracketAt(text: Content, index: Int): PairedBracket? {
        if (!canQuery()) return null
        val safeIndex = index.coerceIn(0, max(0, text.length))

        val position = text.indexer.getCharPosition(safeIndex)

        // Use the optimized matchBracket method
        val matched = matcher.matchBracket(position.line, position.column)
            ?: return null

        try {
            return toPairedBracket(text, matched)
        } finally {
            BracketPair.recycle(matched)
        }

    }

    override fun computePairedBracketsForRange(
        text: Content,
        leftRange: Long,
        rightRange: Long
    ): List<PairedBracket>? {
        if (!canQuery()) return null

        val scratchPairs = ArrayList<BracketPair>(32)
        matcher.collectPairsInRange(leftRange, rightRange, scratchPairs)
        if (scratchPairs.isEmpty()) {
            return null
        }
        val result = ArrayList<PairedBracket>(scratchPairs.size)
        for (pair in scratchPairs) {
            try {
                val converted = toPairedBracket(text, pair)
                if (converted != null) {
                    result.add(converted)
                }
            } finally {
                BracketPair.recycle(pair)
            }
        }
        scratchPairs.clear()
        return if (result.isEmpty()) emptyList() else result
    }

    private fun canQuery(): Boolean {
        return hasBrackets && astManager.isInitialized()
    }

    private fun toPairedBracket(text: Content, pair: BracketPair): PairedBracket? {
        return try {
            val leftIndex = text.getCharIndex(pair.leftLine, pair.leftColumn)
            val rightIndex = text.getCharIndex(pair.rightLine, pair.rightColumn)
            PairedBracket(leftIndex, pair.leftLength, rightIndex, pair.rightLength, pair.level)
        } catch (_: IndexOutOfBoundsException) {
            null
        }
    }

    private companion object {
        /**
         * Returns (lineCount, columnCount) for the given text chunk.
         * Mirrors VSCode's Length arithmetic where column count is taken
         * from the last line of the inserted text.
         */
        fun measureTextLength(text: CharSequence): Pair<Int, Int> {
            var lineCount = 0
            var lastBreakIndex = -1
            var i = 0
            val length = text.length

            while (i < length) {
                when (text[i]) {
                    '\n' -> {
                        lineCount++
                        lastBreakIndex = i
                    }

                    '\r' -> {
                        lineCount++
                        if (i + 1 < length && text[i + 1] == '\n') {
                            i++
                        }
                        lastBreakIndex = i
                    }
                }
                i++
            }

            val columnCount = if (lineCount == 0) {
                length
            } else {
                length - (lastBreakIndex + 1)
            }
            return lineCount to columnCount
        }

        private fun extractBracketPairs(configuration: LanguageConfiguration?): List<CharacterPair> {
            if (configuration == null) {
                return emptyList()
            }
            val prioritized = configuration.colorizedBracketPairs
            return when {
                !prioritized.isNullOrEmpty() -> prioritized
                configuration.brackets != null && configuration.brackets.isNotEmpty() && (prioritized == null || prioritized.isNotEmpty()) -> configuration.brackets
                else -> null
            } ?: return emptyList()
        }
    }

    /**
     * Tracks pending edits for Token AST updates.
     *
     * This class rebases all incoming edits to the same "before-edit" coordinate system
     * using EditCombiner, mirroring VS Code's combineTextEditInfos behavior.
     */
    private class EditTracker {
        private var pendingTokenEdits: List<EditInfo> = emptyList()

        fun addEdit(edit: EditInfo) {
            // Immediately combine with existing edits to maintain consistent coordinate system
            pendingTokenEdits = EditCombiner.combine(pendingTokenEdits, listOf(edit))
        }

        fun hasPendingTokenEdits(): Boolean = pendingTokenEdits.isNotEmpty()

        fun consumeTokenEdits(): List<EditInfo> {
            val edits = pendingTokenEdits
            pendingTokenEdits = emptyList()
            return edits
        }
    }
}
