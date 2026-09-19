/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSInputEdit
import com.itsaky.androidide.treesitter.TSPoint
import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryError
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content

/**
 * Convert a [CharPosition] object to a [TSPoint] object
 */
fun CharPosition.toTSPoint(): TSPoint = TSPoint.create(line, column * 2)

fun TSQuery.validateOrThrow(name: String = "unknown") {
    if (errorType != TSQueryError.None) {
        throw IllegalArgumentException("query(name:$name) parsing failed: ${errorType.name} at text offset $errorOffset")
    }
}

/**
 * Create a new [TSInputEdit] object for the given positions
 */
fun newTSInputEdit(start: CharPosition, oldEnd: CharPosition, newEnd: CharPosition) =
    TSInputEdit.create(
        start.index * 2,
        oldEnd.index * 2,
        newEnd.index * 2,
        start.toTSPoint(),
        oldEnd.toTSPoint(),
        newEnd.toTSPoint()
    )

/**
 * Char index of a line/column position, clamped to the document.
 *
 * Out-of-range positions resolve to the nearest valid one, and a position the indexer still rejects
 * resolves to the end of the document.
 */
internal fun Content.safeCharIndex(line: Int, column: Int): Int {
    if (line < 0) return 0
    if (line >= lineCount) return length
    val safeColumn = when {
        column < 0 -> 0
        column > getColumnCount(line) -> getColumnCount(line)
        else -> column
    }
    return try {
        indexer.getCharIndex(line, safeColumn)
    } catch (_: IndexOutOfBoundsException) {
        length
    }
}