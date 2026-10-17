/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
package io.github.rosemoe.sora.langs.textmate.brackets

/**
 * One analyzer message, described as the region it replaced.
 *
 * @param start where the change begins, valid in both documents
 * @param oldEnd end of the replaced region in the old document
 * @param newEnd end of the replacement in the new document
 */
internal data class BracketEdit(
    val start: TextOffset,
    val oldEnd: TextOffset,
    val newEnd: TextOffset
) {

    /** Map a position in the new document back to the old one. */
    fun oldPosition(newPosition: TextOffset): TextOffset =
        if (newPosition < start) newPosition else oldEnd + newEnd.lengthTo(newPosition)

    /** Map a position in the old document forward to the new one. */
    fun newPosition(oldPosition: TextOffset): TextOffset =
        if (oldPosition < start) oldPosition else newEnd + oldEnd.lengthTo(oldPosition)

    /**
     * Distance from [offset] to the start of this edit, [TextOffset.ZERO] when the offset sits
     * inside the edit, or `null` when it is past the edit and therefore untouched.
     */
    fun distanceToChange(offset: TextOffset): TextOffset? = when {
        offset < start -> offset.lengthTo(start)
        offset < newEnd -> TextOffset.ZERO
        else -> null
    }
}
