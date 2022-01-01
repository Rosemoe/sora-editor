/*
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
 */
package io.github.rosemoe.sora.text;

/**
 * Define a kind of text style, which can be used in spans.
 * The text style is unchangeable, so when you want to change the style, you'll
 * have to create a new one.
 *
 * @author Rosemoe
 */
public class TextStyle {

    /**
     * The color ID number obtained from editor
     */
    public final int colorId;

    /**
     * Bold text
     */
    public final boolean bold;

    /**
     * Italic text
     */
    public final boolean italic;

    /**
     * Show strikeThrough line
     */
    public final boolean strikeThrough;

    /**
     * Paint's skewX
     */
    public final float skewX;

    /**
     * Create a new TextStyle with the given colorId, but without any other special styles.
     */
    public TextStyle(int colorId) {
        this(colorId, false, false, false, 0f);
    }

    /**
     * Create a TextStyle with the given style arguments
     */
    public TextStyle(int colorId, boolean bold, boolean italic, boolean strikeThrough, float skewX) {
        this.colorId = colorId;
        this.bold = bold;
        this.italic = italic;
        this.strikeThrough = strikeThrough;
        this.skewX = skewX;
    }


}
