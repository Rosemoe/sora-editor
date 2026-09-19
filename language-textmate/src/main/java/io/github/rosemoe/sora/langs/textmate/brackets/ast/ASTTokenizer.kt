/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
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

