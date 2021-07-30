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
package io.github.rosemoe.editor.text;


import io.github.rosemoe.editor.util.IntPair;

/**
 * This a data class of a character position in {@link Content}
 *
 * @author Rose
 */
public final class CharPosition {

    //Packaged due to make changes

    public int index;

    public int line;

    public int column;

    /**
     * Get the index
     *
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get column
     *
     * @return column
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get line
     *
     * @return line
     */
    public int getLine() {
        return line;
    }

    /**
     * Make this CharPosition zero and return self
     *
     * @return self
     */
    public CharPosition zero() {
        index = line = column = 0;
        return this;
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof CharPosition) {
            CharPosition a = (CharPosition) another;
            return a.column == column &&
                    a.line == line &&
                    a.index == index;
        }
        return false;
    }

    /**
     * Convert {@link CharPosition#line} and {@link CharPosition#column} to a Long number
     *
     * First integer is line and second integer is column
     * @return A Long integer describing the position
     */
    public long toIntPair() {
        return IntPair.pack(line, column);
    }

    /**
     * Make a copy of this CharPosition and return the copy
     *
     * @return New CharPosition including info of this CharPosition
     */
    public CharPosition fromThis() {
        CharPosition pos = new CharPosition();
        pos.index = index;
        pos.line = line;
        pos.column = column;
        return pos;
    }

    @Override
    public String toString() {
        return "CharPosition(line = " + line + ",column = " + column + ",index = " + index + ")";
    }

}
