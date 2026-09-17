/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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

package io.github.rosemoe.sora.lang.styling.patching

import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.Collections

/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
class SparseStylePatches {

    private val patches = mutableListOf<StylePatch>()

    private var immutable = false

    private fun getInsertionPoint(patch: StylePatch): Int {
        val result = patches.binarySearch(patch)
        val insertionPoint = if (result < 0) {
            -(result + 1)
        } else {
            result
        }
        return insertionPoint
    }

    fun addPatch(patch: StylePatch) {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        patches.add(getInsertionPoint(patch), patch)
    }

    fun removePatch(patch: StylePatch): Boolean {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        return patches.remove(patch)
    }

    fun clear() {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        patches.clear()
    }

    fun getPatches(): List<StylePatch> = Collections.unmodifiableList(patches)

    fun getPatchesOnLine(line: Int): List<StylePatch> {
        if (patches.isEmpty()) return emptyList()
        return patches.filter { it.startLine <= line && line <= it.endLine }
    }

    fun setImmutable() {
        immutable = true
    }

    fun updateForInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        patches.forEach { patch ->
            val start = mapInsertion(patch.startLine, patch.startColumn, startLine, startColumn, endLine, endColumn)
            val end = mapInsertion(patch.endLine, patch.endColumn, startLine, startColumn, endLine, endColumn)
            patch.startLine = start.first
            patch.startColumn = start.second
            patch.endLine = end.first
            patch.endColumn = end.second
        }
    }

    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        patches.forEach { patch ->
            val start = mapDeletion(patch.startLine, patch.startColumn, startLine, startColumn, endLine, endColumn)
            val end = mapDeletion(patch.endLine, patch.endColumn, startLine, startColumn, endLine, endColumn)
            patch.startLine = start.first
            patch.startColumn = start.second
            patch.endLine = maxOf(start.first, end.first)
            patch.endColumn = if (patch.endLine == start.first) maxOf(start.second, end.second) else end.second
        }
    }

    private fun compare(line1: Int, column1: Int, line2: Int, column2: Int): Int =
        if (line1 != line2) line1.compareTo(line2) else column1.compareTo(column2)

    private fun mapInsertion(line: Int, column: Int, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): Pair<Int, Int> {
        if (compare(line, column, startLine, startColumn) < 0) return line to column
        return if (startLine == endLine && line == startLine) {
            line to (column + endColumn - startColumn)
        } else {
            val delta = endLine - startLine
            if (line == startLine) endLine to (endColumn + column - startColumn)
            else line + delta to column
        }
    }

    private fun mapDeletion(line: Int, column: Int, startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): Pair<Int, Int> {
        if (compare(line, column, startLine, startColumn) <= 0) return line to column
        if (compare(line, column, endLine, endColumn) <= 0) return startLine to startColumn
        val delta = endLine - startLine
        return if (line == endLine) startLine to (startColumn + column - endColumn)
        else line - delta to column
    }

}
