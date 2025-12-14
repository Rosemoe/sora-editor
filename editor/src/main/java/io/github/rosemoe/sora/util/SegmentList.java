/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SegmentList<T> extends AbstractList<T> implements AutoCloseable {

    public final static int DEFAULT_SEGMENT_CAPACITY = 4096;

    protected final List<Segment<T>> segments;
    private final int segmentCapacity;

    private int length;

    public SegmentList() {
        this(DEFAULT_SEGMENT_CAPACITY);
    }

    public SegmentList(int segmentCapacity) {
        if (segmentCapacity < 4) {
            throw new IllegalArgumentException("block size should be at least 4");
        }
        this.segmentCapacity = segmentCapacity;
        segments = new ArrayList<>();
    }

    protected Segment<T> onCreateSegment(int capacity) {
        return new Segment<>(capacity);
    }

    protected void checkInsertIndex(int index) {
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds. length = " + length);
        }
    }

    protected void checkAccessIndex(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds. length = " + length);
        }
    }

    protected static class FindResult<T> {

        public Segment<T> segment;
        public int offset;
        public int blockIndex;

        public FindResult(Segment<T> segment, int offset, int segIndex) {
            this.segment = segment;
            this.offset = offset;
            this.blockIndex = segIndex;
        }
    }

    private FindResult<T> makeResult(Segment<T> segment, int offset, int segIndex) {
        return new FindResult<>(segment, offset, segIndex);
    }

    protected FindResult<T> getSegment(int elementIndex) {
        if (segments.isEmpty()) {
            segments.add(onCreateSegment(segmentCapacity));
        }
        int offset = 0;
        var backBlock = segments.get(segments.size() - 1);
        int backOffset = size() - backBlock.size();
        for (int i = 0, j = segments.size() - 1; i <= j; i++, j--) {
            var block = segments.get(i);
            if ((elementIndex >= offset && elementIndex < offset + block.size()) || i + 1 == segments.size()) {
                return makeResult(block, offset, i);
            }
            offset += block.size();

            block = backBlock;
            if ((elementIndex >= backOffset && elementIndex < backOffset + block.size()) || (j == segments.size() - 1 && elementIndex == length)) {
                return makeResult(block, backOffset, j);
            }
            if (j > 0) {
                backBlock = segments.get(j - 1);
                backOffset -= backBlock.size();
            }
        }
        throw new IllegalStateException("unreachable");
    }

    private FindResult<T> getSegmentMut(int elementIndex) {
        var res = getSegment(elementIndex);
        res.segment = ensureMutable(res.blockIndex);
        return res;
    }

    private Segment<T> ensureMutable(int segIdx) {
        var block = segments.get(segIdx);
        var n = block.toMutable();
        if (block != n) {
            segments.set(segIdx, n);
            block.release();
            return n;
        }
        return block;
    }

    @Override
    public T set(int index, T element) {
        checkAccessIndex(index);
        var result = getSegmentMut(index);
        var x = result.segment.setElementAt(index - result.offset, element);
        modCount++;
        return x;
    }

    @Override
    public void add(int index, T element) {
        checkInsertIndex(index);
        var result = getSegmentMut(index);
        result.segment.addElementAt(index - result.offset, element);
        length++;
        adjustElements(result.blockIndex, result.segment);
        if (result.segment.size() >= segmentCapacity) {
            var divPoint = segmentCapacity / 2;
            var seg = onCreateSegment(segmentCapacity);
            var sub = result.segment.subList(divPoint, result.segment.size());
            seg.addAll(sub);
            sub.clear();
            segments.add(result.blockIndex + 1, seg);
            seg.onSegmentBatchUpdated();
            result.segment.onSegmentBatchUpdated();
        }
        modCount++;
    }

    @NonNull
    @Override
    public T remove(int index) {
        checkAccessIndex(index);
        var result = getSegmentMut(index);
        var res = result.segment.removeElementAt(index - result.offset);
        length--;
        mergeSegment(index - 1, index);
        mergeSegment(index, index + 1);
        modCount++;
        return res;
    }

    @NonNull
    @Override
    public T get(int index) {
        checkAccessIndex(index);
        var result = getSegment(index);
        return result.segment.get(index - result.offset);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) throw new IndexOutOfBoundsException("start > end");
        if (fromIndex < 0 || toIndex > length)
            throw new IndexOutOfBoundsException("start = " + fromIndex + ", end = " + toIndex + ", length = " + size());
        if (fromIndex == toIndex) return;
        var res = getSegment(fromIndex);
        int offset = res.offset;
        int index = res.blockIndex;
        var seg = res.segment;

        while (toIndex - offset > 0 && index < segments.size()) {
            int segLength = seg.size();
            if (fromIndex <= offset && toIndex >= offset + segLength) {
                // Remove the segment
                segments.remove(index);
                seg.release();
            } else {
                ensureMutable(index);
                seg = segments.get(index);
                var sub = seg.subList(Math.max(fromIndex - offset, 0), Math.min(toIndex - offset, segLength));
                sub.clear();
                seg.onSegmentBatchUpdated();
                index++;
            }
            offset += segLength;
            if (index < segments.size())
                seg = segments.get(index);
        }
        mergeSegment(index - 1, index);
        length -= toIndex - fromIndex;
        modCount++;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public void clear() {
        for (var seg : segments) {
            seg.release();
        }
        segments.clear();
        length = 0;
    }

    @Override
    public void close() {
        clear();
    }

    public SegmentList<T> shallowCopy() {
        var list = onCreateShallowCopyInstance();
        list.segments.clear();
        for (var seg : segments) {
            seg.retain();
        }
        list.segments.addAll(segments);
        list.length = length;
        return list;
    }

    protected SegmentList<T> onCreateShallowCopyInstance() {
        return new SegmentList<>(getSegmentCapacity());
    }

    public int getSegmentCapacity() {
        return segmentCapacity;
    }

    private void mergeSegment(int seg1, int seg2) {
        if (seg1 > seg2) {
            int tmp = seg1;
            seg1 = seg2;
            seg2 = tmp;
        }
        if (seg1 == seg2 || seg1 < 0 || seg2 >= segments.size()) return;
        var pre = segments.get(seg1);
        var aft = segments.get(seg2);
        if (pre.size() + aft.size() <= segmentCapacity * 3 / 4) {
            ensureMutable(seg1);
            pre = segments.get(seg1);
            pre.addAll(aft);
            pre.onSegmentBatchUpdated();

            segments.remove(seg2);
            aft.release();
        }
    }

    private void adjustElements(int segIdx, Segment<T> mutCur) {
        if (segIdx > 0) {
            var pre = segments.get(segIdx - 1);
            if (pre.isMutable() && pre.size() <= segmentCapacity * 4 / 5 && mutCur.size() > segmentCapacity * 4 / 5) {
                var sub = mutCur.subList(0, segmentCapacity * 4 / 5 - pre.size());
                pre.addAll(sub);
                sub.clear();

                pre.onSegmentBatchUpdated();
                mutCur.onSegmentBatchUpdated();
            }
        }
    }

    protected static class Segment<T> extends ArrayList<T> implements ShareableData<Segment<T>> {

        public Segment(int initialCapacity) {
            super(initialCapacity);
        }

        private final AtomicInteger refCount = new AtomicInteger(1);

        @Override
        public void retain() {
            refCount.incrementAndGet();
        }

        @Override
        public void release() {
            if (refCount.decrementAndGet() < 0) {
                throw new IllegalStateException("illegal release invocation");
            }
        }

        @Override
        public boolean isMutable() {
            return refCount.get() == 1;
        }

        @Override
        public Segment<T> toMutable() {
            if (isMutable()) {
                return this;
            } else {
                return copy();
            }
        }

        @CallSuper
        public Segment<T> copy() {
            var res = onCreateCopyInstance();
            res.addAll(this);
            return res;
        }

        protected Segment<T> onCreateCopyInstance() {
            return new Segment<>(this.size());
        }

        @CallSuper
        protected T setElementAt(int index, T e) {
            return set(index, e);
        }

        @CallSuper
        protected void addElementAt(int index, T e) {
            add(index, e);
        }

        @CallSuper
        protected T removeElementAt(int index) {
            return remove(index);
        }

        @CallSuper
        protected void onSegmentBatchUpdated() {

        }
    }

}
