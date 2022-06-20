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
package org.eclipse.tm4e.core.internal.oniguruma;

class OnigNextMatchResult implements IOnigNextMatchResult {

    private final int index;

    private final IOnigCaptureIndex[] captureIndices;

    public OnigNextMatchResult(OnigResult result, OnigString source) {
        this.index = result.getIndex();
        this.captureIndices = captureIndicesForMatch(result, source);
    }

    private static IOnigCaptureIndex[] captureIndicesForMatch(OnigResult result, OnigString source) {
        int resultCount = result.count();
        IOnigCaptureIndex[] captures = new IOnigCaptureIndex[resultCount];
        for (int index = 0; index < resultCount; index++) {
            int captureStart = source.convertUtf8OffsetToUtf16(result.locationAt(index));
            int captureEnd = source.convertUtf8OffsetToUtf16(result.locationAt(index) + result.lengthAt(index));
            captures[index] = new OnigCaptureIndex(index, captureStart, captureEnd);
        }
        return captures;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public IOnigCaptureIndex[] getCaptureIndices() {
        return captureIndices;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{\n");
        result.append("  \"index\": ");
        result.append(getIndex());
        result.append(",\n");
        result.append("  \"captureIndices\": [\n");
        int i = 0;
        for (IOnigCaptureIndex captureIndex : getCaptureIndices()) {
            if (i > 0) {
                result.append(",\n");
            }
            result.append("    ");
            result.append(captureIndex);
            i++;
        }
        result.append("\n");
        result.append("  ]\n");
        result.append("}");
        return result.toString();
    }

    private static class OnigCaptureIndex implements IOnigCaptureIndex {

        private final int index;
        private final int start;
        private final int end;

        public OnigCaptureIndex(int index, int start, int end) {
            this.index = index;
            this.start = start >= 0 ? start : 0;
            this.end = end >= 0 ? end : 0;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public int getLength() {
            return end - start;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("{\"index\": ");
            result.append(getIndex());
            result.append(", \"start\": ");
            result.append(getStart());
            result.append(", \"end\": ");
            result.append(getEnd());
            result.append(", \"length\": ");
            result.append(getLength());
            result.append("}");
            return result.toString();
        }
    }

}
