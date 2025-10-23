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
package io.github.rosemoe.sora.langs.textmate.utils;

import android.graphics.Color;

public class ColorUtils {

    /**
     * This method is used to convert common web-standard color formats,
     * primarily RGBA (including RGB), into the ARGB integer format widely
     * used on the Android platform.
     *
     * <p>
     * The web/VSCode color theme often uses the RGBA hex format (#RRGGBBAA),
     * where the Alpha channel is placed at the end. Android, however, uses
     * the ARGB integer format, where Alpha is the most significant byte
     * (#AARRGGBB). This method handles the necessary byte swapping for
     * an 8-character hex string.
     * </p>
     *
     * @param colorString The color string to be parsed. Supported formats are:
     * <ul>
     * <li>Color names (e.g., "red", "blue"), which are passed to Color.parseColor.</li>
     * <li>Standard 6-digit hex (#RRGGBB).</li>
     * <li>Web 8-digit hex (#RRGGBBAA).</li>
     * </ul>
     * @return The 32-bit integer color in ARGB format.
     * @throws IllegalArgumentException if the hex string length is not 7 or 9 (including '#').
     */
    public static int parseRGBAToARGB(String colorString) {
        if (colorString.charAt(0) != '#') {
            // See https://android.googlesource.com/platform/frameworks/base/+/876dbfb/graphics/java/android/graphics/Color.java#157
            // For non-hex strings (e.g., color names, rgb functions), rely on Android's built-in parser.
            return Color.parseColor(colorString);
        }
        // Use a long to avoid rollovers on #ffXXXXXX
        long color = Long.parseLong(colorString.substring(1), 16);
        if (colorString.length() == 7) {
            // Set the alpha value
            color |= 0x00000000ff000000L;

            // RGB has no alpha; the format is current;
            return (int) color;
        } else if (colorString.length() != 9) {
            throw new IllegalArgumentException("Unknown color");
        }


        int r = (int) (color >> 24) & 0xFF;
        int g = (int) (color >> 16) & 0xFF;
        int b = (int) (color >> 8) & 0xFF;
        int a = (int) (color & 0xFFL);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
