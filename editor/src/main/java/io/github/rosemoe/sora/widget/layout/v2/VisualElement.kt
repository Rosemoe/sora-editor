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

import android.graphics.Canvas
import io.github.rosemoe.sora.graphics.InlayHintRenderParams
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Visual element created from a logic element.
 */
interface VisualElement {

    /**
     * Width of this visual element.
     * It's determined when this element is created and should not be changed after.
     */
    val width: Float

    /**
     * Source logic element of this visual element. Used to dispatch click events for.
     */
    val logicElement: LogicElement

    /**
     * Column count of this element. Minimum column count is 1.
     */
    val columnCount: Int

    /**
     * Render this visual element into canvas, with given params.
     */
    fun renderElement(
        canvas: Canvas,
        paint: Paint,
        visibleRange: HorizontalOffsetRange,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme
    )

    fun getBackgroundRegion(
        startColumn: Int,
        endColumn: Int
    ): HorizontalOffsetRange

}
