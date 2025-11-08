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

public class BidiUtils {

    private static int[] prepareReorder(byte[] levels, byte[] pMinLevel, byte[] pMaxLevel) {
        int start;
        byte level, minLevel, maxLevel;

        if (levels == null || levels.length <= 0) {
            return null;
        }

        /* determine minLevel and maxLevel */
        minLevel = Bidi.MAX_EXPLICIT_LEVEL + 1;
        maxLevel = 0;
        for (start = levels.length; start > 0; ) {
            level = levels[--start];
            if (level < 0) {
                return null;
            }
            if (level > (Bidi.MAX_EXPLICIT_LEVEL + 1)) {
                return null;
            }
            if (level < minLevel) {
                minLevel = level;
            }
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
        pMinLevel[0] = minLevel;
        pMaxLevel[0] = maxLevel;

        /* initialize the index map */
        int[] indexMap = new int[levels.length];
        for (start = levels.length; start > 0; ) {
            --start;
            indexMap[start] = start;
        }

        return indexMap;
    }

    public static int[] reorderVisual(byte[] levels) {
        byte[] aMinLevel = new byte[1];
        byte[] aMaxLevel = new byte[1];
        int start, end, limit, temp;
        byte minLevel, maxLevel;

        int[] indexMap = prepareReorder(levels, aMinLevel, aMaxLevel);
        if (indexMap == null) {
            return null;
        }

        minLevel = aMinLevel[0];
        maxLevel = aMaxLevel[0];

        /* nothing to do? */
        if (minLevel == maxLevel && (minLevel & 1) == 0) {
            return indexMap;
        }

        /* reorder only down to the lowest odd level */
        minLevel |= 1;

        /* loop maxLevel..minLevel */
        do {
            start = 0;

            /* loop for all sequences of levels to reorder at the current maxLevel */
            for (; ; ) {
                /* look for a sequence of levels that are all at >=maxLevel */
                /* look for the first index of such a sequence */
                while (start < levels.length && levels[start] < maxLevel) {
                    ++start;
                }
                if (start >= levels.length) {
                    break;  /* no more such runs */
                }

                /* look for the limit of such a sequence (the index behind it) */
                for (limit = start; ++limit < levels.length && levels[limit] >= maxLevel; ) {
                }

                /*
                 * Swap the entire interval of indexes from start to limit-1.
                 * We don't need to swap the levels for the purpose of this
                 * algorithm: the sequence of levels that we look at does not
                 * move anyway.
                 */
                end = limit - 1;
                while (start < end) {
                    temp = indexMap[start];
                    indexMap[start] = indexMap[end];
                    indexMap[end] = temp;

                    ++start;
                    --end;
                }

                if (limit == levels.length) {
                    break;  /* no more such sequences */
                } else {
                    start = limit + 1;
                }
            }
        } while (--maxLevel >= minLevel);

        return indexMap;
    }

}
