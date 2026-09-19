/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets.ast


internal class BeforeEditPositionMapper(
    private val edits: List<TextEditInfoCache>
) {

    // Index of the next edit to process
    private var nextEditIdx = 0

    // Delta from old to new document (accumulated as we process edits)
    private var deltaOldToNewLineCount = 0
    private var deltaOldToNewColumnCount = 0

    // Line index in the old document where the column delta applies
    private var deltaLineIdxInOld = -1


    fun getOffsetBeforeChange(offset: Length): Length {
        val pos = Position.fromLength(offset)
        adjustNextEdit(pos)
        val result = translateCurToOld(pos)
        return result.toLength()
    }

    fun getDistanceToNextChange(offset: Length): Length? {
        val pos = Position.fromLength(offset)
        adjustNextEdit(pos)

        if (nextEditIdx >= edits.size) {
            return null
        }

        val nextEdit = edits[nextEditIdx]
        val nextChangeOffset = translateOldToCur(nextEdit.offsetPosition)
        val distance = offset.diffNonNegative(nextChangeOffset.toLength())

        return distance
    }

    private fun translateOldToCur(oldPos: Position): Position {
        val oldLine = oldPos.line
        val oldColumn = oldPos.column

        val result = if (oldLine == deltaLineIdxInOld) {
            // This line was affected by column delta
            Position.of(
                oldLine + deltaOldToNewLineCount,
                oldColumn + deltaOldToNewColumnCount
            )
        } else {
            // Only line delta applies
            Position.of(
                oldLine + deltaOldToNewLineCount,
                oldColumn
            )
        }

        return result
    }

    private fun translateCurToOld(newPos: Position): Position {
        val newLine = newPos.line
        val newColumn = newPos.column

        val result = if (newLine - deltaOldToNewLineCount == deltaLineIdxInOld) {
            // This line has column delta
            Position.of(
                newLine - deltaOldToNewLineCount,
                newColumn - deltaOldToNewColumnCount
            )
        } else {
            // Only line delta applies
            Position.of(
                newLine - deltaOldToNewLineCount,
                newColumn
            )
        }

        return result
    }

    private fun adjustNextEdit(offset: Position) {
        while (nextEditIdx < edits.size) {
            val nextEdit = edits[nextEditIdx]

            // Calculate where this edit ends in the current (new) document
            val nextEditEndOffsetInCur = translateOldToCur(nextEdit.endOffsetAfterPosition)

            if (nextEditEndOffsetInCur <= offset) {
                // We've passed this edit - update deltas and move to next
                nextEditIdx++

                // Calculate line delta from this edit
                val nextEditEndOffsetBeforeInCur = translateOldToCur(nextEdit.endOffsetBeforePosition)

                val lineDelta = nextEditEndOffsetInCur.line - nextEditEndOffsetBeforeInCur.line
                deltaOldToNewLineCount += lineDelta

                // Calculate column delta
                // Only applies if we're still on the same line as before the edit
                val previousColumnDelta = if (deltaLineIdxInOld == nextEdit.endOffsetBeforePosition.line) {
                    deltaOldToNewColumnCount
                } else {
                    0
                }

                val columnDelta = nextEditEndOffsetInCur.column - nextEditEndOffsetBeforeInCur.column
                deltaOldToNewColumnCount = previousColumnDelta + columnDelta
                deltaLineIdxInOld = nextEdit.endOffsetBeforePosition.line

            } else {
                // This edit is at or after our current position
                break
            }
        }
    }

    companion object {
        private const val TAG = "AST-BeforeEditMap"
                fun fromEdits(edits: List<EditInfo>): BeforeEditPositionMapper {
            val cached = edits.map { TextEditInfoCache.from(it) }
            return BeforeEditPositionMapper(cached)
        }

                fun empty(): BeforeEditPositionMapper {
            return BeforeEditPositionMapper(emptyList())
        }
    }
}

data class EditInfo(
    val startLine: Int,
    val startColumn: Int,
    val oldLineCount: Int,
    val oldColumnCount: Int,
    val newLineCount: Int,
    val newColumnCount: Int
) {
    init {
        require(startLine >= 0) { "startLine must be >= 0: $startLine" }
        require(startColumn >= 0) { "startColumn must be >= 0: $startColumn" }
        require(oldLineCount >= 0) { "oldLineCount must be >= 0: $oldLineCount" }
        require(oldColumnCount >= 0) { "oldColumnCount must be >= 0: $oldColumnCount" }
        require(newLineCount >= 0) { "newLineCount must be >= 0: $newLineCount" }
        require(newColumnCount >= 0) { "newColumnCount must be >= 0: $newColumnCount" }
    }

    fun toOldLength(): Length = Length.of(oldLineCount, oldColumnCount)
    fun toNewLength(): Length = Length.of(newLineCount, newColumnCount)
    fun toPosition(): Position = Position.of(startLine, startColumn)
}

internal data class TextEditInfoCache(
    val offsetPosition: Position,
    val endOffsetBeforePosition: Position,
    val endOffsetAfterPosition: Position
) {
    companion object {
        fun from(edit: EditInfo): TextEditInfoCache {
            val startPos = Position.of(edit.startLine, edit.startColumn)
            val oldLength = Length.of(edit.oldLineCount, edit.oldColumnCount)
            val newLength = Length.of(edit.newLineCount, edit.newColumnCount)

            return TextEditInfoCache(
                offsetPosition = startPos,
                endOffsetBeforePosition = startPos + oldLength,
                endOffsetAfterPosition = startPos + newLength
            )
        }
    }
}
