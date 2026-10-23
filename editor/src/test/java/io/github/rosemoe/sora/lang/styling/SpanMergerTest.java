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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.rosemoe.sora.lang.styling.span.SpanConstColorResolver;
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs;
import io.github.rosemoe.sora.util.RendererUtils;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SpanMergerTest {
    @Test
    public void mergesColorsAndFlagsWithoutChangingInputs() {
        Span base = SpanFactory.obtainNoExt(0, TextStyle.makeStyle(5, 6, true, false, false));
        Span overlay = SpanFactory.obtainNoExt(2, TextStyle.makeStyle(0, 7, false, true, false));
        List<Span> lower = List.of(base, SpanFactory.obtainNoExt(8, 9));
        List<Span> upper = List.of(overlay, SpanFactory.obtainNoExt(5, 0));
        List<Span> merged = SpanMerger.merge(lower, upper);

        assertColumns(merged, 0, 2, 5, 8);
        assertSame(base, merged.get(0));
        assertSame(lower.get(1), merged.get(3));
        assertEquals(5, merged.get(1).getForegroundColorId());
        assertEquals(7, merged.get(1).getBackgroundColorId());
        assertTrue(TextStyle.isBold(merged.get(1).getStyle()));
        assertTrue(TextStyle.isItalics(merged.get(1).getStyle()));
        assertEquals(base.getStyle(), merged.get(2).getStyle());
        assertEquals(0, base.getColumn());
        assertEquals(6, base.getBackgroundColorId());
        assertEquals(2, overlay.getColumn());
        assertEquals(0, overlay.getForegroundColorId());
        assertThrows(UnsupportedOperationException.class, () -> merged.clear());
        assertThrows(UnsupportedOperationException.class, () -> merged.set(0, overlay));
        assertThrows(UnsupportedOperationException.class, () -> merged.get(1).setStyle(0));
        assertThrows(UnsupportedOperationException.class, () -> merged.get(1).recycle());
    }

    @Test
    public void resolvesEachColorChannelWithUpperPrecedence() {
        Span lower = SpanFactory.obtain(0, TextStyle.makeStyle(5, 6, true, false, false));
        lower.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, new SpanConstColorResolver(0xff112233, 0xff445566));
        lower.setUnderlineColor(0xff010203);
        Object extra = new Object();
        lower.setExtra(extra);
        Span upper = SpanFactory.obtain(1, 0);
        upper.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, new SpanConstColorResolver(0, 0x66ef3566));
        Span merged = SpanMerger.merge(List.of(lower), List.of(upper)).get(1);
        EditorColorScheme scheme = new EditorColorScheme();

        assertEquals(0xff112233, RendererUtils.getForegroundColor(merged, scheme));
        assertEquals(0x66ef3566, RendererUtils.getBackgroundColor(merged, scheme));
        assertSame(lower.getUnderlineColor(), merged.getUnderlineColor());
        assertSame(extra, merged.getExtra());
        assertSame(extra, merged.copy().getExtra());

        Span numeric = SpanFactory.obtainNoExt(0, TextStyle.makeStyle(7, 8, false, false, false));
        scheme.setColor(7, 0xff778899);
        scheme.setColor(8, 0xffaabbcc);
        Span replaced = SpanMerger.merge(List.of(lower), List.of(numeric)).get(0);
        assertEquals(0xff778899, RendererUtils.getForegroundColor(replaced, scheme));
        assertEquals(0xffaabbcc, RendererUtils.getBackgroundColor(replaced, scheme));
        assertNull(replaced.<io.github.rosemoe.sora.lang.styling.span.SpanColorResolver>getSpanExt(
                SpanExtAttrs.EXT_COLOR_RESOLVER).getForegroundColor(replaced));
    }

    @Test
    public void reusesLargeUnaffectedRegionsAndSupportsIterationAndSubLists() {
        List<Span> lower = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            lower.add(SpanFactory.obtainNoExt(i * 2, 1));
        }
        List<Span> merged = SpanMerger.merge(List.copyOf(lower), List.of(
                SpanFactory.obtainNoExt(501, TextStyle.BACKGROUND_BITS), SpanFactory.obtainNoExt(503, 0)));
        assertEquals(1002, merged.size());
        assertSame(lower.get(0), merged.get(0));
        assertSame(lower.get(250), merged.get(250));
        assertSame(lower.get(252), merged.get(254));
        assertSame(lower.get(999), merged.get(1001));
        int previous = -1;
        for (Span span : merged) {
            assertTrue(span.getColumn() > previous);
            previous = span.getColumn();
        }
        assertEquals(3, merged.subList(250, 253).size());
        assertThrows(IndexOutOfBoundsException.class, () -> merged.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> merged.get(merged.size()));
        assertSame(lower.get(0), SpanMerger.merge(lower, List.of()).get(0));
    }

    @Test
    public void acceptsRepeatedColumnsAndUsesLastSpanInEachLayer() {
        List<Span> lower = List.of(
                SpanFactory.obtainNoExt(0, TextStyle.makeStyle(5, 0, true, false, false)),
                SpanFactory.obtainNoExt(0, TextStyle.makeStyle(6, 0, false, true, false)),
                SpanFactory.obtainNoExt(2, TextStyle.makeStyle(7, 0, false, false, true)),
                SpanFactory.obtainNoExt(2, 8),
                SpanFactory.obtainNoExt(6, 9),
                SpanFactory.obtainNoExt(6, 10));
        List<Span> upper = List.of(
                SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 1, true, false, false)),
                SpanFactory.obtainNoExt(0, 0),
                SpanFactory.obtainNoExt(2, TextStyle.makeStyle(11, 2, true, false, false)),
                SpanFactory.obtainNoExt(2, 0),
                SpanFactory.obtainNoExt(2, TextStyle.makeStyle(0, 3, false, true, false)),
                SpanFactory.obtainNoExt(6, TextStyle.makeStyle(0, 4, false, false, true)),
                SpanFactory.obtainNoExt(6, 0));
        List<Span> merged = SpanMerger.merge(lower, upper);

        for (int column = 0; column < 8; column++) {
            long expected = column < 2 ? lower.get(1).getStyle()
                    : column < 6 ? TextStyle.makeStyle(8, 3, false, true, false) : 10;
            assertEquals(expected, styleAt(merged, column));
        }
        int previous = -1;
        for (Span span : merged) {
            assertTrue(span.getColumn() >= previous);
            previous = span.getColumn();
        }
        assertSame(lower.get(5), merged.get(merged.size() - 1));
        List<Span> lowerOnly = SpanMerger.merge(lower, List.of());
        List<Span> upperOnly = SpanMerger.merge(List.of(), upper);
        for (int i = 0; i < lower.size(); i++) {
            assertSame(lower.get(i), lowerOnly.get(i));
        }
        for (int i = 0; i < upper.size(); i++) {
            assertSame(upper.get(i), upperOnly.get(i));
        }
    }

    @Test
    public void rejectsNegativeAndDescendingColumns() {
        List<Span> valid = List.of(SpanFactory.obtainNoExt(0, 1));
        List<Span> negative = List.of(SpanFactory.obtainNoExt(-1, 1));
        List<Span> descending = List.of(SpanFactory.obtainNoExt(0, 1),
                SpanFactory.obtainNoExt(2, 1), SpanFactory.obtainNoExt(1, 1));
        assertThrows(IllegalArgumentException.class, () -> SpanMerger.merge(negative, valid));
        assertThrows(IllegalArgumentException.class, () -> SpanMerger.merge(valid, negative));
        assertThrows(IllegalArgumentException.class, () -> SpanMerger.merge(descending, valid));
        assertThrows(IllegalArgumentException.class, () -> SpanMerger.merge(valid, descending));
    }

    @Test
    public void matchesReferenceCompositionForRandomBoundaries() {
        Random random = new Random(883);
        for (int trial = 0; trial < 100; trial++) {
            List<Span> lower = new ArrayList<>();
            List<Span> upper = new ArrayList<>();
            lower.add(SpanFactory.obtainNoExt(0, randomStyle(random)));
            for (int column = 0; column < 100; column++) {
                if (random.nextInt(5) == 0) {
                    int count = 1 + random.nextInt(3);
                    for (int i = 0; i < count; i++)
                        lower.add(SpanFactory.obtainNoExt(column, randomStyle(random)));
                }
                if (random.nextInt(5) == 0) {
                    int count = 1 + random.nextInt(3);
                    for (int i = 0; i < count; i++)
                        upper.add(SpanFactory.obtainNoExt(column, randomStyle(random)));
                }
            }
            List<Span> merged = SpanMerger.merge(lower, upper);
            for (int column = 0; column < 110; column++) {
                long a = styleAt(lower, column);
                long b = styleAt(upper, column);
                long mask = (TextStyle.getForegroundColorId(b) != 0 ? TextStyle.FOREGROUND_BITS : 0)
                        | (TextStyle.getBackgroundColorId(b) != 0 ? TextStyle.BACKGROUND_BITS : 0);
                assertEquals((a & ~mask) | b, styleAt(merged, column));
            }
        }
    }

    private static long randomStyle(Random random) {
        return TextStyle.makeStyle(random.nextInt(3), random.nextInt(3), random.nextBoolean(),
                random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
    }

    private static long styleAt(List<Span> spans, int column) {
        long style = 0;
        for (Span span : spans) {
            if (span.getColumn() > column) break;
            style = span.getStyle();
        }
        return style;
    }

    private static void assertColumns(List<Span> spans, int... columns) {
        assertEquals(columns.length, spans.size());
        for (int i = 0; i < columns.length; i++) {
            assertEquals(columns[i], spans.get(i).getColumn());
        }
    }
}
