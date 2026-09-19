/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

/**
 * Immutable node of the bracket tree.
 *
 * An incremental parse copies only the nodes on the changed path, so a reader on the UI thread can
 * keep an older root alive while the analyzer builds a new one. Every node caches what a subtree
 * skip or a range query needs:
 *
 * | field | meaning |
 * | --- | --- |
 * | [length] | total span of the node, as a relative offset |
 * | [height] | height of the balanced subtree, used by `concat` and `balance` |
 * | [missingOpeners] | opener ids this node still expects a closer for |
 * | [reusable] | whether an incremental parse may take the node over |
 * | [nesting] | colorized pairs inside the node, used to bound recursion |
 */
internal sealed class BracketNode(
    val length: TextOffset,
    val height: Int = 0,
    val missingOpeners: BracketIdSet = BracketIdSet.EMPTY,
    val reusable: Boolean = true,
    val nesting: Int = 0
) {

    open val children: List<BracketNode>
        get() = emptyList()
}

/** A run of plain text or whitespace. */
internal class TextNode(length: TextOffset) : BracketNode(length)

/**
 * A bracket-shaped token produced by the tokenizer.
 *
 * Words are supported too (`begin`/`end` style pairs), which is why [text] holds the whole token
 * rather than one character.
 *
 * @param openingId id of the bracket type this token opens, or `null` if it can only close
 * @param ids bracket types this token is able to close
 * @param colorizedIds subset of [ids] that is configured for colorization
 */
internal class Delimiter(
    val text: String,
    val openingId: Int?,
    val ids: BracketIdSet,
    val colorizedIds: BracketIdSet = BracketIdSet.EMPTY
) : BracketNode(TextOffset.of(0, text.length), reusable = false)

/** A closer with no matching opener. Never reusable. */
internal class UnexpectedNode(val bracket: Delimiter) : BracketNode(
    bracket.length,
    missingOpeners = bracket.ids
)

/**
 * One matched pair. A `null` [close] marks an opener that never found its closer.
 */
internal class PairNode(
    val open: Delimiter,
    val body: BracketNode?,
    val close: Delimiter?
) : BracketNode(
    open.length + (body?.length ?: TextOffset.ZERO) + (close?.length ?: TextOffset.ZERO),
    missingOpeners = body?.missingOpeners ?: BracketIdSet.EMPTY,
    reusable = close != null,
    nesting = 1 + (body?.nesting ?: 0)
) {

    override val children = listOfNotNull(open, body, close)

    /** Whether the pair draws a color, that is it is closed by a colorized bracket type. */
    val colorized = close == null || close.colorizedIds.intersects(open.ids)
}

/** A node holding 2..3 children, all of equal height. */
internal class ListNode(override val children: List<BracketNode>) : BracketNode(
    children.totalLength(),
    children.first().height + 1,
    children.fold(BracketIdSet.EMPTY) { ids, child -> ids + child.missingOpeners },
    children.last().reusable,
    children.maxOf { it.nesting }
)

private fun List<BracketNode>.totalLength() =
    fold(TextOffset.ZERO) { total, child -> total + child.length }
