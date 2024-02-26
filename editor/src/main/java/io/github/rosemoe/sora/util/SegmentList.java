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

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SegmentList<T> extends AbstractList<T> {

    public final static int DEFAULT_SEGMENT_CAPACITY = 1024;

    private final List<Segment<T>> segments;

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

    private void checkInsertIndex(int index) {
        if (index < 0 || index > length) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds. length = " + length);
        }
    }

    private void checkAccessIndex(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds. length = " + length);
        }
    }

    private static class FindResult<T> {

        Segment<T> segment;
        int offset;
        int blockIndex;

        public FindResult(Segment<T> segment, int offset, int segIndex) {
            this.segment = segment;
            this.offset = offset;
            this.blockIndex = segIndex;
        }
    }

    private FindResult<T> getSegment(int index) {
        int offset = 0;
        for (int i = 0; i < segments.size(); i++) {
            var block = segments.get(i);
            if (index >= offset && index < offset + block.size() || i + 1 == segments.size()) {
                return new FindResult<>(block, offset, i);
            }
            offset += block.size();
        }
        throw new IllegalStateException("unreachable");
    }

    private FindResult<T> getSegmentMut(int index) {
        var res = getSegment(index);
        ensureMutable(res.blockIndex);
        res.segment = segments.get(res.blockIndex);
        return res;
    }

    private void ensureMutable(int segIdx) {
        var block = segments.get(segIdx);
        if (!block.isMutable()) {
            var n = block.toMutable();
            if (n != block) {
                segments.set(segIdx, n);
                block.release();
            }
        }
    }

    @Override
    public T set(int index, T element) {
        checkAccessIndex(index);
        var result = getSegmentMut(index);
        return result.segment.set(index - result.offset, element);
    }

    @Override
    public void add(int index, T element) {
        checkInsertIndex(index);
        var result = getSegmentMut(index);
        result.segment.add(index - result.offset, element);
        length++;
        if (result.segment.size() >= segmentCapacity) {
            var divPoint = segmentCapacity / 2;
            var seg = new Segment<T>();
            seg.addAll(result.segment.subList(divPoint, result.segment.size()));
            result.segment.removeRange(divPoint, result.segment.size());
            segments.add(result.blockIndex + 1, seg);
        }
    }

    @Override
    public T remove(int index) {
        checkAccessIndex(index);
        var result = getSegmentMut(index);
        var res = result.segment.remove(index - result.offset);
        length--;
        mergeSegment(index - 1, index);
        mergeSegment(index, index + 1);
        return res;
    }

    @Override
    public T get(int index) {
        checkAccessIndex(index);
        var result = getSegment(index);
        return result.segment.get(index - result.offset);
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

    private void mergeSegment(int blk1, int blk2) {
        if (blk1 > blk2) {
            int tmp = blk1;
            blk1 = blk2;
            blk2 = tmp;
        }
        if (blk1 == blk2 || blk1 < 0 || blk2 >= length) return;
        var pre = segments.get(blk1);
        var aft = segments.get(blk2);
        if (pre.size() + aft.size() <= segmentCapacity * 3 / 4) {
            ensureMutable(blk1);
            pre = segments.get(blk1);
            pre.addAll(aft);
            segments.remove(blk2);
            aft.release();
        }
    }

    private static class Segment<T> extends ArrayList<T> implements ShareableData<Segment<T>> {

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

        public Segment<T> copy() {
            var res = new Segment<T>();
            res.addAll(this);
            return res;
        }
    }

}
