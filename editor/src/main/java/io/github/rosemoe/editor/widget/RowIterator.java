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

import java.util.NoSuchElementException;

/**
 * Row iterator.
 * This iterator is able to return a series of Row objects linearly
 * Editor uses this to get information of rows and paint them accordingly
 *
 * @author Rose
 */
interface RowIterator {

    /**
     * Return next Row object
     * <p>
     * The result should not be stored, because implementing classes will always return the same
     * object due to performance
     *
     * @return Row object contains the information about a row
     * @throws NoSuchElementException If no more row available
     */
    Row next();

    /**
     * Whether there is more Row object
     *
     * @return Whether more row available
     */
    boolean hasNext();

}
