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

package io.github.rosemoe.sora.langs.monarch.theme

class ColorMap {

    private var lastColorId: Int = -1
    private val id2color = mutableListOf<String>()
    private val color2id = mutableMapOf<String, Int>()


    fun getId(color: String?): Int {
        if (color == null) {
            return 0
        }
        return color2id.getOrPut(color) {
            lastColorId++
            id2color.add(lastColorId, color)
            lastColorId
        }
    }

    fun getColor(id: Int): String {
        return id2color[id]
    }

    fun getColorMap(): List<String> {
        return id2color.toList()
    }

    override fun toString(): String {
        return "ColorMap(lastColorId=$lastColorId, id2color=$id2color, color2id=$color2id)"
    }


}
