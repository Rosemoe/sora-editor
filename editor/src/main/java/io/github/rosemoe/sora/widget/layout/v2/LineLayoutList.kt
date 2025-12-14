/*
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
 */
package io.github.rosemoe.sora.widget.layout.v2

import io.github.rosemoe.sora.widget.layout.Row
import io.github.rosemoe.sora.widget.layout.RowIterator

/**
 * A collection of line layouts.
 *
 * Maintains the max row width and row count of lines in this collection.
 */
interface LineLayoutList {
    fun add(index: Int, layout: LineLayout)

    fun set(index: Int, layout: LineLayout): LineLayout

    fun remove(index: Int): LineLayout

    fun removeRange(start: Int, end: Int)

    fun get(index: Int): LineLayout

    val lineCount: Int

    val rowCount: Int

    val maxRowWidth: Int

    fun getRowAt(rowIndex: Int): Row

    fun rowIterator(firstRowIndex: Int): RowIterator
}
