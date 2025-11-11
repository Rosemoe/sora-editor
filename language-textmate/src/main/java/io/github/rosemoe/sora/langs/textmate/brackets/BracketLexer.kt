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

import android.util.SparseArray
import io.github.rosemoe.sora.text.Content
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.StandardTokenType
import org.eclipse.tm4e.languageconfiguration.internal.model.CharacterPair
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Lexer-style bracket scanner with native skip support. */
internal class BracketLexer(
    private val content: Content,
    private val snapshot: SpanSnapshot,
    bracketPairs: List<CharacterPair>
) {

    private val buckets = SparseArray<List<Descriptor>>()
    private val hasDescriptors: Boolean

    private var scanStartLine: Int = 0
    private var scanEndLine: Int = 0
    private var currentLine: Int = 0
    private var currentColumn: Int = 0
    private var initialized: Boolean = false

    private var cachedToken: BracketToken? = null
    private var cachedTokenValid: Boolean = false

    private var cachedLine: Int = -1
    private var cachedLineContent: CharSequence? = null
    private var cachedLineData: SpanSnapshot.LineData? = null

    init {
        if (bracketPairs.isEmpty()) {
            hasDescriptors = false
        } else {
            val tempBuckets = hashMapOf<Int, MutableList<Descriptor>>()

            bracketPairs.forEachIndexed { index, pair ->
                addDescriptor(tempBuckets, pair.open, index, true)
                addDescriptor(tempBuckets, pair.close, index, false)
            }

            // Convert to SparseArray and sort by length (longest first for greedy matching)
            for ((charCode, descriptorList) in tempBuckets) {
                descriptorList.sortByDescending { it.length }
                buckets.put(charCode, descriptorList.toList())
            }

            hasDescriptors = buckets.size() > 0
        }
    }

    fun beginScan(startLine: Int = 0, endLine: Int = BracketToken.MAX_LINE_INDEX) {
        if (!hasDescriptors) {
            initialized = false
            return
        }

        val contentLineCount = content.lineCount
        val spansLineCount = snapshot.lineCount()
        if (contentLineCount == 0 || spansLineCount == 0) {
            initialized = false
            return
        }

        val upperBound = min(contentLineCount - 1, spansLineCount - 1)
        val maxUsableLine = min(upperBound, endLine)
        val minUsableLine = max(0, startLine)

        if (maxUsableLine < minUsableLine) {
            initialized = false
            return
        }

        scanStartLine = minUsableLine
        scanEndLine = maxUsableLine
        currentLine = minUsableLine
        currentColumn = 0
        initialized = true
        cachedTokenValid = false
        cachedToken = null
        cachedLine = -1
        cachedLineContent = null
        cachedLineData = null
    }

    /** Skips directly to a position without scanning intermediate tokens. */
    fun skipTo(targetLine: Int, targetColumn: Int) {
        if (!initialized) return

        currentLine = targetLine.coerceIn(scanStartLine, scanEndLine + 1)
        currentColumn = max(0, targetColumn)
        cachedTokenValid = false

        if (cachedLine != currentLine) {
            cachedLine = -1
            cachedLineContent = null
            cachedLineData = null
        }
    }

    fun hasNext(): Boolean {
        if (!initialized) return false
        if (cachedTokenValid) return true

        cachedToken = scanNext()
        cachedTokenValid = cachedToken != null
        return cachedTokenValid
    }

    fun peek(): BracketToken? {
        if (!initialized) return null
        if (cachedTokenValid) return cachedToken

        cachedToken = scanNext()
        cachedTokenValid = cachedToken != null
        return cachedToken
    }

    fun next(): BracketToken? {
        if (!initialized) return null

        if (cachedTokenValid) {
            cachedTokenValid = false
            return cachedToken
        }

        return scanNext()
    }

    fun reset() {
        if (!initialized) return
        currentLine = scanStartLine
        currentColumn = 0
        cachedTokenValid = false
        cachedLine = -1
        cachedLineContent = null
        cachedLineData = null
    }

    private fun scanNext(): BracketToken? {
        return scanNextWithToken()
    }

    private fun scanNextWithToken(): BracketToken? {
        while (currentLine <= scanEndLine) {
            if (cachedLine != currentLine) {
                val lineData = snapshot.line(currentLine)
                val lineContent = content.getLine(currentLine)
                if (lineContent.isEmpty()) {
                    currentLine++
                    currentColumn = 0
                    continue
                }

                cachedLine = currentLine
                cachedLineContent = lineContent
                cachedLineData = lineData
            }

            val lineContent = cachedLineContent!!
            val lineData = cachedLineData!!
            val lineLength = lineContent.length
            if (lineLength == 0) {
                currentLine++
                currentColumn = 0
                continue
            }

            val useFallback = lineData.synthetic || lineData.spanCount == 0
            val columns: IntArray
            val tokenTypes: IntArray
            val spanCount: Int
            if (useFallback) {
                columns = FALLBACK_COLUMNS
                tokenTypes = FALLBACK_TOKENS
                spanCount = 1
            } else {
                columns = lineData.columns
                tokenTypes = lineData.tokenTypes
                spanCount = lineData.spanCount
            }

            for (segIdx in 0 until spanCount) {
                if (!isOtherToken(tokenTypes[segIdx])) continue

                val segStart = columns[segIdx].coerceIn(0, lineLength)
                val segEnd = if (segIdx + 1 < spanCount) {
                    columns[segIdx + 1].coerceIn(segStart, lineLength)
                } else {
                    lineLength
                }

                if (segStart >= segEnd) continue
                if (segEnd <= currentColumn) continue  // Skip past segments

                val startCol = max(currentColumn, segStart)

                for (col in startCol until segEnd) {
                    val ch = lineContent[col]
                    val lower = lowerCase(ch)

                    val bucket = buckets.get(lower.code)
                    if (bucket != null) {
                        for (desc in bucket) {
                            if (col + desc.length > lineLength) continue
                            if (desc.matches(lineContent, col)) {
                                currentColumn = col + desc.length
                                return BracketToken.create(
                                    currentLine, col, desc.length,
                                    desc.bracketId, desc.isOpening
                                )
                            }
                        }
                    }
                }
            }

            currentLine++
            currentColumn = 0
            cachedLine = -1
            cachedLineContent = null
            cachedLineData = null
        }

        return null
    }

    private fun isOtherToken(tokenType: Int): Boolean {
        return tokenType == StandardTokenType.Other
    }

    private fun addDescriptor(
        buckets: MutableMap<Int, MutableList<Descriptor>>,
        text: String?,
        bracketId: Int,
        isOpening: Boolean
    ) {
        if (text.isNullOrEmpty()) return
        require(bracketId in 0..BracketToken.MAX_BRACKET_ID) {
            "Bracket id exceeds supported range: $bracketId"
        }
        require(text.length <= BracketToken.MAX_LENGTH) {
            "Bracket text too long (max ${BracketToken.MAX_LENGTH}): $text"
        }

        val lower = text.lowercase(Locale.ROOT)
        val descriptor = Descriptor(
            lowerText = lower.toCharArray(),
            length = text.length,
            bracketId = bracketId,
            isOpening = isOpening
        )

        val key = descriptor.leadingChar.code
        buckets.getOrPut(key) { ArrayList(4) }.add(descriptor)
    }

    private data class Descriptor(
        val lowerText: CharArray,
        val length: Int,
        val bracketId: Int,
        val isOpening: Boolean
    ) {
        val leadingChar: Char
            get() = lowerText[0]

        fun matches(chars: CharSequence, offset: Int): Boolean {
            // Fast path for single character brackets
            if (length == 1) {
                return lowerCase(chars[offset]) == lowerText[0]
            }

            var i = 0
            while (i < length) {
                if (lowerCase(chars[offset + i]) != lowerText[i]) {
                    return false
                }
                i++
            }
            return true
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Descriptor

            if (length != other.length) return false
            if (bracketId != other.bracketId) return false
            if (isOpening != other.isOpening) return false
            if (!lowerText.contentEquals(other.lowerText)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = length
            result = 31 * result + bracketId
            result = 31 * result + isOpening.hashCode()
            result = 31 * result + lowerText.contentHashCode()
            return result
        }
    }

    private companion object {
        private const val ASCII_CACHE_SIZE = 128
        private val ASCII_LOWER_CACHE = CharArray(ASCII_CACHE_SIZE) { index ->
            Character.toLowerCase(index.toChar())
        }
        private val FALLBACK_COLUMNS = intArrayOf(0)
        private val FALLBACK_TOKENS = intArrayOf(StandardTokenType.Other)

        private fun lowerCase(value: Char): Char {
            val code = value.code
            return if (code < ASCII_CACHE_SIZE) {
                ASCII_LOWER_CACHE[code]
            } else {
                Character.toLowerCase(value)
            }
        }
    }
}
