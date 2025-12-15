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
package io.github.rosemoe.sora.widget.rendering;

import androidx.annotation.IntRange;

/**
 * This class is introduced in order to avoid accumulated floating-point error for extreme long lines
 *
 * @author Rosemoe
 */
public class TextAdvancesCache {

    /**
     * Divide the prefix sum by this block size
     */
    private final static int BLOCK_SIZE = 1 << 18;

    private final float[][] cache;
    private final int size;

    public TextAdvancesCache(@IntRange(from = 0) int size) {
        if (size < 0) {
            throw new IllegalArgumentException("invalid size: " + size);
        }
        this.size = size;
        int count = (size + BLOCK_SIZE - 1) / BLOCK_SIZE;
        cache = new float[count][];
        for (int i = 0; i < count; i++) {
            int elementCount = i == count - 1 ? size - BLOCK_SIZE * (count - 1) : BLOCK_SIZE;
            cache[i] = new float[elementCount + 1];
        }
    }

    /**
     * Get the size of this cache object
     */
    public int getSize() {
        return size;
    }

    /**
     * Set advance at the given index
     */
    public void setAdvanceAt(int index, float advance) {
        int i = index / BLOCK_SIZE;
        int j = index % BLOCK_SIZE;
        cache[i][j] = advance;
    }

    /**
     * Compute the prefix sum cache
     */
    public void finishBuilding() {
        for (float[] arr : cache) {
            var pending = arr[0];
            arr[0] = 0f;
            for (int i = 1; i <= arr.length - 1; i++) {
                var tmp = arr[i];
                arr[i] = arr[i - 1] + pending;
                pending = tmp;
            }
        }
    }

    /**
     * Get advance for character at the given index
     */
    public float getAdvanceAt(int index) {
        int i = index / BLOCK_SIZE;
        int j = index % BLOCK_SIZE;
        return cache[i][j + 1] - cache[i][j];
    }

    /**
     * Get the sum of character advances of the given text region
     *
     * @param start inclusive start
     * @param end   exclusive end
     */
    public float getAdvancesSum(int start, int end) {
        if (cache.length == 1) {
            // Normal case
            return cache[0][end] - cache[0][start];
        }
        int low = start / BLOCK_SIZE, high = end / BLOCK_SIZE;
        float result = 0f;
        for (int i = low; i <= high; i++) {
            int segStart = i * BLOCK_SIZE;
            int segEnd = Math.min((i + 1) * BLOCK_SIZE, size);
            int sharedStart = Math.max(start, segStart);
            int sharedEnd = Math.min(end, segEnd);
            if (sharedStart < sharedEnd) {
                result += cache[i][sharedEnd - segStart] - cache[i][sharedStart - segStart];
            }
        }
        return result;
    }

}
