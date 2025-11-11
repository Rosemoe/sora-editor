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
package io.github.rosemoe.sora.langs.textmate.brackets.ast

import io.github.rosemoe.sora.langs.textmate.brackets.BracketLexer
import io.github.rosemoe.sora.langs.textmate.brackets.BracketToken

/** Supplies bracket tokens to the AST parser. */
interface ASTTokenizer {
    val position: Position

    val hasMore: Boolean

    fun peek(): BracketToken?

    fun nextBracket(): BracketToken?

    fun skip(length: Length)

    fun lengthToNextToken(): Length?

    fun reset()
}

/** Adapter over [BracketLexer] that exposes the [ASTTokenizer] contract. */
internal class BracketTokenizer(
    private val lexer: BracketLexer,
    private val startLine: Int = 0,
    private val endLine: Int = BracketToken.MAX_LINE_INDEX
) : ASTTokenizer {
    private var currentPosition = Position.ZERO
    private var initialized = false

    override val position: Position
        get() = currentPosition

    override val hasMore: Boolean
        get() {
            ensureInitialized()
            return lexer.hasNext()
        }

    override fun peek(): BracketToken? {
        ensureInitialized()
        return lexer.peek()
    }

    override fun nextBracket(): BracketToken? {
        ensureInitialized()
        val token = lexer.next() ?: return null

        // Update position to end of this token
        val tokenPos = Position.of(token.line, token.column)
        currentPosition = tokenPos + Length.of(0, token.length)

        return token
    }

    override fun skip(length: Length) {
        ensureInitialized()

        // Update position
        currentPosition += length

        // Use lexer's native skipTo for efficiency
        // This is much faster than repeatedly calling nextBracket()
        lexer.skipTo(currentPosition.line, currentPosition.column)
    }

    override fun lengthToNextToken(): Length? {
        ensureInitialized()
        val next = peek() ?: return null
        val nextPos = Position.of(next.line, next.column)

        if (nextPos <= currentPosition) {
            return Length.ZERO
        }

        return nextPos - currentPosition
    }

    override fun reset() {
        lexer.beginScan(startLine, endLine)
        currentPosition = Position.ZERO
        initialized = true
    }

    private fun ensureInitialized() {
        if (!initialized) {
            lexer.beginScan(startLine, endLine)
            initialized = true
        }
    }
}

