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

package io.github.rosemoe.sora.lang.styling.inlayHint

import io.github.rosemoe.sora.lang.styling.util.PointAnchoredObject

/**
 * Choose which side of character to display the inlay hint.
 *
 * No effect if the given character position is at line end.
 */
enum class CharacterSide {
    LEFT,
    RIGHT
}

open class InlayHint(
    override var line: Int,
    override var column: Int,
    val type: String,
    val displaySide: CharacterSide = CharacterSide.LEFT
) : PointAnchoredObject {

    init {
        if (line < 0 || column < 0) {
            throw IllegalArgumentException("negative number")
        }
    }

}