/*
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
 */
package io.github.rosemoe.sora.widget.minimap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.lang.styling.EmptyReader
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.util.RendererUtils
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.rendering.RenderingConstants
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.subscribeAlways
import java.lang.AutoCloseable
import kotlin.math.max
import kotlin.math.min

/**
 * Experimental minimap renderer.
 *
 * @author Rosemoe
 */
@Experimental
class MinimapRenderer(val editor: CodeEditor) : AutoCloseable {

    companion object Config {
        const val CharHeight = 10
        const val WidthRatio = 0.12f
        const val MaxWidthDp = 120
        const val MinWidthDp = 15
        const val ContentAlpha = 180
    }

    private val charRenderer = MinimapCharRenderer(CharHeight)
    private val dstRect = RectF()
    private val tempRect = RectF()
    private val paint = Paint()
    private var bitmap: Bitmap? = null
    private var pixelBuffer = IntArray(0)
    private var bitmapWidth = -1
    private var bitmapHeight = -1
    private var bitmapRowCount = -1
    private var lastScrollBucket = Int.MIN_VALUE
    private var lastRenderTimestamp = Long.MIN_VALUE
    private var dirty = true
    private var closed = false
    private var lastMinimapConfig: MinimapConfig? = null

    private val subscriptions = ArrayList<SubscriptionReceipt<*>>(4).apply {
        add(editor.subscribeAlways<ContentChangeEvent> { dirty = true })
        add(editor.subscribeAlways<ColorSchemeUpdateEvent> { dirty = true })
        add(editor.subscribeAlways<TextSizeChangeEvent> { dirty = true })
        add(editor.subscribeAlways<EditorReleaseEvent> { close() })
    }

    /**
     * Clears cached rendering state.
     */
    fun reset() {
        lastScrollBucket = Int.MIN_VALUE
        lastRenderTimestamp = Long.MIN_VALUE
        bitmapWidth = -1
        bitmapHeight = -1
        bitmapRowCount = -1
        pixelBuffer = IntArray(0)
        bitmap?.recycle()
        bitmap = null
        dirty = true
    }

    /**
     * Draws the minimap and returns its rendered width.
     */
    fun onDrawToCanvas(canvas: Canvas, rectRight: Int, renderTimestamp: Long): Int {
        if (closed) {
            return 0
        }
        if (!editor.props.showMinimap) {
            reset()
            return 0
        }
        updateBitmapIfNeeded(renderTimestamp)
        val renderedBitmap = bitmap ?: return 0
        if (renderedBitmap.width <= 0 || renderedBitmap.height <= 0) {
            return 0
        }
        val left = max(0f, rectRight - renderedBitmap.width.toFloat())
        dstRect.set(left, 0f, rectRight.toFloat(), editor.height.toFloat())
        tempRect.set(dstRect)
        drawBackground(canvas)
        canvas.drawBitmap(renderedBitmap, null, dstRect, null)
        drawViewportIndicator(canvas)
        return renderedBitmap.width
    }

    /**
     * Rebuilds the cached bitmap when the minimap state changes.
     */
    private fun updateBitmapIfNeeded(renderTimestamp: Long) {
        val rowCount = editor.layout.rowCount
        val requiredWidth = computeBitmapWidth()
        val requiredHeight = max(1, editor.height)
        val scrollBucket = computeScrollBucket(rowCount, requiredHeight)
        if (!dirty &&
            lastRenderTimestamp == renderTimestamp &&
            bitmapRowCount == rowCount &&
            bitmapWidth == requiredWidth &&
            bitmapHeight == requiredHeight &&
            lastScrollBucket == scrollBucket &&
            lastMinimapConfig == editor.props.minimapConfig
        ) {
            return
        }
        charRenderer.updateTypeface(editor.textPaint.typeface)
        ensureBitmap(requiredWidth, requiredHeight)
        bitmapWidth = requiredWidth
        bitmapHeight = requiredHeight
        pixelBuffer.fill(Color.TRANSPARENT)
        renderRows(rowCount, scrollBucket * CharHeight)
        bitmap?.setPixels(pixelBuffer, 0, requiredWidth, 0, 0, requiredWidth, requiredHeight)
        bitmapRowCount = rowCount
        lastScrollBucket = scrollBucket
        lastRenderTimestamp = renderTimestamp
        lastMinimapConfig = editor.props.minimapConfig
        dirty = false
    }

    /**
     * Computes the bitmap width from editor size constraints.
     */
    private fun computeBitmapWidth(): Int {
        val ratioWidth = (editor.width * WidthRatio).toInt()
        val maxWidth = (editor.dpUnit * MaxWidthDp).toInt()
        val minWidth = (editor.dpUnit * MinWidthDp).toInt()
        return ratioWidth.coerceIn(minWidth, maxWidth).coerceAtLeast(1)
    }

    /**
     * Maps the editor scroll offset to a minimap row bucket.
     */
    private fun computeScrollBucket(rowCount: Int, targetHeight: Int): Int {
        val contentHeight = rowCount * CharHeight
        val maxMinimapScroll = max(0, contentHeight - targetHeight)
        if (maxMinimapScroll <= 0) {
            return 0
        }
        val editorScrollRange = editor.scrollMaxY + editor.height
        if (editorScrollRange <= 0) {
            return 0
        }
        val mappedScroll =
            (editor.offsetY.toFloat() / editorScrollRange.toFloat()) * maxMinimapScroll
        return mappedScroll.toInt() / CharHeight
    }

    /**
     * Ensures that the backing bitmap matches the required size.
     */
    private fun ensureBitmap(width: Int, height: Int) {
        val current = bitmap
        if (current != null && current.width == width && current.height == height) {
            if (pixelBuffer.size != width * height) {
                pixelBuffer = IntArray(width * height)
            }
            return
        }
        current?.recycle()
        bitmap = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
        pixelBuffer = IntArray(max(1, width * height))
    }

    /**
     * Renders visible rows into the pixel buffer.
     */
    private fun renderRows(rowCount: Int, scrollOffset: Int) {
        val spanReader = editor.styles?.spans?.read() ?: EmptyReader.getInstance()
        try {
            val layout = editor.layout
            val text = editor.text
            val startRow = min(rowCount, max(0, scrollOffset / CharHeight))
            val startTop = -(scrollOffset % CharHeight)
            for (rowIndex in startRow until rowCount) {
                val row = layout.getRowAt(rowIndex)
                val line = text.getLine(row.lineIndex)
                val spans = spanReader.getSpansOnLine(row.lineIndex)
                val rowTop = startTop + (rowIndex - startRow) * CharHeight
                if (rowTop >= bitmapHeight) {
                    break
                }
                if (rowTop + CharHeight <= 0) {
                    continue
                }
                renderRow(line, row.startColumn, row.endColumn, rowTop, spans)
            }
        } finally {
            spanReader.moveToLine(-1)
        }
    }

    /**
     * Renders a single layout row.
     */
    private fun renderRow(
        line: CharSequence,
        startColumn: Int,
        endColumn: Int,
        rowTop: Int,
        spans: List<Span>
    ) {
        var x = 0
        var spanIndex = findSpanIndex(spans, startColumn)
        val top = rowTop
        val bottom = rowTop + CharHeight
        for (column in startColumn until endColumn) {
            if (x >= bitmapWidth) {
                break
            }
            while (spanIndex + 1 < spans.size && spans[spanIndex + 1].column <= column) {
                spanIndex++
            }
            val color = applyAlpha(
                RendererUtils.getForegroundColor(spans[spanIndex], editor.colorScheme),
                ContentAlpha
            )
            x += renderCharacter(line[column], x, top, bottom, color)
        }
    }

    /**
     * Renders one logical character and returns its drawn width.
     */
    private fun renderCharacter(ch: Char, x: Int, top: Int, bottom: Int, color: Int): Int {
        if (ch == ' ') {
            return charRenderer.getGlyphWidth(' ')
        }
        if (ch == '\t') {
            val tabWidth = max(1, editor.tabWidth * charRenderer.getGlyphWidth(' '))
            val remainder = x % tabWidth
            return if (remainder == 0) tabWidth else tabWidth - remainder
        }
        return if (charRenderer.isVisibleAscii(ch)) {
            renderAsciiCharacter(ch, x, top, bottom, color)
        } else {
            renderMappedDoubleCharacter(
                charRenderer.getMappedVisibleAscii(ch),
                x,
                top,
                bottom,
                color
            )
        }
    }

    /**
     * Renders a visible ASCII character.
     */
    private fun renderAsciiCharacter(ch: Char, x: Int, top: Int, bottom: Int, color: Int): Int {
        val width = charRenderer.getGlyphWidth(ch)
        if (editor.props.minimapConfig.minimapDrawTextAsBlocks) {
            fillRect(x, top, min(bitmapWidth, x + width), bottom, color)
        } else {
            charRenderer.blitGlyph(
                pixelBuffer,
                bitmapWidth,
                bitmapWidth,
                bitmapHeight,
                ch,
                x,
                top,
                color
            )
        }
        return width
    }

    /**
     * Renders a mapped wide character as two adjacent glyphs.
     */
    private fun renderMappedDoubleCharacter(
        ch: Char,
        x: Int,
        top: Int,
        bottom: Int,
        color: Int
    ): Int {
        val singleWidth = charRenderer.getGlyphWidth(ch)
        if (editor.props.minimapConfig.minimapDrawTextAsBlocks) {
            fillRect(x, top, min(bitmapWidth, x + singleWidth), bottom, color)
            fillRect(x + singleWidth, top, min(bitmapWidth, x + singleWidth * 2), bottom, color)
        } else {
            charRenderer.blitGlyph(
                pixelBuffer,
                bitmapWidth,
                bitmapWidth,
                bitmapHeight,
                ch,
                x,
                top,
                color
            )
            charRenderer.blitGlyph(
                pixelBuffer,
                bitmapWidth,
                bitmapWidth,
                bitmapHeight,
                ch,
                x + singleWidth,
                top,
                color
            )
        }
        return singleWidth * 2
    }

    /**
     * Fills a clipped rectangle in the pixel buffer.
     */
    private fun fillRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        if (right <= left || bottom <= top) {
            return
        }
        val clippedLeft = left.coerceIn(0, bitmapWidth)
        val clippedRight = right.coerceIn(0, bitmapWidth)
        val clippedTop = top.coerceIn(0, bitmapHeight)
        val clippedBottom = bottom.coerceIn(0, bitmapHeight)
        for (y in clippedTop until clippedBottom) {
            val rowStart = y * bitmapWidth
            for (x in clippedLeft until clippedRight) {
                pixelBuffer[rowStart + x] = color
            }
        }
    }

    /**
     * Finds the active span index for the given column.
     */
    private fun findSpanIndex(spans: List<Span>, column: Int): Int {
        if (spans.isEmpty()) {
            return 0
        }
        var result = 0
        for (index in 1 until spans.size) {
            if (spans[index].column > column) {
                break
            }
            result = index
        }
        return result
    }

    /**
     * Draws the minimap background.
     */
    private fun drawBackground(canvas: Canvas) {
        val color = editor.colorScheme.getColor(EditorColorScheme.MINIMAP_BACKGROUND)
        val oldColor = canvas.drawFilter
        tempRect.set(dstRect)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = color
        canvas.drawRect(tempRect, paint)
        canvas.drawFilter = oldColor
    }

    /**
     * Draws the viewport indicator on the minimap.
     */
    private fun drawViewportIndicator(canvas: Canvas) {
        val renderedBitmap = bitmap ?: return
        val height = editor.height
        val all = (editor.getScrollMaxY() + height).toFloat()
        if (renderedBitmap.height <= 0 || all <= 0) {
            return
        }
        val length = max(
            height / all * height,
            editor.dpUnit * RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP
        )
        val viewportTop = editor.offsetY * 1.0f / editor.getScrollMaxY() * (height - length)
        val top = viewportTop.coerceIn(0f, renderedBitmap.height.toFloat())
        val bottom = (viewportTop + length).coerceIn(top, renderedBitmap.height.toFloat())
        if (bottom <= top) {
            return
        }
        val viewportColor = editor.colorScheme.getColor(EditorColorScheme.MINIMAP_VIEWPORT)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = viewportColor
        canvas.drawRect(dstRect.left, top, dstRect.right, bottom, paint)

        val viewportBorderColor =
            editor.colorScheme.getColor(EditorColorScheme.MINIMAP_VIEWPORT_BORDER)
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = max(1f, editor.dpUnit)
        paint.color = viewportBorderColor
        canvas.drawRect(dstRect.left, top, dstRect.right, bottom, paint)
    }

    /**
     * Replaces the color alpha channel with the given value.
     */
    private fun applyAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    /**
     * Releases cached resources and subscriptions.
     */
    override fun close() {
        subscriptions.forEach(SubscriptionReceipt<*>::unsubscribe)
        subscriptions.clear()
        bitmap?.recycle()
        bitmap = null
        pixelBuffer = IntArray(0)
        closed = true
    }
}
