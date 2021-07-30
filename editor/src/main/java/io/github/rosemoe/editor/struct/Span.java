/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.struct;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.github.rosemoe.editor.widget.EditorColorScheme;

/**
 * The span model
 *
 * @author Rose
 */
public class Span {

    private static final BlockingQueue<Span> cacheQueue = new ArrayBlockingQueue<>(8192 * 2);
    public int column;
    public int colorId;
    public int underlineColor = 0;

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
     * @return Self
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
        Span copy = obtain(column, colorId);
        copy.setUnderlineColor(underlineColor);
        return copy;
    }

    public boolean recycle() {
        colorId = column = underlineColor = 0;
        return cacheQueue.offer(this);
    }

}
