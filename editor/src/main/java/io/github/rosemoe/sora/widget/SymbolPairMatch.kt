/*
 * sora-editor - the awesome code editor for Android
 * https://github.com/Rosemoe/sora-editor
 * Copyright (C) 2020-2024  Rosemoe
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details M
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 *
 * Please contact Rosemoe by email 2073412493@qq.com if you need
 * additional information or have any questions
 */
package io.github.rosemoe.sora.widget

import android.util.Log
import android.util.SparseArray
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import java.util.ArrayDeque
import kotlin.math.max
import kotlin.math.min

/**
 * Define symbol pairs to complete them automatically when the user
 * enters the first character of pair.
 *
 * This implementation is optimized to reduce object allocations by using sparse data structures
 * and efficient scanning algorithms inspired by VSCode's bracket matching system.
 * It scans a window of text around the cursor, identifies all brackets,
 * and uses a stack-based algorithm to determine which brackets are matched or unmatched.
 *
 * @author Rosemoe
 */
open class SymbolPairMatch(private var parent: SymbolPairMatch? = null) {

    companion object {
        private const val TAG = "SymbolPairMatch"

        /**
         * The number of characters to scan around the cursor for bracket matching.
         * Half will be scanned before the cursor, and half after.
         */
        private const val BRACKET_SCAN_LIMIT = 200
        private const val INVALID_INDEX = -1

        // Flags for bracket information encoding
        private const val FLAG_IS_OPENER = 1 shl 0
        private const val FLAG_IS_MATCHED = 1 shl 1
        private const val FLAG_IS_MISSING = 1 shl 2
    }

    private val pairs = mutableListOf<String>()
    private val extensions = mutableMapOf<String, SymbolPair.SymbolPairEx>()

    // Sparse arrays for memory efficiency
    private val lookupMap = mutableMapOf<Char, List<Int>>()
    private var sortedPairIndices = emptyList<Int>()
    private var lookupNeedsRebuild = true

    // Sparse array to store found brackets with their position and metadata flags.
    // This is rebuilt on every insertion check.
    private val bracketInfo = SparseArray<BracketData>()

    @JvmInline
    private value class BracketData(private val packedValue: Long) {
        constructor(pairIndex: Int, flags: Int) : this(
            (pairIndex.toLong() shl 32) or (flags.toLong() and 0xFFFFFFFFL)
        )

        val pairIndex: Int
            get() = (packedValue ushr 32).toInt()

        val flags: Int
            get() = packedValue.toInt()

        val isOpener: Boolean get() = (flags and FLAG_IS_OPENER) != 0
        val isMatched: Boolean get() = (flags and FLAG_IS_MATCHED) != 0
        val isMissing: Boolean get() = (flags and FLAG_IS_MISSING) != 0

        override fun toString(): String {
            return "BracketData(pairIndex=$pairIndex, isOpener=$isOpener, isMatched=$isMatched, isMissing=$isMissing)"
        }
    }

    fun setParent(parent: SymbolPairMatch?) {
        this.parent = parent
    }

    fun getParent(): SymbolPairMatch? {
        return parent
    }

    @Deprecated(
        "Use addPair instead",
        ReplaceWith("addPair(pair.open, pair.close,pair.symbolPairEx)")
    )
    fun putPair(singleChar: Char, pair: SymbolPair) {
        addPair(singleChar.toString(), pair.close, pair.symbolPairEx)
    }

    @Deprecated(
        "Use addPair instead",
        ReplaceWith("addPair(pair.open, pair.close,pair.symbolPairEx)")
    )
    fun putPair(singleChar: String, pair: SymbolPair) {
        addPair(singleChar, pair.close, pair.symbolPairEx)
    }

    fun addPair(pair: SymbolPair) {
        addPair(pair.open, pair.close, pair.symbolPairEx)
    }

    /**
     * Registers a symbol pair for auto-completion.
     * @param open The opening symbol (e.g., "{", "begin").
     * @param close The closing symbol (e.g., "}", "end").
     * @param extension Optional extension for custom logic like conditional replacement.
     */
    fun addPair(open: String, close: String, extension: SymbolPair.SymbolPairEx? = null) {
        if (open.isEmpty() || close.isEmpty()) {
            return
        }
        pairs.add(open)
        pairs.add(close)
        if (extension != null) {
            extensions[open] = extension
        }
        lookupNeedsRebuild = true
    }

    private fun ensureLookupsUpToDate() {
        if (lookupNeedsRebuild) {
            val pairIndices = pairs.indices.step(2)

            // Sort by length descending to match longer symbols first (e.g., `[[` before `[`)
            sortedPairIndices = pairIndices.sortedByDescending {
                max(pairs[it].length, pairs[it + 1].length)
            }

            lookupMap.clear()
            val tempMap = mutableMapOf<Char, MutableList<Int>>()
            for (i in pairIndices) {
                val open = pairs[i]
                val close = pairs[i + 1]
                tempMap.getOrPut(open[0]) { mutableListOf() }.add(i)
                tempMap.getOrPut(close[0]) { mutableListOf() }.add(i)
            }
            // Ensure unique indices, though the logic should already handle this.
            tempMap.forEach { (key, value) -> lookupMap[key] = value.distinct() }

            lookupNeedsRebuild = false
        }
    }

    /**
     * The main entry point to determine the action upon text insertion.
     * This method is called BEFORE the user's text is inserted into the content.
     */
    fun matchAndInsert(
        editor: CodeEditor,
        position: CharPosition,
        insertText: String
    ): InsertAction {
        Log.d(TAG, "========================")
        Log.d(TAG, "matchAndInsert: position=${position.index}, insertText='${insertText}'")
        ensureLookupsUpToDate()
        val content = editor.text

        val userTypedPairIndex = findPairForInsertionTrigger(content, position.index, insertText)
            ?: parent?.findPairForInsertionTrigger(content, position.index, insertText)
            ?: return InsertAction.None.also { Log.d(TAG, "  -> No trigger pair found. Returning None.") }

        val openSymbol = pairs[userTypedPairIndex]
        val closeSymbol = pairs[userTypedPairIndex + 1]

        val isTypingOpener = insertText == openSymbol || (openSymbol.length > 1 && openSymbol.endsWith(insertText))
        Log.d(TAG, "  -> Trigger found: pairIndex=$userTypedPairIndex, open='$openSymbol', close='$closeSymbol', isTypingOpener=$isTypingOpener")


        if (extensions[openSymbol]?.shouldReplace(
                editor,
                content.getLine(position.line),
                position.column
            ) == false
        ) {
            return InsertAction.None.also { Log.d(TAG, "  -> Extension#shouldReplace returned false. Returning None.") }
        }

        if (content.cursor.isSelected) {
            if (extensions[openSymbol]?.shouldDoAutoSurround(content) == true) {
                return InsertAction.SurroundSelection(openSymbol, closeSymbol).also { Log.d(TAG, "  -> Selection active and surround is true. Returning SurroundSelection.") }
            }
        }

        // Scan the document window around the cursor to understand the bracket context.
        scanDocumentWindow(content, position)
        Log.d(TAG, "  -> Scan complete. Bracket at cursor (pos=${position.index}): ${getBracketAtPosition(position.index)}")


        val action = if (isTypingOpener) {
            // Heuristic 1: Typing an opener right before the exact same opener.
            // e.g., typing '{' at <cursor> in "<cursor>{...}"
            val nextCharIsSameOpener =
                (insertText == openSymbol) &&
                        (position.index + openSymbol.length <= content.length) &&
                        (content.substring(
                            position.index,
                            position.index + openSymbol.length
                        ) == openSymbol)

            // Heuristic 2: Typing an opener right before its corresponding, but currently unmatched, closer.
            // e.g. typing '(' at <cursor> in "<cursor>)"
            val bracketAtCursor = getBracketAtPosition(position.index)
            val isBeforeUnmatchedCloser = bracketAtCursor != null &&
                    !bracketAtCursor.isOpener &&
                    bracketAtCursor.pairIndex == userTypedPairIndex &&
                    !bracketAtCursor.isMatched // Key: the closer is not part of a valid pair yet

            if (nextCharIsSameOpener || isBeforeUnmatchedCloser) {
                Log.d(
                    TAG,
                    "  -> Suppressing completion. Same opener ahead: $nextCharIsSameOpener, Before unmatched closer: $isBeforeUnmatchedCloser"
                )
                InsertAction.None
            } else {
                Log.d(TAG, "  -> Safe to complete pair.")
                InsertAction.CompletePair(closeSymbol)
            }
        } else {
            // Logic for typing a closing symbol.
            val bracketAtCursor = getBracketAtPosition(position.index)
            if (bracketAtCursor != null &&
                !bracketAtCursor.isOpener &&
                bracketAtCursor.pairIndex == userTypedPairIndex &&
                bracketAtCursor.isMatched
            ) {
                // This is a potential skip case.
                // We should skip only if there isn't an unmatched opener of the same type
                // to the left of the current pair's context.
                val partnerPos = findMatchingBracketPos(position.index)
                if (partnerPos == -1) {
                    // This should not happen if isMatched is true, but as a safeguard:
                    Log.d(TAG, "  -> Matched bracket at cursor has no partner. Inserting.")
                    InsertAction.None
                } else {
                    var hasUnmatchedOuterOpener = false
                    val pairIndex = bracketAtCursor.pairIndex
                    for (i in 0 until bracketInfo.size()) {
                        val pos = bracketInfo.keyAt(i)
                        if (pos >= partnerPos) {
                            // We only care about brackets before our matched pair begins.
                            break
                        }
                        val bracket = bracketInfo.valueAt(i)
                        if (bracket.isOpener && bracket.pairIndex == pairIndex && bracket.isMissing) {
                            hasUnmatchedOuterOpener = true
                            break
                        }
                    }

                    Log.d(TAG, "  -> Typing closer. Found unmatched outer opener: $hasUnmatchedOuterOpener")
                    if (hasUnmatchedOuterOpener) {
                        Log.d(TAG, "  -> Unmatched outer opener found. Inserting instead of skipping.")
                        InsertAction.None // Insert a new bracket
                    } else {
                        Log.d(TAG, "  -> No unmatched outer openers found. Skipping.")
                        InsertAction.MoveCursor(closeSymbol.length)
                    }
                }
            } else {
                Log.d(TAG, "  -> Typing closer. No matched closer at cursor. Inserting.")
                InsertAction.None
            }
        }
        Log.d(TAG, "  -> Returning action: $action")
        Log.d(TAG, "========================")
        return action
    }

    /**
     * Finds a symbol pair that could be triggered by the user's insertion.
     * @return The index of the opening symbol in the `pairs` list, or null if no pair is triggered.
     */
    private fun findPairForInsertionTrigger(
        text: CharSequence,
        pos: Int,
        insertText: String
    ): Int? {
        // First, check for an exact match for the typed text (e.g., "{", "(", "\"").
        val exactMatchIndex = pairs.indexOf(insertText)
        if (exactMatchIndex != INVALID_INDEX) {
            // Return the index of the opening symbol of the pair.
            return if (exactMatchIndex % 2 == 0) exactMatchIndex else exactMatchIndex - 1
        }

        // Second, for multi-character symbols, check if typing the last character completes the symbol.
        // Example: If "begin" is a symbol, typing "n" after "begi" should trigger.
        for (i in sortedPairIndices) {
            val open = pairs[i]
            // We assume a single character is typed to complete a multi-char symbol.
            if (open.length > 1 && insertText.length == 1 && open.endsWith(insertText)) {
                val prefixStart = pos - (open.length - 1)
                if (prefixStart >= 0 && text.regionMatches(
                        prefixStart,
                        open,
                        0,
                        open.length - 1
                    )
                ) {
                    return i
                }
            }
        }
        return null
    }

    /**
     * Finds a symbol (open or close) that starts at the given position in the content.
     * @return A pair containing the open symbol's index and a a boolean indicating if it's an opener.
     */
    private fun findSymbolStartingAt(
        content: Content,
        startPos: Int
    ): Pair<Int, Boolean>? {
        if (startPos < 0 || startPos >= content.length) return null

        val char = content[startPos]
        val candidateIndices = lookupMap[char] ?: return null

        // Use sortedPairIndices to prioritize longer matches (e.g., `[[` over `[`)
        for (i in sortedPairIndices) {
            if (i in candidateIndices) {
                val open = pairs[i]
                if (open.startsWith(char) && content.length >= startPos + open.length) {
                    if (content.substring(startPos, startPos + open.length) == open) {
                        return i to true // isOpener
                    }
                }
                val close = pairs[i + 1]
                if (close.startsWith(char) && content.length >= startPos + close.length) {
                    if (content.substring(startPos, startPos + close.length) == close) {
                        return i to false // isCloser
                    }
                }
            }
        }
        return null
    }

    /**
     * Scans a window around the cursor position and builds bracket information.
     */
    private fun scanDocumentWindow(content: Content, cursorPos: CharPosition) {
        bracketInfo.clear()

        val halfLimit = BRACKET_SCAN_LIMIT / 2
        val scanStart = max(0, cursorPos.index - halfLimit)
        val scanEnd = min(content.length, cursorPos.index + halfLimit)
        Log.d(TAG, "scanDocumentWindow: cursorPos=${cursorPos.index}, scanRange=[$scanStart, $scanEnd)")
        try {
            Log.d(TAG, "  -> scanContent='${content.subSequence(scanStart, scanEnd)}'")
        } catch (e: Exception) {
            Log.w(TAG, "  -> Failed to log scan content: $e")
        }


        // First pass: find all symbols in the window and store them with initial flags.
        var currentPos = scanStart
        while (currentPos < scanEnd) {
            val found = findSymbolStartingAt(content, currentPos)
            if (found != null) {
                val (pairIndex, isOpener) = found
                val flags = if (isOpener) FLAG_IS_OPENER else 0
                val bracket = BracketData(pairIndex, flags)
                bracketInfo.put(currentPos, bracket)
                Log.d(TAG, "  -> Found symbol at $currentPos: $bracket")
                val symbolLen = if (isOpener) pairs[pairIndex].length else pairs[pairIndex + 1].length
                currentPos += symbolLen
            } else {
                currentPos++
            }
        }

        // Second pass: match the found brackets.
        matchBracketsInScannedWindow()
    }

    /**
     * Matches brackets using a more robust stack-based algorithm that can handle improperly nested
     * pairs (e.g., "([)]").
     * This pass updates the flags in `bracketInfo` to mark brackets as matched or missing.
     */
    private fun matchBracketsInScannedWindow() {
        Log.d(TAG, "matchBracketsInScannedWindow: processing ${bracketInfo.size()} brackets")
        val stack = ArrayDeque<Pair<Int, BracketData>>() // Stores: position to bracket

        // The keys in SparseArray are sorted, so we can iterate by index.
        for (i in 0 until bracketInfo.size()) {
            val position = bracketInfo.keyAt(i)
            val bracket = bracketInfo.valueAt(i)
            Log.d(TAG, "  -> Processing bracket at $position: $bracket")

            if (bracket.isOpener) {
                stack.push(position to bracket)
                Log.d(TAG, "     -> Pushing opener to stack. Stack size: ${stack.size}")
            } else { // isCloser
                if (stack.isNotEmpty() && stack.peek().second.pairIndex == bracket.pairIndex) {
                    // Simple case: The opener is right on top of the stack.
                    val (openerPos, openerBracket) = stack.pop()
                    val newOpener = BracketData(openerBracket.pairIndex, openerBracket.flags or FLAG_IS_MATCHED)
                    val newCloser = BracketData(bracket.pairIndex, bracket.flags or FLAG_IS_MATCHED)
                    bracketInfo.put(openerPos, newOpener)
                    bracketInfo.put(position, newCloser)
                    Log.d(TAG, "     -> Found simple match for closer at $position with opener at $openerPos. Stack size: ${stack.size}")
                } else {
                    // Complex case: The opener is not on top. This might be an incorrect nesting like ([)].
                    // We search back through the stack to find a matching opener.
                    val iterator = stack.descendingIterator()
                    var openerToMatch: Pair<Int, BracketData>? = null
                    var matched = false

                    // Find the most recent matching opener on the stack
                    while (iterator.hasNext()) {
                        val openerCandidate = iterator.next()
                        if (openerCandidate.second.pairIndex == bracket.pairIndex) {
                            openerToMatch = openerCandidate
                            break
                        }
                    }

                    if (openerToMatch != null) {
                        Log.d(TAG, "     -> Found complex match for closer at $position with opener at ${openerToMatch.first}.")
                        // We found a match. Pop from the real stack until we get to it, marking intervening brackets as missing.
                        while (stack.isNotEmpty()) {
                            val popped = stack.pop()
                            if (popped.first == openerToMatch.first) {
                                // This is our matched opener. Mark it and the closer.
                                val newOpener = BracketData(popped.second.pairIndex, popped.second.flags or FLAG_IS_MATCHED)
                                val newCloser = BracketData(bracket.pairIndex, bracket.flags or FLAG_IS_MATCHED)
                                bracketInfo.put(popped.first, newOpener)
                                bracketInfo.put(position, newCloser)
                                Log.d(TAG, "        -> Marked opener at ${popped.first} as matched. Stack size: ${stack.size}")
                                matched = true
                                break // Exit the inner while loop
                            } else {
                                // This is an intervening, unmatched opener.
                                val newUnmatched = BracketData(popped.second.pairIndex, popped.second.flags or FLAG_IS_MISSING)
                                bracketInfo.put(popped.first, newUnmatched)
                                Log.d(TAG, "        -> Intervening bracket at ${popped.first} marked as missing.")
                            }
                        }
                    }

                    if (!matched) {
                        // No matching opener found on stack at all.
                        val newCloser = BracketData(bracket.pairIndex, bracket.flags or FLAG_IS_MISSING)
                        bracketInfo.put(position, newCloser)
                        Log.d(TAG, "     -> No match for closer at $position. Marking as missing.")
                    }
                }
            }
        }

        // Any openers left on the stack are unmatched.
        while (stack.isNotEmpty()) {
            val (pos, b) = stack.pop()
            val newOpener = BracketData(b.pairIndex, b.flags or FLAG_IS_MISSING)
            bracketInfo.put(pos, newOpener)
            Log.d(TAG, "  -> Marking remaining opener at $pos as missing.")
        }

        Log.d(TAG, "matchBracketsInScannedWindow: Final bracket states:")
        for (i in 0 until bracketInfo.size()) {
            val position = bracketInfo.keyAt(i)
            val finalBracket = bracketInfo.valueAt(i)
            Log.d(TAG, "  -> Final state at $position: $finalBracket")
        }
    }

    /**
     * Given the position of a bracket, finds the position of its matched partner.
     * @param startPos The character index of the bracket to find the match for.
     * @return The position of the matching bracket, or -1 if not found or not matched.
     */
    private fun findMatchingBracketPos(startPos: Int): Int {
        val startIndex = bracketInfo.indexOfKey(startPos)
        if (startIndex < 0) return -1

        val bracketData = bracketInfo.valueAt(startIndex)
        if (!bracketData.isMatched) return -1

        val pairIndex = bracketData.pairIndex
        if (bracketData.isOpener) {
            var balance = 1
            for (i in startIndex + 1 until bracketInfo.size()) {
                val bracket = bracketInfo.valueAt(i)
                if (bracket.pairIndex == pairIndex) {
                    if (bracket.isOpener) balance++ else balance--
                    if (balance == 0) return bracketInfo.keyAt(i)
                }
            }
        } else { // isCloser
            var balance = 1
            for (i in startIndex - 1 downTo 0) {
                val bracket = bracketInfo.valueAt(i)
                if (bracket.pairIndex == pairIndex) {
                    if (!bracket.isOpener) balance++ else balance--
                    if (balance == 0) return bracketInfo.keyAt(i)
                }
            }
        }
        return -1
    }


    /**
     * Gets bracket information for a symbol starting at a specific position.
     */
    private fun getBracketAtPosition(position: Int): BracketData? {
        return bracketInfo.get(position)
    }

    fun removeAllPairs() {
        pairs.clear()
        extensions.clear()
        sortedPairIndices = emptyList()
        lookupMap.clear()
        bracketInfo.clear()
        lookupNeedsRebuild = true
    }

    sealed class InsertAction {
        object None : InsertAction()
        data class MoveCursor(val characterCount: Int) : InsertAction()
        data class CompletePair(val closeText: String) : InsertAction()
        data class SurroundSelection(val openText: String, val closeText: String) : InsertAction()
    }

    class SymbolPair(
        val open: String,
        val close: String,
        val symbolPairEx: SymbolPairEx? = null
    ) {
        companion object {
            @JvmStatic
            val EMPTY_SYMBOL_PAIR = SymbolPair("", "")
        }

        interface SymbolPairEx {
            fun shouldReplace(
                editor: CodeEditor,
                currentLine: io.github.rosemoe.sora.text.ContentLine,
                leftColumn: Int
            ): Boolean = true

            fun shouldDoAutoSurround(content: Content): Boolean = false
        }
    }

    class DefaultSymbolPairs : SymbolPairMatch() {
        init {
            addPair("{", "}")
            addPair("(", ")")
            addPair("[", "]")
            addPair("<", ">")
            val quoteEx = object : SymbolPair.SymbolPairEx {
                override fun shouldDoAutoSurround(content: Content): Boolean {
                    return content.cursor.isSelected
                }
            }
            addPair("\"", "\"", quoteEx)
            addPair("'", "'", quoteEx)
        }
    }
}

