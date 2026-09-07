/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.compose

import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode

/**
 * Strategy for positioning the line number panel.
 *
 * @see CodeEditorState.lineNumberPanelPositionMode
 */
@JvmInline
value class LineNumberPanelPositionMode internal constructor(internal val mode: Int) {
    companion object {
        /**
         * The panel stays at a fixed position on the screen.
         */
        val Fixed = LineNumberPanelPositionMode(LineInfoPanelPositionMode.FIXED)

        /**
         * The panel follows the scrollbar's movement.
         */
        val Follow = LineNumberPanelPositionMode(LineInfoPanelPositionMode.FOLLOW)
    }
}

/**
 * Alignment options for the line number panel.
 *
 * @see CodeEditorState.lineNumberPanelPosition
 */
@JvmInline
value class LineNumberPanelPosition internal constructor(val position: Int) {

    /**
     * Combine multiple positions.
     */
    infix fun or(other: LineNumberPanelPosition) = LineNumberPanelPosition(position or other.position)

    companion object {
        /**
         * Align to the left of the reference point.
         */
        val Left = LineNumberPanelPosition(LineInfoPanelPosition.LEFT)

        /**
         * Align to the right of the reference point.
         */
        val Right = LineNumberPanelPosition(LineInfoPanelPosition.RIGHT)

        /**
         * Align to the top of the reference point.
         */
        val Top = LineNumberPanelPosition(LineInfoPanelPosition.TOP)

        /**
         * Align to the bottom of the reference point.
         */
        val Bottom = LineNumberPanelPosition(LineInfoPanelPosition.BOTTOM)

        /**
         * Align to the center of the reference point.
         */
        val Center = LineNumberPanelPosition(LineInfoPanelPosition.CENTER)
    }
}
