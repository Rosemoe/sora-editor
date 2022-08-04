/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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
package io.github.rosemoe.sora.util;

public class RegionIterator {

    private final int max;
    private final RegionProvider[] providers;
    private final int[] pointers;
    private final boolean[] pointerStates;
    protected int startIndex;
    protected int endIndex;

    public RegionIterator(int max, RegionProvider... providers) {
        this.max = max;
        this.providers = providers;
        pointers = new int[providers.length];
        pointerStates = new boolean[providers.length];
    }

    public void nextRegion() {
        startIndex = endIndex;
        int minNext = max;
        for (int i = 0; i < providers.length; i++) {
            int next, value;
            if (pointers[i] < providers[i].getPointCount() && (value = providers[i].getPointAt(pointers[i])) <= max) {
                next = value;
            } else {
                next = max;
            }
            minNext = Math.min(next, minNext);
        }
        endIndex = minNext;
        for (int i = 0; i < providers.length; i++) {
            if (pointers[i] < providers[i].getPointCount() && providers[i].getPointAt(pointers[i]) == minNext) {
                pointers[i]++;
                pointerStates[i] = true;
            } else {
                pointerStates[i] = false;
            }
        }
    }

    public boolean hasNextRegion() {
        return endIndex < max;
    }

    public int getPointer(int i) {
        return pointers[i];
    }

    public int getRegionSourcePointer(int i) {
        var pointerValue = pointers[i] < providers[i].getPointCount() ? providers[i].getPointAt(i) : max;
        return (endIndex <= pointerValue && pointerValue < max || pointerStates[i]) ? pointers[i] - 1 : pointers[i];
    }

    public int getPointerValue(int i, int j) {
        var provider = providers[i];
        if (j < 0) {
            return 0;
        }
        if (j >= provider.getPointCount()) {
            return max;
        }
        var value = provider.getPointAt(j);
        return Math.min(value, max);
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return Math.min(endIndex, max);
    }

    public int getMax() {
        return max;
    }

    public interface RegionProvider {

        int getPointCount();

        int getPointAt(int index);

    }

}
