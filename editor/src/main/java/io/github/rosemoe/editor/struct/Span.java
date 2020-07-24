/*
 *   Copyright 2020 Rosemoe
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

    public int column;

    public int colorId;

    public int underlineColor = 0;

    /**
     * Create a new span
     * @param column Start column of span
     * @param colorId Type of span
     */
    public Span(int column, int colorId) {
        this.column = column;
        this.colorId = colorId;
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
     * Get span start column
     * @return Start column
     */
    public int getColumn(){
        return column;
    }

    public Span setColumn(int column) {
        this.column = column;
        return this;
    }

    public Span copy() {
        Span copy = new Span(column, colorId);
        copy.setUnderlineColor(underlineColor);
        return copy;
    }

}
