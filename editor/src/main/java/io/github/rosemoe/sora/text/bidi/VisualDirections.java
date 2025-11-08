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


import androidx.annotation.NonNull;


import java.text.Bidi;

import io.github.rosemoe.sora.util.IntPair;

/**
 * Helper class for reordering logical text runs to visual runs.
 *
 * @author Rosemoe
 */
public class VisualDirections implements IDirections {

    private final RunInfo[] runs;

    private static class RunInfo {
        long range;
        int level;

        public RunInfo(long range, int level) {
            this.range = range;
            this.level = level;
        }
    }


    public VisualDirections(@NonNull Directions dirs) {
        int runCount = dirs.getRunCount();
        runs = new RunInfo[runCount];
        var paramLevels = new byte[runCount];
        for (int i = 0; i < runCount; i++) {
            paramLevels[i] = (byte) dirs.getRunLevel(i);
            runs[i] = new RunInfo(IntPair.pack(dirs.getRunStart(i), dirs.getRunEnd(i)), dirs.getRunLevel(i));
        }
        Bidi.reorderVisually(paramLevels, 0, runs, 0, runCount);
    }


    public int getRunCount() {
        return runs.length;
    }

    public int getRunStart(int i) {
        return IntPair.getFirst(runs[i].range);
    }

    public int getRunEnd(int i) {
        return IntPair.getSecond(runs[i].range);
    }

    public int getRunLevel(int i) {
        return runs[i].level;
    }

    public boolean isRunRtl(int i) {
        return (getRunLevel(i) & 1) != 0;
    }


}
