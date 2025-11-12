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

/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

package io.github.rosemoe.sora.graphics.inlayHint

import android.graphics.Canvas
import io.github.rosemoe.sora.graphics.InlayHintRenderParams
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Base class for all inlay hint renderers.
 *
 * @author Rosemoe
 */
abstract class InlayHintRenderer() {

    /**
     * The type name of inlay hint
     */
    abstract val typeName: String

    fun measure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float = onMeasure(inlayHint, paint, params)

    fun render(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) = onRender(
        inlayHint,
        canvas,
        paint,
        params,
        colorScheme,
        measuredWidth
    )

    /**
     * Measure the width of this inlay hint so that editor can properly place all the elements.
     * Be careful that the given objects should not be modified, especially [paint] and [params]. They
     * are currently used by editor instance to measure and render.
     *
     * @param inlayHint the inlay hint to be measured
     * @param paint the text paint currently used by editor
     * @return the width of this inlay hint
     */
    abstract fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float

    /**
     * Render the inlay hint on the given canvas. The [Canvas.translate] is called in advance so you do
     * not need to consider the exact line index. The left of the given canvas is where you should start render
     * your content and the top of the given canvas is the top of target line.
     *
     * Your measured width previously generated is passed to you. You are expected to make your content
     * in range, according to the [measuredWidth] and given line height in [params].
     *
     * @param inlayHint the inlay hint to be rendered
     * @param canvas the canvas to render your content
     * @param paint the text paint currently used by editor
     * @param colorScheme the [EditorColorScheme] of editor
     * @param measuredWidth the width previously measured
     */
    abstract fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    )

}