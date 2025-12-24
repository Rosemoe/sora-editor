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

package io.github.rosemoe.sora.graphics

import android.graphics.Canvas
import io.github.rosemoe.sora.lang.styling.inline.InlineElement
import io.github.rosemoe.sora.lang.styling.inline.InlineElementRenderParams
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Base class for all inline element renderers.
 *
 * @author Vivek
 */
abstract class InlineElementRenderer<E : InlineElement> {

    /**
     * The name of this inline element renderer.
     * It must match the name provided in [InlineElement.name].
     */
    abstract val name: String

    @JvmName("measure")
    internal fun measure(
        element: E,
        paint: Paint,
        params: InlineElementRenderParams
    ): Float = onMeasure(element, paint, params)

    @JvmName("render")
    internal fun render(
        element: E,
        canvas: Canvas,
        paint: Paint,
        params: InlineElementRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) = onRender(element, canvas, paint, params, colorScheme, measuredWidth)

    /**
     * Measure the width of this inline element so that editor can properly place all the elements.
     * Be careful that the given objects should not be modified, especially [paint] and [params]. They
     * are currently used by editor instance to measure and render.
     *
     * @param element the inline element to be measured
     * @param paint the text paint currently used by editor
     * @return the width of this inline element
     */
    abstract fun onMeasure(element: E, paint: Paint, params: InlineElementRenderParams): Float

    /**
     * Render the inline element on the given canvas. The [Canvas.translate] is called in advance so you do
     * not need to consider the exact line index. The left of the given canvas is where you should start render
     * your content and the top of the given canvas is the top of target line.
     *
     * Your measured width previously generated is passed to you. You are expected to make your content
     * in range, according to the [measuredWidth] and given line height in [params].
     *
     * @param element the inline element to be rendered
     * @param canvas the canvas to render your content
     * @param paint the text paint currently used by editor
     * @param colorScheme the [EditorColorScheme] of editor
     * @param measuredWidth the width previously measured
     */
    abstract fun onRender(
        element: E,
        canvas: Canvas,
        paint: Paint,
        params: InlineElementRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    )
}
