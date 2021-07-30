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
package io.github.rosemoe.editor.widget;

/**
 * This class represents a 'row' in editor
 * Editor uses this to draw rows
 *
 * @author Rose
 */
class Row {

    /**
     * The index in lines
     * But not row index
     */
    public int lineIndex;

    /**
     * Whether this row is a start of a line
     * Editor will draw line number to left of this row to indicate this
     */
    public boolean isLeadingRow;

    /**
     * Start index in target line
     */
    public int startColumn;

    /**
     * End index in target line
     */
    public int endColumn;

}
