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
package io.github.rosemoe.sora.graphics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.github.rosemoe.sora.lang.styling.Span;

/**
 * Helper class for {@link GraphicTextRow} to iterate text regions
 *
 * @author Rosemoe
 */
class TextRegionIterator {

    private List<Span> spans;

    private List<Integer> softBreaks;

    private int pointerSpan;
    private int pointerSoftBreak;
    private int startIndex;
    private int endIndex;
    private int length;

    public void set(int length, @NonNull List<Span> spans, @Nullable List<Integer> softBreaks) {
        resetPrimitives();
        this.spans = spans;
        this.softBreaks = softBreaks;
        this.length = length;
        System.out.println("New itr");
    }

    public void reset() {
        spans = null;
        softBreaks = null;
        resetPrimitives();
    }

    private void resetPrimitives() {
        startIndex = endIndex = length = pointerSpan = pointerSoftBreak = 0;
    }

    public void requireStartOffset(int index) {
        if (index > length) {
            throw new IllegalArgumentException();
        }
        if (startIndex != 0) {
            throw new IllegalStateException();
        }
        while (pointerSpan < spans.size() &&  spans.get(pointerSpan).column <= index) {
            pointerSpan++;
        }
        if (softBreaks != null) {
            while (pointerSoftBreak < softBreaks.size() && softBreaks.get(pointerSoftBreak) <= index) {
                pointerSoftBreak++;
            }
        }
        startIndex = endIndex = index;
    }

    public boolean hasNextRegion() {
        return endIndex < length;
    }

    public void nextRegion() {
        if (endIndex == 0 && spans.size() > 0 && spans.get(0).column == 0) {
            pointerSpan++;
        }
        if (softBreaks == null) {
            startIndex = endIndex;
            endIndex = pointerSpan >= spans.size() ? length : spans.get(pointerSpan).column;
            pointerSpan++;
        } else {
            startIndex = endIndex;
            var nextIndexSpan = pointerSpan >= spans.size() ? length : spans.get(pointerSpan).column;
            var nextIndexSoftBreak = pointerSoftBreak >= softBreaks.size() ? length : softBreaks.get(pointerSoftBreak);
            nextIndexSpan = Math.min(length, nextIndexSpan);
            nextIndexSoftBreak = Math.min(length, nextIndexSoftBreak);
            if (nextIndexSpan < nextIndexSoftBreak) {
                endIndex = nextIndexSpan;
                pointerSpan++;
            } else if (nextIndexSoftBreak < nextIndexSpan) {
                endIndex = nextIndexSoftBreak;
                pointerSoftBreak++;
            } else {
                endIndex = nextIndexSpan;
                pointerSoftBreak++;
                pointerSpan++;
            }
        }
        System.out.println("region: " + startIndex + ".." + endIndex);
    }

    public Span getSpan() {
        return spans.get(Math.min(spans.size() - 1, Math.max(0, pointerSpan - 1)));
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }
}
