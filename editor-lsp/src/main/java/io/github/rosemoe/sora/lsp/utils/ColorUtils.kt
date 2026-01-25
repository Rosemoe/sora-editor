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

package io.github.rosemoe.sora.lsp.utils

import android.annotation.SuppressLint
import android.graphics.Color

/**
 * Utility object for parsing and converting colors from various formats.
 *
 * Supported formats:
 *  - Hex colors: #RGB, #RRGGBB, #RGBA, #RRGGBBAA
 *  - RGB/RGBA colors: rgb(r,g,b), rgba(r,g,b,a)
 *  - HSL/HSLA colors: hsl(h,s%,l%), hsla(h,s%,l%,a)
 *
 *  @author KonerDev
 */
object ColorUtils {
    /**
     * Converts HSL color values to RGB.
     *
     * @param h Hue (0–1)
     * @param s Saturation (0–1)
     * @param l Lightness (0–1)
     * @return RGB values as an array `[r, g, b]` in range 0–255
     */
    // Algorithm from https://stackoverflow.com/a/53095879
    fun hslToRgb(h: Float, s: Float, l: Float): IntArray {
        val r: Float
        val g: Float
        val b: Float

        if (s == 0f) {
            b = l
            g = b
            r = g
        } else {
            val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            r = hueToRgb(p, q, h + 1f / 3f)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1f / 3f)
        }
        return intArrayOf((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    /**
     * Helper method for converting hue to an RGB channel value.
     */
    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var t = t
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        if (t < 1f / 6f) return p + (q - p) * 6f * t
        if (t < 1f / 2f) return q
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
        return p
    }

    /**
     * Parses a HSL/HSLA color string into an Android [Color] integer.
     *
     * @param hsl HSL or HSLA string
     * @return Parsed color integer, or null if input is invalid
     */
    fun parseHsl(hsl: String): Int? {
        val hslRegex = Regex("""hsla?\(\s*([0-9.]+)\s*(?:,|\s)\s*([0-9.]+%)\s*(?:,|\s)\s*([0-9.]+%)(?:\s*[/,]\s*([0-9.]+%?))?\s*\)""")
        val match = hslRegex.matchEntire(hsl) ?: return null

        fun parseHue(value: String): Float {
            val h = value.toFloat()
            return ((h % 360f) + 360f) % 360f / 360f
        }

        fun parsePercent(value: String): Float {
            return value.dropLast(1).toFloat().coerceIn(0f, 100f) / 100f
        }

        fun parseAlpha(value: String?): Int {
            if (value.isNullOrEmpty()) return 255
            return if (value.endsWith("%")) {
                (value.dropLast(1).toFloat().coerceIn(0f, 100f) * 255 / 100).toInt()
            } else {
                (value.toFloat().coerceIn(0f, 1f) * 255).toInt()
            }
        }

        val h = parseHue(match.groupValues[1])
        val s = parsePercent(match.groupValues[2])
        val l = parsePercent(match.groupValues[3])
        val a = parseAlpha(match.groupValues[4])

        val rgb = hslToRgb(h, s, l)
        return Color.argb(a, rgb[0], rgb[1], rgb[2])
    }

    /**
     * Parses an RGB/RGBA color string into an Android [Color] integer.
     *
     * @param rgb RGB or RGBA string
     * @return Parsed color integer, or null if input is invalid
     */
    fun parseRgb(rgb: String): Int? {
        val rgbRegex = Regex("""rgba?\(\s*(\d{1,3}%?)\s*(?:,|\s)\s*(\d{1,3}%?)\s*(?:,|\s)\s*(\d{1,3}%?)(?:\s*[/,]\s*([0-9.]+%?))?\s*\)""")
        val match = rgbRegex.matchEntire(rgb) ?: return null

        fun parseChannel(value: String): Int {
            return if (value.endsWith("%")) {
                (value.dropLast(1).toFloat().coerceIn(0f, 100f) * 255 / 100).toInt()
            } else {
                value.toInt().coerceIn(0, 255)
            }
        }

        fun parseAlpha(value: String?): Int {
            if (value.isNullOrEmpty()) return 255
            return if (value.endsWith("%")) {
                (value.dropLast(1).toFloat().coerceIn(0f, 100f) * 255 / 100).toInt()
            } else {
                (value.toFloat().coerceIn(0f, 1f) * 255).toInt()
            }
        }

        val r = parseChannel(match.groupValues[1])
        val g = parseChannel(match.groupValues[2])
        val b = parseChannel(match.groupValues[3])
        val a = parseAlpha(match.groupValues[4])

        return Color.argb(a, r, g, b)
    }

    /**
     * Parses a hexadecimal color string into an Android [Color] integer.
     *
     * Accepts:
     *  - `#RGB`
     *  - `#RGBA`
     *  - `#RRGGBB`
     *  - `#RRGGBBAA`
     *
     * @param hex Hexadecimal color string
     * @return Android color integer, or null if input is invalid
     */
    @SuppressLint("UseKtx")
    fun parseHex(hex: String): Int? {
        val normalizedHex = normalizeHex(hex)
        return runCatching { Color.parseColor(normalizedHex) }.getOrNull()
    }

    /**
     * Normalizes a hex color string to a format compatible with Android [Color.parseColor].
     *
     * - `#RGB` → `#RRGGBB`
     * - `#RGBA` → `#AARRGGBB`
     * - `#RRGGBBAA` → `#AARRGGBB`
     *
     * @param hex Original hex color string
     * @return Normalized hex string
     */
    fun normalizeHex(hex: String): String {
        val hexValue = hex.removePrefix("#")
        return when (hexValue.length) {
            // #RGB -> #RRGGBB
            3 -> "#" + hexValue.map { "$it$it" }.joinToString("")

            // #RGBA -> #AARRGGBB
            4 -> "#" + hexValue[3] + hexValue[3] + hexValue[0] + hexValue[0] + hexValue[1] + hexValue[1] + hexValue[2] + hexValue[2]

            // #RRGGBBAA -> #AARRGGBB
            8 -> "#" + hexValue.substring(6, 8) + hexValue.take(6)

            // Unchanged
            else -> hex
        }
    }

    /**
     * Parses a color string in any supported format (HSL, RGB, or Hex) into an Android [Color] integer.
     *
     * @param color Color string in any supported format
     * @return Android color integer, or null if input is invalid
     */
    fun parseColor(color: String): Int? {
        return parseHsl(color) ?: parseRgb(color) ?: parseHex(color)
    }
}