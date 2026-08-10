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

package io.github.rosemoe.sora.graphics.inlayHint

import android.graphics.Canvas
import io.github.rosemoe.sora.graphics.InlayHintRenderParams
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.lang.styling.inlayHint.GhostTextInlayHint
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * A ghost text inlay hint renderer.
 *
 * The ghost text is drawn with the same font size as the editor text but in
 * the original text color with 50% transparency, so it looks like a faded
 * preview of the suggested text.
 *
 * @see GhostTextInlayHint
 * @author Rosemoe
 */
open class GhostTextInlayHintRenderer : InlayHintRenderer() {

    companion object {
        val DefaultInstance = GhostTextInlayHintRenderer()

        /**
         * Alpha value of 50% transparency
         */
        private const val GHOST_TEXT_ALPHA = 0x80
    }

    protected val localPaint = Paint().also { it.isAntiAlias = true }

    override val typeName: String
        get() = GhostTextInlayHint.TYPE_NAME

    override fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float {
        return paint.measureText((inlayHint as? GhostTextInlayHint)?.text ?: "")
    }

    override fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        val ghostText = inlayHint as? GhostTextInlayHint ?: return
        localPaint.typeface = paint.typeface
        localPaint.textSize = paint.textSize
        val baseColor = colorScheme.getColor(EditorColorScheme.TEXT_NORMAL)
        localPaint.color = (baseColor and 0x00FFFFFF) or (GHOST_TEXT_ALPHA shl 24)
        canvas.drawText(ghostText.text, 0f, params.textBaseline.toFloat(), localPaint)
    }

}
