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
package io.github.rosemoe.editor.util;

/**
 * ArrayList for primitive type long
 */
public class LongArrayList {

    private long[] data;
    private int length;

    public LongArrayList() {
        data = new long[128];
    }

    public void add(long value) {
        data[length++] = value;
        if (data.length == length) {
            long[] newData = new long[length << 1];
            System.arraycopy(data, 0, newData, 0, length);
            data = newData;
        }
    }

    public int size() {
        return length;
    }

    public long get(int index) {
        if (index > length || index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return data[index];
    }

    public void clear() {
        length = 0;
    }

}
