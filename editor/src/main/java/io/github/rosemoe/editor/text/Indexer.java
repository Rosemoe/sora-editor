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

/**
 * A helper class for ITextContent to transform (line,column) and index
 *
 * @author Rose
 */
public interface Indexer {

    /**
     * Get the index of (line,column)
     *
     * @param line   The line position of index
     * @param column The column position of index
     * @return Calculated index
     */
    int getCharIndex(int line, int column);

    /**
     * Get the line position of index
     *
     * @param index The index you want to know its line
     * @return Line position of index
     */
    int getCharLine(int index);

    /**
     * Get the column position of index
     *
     * @param index The index you want to know its column
     * @return Column position of index
     */
    int getCharColumn(int index);

    /**
     * Get the CharPosition for the given index
     * You are not expected to make changes with this CharPosition
     *
     * @param index The index you want to get
     * @return The CharPosition object.
     */
    CharPosition getCharPosition(int index);

    /**
     * Get the CharPosition for the given (line,column)
     * You are not expected to make changes with this CharPosition
     *
     * @param line   The line position you want to get
     * @param column The column position you want to get
     * @return The CharPosition object.
     */
    CharPosition getCharPosition(int line, int column);

}
