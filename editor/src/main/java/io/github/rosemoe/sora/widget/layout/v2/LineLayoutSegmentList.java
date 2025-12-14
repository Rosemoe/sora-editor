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
package io.github.rosemoe.sora.widget.layout.v2;

import androidx.annotation.NonNull;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Objects;

import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.SegmentList;
import io.github.rosemoe.sora.widget.layout.Row;
import io.github.rosemoe.sora.widget.layout.RowIterator;

public class LineLayoutSegmentList extends SegmentList<LineLayout> implements LineLayoutList {

    private final CachedIntValue rowCount = new CachedIntValue(() -> {
        var cnt = 0;
        for (int i = 0; i < segments.size(); i++) {
            cnt += ((LayoutListSegment) segments.get(i)).rowCount;
        }
        return cnt;
    });

    private final CachedIntValue maxRowWidth = new CachedIntValue(() -> {
        var w = 0;
        for (int i = 0; i < segments.size(); i++) {
            w = Math.max(w, ((LayoutListSegment) segments.get(i)).maxRowWidth);
        }
        return w;
    });

    @NonNull
    @Override
    public LineLayout set(int index, @NonNull LineLayout element) {
        Objects.requireNonNull(element);
        return super.set(index, element);
    }

    @Override
    public void add(int index, @NonNull LineLayout element) {
        Objects.requireNonNull(element);
        super.add(index, element);
    }

    @Override
    protected LayoutListSegment onCreateSegment(int capacity) {
        return new LayoutListSegment(capacity);
    }

    @Override
    protected SegmentList<LineLayout> onCreateShallowCopyInstance() {
        return new LineLayoutSegmentList();
    }

    private long findBlockForRowIndex(long continuation, int rowIndex) {
        int offset = IntPair.getSecond(continuation);
        for (int i = IntPair.getFirst(continuation); i < segments.size(); i++) {
            var x = segments.get(i);
            var s = ((LayoutListSegment) x).rowCount;
            if (offset <= rowIndex && rowIndex < offset + s) {
                return IntPair.pack(i, offset);
            }
            offset += s;
        }
        throw new IllegalStateException("unreachable");
    }

    @NonNull
    public Row getRowAt(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            throw new IndexOutOfBoundsException();
        }
        long result = findBlockForRowIndex(0, rowIndex);
        int blockIndex = IntPair.getFirst(result);
        int offset = IntPair.getSecond(result);
        var seg = (LayoutListSegment) segments.get(blockIndex);
        for (int i = 0; i < seg.size(); i++) {
            var s = seg.get(i).getRowCount();
            if (offset <= rowIndex && rowIndex < offset + s) {
                return seg.get(i).getRowAt(rowIndex - offset);
            }
            offset += s;
        }
        throw new IllegalStateException("unreachable");
    }

    @NonNull
    public RowIterator rowIterator(int firstRowIndex) {
        if (firstRowIndex < 0 || firstRowIndex >= getRowCount()) {
            throw new IndexOutOfBoundsException();
        }
        long result = findBlockForRowIndex(0, firstRowIndex);
        int blockIndex = IntPair.getFirst(result);
        int offset = IntPair.getSecond(result);
        var seg = (LayoutListSegment) segments.get(blockIndex);
        for (int i = 0; i < seg.size(); i++) {
            var s = seg.get(i).getRowCount();
            if (offset <= firstRowIndex && firstRowIndex < offset + s) {
                return new LayoutListRowIterator(new LayoutListRowIterator.ItrState(result, i, firstRowIndex - offset, firstRowIndex));
            }
            offset += s;
        }
        throw new IllegalStateException("unreachable");
    }

    @Override
    public void removeRange(int start, int end) {
        super.removeRange(start, end);
    }

    @Override
    public int getLineCount() {
        return size();
    }

    public int getRowCount() {
        return rowCount.getValue();
    }

    public int getMaxRowWidth() {
        return maxRowWidth.getValue();
    }

    protected static class LayoutListSegment extends Segment<LineLayout> {

        private int maxRowWidth;
        private int rowCount;

        public LayoutListSegment(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        protected LineLayout removeElementAt(int index) {
            var r = super.removeElementAt(index);
            processElementRemoval(r);
            return r;
        }

        @Override
        protected LineLayout setElementAt(int index, LineLayout e) {
            var r = super.setElementAt(index, e);
            processElementAddition(e);
            processElementRemoval(r);
            return r;
        }

        @Override
        protected void addElementAt(int index, LineLayout e) {
            super.addElementAt(index, e);
            processElementAddition(e);
        }

        @Override
        protected void onSegmentBatchUpdated() {
            super.onSegmentBatchUpdated();
            processAllElements();
        }

        @Override
        protected LayoutListSegment onCreateCopyInstance() {
            return new LayoutListSegment(this.size());
        }

        @Override
        public Segment<LineLayout> copy() {
            var x = (LayoutListSegment) super.copy();
            x.rowCount = rowCount;
            x.maxRowWidth = maxRowWidth;
            return x;
        }

        private void processElementAddition(LineLayout e) {
            rowCount += e.getRowCount();
            maxRowWidth = Math.max(e.getRowMaxWidth(), maxRowWidth);
        }

        private void processElementRemoval(LineLayout e) {
            rowCount -= e.getRowCount();
            if (maxRowWidth == e.getRowMaxWidth()) {
                int w = 0;
                for (int i = 0; i < size(); i++) {
                    var x = get(i);
                    w = Math.max(w, x.getRowMaxWidth());
                }
                maxRowWidth = w;
            }
        }

        private void processAllElements() {
            int w = 0, cnt = 0;
            for (int i = 0; i < size(); i++) {
                var x = get(i);
                w = Math.max(w, x.getRowMaxWidth());
                cnt += x.getRowCount();
            }
            maxRowWidth = w;
            rowCount = cnt;
        }
    }

    protected class CachedIntValue {

        private int expectedModCount;
        private int value;
        private final IntComputation computation;

        public CachedIntValue(@NonNull IntComputation computation) {
            this.computation = computation;
            // set to invalid modCount
            this.expectedModCount = modCount - 1;
        }

        public int getValue() {
            if (modCount == expectedModCount) {
                return value;
            }
            value = computation.compute();
            expectedModCount = modCount;
            return value;
        }

    }

    @FunctionalInterface
    protected interface IntComputation {
        int compute();
    }

    private class LayoutListRowIterator implements RowIterator {

        public static class ItrState {
            public long blockContinuation;
            public int inBlockIndex, inLayoutIndex, offset;

            public ItrState(long blockContinuation, int inBlockIndex, int inLayoutIndex, int offset) {
                this.blockContinuation = blockContinuation;
                this.inBlockIndex = inBlockIndex;
                this.inLayoutIndex = inLayoutIndex;
                this.offset = offset;
            }

            public ItrState copy() {
                return new ItrState(blockContinuation, inBlockIndex, inLayoutIndex, offset);
            }

        }

        private final int expectedModCount;

        private final ItrState initState;
        private ItrState currState;

        public LayoutListRowIterator(ItrState state) {
            initState = state;
            currState = state.copy();
            expectedModCount = modCount;
        }

        private void validateModCount() {
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }

        private Row nextRow() {
            var curr = segments.get(IntPair.getFirst(currState.blockContinuation))
                    .get(currState.inBlockIndex)
                    .getRowAt(currState.inLayoutIndex);
            // First, check if the block should be shifted
            currState.offset++;
            if (hasNext()) {
                var newContinuation = findBlockForRowIndex(currState.blockContinuation, currState.offset);
                var blockIndex = IntPair.getFirst(newContinuation);
                if (blockIndex != IntPair.getFirst(currState.blockContinuation)) {
                    // Block changed
                    currState.blockContinuation = newContinuation;
                    var seg = segments.get(blockIndex);
                    currState.inBlockIndex = 0;
                    while (currState.inBlockIndex < seg.size()
                            && seg.get(currState.inBlockIndex).getRowCount() == 0) {
                        currState.inBlockIndex++;
                    }
                    currState.inLayoutIndex = 0;
                    return curr;
                }
                // Block not changed
                var seg = segments.get(blockIndex);
                var layout = seg.get(currState.inBlockIndex);
                if (currState.inLayoutIndex + 1 >= layout.getRowCount()) {
                    currState.inLayoutIndex = 0;
                    do {
                        currState.inBlockIndex++;
                    } while (currState.inBlockIndex < seg.size()
                            && seg.get(currState.inBlockIndex).getRowCount() == 0);
                } else {
                    currState.inLayoutIndex++;
                }
            }
            return curr;
        }

        @NonNull
        @Override
        public Row next() {
            if (!hasNext()) throw new NoSuchElementException();
            validateModCount();
            return nextRow();
        }

        @Override
        public boolean hasNext() {
            return currState.offset < getRowCount();
        }

        @Override
        public void reset() {
            currState = initState.copy();
        }
    }
}
