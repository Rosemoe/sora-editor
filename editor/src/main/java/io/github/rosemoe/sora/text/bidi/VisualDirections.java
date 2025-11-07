/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.text.bidi;

import android.icu.text.Bidi;

import androidx.annotation.NonNull;


import io.github.rosemoe.sora.util.IntPair;

public class VisualDirections implements IDirections {

    private final long[] ranges;
    private final int[] levels;

    public VisualDirections(@NonNull Directions dirs) {
        int runCount = dirs.getRunCount();
        ranges = new long[runCount];
        levels = new int[runCount];
        var paramLevels = new byte[runCount];
        for (int i = 0; i < runCount; i++) {
            paramLevels[i] = (byte) dirs.getRunLevel(i);
        }
        int[] indices = Bidi.reorderVisual(paramLevels);
        for (int i = 0; i < runCount; i++) {
            int j = indices[i];
            ranges[i] = IntPair.pack(dirs.getRunStart(j), dirs.getRunEnd(j));
            levels[i] = dirs.getRunLevel(j);
        }
    }


    public int getRunCount() {
        return ranges.length;
    }

    public int getRunStart(int i) {
        return IntPair.getFirst(ranges[i]);
    }

    public int getRunEnd(int i) {
        return IntPair.getSecond(ranges[i]);
    }

    public int getRunLevel(int i) {
        return levels[i];
    }

    public boolean isRunRtl(int i) {
        return (getRunLevel(i) & 1) != 0;
    }


}
