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

import io.github.rosemoe.sora.lang.styling.color.ResolvableColor

class StylePatch(
    var startLine: Int,
    var startColumn: Int,
    var endLine: Int,
    var endColumn: Int
) : Comparable<StylePatch> {

    init {
        if (startLine < 0 || startColumn < 0 || endLine < 0 || endColumn < 0) {
            throw IllegalArgumentException("negative number")
        }
        if (endLine < startLine || (endLine == startLine && endColumn < startColumn)) {
            throw IllegalArgumentException("end < start")
        }
    }

    var overrideForeground: ResolvableColor? = null
    var overrideBackground: ResolvableColor? = null
    var overrideItalics: Boolean? = null
    var overrideBold: Boolean? = null

    override fun compareTo(other: StylePatch): Int {
        var res = startLine.compareTo(other.startLine)
        if (res != 0) return res
        res = startColumn.compareTo(other.startColumn)
        if (res != 0) return res
        res = endLine.compareTo(other.endLine)
        if (res != 0) return res
        return endColumn.compareTo(other.endColumn)
    }
}
