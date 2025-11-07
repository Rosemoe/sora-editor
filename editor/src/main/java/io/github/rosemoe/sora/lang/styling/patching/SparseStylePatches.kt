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
import java.util.Arrays
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
        if (patch.startLine != patch.endLine) throw UnsupportedOperationException("crossline patch is not supported now")
        patches.add(getInsertionPoint(patch), patch)
    }

    fun setImmutable() {
        immutable = true
    }

    fun updateForInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val coordinator = StylePatch(startLine, 0, startLine, 0)
        var index = getInsertionPoint(coordinator)
        val delta = endLine - startLine
        while (index < patches.size) {
            val e = patches[index++]
            if (e.startLine == startLine && e.startColumn >= startColumn) {
                val length = e.endColumn - e.startColumn
                e.startLine = endLine
                e.endLine = endLine
                e.startColumn = endColumn + (e.startColumn - startColumn)
                e.endColumn = e.startColumn + length
            } else if (e.startLine > startLine) {
                if (delta == 0) break
                e.startLine += delta
                e.endLine += delta
            }
        }
    }

    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val coordinator = StylePatch(startLine, 0, startLine, 0)
        var index = getInsertionPoint(coordinator)
        val delta = endLine - startLine
        while (index < patches.size) {
            val e = patches[index]
            // TODO
            if (e.startLine < endLine || (e.startLine == endLine && e.endColumn < endColumn)) {

            }
            index++
        }
    }

}