/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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
package io.github.rosemoe.sora.lang.styling;

import androidx.annotation.NonNull;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import java.util.Collection;

/**
 * The span model
 *
 * @author Rosemoe
 */
public class Span {

    private static final SpanPool<Span> spanPool = new SpanPool<>(Span::new);

    public int column;
    /**
     * @see TextStyle
     */
    public long style;
    public int underlineColor;

    public Object extra;

    /**
     * Create a new span
     *
     * @param column Start column of span
     * @param style  Style made from {@link TextStyle}
     * @see Span#obtain(int, long)
     * @see TextStyle
     */
    Span(int column, long style) {
        this.column = column;
        this.style = style;
    }

    /**
     * Get an available Span object from either cache or new instance.
     * The result object will be initialized with the given arguments.
     */
    public static Span obtain(int column, long style) {
        return spanPool.obtain(column, style);
    }

    /**
     * Recycle all spans in the given collection
     */
    public static void recycleAll(Collection<Span> spans) {
        for (Span span : spans) {
            if (!span.recycle()) {
                return;
            }
        }
    }

    /**
     * Set a underline for this region
     * Zero for no underline
     *
     * @param color Color for this underline (not color id of {@link EditorColorScheme})
     */
    public Span setUnderlineColor(int color) {
        underlineColor = color;
        return this;
    }

    /**
     * Get span start column
     *
     * @return Start column
     */
    public int getColumn() {
        return column;
    }

    /**
     * Set column of this span
     */
    public Span setColumn(int column) {
        this.column = column;
        return this;
    }

    /**
     * Make a copy of this span
     */
    public Span copy() {
        var copy = obtain(column, style);
        copy.setUnderlineColor(underlineColor);
        return copy;
    }

    /**
     * Recycle the object
     *
     * @return Is successful?
     */
    public boolean recycle() {
        column = underlineColor = 0;
        style = 0;
        return spanPool.offer(this);
    }

    public int getForegroundColorId() {
        return TextStyle.getForegroundColorId(style);
    }

    public int getBackgroundColorId() {
        return TextStyle.getBackgroundColorId(style);
    }

    public long getStyleBits() {
        return TextStyle.getStyleBits(style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var span = (Span) o;
        return column == span.column && style == span.style && underlineColor == span.underlineColor;
    }

    @Override
    public int hashCode() {
        int hash = 31 * column;
        hash = 31 * hash + Long.hashCode(style);
        hash = 31 * hash + underlineColor;
        return hash;
    }

    @NonNull
    @Override
    public String toString() {
        return "Span{" +
                "column=" + column +
                ", style=" + style +
                ", underlineColor=" + underlineColor +
                "}";
    }

}
