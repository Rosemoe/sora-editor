/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
package io.github.rosemoe.sora.langs.textmate.brackets

/**
 * Turns a token stream into a balanced bracket tree.
 *
 * When an [edit] and the previous [oldRoot] are supplied, unchanged subtrees are taken over instead
 * of being parsed again. That is what keeps typing cheap on very long lines.
 */
internal class BracketParser(
    private val tokens: BracketTokenizer,
    oldRoot: BracketNode? = null,
    private val edit: BracketEdit? = null
) {

    private val reader = oldRoot?.let(::BracketTreeReader)

    /** Number of subtrees taken over from the old tree. Reported by tests and diagnostics. */
    var reusedNodes = 0
        private set

    fun parse(): BracketNode = parseList(BracketIdSet.EMPTY, 0) ?: TextNode(TextOffset.ZERO)

    /**
     * Parse items until the current token cannot belong to this list any more: either an unmatched
     * closer that would terminate an [ancestors] opener, or the end of the document.
     */
    private fun parseList(ancestors: BracketIdSet, depth: Int): BracketNode? {
        val items = ArrayList<BracketNode>()
        while (true) {
            // Reuse before peeking: a peek would scan text that a skipped subtree covers entirely.
            val limit = edit?.distanceToChange(tokens.offset)
            val reused = tryReuse(ancestors, depth, limit)
            if (reused != null) {
                reusedNodes++
                tokens.skip(reused.length)
                items.add(reused)
                continue
            }
            limitTextChunk(limit)
            val token = tokens.peek() ?: break
            if (token is Delimiter && token.openingId == null && token.ids.intersects(ancestors)) break
            tokens.read()
            items.add(parseItem(token, ancestors, depth))
        }
        return balance(items)
    }

    /**
     * Take over the old subtree starting at the current offset if it is reusable, still inside the
     * changed region's reach and does not nest deeper than [MAX_NESTING].
     */
    private fun tryReuse(ancestors: BracketIdSet, depth: Int, limit: TextOffset?): BracketNode? {
        val oldReader = reader ?: return null
        if (limit == TextOffset.ZERO) return null
        val oldOffset = edit?.oldPosition(tokens.offset) ?: tokens.offset
        return oldReader.read(oldOffset) { node ->
            node.reusable &&
                (limit == null || node.length < limit) &&
                !node.missingOpeners.intersects(ancestors) &&
                depth + node.nesting <= MAX_NESTING
        }
    }

    /**
     * Keep the tokenizer's text chunks aligned with the old tree. Without this bound a
     * one-character shift makes every later chunk miss its previous start position.
     */
    private fun limitTextChunk(limit: TextOffset?) {
        val next = reader?.nextOffset?.takeIf { limit != TextOffset.ZERO } ?: return
        val mapped = edit?.newPosition(next) ?: next
        tokens.textLimit = mapped.takeIf { it > tokens.offset }
    }

    private fun parseItem(token: BracketNode, ancestors: BracketIdSet, depth: Int): BracketNode = when {
        token !is Delimiter -> token
        token.openingId == null -> UnexpectedNode(token)
        depth >= MAX_NESTING -> TextNode(token.length)
        else -> {
            val body = parseList(ancestors + token.ids, depth + 1)
            val next = tokens.peek()
            val close = (next as? Delimiter)?.takeIf {
                it.openingId == null && it.ids.intersects(token.ids)
            }
            if (close != null) tokens.read()
            PairNode(token, body, close)
        }
    }

    companion object {
        /** Deepest nesting the parser tracks; deeper openers degrade to plain text. */
        const val MAX_NESTING = 150
    }
}
