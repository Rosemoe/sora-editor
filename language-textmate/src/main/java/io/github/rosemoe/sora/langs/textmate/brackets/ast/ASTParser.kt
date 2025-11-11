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

import io.github.rosemoe.sora.langs.textmate.brackets.BracketToken

/** Recursive descent parser with incremental reuse. */
internal class ASTParser(
    private val tokenizer: ASTTokenizer,
    private val positionMapper: BeforeEditPositionMapper?,
    private val nodeReader: NodeReader?
) {

    fun parse(): ASTNode {
        return ASTObjectPool.withArrayList { nodes ->
            parseList(anchorSet = AnchorSet.EMPTY, level = 0, receiver = nodes)
            if (nodes.isEmpty()) {
                TextAST.EMPTY
            } else {
                buildList(nodes)
            }
        }
    }

    private fun parseList(anchorSet: AnchorSet, level: Int, receiver: ArrayList<ASTNode>) {

        while (tokenizer.hasMore) {
            val reused = tryReuseNode(anchorSet)
            if (reused != null) {
                receiver.add(reused)
                continue
            }

            val child = parseChild(anchorSet, level + 1) ?: break

            if (child is ListAST && child.children.isEmpty()) {
                continue
            }

            receiver.add(child)
        }
    }

    private fun parseChild(anchorSet: AnchorSet, level: Int): ASTNode? {
        val textLength = tokenizer.lengthToNextToken()
        if (textLength != null && textLength.isNonZero()) {
            tokenizer.skip(textLength)
            return TextAST.of(textLength)
        }

        val token = tokenizer.peek() ?: return null

        return when {
            !token.isOpening -> {
                if (token.bracketId in anchorSet) {
                    null
                } else {
                    tokenizer.nextBracket()
                    InvalidBracketAST(
                        bracketId = token.bracketId,
                        length = Length.of(0, token.length)
                    )
                }
            }

            level > MAX_PARSE_DEPTH -> {
                tokenizer.nextBracket()
                TextAST.of(Length.of(0, token.length))
            }

            else -> {
                parseBracketPair(token, anchorSet, level)
            }
        }
    }

    private fun parseBracketPair(opening: BracketToken, parentAnchorSet: AnchorSet, level: Int): ASTNode {
        tokenizer.nextBracket() // Consume opening bracket

        val openingNode = BracketAST(
            Length.of(0, opening.length),
            opening.bracketId,
            true
        )

        val newAnchorSet = parentAnchorSet + opening.bracketId

        val child = ASTObjectPool.withArrayList { content ->
            parseList(newAnchorSet, level, content)

            when {
                content.isEmpty() -> null
                content.size == 1 -> content.first()
                else -> buildList(content)
            }
        }

        val next = tokenizer.peek()
        val closingNode = if (next != null && !next.isOpening && next.bracketId == opening.bracketId) {
            tokenizer.nextBracket()
            BracketAST(
                Length.of(0, next.length),
                next.bracketId,
                false
            )
        } else {
            null
        }

        return BracketPairAST(openingNode, child, closingNode)
    }

    private fun buildList(nodes: List<ASTNode>): ASTNode {
        return if (nodeReader != null) {
            TreeBalancer.balance(nodes)
        } else {
            TreeBalancer.balanceSameHeight(nodes, makeImmutable = true)
        }
    }

    private fun tryReuseNode(anchorSet: AnchorSet): ASTNode? {
        if (nodeReader == null || positionMapper == null) {
            return null
        }

        val currentOffset = tokenizer.position.toLength()
        val maxCacheableLength = positionMapper.getDistanceToNextChange(currentOffset)

        if (maxCacheableLength != null && maxCacheableLength.isZero()) {
            return null
        }

        val oldOffset = positionMapper.getOffsetBeforeChange(currentOffset)

        val node = nodeReader.readLongestNodeAt(oldOffset) { curNode ->
            // CRITICAL: Check length BEFORE calling canBeReused
            // The edit could extend the ending token, thus we cannot re-use nodes that touch the edit.
            // If there is no edit anymore, we can re-use the node in any case.
            if (maxCacheableLength != null && curNode.length >= maxCacheableLength) {
                // Either the node contains edited text or touches edited text.
                // In the latter case, brackets might have been extended (`end` -> `ending`),
                // so even touching nodes cannot be reused.

                return@readLongestNodeAt false
            }

            // Now check if the node can be reused based on its content
            // (anchor set compatibility, closed brackets, etc.)
            curNode.canBeReused(anchorSet)
        }

        if (node != null) {
            tokenizer.skip(node.length)
            return node
        }

        return null
    }

    companion object {
        private const val TAG = "ASTParser"
        private const val MAX_PARSE_DEPTH = 300

        fun parseFromScratch(tokenizer: ASTTokenizer): ASTNode {
            tokenizer.reset()
            val parser = ASTParser(
                tokenizer,
                null,
                null
            )
            return parser.parse()
        }

        fun parseIncremental(
            tokenizer: ASTTokenizer,
            oldRoot: ASTNode?,
            positionMapper: BeforeEditPositionMapper
        ): ASTNode {
            tokenizer.reset()
            val nodeReader = if (oldRoot != null) NodeReader(oldRoot) else null
            val parser = ASTParser(tokenizer, positionMapper, nodeReader)
            return parser.parse()
        }
    }
}
