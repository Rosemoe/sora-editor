/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
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
package io.github.rosemoe.sora.data;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.github.rosemoe.sora.interfaces.ExternalRenderer;
import io.github.rosemoe.sora.widget.EditorColorScheme;

/**
 * The span model
 *
 * @author Rose
 */
public class Span {

    /**
     * Type for {@link Span#issueType}.
     *
     * Indicates there is no issue. Also the default value.
     */
    public static final int TYPE_NONE = 0;
    /**
     * Type for {@link Span#issueType}.
     *
     * Indicates this span is in ERROR region
     */
    public static final int TYPE_ERROR = 1;
    /**
     * Type for {@link Span#issueType}.
     *
     * Indicates this span is in WARNING region
     */
    public static final int TYPE_WARNING = 2;
    /**
     * Type for {@link Span#issueType}.
     *
     * Indicates this span is in TYPO region
     */
    public static final int TYPE_TYPO = 3;
    /**
     * Type for {@link Span#issueType}.
     *
     * Indicates this span is in DEPRECATED region
     */
    public static final int TYPE_DEPRECATED = 4;

    private static final BlockingQueue<Span> cacheQueue = new ArrayBlockingQueue<>(8192 * 2);
    public int column;
    public int colorId;
    public int underlineColor = 0;

    /**
     * Set this value to draw curly lines for this span to indicates code problems.
     *
     * @see Span#TYPE_NONE
     * @see Span#TYPE_ERROR
     * @see Span#TYPE_WARNING
     * @see Span#TYPE_TYPO
     * @see Span#TYPE_DEPRECATED
     */
    public int issueType = 0;
    public ExternalRenderer renderer = null;

    /**
     * Create a new span
     *
     * @param column  Start column of span
     * @param colorId Type of span
     * @see Span#obtain(int, int)
     */
    private Span(int column, int colorId) {
        this.column = column;
        this.colorId = colorId;
    }

    /**
     * Get an available Span object from either cache or new instance.
     * The result object will be initialized with the given arguments.
     */
    public static Span obtain(int column, int colorId) {
        Span span = cacheQueue.poll();
        if (span == null) {
            return new Span(column, colorId);
        } else {
            span.column = column;
            span.colorId = colorId;
            return span;
        }
    }

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
    public void setUnderlineColor(int color) {
        underlineColor = color;
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
        Span copy = obtain(column, colorId);
        copy.setUnderlineColor(underlineColor);
        copy.issueType = issueType;
        copy.renderer = renderer;
        return copy;
    }

    public boolean recycle() {
        issueType = colorId = column = underlineColor = 0;
        renderer = null;
        return cacheQueue.offer(this);
    }

}
