/*
 *   Copyright 2020 Rose2073
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.struct;

import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.widget.EditorColorScheme;

/**
 * The span model
 * @author Rose
 */
public class Span {

    public int startIndex,line,column;

    public int colorId;

    public int underlineColor = 0;

    /**
     * Create a span with brief position
     * @param i Index
     * @param l Line
     * @param c Column
     * @param colorId Color ID
     */
    public Span(int i, int l, int c, int colorId) {
        this.colorId = colorId;
        startIndex = i;
        line = l;
        column = c;
    }

    /**
     * Create a new span from the given start index and color ID
     * This is not a recommended way.
     * You should call {@link Span#setupLineColumn(Content)} later
     * @param start The start index
     * @param colorId Type of span
     */
    public Span(int start, int colorId) {
        startIndex = start;
        this.colorId = colorId;
    }

    /**
     * Make self zero
     * @return self
     */
    public Span applyZero() {
        line = column = 0;
        return this;
    }

    /**
     * Calculate line and column
     * @param c Target content
     * @return self
     */
    public Span setupLineColumn(Content c) {
        CharPosition pos = c.getIndexer().getCharPosition(startIndex);
        line = pos.line;
        column = pos.column;
        return this;
    }

    /**
     * Set a underline for this region
     * Zero for no underline
     * @param color Color for this underline (not color id of {@link EditorColorScheme})
     * @return Self
     */
    public Span setUnderlineColor(int color) {
        underlineColor = color;
        return this;
    }

    /**
     * Get span start line
     * @return Start line
     */
    public int getLine(){
        return line;
    }

    /**
     * Get span start column
     * @return Start column
     */
    public int getColumn(){
        return column;
    }

}
