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

package io.github.rosemoe.sora.lang.styling.util

/**
 * Container text position based objects. The total amount of objects held by the container
 * is expected to be not very large.
 *
 * @see PointAnchoredObject
 * @author Rosemoe
 */
open class PointAnchoredContainer<T : PointAnchoredObject> {
    companion object {
        val comparator = object : Comparator<PointAnchoredObject> {
            override fun compare(
                o1: PointAnchoredObject?,
                o2: PointAnchoredObject?
            ): Int {
                if (o1 == null && o2 == null) return 0
                if (o1 == null) return -1
                if (o2 == null) return 1
                val res = o1.line.compareTo(o2.line)
                if (res != 0) return res
                return o1.column.compareTo(o2.column)
            }
        }
    }

    private val objects = mutableListOf<T>()

    private fun getInsertionPoint(e: PointAnchoredObject): Int {
        val result = objects.binarySearch(e, comparator)
        val insertionPoint = if (result < 0) {
            -(result + 1)
        } else {
            result
        }
        return insertionPoint
    }

    private fun getInsertionPointLast(e: PointAnchoredObject): Int {
        var index = getInsertionPoint(e)
        while (index + 1 < objects.size && comparator.compare(
                objects[index],
                objects[index + 1]
            ) == 0
        ) {
            index++
        }
        return index
    }

    private fun getInsertionPointFirst(e: PointAnchoredObject): Int {
        var index = getInsertionPoint(e)
        if (index == objects.size) {
            return index
        }
        while (index - 1 > 0 && comparator.compare(objects[index], objects[index - 1]) == 0) {
            index--
        }
        return index
    }

    fun add(e: T) {
        val index = getInsertionPointLast(e)
        objects.add(index, e)
    }

    fun remove(e: T) {
        val start = getInsertionPointFirst(e)
        val end = getInsertionPointLast(e)
        for (index in start..end) {
            if (index < objects.size && objects[index] == e) {
                objects.removeAt(index)
                break
            }
        }
    }

    fun getForLine(line: Int): List<T> {
        val index = getInsertionPointFirst(Anchor(line, 0))
        if (index >= objects.size || objects[index].line > line) {
            return emptyList()
        }
        var end = index
        while (end < objects.size && objects[end].line == line) {
            end++
        }
        return ArrayList(objects.subList(index, end))
    }

    fun getLineNumbers(): IntArray {
        return objects.map { it.line }.distinct().toIntArray()
    }

    fun updateOnInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        var index = getInsertionPointFirst(Anchor(startLine, startColumn))
        val delta = endLine - startLine
        while (index < objects.size) {
            val e = objects[index]
            if (e.line == startLine) {
                e.line = endLine
                e.column = e.column - startColumn + endColumn
            } else {
                if (delta == 0) break
                e.line += delta
            }
            index++
        }
    }

    fun updateOnDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val start = getInsertionPointFirst(Anchor(startLine, startColumn))
        var index = start
        var deleteEnd = -1
        val delta = endLine - startLine
        while (index < objects.size) {
            val e = objects[index]
            if (e.line < endLine || (e.line == endLine && e.column < endColumn)) {
                deleteEnd = index
            } else if (e.line == endLine) {
                /* e.column >= endColumn */
                val columnDelta = if (startLine == endLine) (endColumn - startColumn) else endColumn
                e.column -= columnDelta
            } else {
                if (delta == 0) break
                e.line -= delta
            }
            index++
        }
        if (deleteEnd != -1) {
            objects.subList(start, deleteEnd + 1).clear()
        }
    }

    private class Anchor(
        override var line: Int,
        override var column: Int
    ) : PointAnchoredObject

}

interface PointAnchoredObject {
    var line: Int
    var column: Int
}