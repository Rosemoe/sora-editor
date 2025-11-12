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

import android.graphics.Paint
import io.github.rosemoe.sora.graphics.inlayHint.InlayHintRendererProvider
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

data class TextRowParams(
    val tabWidth: Int,
    val textMetrics: Paint.FontMetricsInt,
    val textTop: Int,
    val textBottom: Int,
    val textHeight: Int,
    val textBaseline: Int,
    val rowTop: Int,
    val rowBottom: Int,
    val rowHeight: Int,
    val roundTextBackgroundFactor: Float,
    val inlayHintRendererProvider: InlayHintRendererProvider,
    val colorScheme: EditorColorScheme,
    val miscPaint: Paint,
    val graphPaint: Paint,
    val graphMetrics: Paint.FontMetricsInt
) {
    fun toInlayHintRenderParams() = InlayHintRenderParams(
        tabWidth,
        textMetrics,
        textTop,
        textBottom,
        textHeight,
        textBaseline,
        rowTop,
        rowBottom,
        rowHeight,
        roundTextBackgroundFactor
    )
}

data class InlayHintRenderParams(
    val tabWidth: Int,
    val textMetrics: Paint.FontMetricsInt,
    val textTop: Int,
    val textBottom: Int,
    val textHeight: Int,
    val textBaseline: Int,
    val rowTop: Int,
    val rowBottom: Int,
    val rowHeight: Int,
    val roundTextBackgroundFactor: Float
)