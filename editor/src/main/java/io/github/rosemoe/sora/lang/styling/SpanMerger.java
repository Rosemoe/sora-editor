/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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
package io.github.rosemoe.sora.lang.styling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

import io.github.rosemoe.sora.lang.styling.color.ResolvableColor;
import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver;
import io.github.rosemoe.sora.lang.styling.span.SpanExt;
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs;

/**
 * Composes two read-only span layers without copying unaffected spans.
 *
 * <p>Columns must be non-negative and non-decreasing. Spans at the same column retain their
 * order within each layer; the last one applies to subsequent text. A nonempty lower layer
 * must start at column zero. The upper layer can start later; a zero-style span ends an override.
 * Nonzero upper color IDs replace lower color IDs, style flags are combined, and nonnull
 * upper extensions and extra data take precedence. Color resolvers are merged per channel.</p>
 *
 * <p>The result is an unmodifiable view made of ranges referencing the input lists and a
 * list of generated spans. Neither the inputs nor their spans are modified. The input lists,
 * spans and their extensions must remain unchanged while the result is in use. Spans read
 * from the result are borrowed and must not be modified or recycled.</p>
 */
public final class SpanMerger {

    private SpanMerger() {
    }

    @NonNull
    public static List<Span> merge(@NonNull List<Span> layerLower, @NonNull List<Span> layerUpper) {
        checkColumns(layerLower, true);
        checkColumns(layerUpper, false);
        if (layerLower.isEmpty()) {
            return Collections.unmodifiableList(layerUpper);
        }
        if (layerUpper.isEmpty()) {
            return Collections.unmodifiableList(layerLower);
        }
        var builder = new Builder();
        int lowerIndex = -1;
        int upperIndex = -1;
        boolean wasOverridden = false;
        while (lowerIndex + 1 < layerLower.size() || upperIndex + 1 < layerUpper.size()) {
            long nextLower = lowerIndex + 1 < layerLower.size()
                    ? layerLower.get(lowerIndex + 1).getColumn() : Long.MAX_VALUE;
            long nextUpper = upperIndex + 1 < layerUpper.size()
                    ? layerUpper.get(upperIndex + 1).getColumn() : Long.MAX_VALUE;
            int column = (int) Math.min(nextLower, nextUpper);
            if (nextLower == column) {
                lowerIndex++;
            }
            if (nextUpper == column) {
                upperIndex++;
            }
            Span lower = layerLower.get(lowerIndex);
            Span upper = upperIndex < 0 ? null : layerUpper.get(upperIndex);
            boolean overridden = upper != null && !isEmpty(upper);
            if (!overridden) {
                if (lower.getColumn() == column) {
                    builder.append(layerLower, lowerIndex);
                } else if (wasOverridden) {
                    builder.appendGenerated(new MergedSpan(column, lower, null));
                }
            } else if (isEmpty(lower) && upper.getColumn() == column) {
                builder.append(layerUpper, upperIndex);
            } else {
                builder.appendGenerated(new MergedSpan(column, lower, upper));
            }
            wasOverridden = overridden;
        }
        return Collections.unmodifiableList(new MergedList(builder.regions));
    }

    private static void checkColumns(List<Span> spans, boolean startsAtZero) {
        int previous = -1;
        for (int i = 0; i < spans.size(); i++) {
            int column = spans.get(i).getColumn();
            if (column < 0 || column < previous || (i == 0 && startsAtZero && column != 0)) {
                throw new IllegalArgumentException("Span columns must be nonnegative and nondecreasing; the lower layer must start at zero");
            }
            previous = column;
        }
    }

    private static boolean isEmpty(Span span) {
        return span.getStyle() == 0 && span.getExtra() == null
                && !span.hasSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER)
                && !span.hasSpanExt(SpanExtAttrs.EXT_EXTERNAL_RENDERER)
                && !span.hasSpanExt(SpanExtAttrs.EXT_INTERACTION_INFO)
                && span.getUnderlineColor() == null;
    }

    private static final class Region {
        final List<Span> source;
        final int sourceStart;
        final int resultStart;
        int size = 1;

        Region(List<Span> source, int sourceStart, int resultStart) {
            this.source = source;
            this.sourceStart = sourceStart;
            this.resultStart = resultStart;
        }
    }

    private static final class Builder {
        final List<Region> regions = new ArrayList<>();
        final List<Span> generated = new ArrayList<>();
        int size;

        void append(List<Span> source, int index) {
            Region last = regions.isEmpty() ? null : regions.get(regions.size() - 1);
            if (last != null && last.source == source && last.sourceStart + last.size == index) {
                last.size++;
            } else {
                regions.add(new Region(source, index, size));
            }
            size++;
        }

        void appendGenerated(Span span) {
            generated.add(span);
            append(generated, generated.size() - 1);
        }
    }

    private static final class MergedList extends AbstractList<Span> implements RandomAccess {
        private final Region[] regions;
        private final int size;

        MergedList(List<Region> regions) {
            this.regions = regions.toArray(new Region[0]);
            Region last = this.regions[this.regions.length - 1];
            size = last.resultStart + last.size;
        }

        @Override
        public Span get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
            }
            int low = 0;
            int high = regions.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                Region region = regions[mid];
                if (index < region.resultStart) {
                    high = mid - 1;
                } else if (index >= region.resultStart + region.size) {
                    low = mid + 1;
                } else {
                    return region.source.get(region.sourceStart + index - region.resultStart);
                }
            }
            throw new AssertionError("Missing span region");
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static final class MergedSpan implements Span, SpanColorResolver {
        private final int column;
        private final Span lower;
        @Nullable
        private final Span upper;

        MergedSpan(int column, Span lower, @Nullable Span upper) {
            this.column = column;
            this.lower = lower;
            this.upper = upper;
        }

        @Override
        public int getColumn() {
            return column;
        }

        @Override
        public long getStyle() {
            long style = lower.getStyle();
            if (upper != null) {
                long overlay = upper.getStyle();
                if ((overlay & TextStyle.FOREGROUND_BITS) != 0) {
                    style &= ~TextStyle.FOREGROUND_BITS;
                }
                if ((overlay & TextStyle.BACKGROUND_BITS) != 0) {
                    style &= ~TextStyle.BACKGROUND_BITS;
                }
                style |= overlay;
            }
            return style;
        }

        @Override
        public Object getExtra() {
            return upper != null && upper.getExtra() != null ? upper.getExtra() : lower.getExtra();
        }

        @Override
        public ResolvableColor getUnderlineColor() {
            return getSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR);
        }

        @Override
        public boolean hasSpanExt(int type) {
            return getSpanExt(type) != null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getSpanExt(int type) {
            if (type == SpanExtAttrs.EXT_COLOR_RESOLVER) {
                return lower.hasSpanExt(type) || upper != null && upper.hasSpanExt(type) ? (T) this : null;
            }
            if (type == SpanExtAttrs.EXT_UNDERLINE_COLOR) {
                ResolvableColor overlay = upper == null ? null : upper.getUnderlineColor();
                return (T) (overlay != null ? overlay : lower.getUnderlineColor());
            }
            T overlay = upper == null ? null : upper.getSpanExt(type);
            return overlay != null ? overlay : lower.getSpanExt(type);
        }

        @Override
        public ResolvableColor getForegroundColor(@NonNull Span span) {
            return resolveColor(true);
        }

        @Override
        public ResolvableColor getBackgroundColor(@NonNull Span span) {
            return resolveColor(false);
        }

        private ResolvableColor resolveColor(boolean foreground) {
            if (upper != null) {
                SpanColorResolver resolver = upper.getSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER);
                if (resolver != null) {
                    ResolvableColor color = foreground ? resolver.getForegroundColor(upper) : resolver.getBackgroundColor(upper);
                    if (color != null) {
                        return color;
                    }
                }
                if ((foreground ? upper.getForegroundColorId() : upper.getBackgroundColorId()) != 0) {
                    return null;
                }
            }
            SpanColorResolver resolver = lower.getSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER);
            return resolver == null ? null : foreground ? resolver.getForegroundColor(lower) : resolver.getBackgroundColor(lower);
        }

        @NonNull
        @Override
        public Span copy() {
            Span copy = SpanFactory.obtain(column, getStyle());
            copy.setExtra(getExtra());
            for (int type = SpanExtAttrs.EXT_COLOR_RESOLVER; type <= SpanExtAttrs.EXT_UNDERLINE_COLOR; type++) {
                copy.setSpanExt(type, getSpanExt(type));
            }
            return copy;
        }

        @Override
        public void setColumn(int column) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setStyle(long style) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setUnderlineColor(@Nullable ResolvableColor color) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setExtra(Object extraData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSpanExt(int type, @Nullable SpanExt ext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAllSpanExt() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean recycle() {
            throw new UnsupportedOperationException();
        }
    }
}
