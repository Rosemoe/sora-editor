/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package org.eclipse.tm4e.core.internal.theme;

/**
 * Font style definitions.
 *
 * @see <a href=
 * "https://github.com/microsoft/vscode-textmate/blob/88baacf1a6637c5ec08dce18cea518d935fcf0a0/src/theme.ts#L306">
 * https://github.com/microsoft/vscode-textmate/blob/main/src/theme.ts</a>
 */
public final class FontStyle {

    public static final int NotSet = -1;

    // This can are bit-flags, so it can be `Italic | Bold`
    public static final int None = 0;
    public static final int Italic = 1;
    public static final int Bold = 2;
    public static final int Underline = 4;
    public static final int Strikethrough = 8;

    public static String fontStyleToString(final int fontStyle) {
        if (fontStyle == NotSet) {
            return "not set";
        }
        if (fontStyle == None) {
            return "none";
        }

        final var style = new StringBuilder();
        if ((fontStyle & Italic) == Italic) {
            style.append("italic ");
        }
        if ((fontStyle & Bold) == Bold) {
            style.append("bold ");
        }
        if ((fontStyle & Underline) == Underline) {
            style.append("underline ");
        }
        if ((fontStyle & Strikethrough) == Strikethrough) {
            style.append("strikethrough ");
        }
		// style.isEmpty() no available in android
        if (style.length() < 1) {
            return "none";
        }
        style.setLength(style.length() - 1);
        return style.toString();
    }

    private FontStyle() {
    }
}
