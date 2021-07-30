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

import java.util.List;

import io.github.rosemoe.editor.struct.BlockLine;

/**
 * A object provider for speed improvement
 * Now meaningless because it is not as well as it expected
 *
 * @author Rose
 */
public class ObjectAllocator {

    private static final int RECYCLE_LIMIT = 1024 * 8;
    private static List<BlockLine> blockLines;

    public static void recycleBlockLine(List<BlockLine> src) {
        if (src == null) {
            return;
        }
        if (blockLines == null) {
            blockLines = src;
            return;
        }
        int size = blockLines.size();
        int sizeAnother = src.size();
        while (sizeAnother > 0 && size < RECYCLE_LIMIT) {
            size++;
            sizeAnother--;
            blockLines.add(src.get(sizeAnother));
        }
    }

    public static BlockLine obtainBlockLine() {
        return (blockLines == null || blockLines.isEmpty()) ? new BlockLine() : blockLines.remove(blockLines.size() - 1);
    }

}
