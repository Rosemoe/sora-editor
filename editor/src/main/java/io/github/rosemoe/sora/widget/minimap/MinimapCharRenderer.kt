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
import android.graphics.Typeface
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.graphics.SingleCharacterWidths
import kotlin.math.max

/**
 * Character renderer for minimap.
 *
 * For performance concerns, only visible ASCII characters are actually rendered. Other characters are
 * replaced by a mapped ASCII visible character.
 * Also, pixels of ASCII characters are cached, and pasted to the right position when drawn.
 *
 * @author Rosemoe
 */
internal class MinimapCharRenderer(private val charHeight: Int) {

    object Constants {
        const val FIRST_VISIBLE_ASCII = 33
        const val LAST_VISIBLE_ASCII = 126
        const val CHAR_COUNT = LAST_VISIBLE_ASCII - FIRST_VISIBLE_ASCII + 1
    }

    private data class Glyph(
        val width: Int,
        val height: Int,
        val alphaMask: ByteArray
    )

    private val paint = Paint().also {
        it.textSize = charHeight.toFloat()
        it.color = Color.WHITE
        it.isAntiAlias = true
    }
    private val widths = SingleCharacterWidths(4)
    private val glyphs = arrayOfNulls<Glyph>(Constants.CHAR_COUNT)

    /**
     * Updates the typeface used for cached glyphs.
     */
    fun updateTypeface(typeface: Typeface) {
        if (typeface != paint.typeface) {
            paint.typeface = typeface
            glyphs.fill(null)
        }
    }

    /**
     * Checks whether the character is a visible ASCII character.
     */
    fun isVisibleAscii(ch: Char): Boolean {
        return ch.code in Constants.FIRST_VISIBLE_ASCII..Constants.LAST_VISIBLE_ASCII
    }

    /**
     * Maps the given character to a visible ASCII character.
     */
    fun getMappedVisibleAscii(ch: Char): Char {
        if (isVisibleAscii(ch)) {
            return ch
        }
        val mapped = (ch.code + Constants.CHAR_COUNT) % Constants.CHAR_COUNT
        return (Constants.FIRST_VISIBLE_ASCII + mapped).toChar()
    }

    /**
     * Returns the cached glyph width for the character.
     */
    fun getGlyphWidth(ch: Char): Int {
        return getGlyph(getMappedVisibleAscii(ch)).width
    }

    /**
     * Draws a glyph into the target pixel buffer.
     */
    fun blitGlyph(
        pixels: IntArray,
        stride: Int,
        bufferWidth: Int,
        bufferHeight: Int,
        ch: Char,
        left: Int,
        top: Int,
        color: Int
    ) {
        val glyph = getGlyph(ch)
        if (left >= bufferWidth || top >= bufferHeight || left + glyph.width <= 0 || top + glyph.height <= 0) {
            return
        }
        val startX = max(0, left)
        val endX = minOf(bufferWidth, left + glyph.width)
        val startY = max(0, top)
        val endY = minOf(bufferHeight, top + glyph.height)
        for (y in startY until endY) {
            val glyphRow = (y - top) * glyph.width
            val dstRow = y * stride
            for (x in startX until endX) {
                val alpha = glyph.alphaMask[glyphRow + (x - left)].toInt() and 0xFF
                if (alpha == 0) {
                    continue
                }
                pixels[dstRow + x] = modulateColorAlpha(color, alpha)
            }
        }
    }

    /**
     * Returns the cached glyph for the given character.
     */
    private fun getGlyph(ch: Char): Glyph {
        val index = ch.code - Constants.FIRST_VISIBLE_ASCII
        glyphs[index]?.let { return it }
        val width = max(1, widths.measureChar(ch, paint).toInt())
        val bitmap = Bitmap.createBitmap(width, charHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseline = charHeight - paint.descent()
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        canvas.drawText(ch.toString(), 0f, baseline, paint)
        val rawPixels = IntArray(width * charHeight)
        bitmap.getPixels(rawPixels, 0, width, 0, 0, width, charHeight)
        bitmap.recycle()
        val alphaMask = ByteArray(rawPixels.size)
        for (i in rawPixels.indices) {
            alphaMask[i] = Color.alpha(rawPixels[i]).toByte()
        }
        return Glyph(width, charHeight, alphaMask).also {
            glyphs[index] = it
        }
    }

    /**
     * Applies the glyph alpha to the target color.
     */
    private fun modulateColorAlpha(color: Int, alpha: Int): Int {
        val baseAlpha = Color.alpha(color)
        val mergedAlpha = (baseAlpha * alpha) shr 8
        return Color.argb(mergedAlpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
