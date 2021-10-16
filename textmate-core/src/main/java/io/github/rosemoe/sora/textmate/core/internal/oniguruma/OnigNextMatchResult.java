/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/atom/node-oniguruma
 * Initial copyright Copyright (c) 2013 GitHub Inc.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - GitHub Inc.: Initial code, written in JavaScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.internal.oniguruma;

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
