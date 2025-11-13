/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets.ast

import io.github.rosemoe.sora.langs.textmate.brackets.BracketPair
import io.github.rosemoe.sora.util.IntPair
import kotlin.math.max

internal class BracketMatcherAST(
    private val rootProvider: () -> ASTNode
) {

    private val scratchPairs = ArrayList<BracketPair>()

    fun matchBracket(line: Int, column: Int): BracketPair? {
        val maxBracketLength = 3
        val searchStartColumn = (column - maxBracketLength).coerceAtLeast(0)
        val searchEndColumn = column + 1

        val tempPairs = ArrayList<BracketPair>()
        collectPairsInRangeRecInternal(
            getRootNode(),
            Position.ZERO,
            Position.of(line, searchStartColumn),
            Position.of(line, searchEndColumn),
            tempPairs,
            0,
            HashMap()
        )

        var bestMatch: BracketPair? = null

        for (pair in tempPairs) {
            val inOpening = pair.openingBracketContainsPosition(line, column)
            val inClosing = pair.closingBracketContainsPosition(line, column)

            if (inOpening || inClosing) {
                if (bestMatch != null && bestMatch != pair) {
                    BracketPair.recycle(bestMatch)
                }
                bestMatch = pair
            } else {
                BracketPair.recycle(pair)
            }
        }

        tempPairs.clear()
        return bestMatch
    }

    fun findPairAt(line: Int, column: Int): BracketPair? {
        val pos = Position.of(line, column)

        scratchPairs.clear()
        collectPairsInRangeRecInternal(
            getRootNode(),
            Position.ZERO,
            pos,
            pos + Length.of(0, 1),
            scratchPairs,
            0,
            HashMap()
        )

        var bestMatch: BracketPair? = null

        for (pair in scratchPairs) {
            if (pair.openingBracketContainsPosition(line, column) ||
                pair.closingBracketContainsPosition(line, column)) {
                bestMatch = pair
            } else {
                BracketPair.recycle(pair)
            }
        }

        scratchPairs.clear()
        return bestMatch
    }

    fun collectPairsInRange(left: Long, right: Long, receiver: MutableList<BracketPair>) {
        receiver.clear()

        val leftLine = (left ushr 32).toInt()
        val leftCol = (left and 0xFFFFFFFF).toInt()
        val rightLine = (right ushr 32).toInt()
        val rightCol = (right and 0xFFFFFFFF).toInt()

        val leftPos = Position.of(leftLine, leftCol)
        val rightPos = Position.of(rightLine, rightCol)

        collectPairsInRangeRec(
            getRootNode(),
            Position.ZERO,
            leftPos,
            rightPos,
            receiver,
            0
        )
    }

    fun invalidateCache() {}

    private fun getRootNode(): ASTNode {
        return rootProvider()
    }

    private fun collectPairsInRangeRec(
        initialNode: ASTNode,
        initialNodePos: Position,
        leftPos: Position,
        rightPos: Position,
        receiver: MutableList<BracketPair>,
        initialNestingLevel: Int
    ) {
        val levelPerBracketType = HashMap<Int, Int>()

        collectPairsInRangeRecInternal(
            initialNode,
            initialNodePos,
            leftPos,
            rightPos,
            receiver,
            initialNestingLevel,
            levelPerBracketType
        )
    }

    private fun collectPairsInRangeRecInternal(
        initialNode: ASTNode,
        initialNodePos: Position,
        leftPos: Position,
        rightPos: Position,
        receiver: MutableList<BracketPair>,
        initialNestingLevel: Int,
        levelPerBracketType: MutableMap<Int, Int>
    ) {
        var node = initialNode
        var nodePos = initialNodePos

        whileLoop@ while (true) {
            val nodeEnd = nodePos + node.length

            if (nodeEnd <= leftPos || nodePos >= rightPos) {
                return
            }

            when (node) {
                is BracketPairAST -> {
                    val levelPerBracket = levelPerBracketType.getOrPut(node.bracketId) { 0 }

                    levelPerBracketType[node.bracketId] = levelPerBracket + 1

                    var pos = nodePos
                    val openLen = node.openingBracket.length
                    val openEnd = pos + openLen

                    pos = openEnd
                    val contentLen = node.child?.length ?: Length.ZERO
                    val contentEnd = pos + contentLen

                    val closingLen = node.closingBracket?.length ?: Length.ZERO
                    val closePos = contentEnd
                    val closeEnd = closePos + closingLen

                    if (node.closingBracket != null) {
                        val openingVisible = nodePos in leftPos..<rightPos
                        val closingVisible = closePos in leftPos..<rightPos

                        if (openingVisible || closingVisible) {
                            val leftPosition = IntPair.pack(nodePos.line, nodePos.column)
                            val rightPosition = IntPair.pack(closePos.line, closePos.column)

                            val pair = BracketPair.obtain(
                                leftPosition,
                                rightPosition,
                                openLen.columnCount,
                                closingLen.columnCount,
                                node.bracketId,
                                levelPerBracket
                            )
                            receiver.add(pair)
                        }
                    }

                    if (node.child != null) {
                        collectPairsInRangeRecInternal(
                            node.child,
                            openEnd,
                            leftPos,
                            rightPos,
                            receiver,
                            initialNestingLevel + 1,
                            levelPerBracketType
                        )
                    }

                    levelPerBracketType[node.bracketId] = levelPerBracket
                    return
                }

                is ListAST, is TwoThreeListAST -> {
                    val listNode = node as IListNode
                    var pos = nodePos
                    val childCount = listNode.childCount

                    for (i in 0 until childCount) {
                        val child = listNode.getChild(i) ?: continue
                        val childEnd = pos + child.length

                        if (childEnd <= leftPos) {
                            pos = childEnd
                            continue
                        }

                        if (pos >= rightPos) {
                            return
                        }

                        val childEndsAfterRange = childEnd >= rightPos
                        if (childEndsAfterRange && child !is BracketAST && child !is TextAST) {
                            node = child
                            nodePos = pos
                            continue@whileLoop
                        }

                        collectPairsInRangeRecInternal(child, pos, leftPos, rightPos, receiver,
                            initialNestingLevel, levelPerBracketType)
                        pos = childEnd
                    }
                    return
                }

                is BracketAST,is TextAST, is InvalidBracketAST -> {
                    return
                }
            }
        }
    }


}
