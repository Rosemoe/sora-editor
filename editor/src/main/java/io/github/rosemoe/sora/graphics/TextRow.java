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
package io.github.rosemoe.sora.graphics;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.inlayHint.CharacterSide;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs;
import io.github.rosemoe.sora.lang.styling.span.SpanExternalRenderer;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.FunctionCharacters;
import io.github.rosemoe.sora.text.bidi.Directions;
import io.github.rosemoe.sora.text.bidi.IDirections;
import io.github.rosemoe.sora.text.bidi.VisualDirections;
import io.github.rosemoe.sora.text.breaker.WordBreaker;
import io.github.rosemoe.sora.text.breaker.WordBreakerEmpty;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.RendererUtils;
import io.github.rosemoe.sora.util.ReversedListView;
import io.github.rosemoe.sora.util.TemporaryFloatBuffer;
import io.github.rosemoe.sora.widget.layout.RowElement;
import io.github.rosemoe.sora.widget.layout.RowElementTypes;
import io.github.rosemoe.sora.widget.rendering.RenderingConstants;
import io.github.rosemoe.sora.widget.rendering.TextAdvancesCache;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * {@link TextRow} is a helper class for a single text row to shape, measure and draw.
 * <p>
 * Each row is firstly reordered according to the logical line directions analysis. Then
 * each run is processed from visually left to right. Elements in a run maybe a segment of text,
 * or an inlay hint (inline element).
 * <p>
 * A special case is text breaking, which happens in wordwrap mode. When breaking text, the runs are
 * processed logically because a single row must represent a logically continuous text segment.
 * <p>
 * The indices are mostly unchecked in this class, so caller have to duty to offer valid indices.
 *
 * @author Rosemoe
 */
public class TextRow {

    private final static String LOG_TAG = "TextRow";
    private final static Comparator<Span> SPAN_COMPARATOR = (a, b) -> {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return Integer.compare(a.getColumn(), b.getColumn());
    };

    /**
     * The minimum character count of a span to be auto-truncated.
     */
    private final static int MIN_AUTO_TRUNCATE_LENGTH = 64;
    /**
     * Max context length when text run is truncated
     */
    private final static int MAX_CONTEXT_LENGTH = 256;

    private final RectF tmpRect = new RectF();
    private final int[] tmpIndices = new int[4];
    private final Span tmpSpan = SpanFactory.obtainNoExt(0, 0);
    private ContentLine text;
    private Directions directions;
    private int textStart;
    private int textEnd;
    private List<Span> spans;
    private List<InlayHint> inlineElements;
    private TextRowParams params;
    private InlayHintRenderParams inlayHintRenderParams;
    private Paint paint;
    private TextAdvancesCache measureCache;
    private int selectedStart = -1;
    private int selectedEnd = -1;

    public TextRow() {

    }

    public void set(@NonNull ContentLine text,
                    int start, int end, @Nullable List<Span> spans, @Nullable List<InlayHint> inlineElements,
                    @NonNull Directions directions, @NonNull Paint paint,
                    @Nullable TextAdvancesCache measureCache, @NonNull TextRowParams params) {
        this.text = text;
        textStart = start;
        textEnd = end;
        this.spans = spans;
        this.inlineElements = inlineElements;
        this.directions = directions;
        this.paint = paint;
        this.params = params;
        this.measureCache = measureCache;
        this.inlayHintRenderParams = params.toInlayHintRenderParams();
    }

    /**
     * Update the range of text
     */
    public void setRange(int start, int end) {
        this.textStart = start;
        this.textEnd = end;
    }

    public int getTextStart() {
        return textStart;
    }

    public int getTextEnd() {
        return textEnd;
    }

    public void setSelectedRange(int start, int end) {
        this.selectedStart = start;
        this.selectedEnd = end;
    }

    /**
     * Get character advances for text breaking, in a single run
     */
    private float getSingleRunAdvancesForBreaking(int start, int end, int contextStart, int contextEnd,
                                                  boolean isRtl, float[] advances) {
        var chars = text.getBackingCharArray();
        int lastEnd = start;
        float tabWidth = params.getTabWidth() * paint.getSpaceWidth();
        float width = 0f;
        for (int i = start; i <= end; i++) {
            if (i == end || chars[i] == '\t') {
                // commit [lastEnd, i)
                if (i > lastEnd)
                    width += paint.myGetTextRunAdvances(chars, lastEnd, i - lastEnd, contextStart, contextEnd - contextStart, isRtl, advances, (lastEnd - start));
                if (i < end) {
                    width += tabWidth;
                    if (advances != null)
                        advances[i - start] = tabWidth;
                }
                lastEnd = i + 1;
            }
        }
        return width;
    }

    /**
     * Get the character advances and horizontal advance in a single text run
     */
    private float getTextRunAdvancesCacheable(int index, int count, int contextIndex, int contextCount, boolean isRtl, @Nullable float[] advances, int advancesIndex) {
        if (measureCache != null) {
            if (advances != null) {
                for (int i = 0; i < count; i++) {
                    advances[advancesIndex + i] = measureCache.getAdvanceAt(index + i);
                }
            }
            return measureCache.getAdvancesSum(index, index + count);
        }
        return paint.myGetTextRunAdvances(text.getBackingCharArray(), index, count, contextIndex, contextCount, isRtl, advances, advancesIndex);
    }

    /**
     * Get the cursor horizontal advance in a single text run
     */
    private float getRunAdvanceCacheable(int offset, int start, int end,
                                         int contextStart, int contextEnd, boolean isRtl) {
        if (measureCache != null) {
            return measureCache.getAdvancesSum(start, offset);
        }
        return GraphicsCompat.getRunAdvance(paint, text.getBackingCharArray(), start, end, contextStart, contextEnd, isRtl, offset);
    }

    /**
     * Find the character index from cursor horizontal offset
     */
    private int findOffsetByAdvanceCacheable(int start, int end, int contextStart, int contextEnd, boolean isRtl, float advance) {
        if (measureCache != null) {
            var cache = measureCache;
            int left = start, right = end;
            var base = cache.getAdvancesSum(0, start);
            while (left <= right) {
                var mid = (left + right) / 2;
                if (mid < start || mid >= end) {
                    left = mid;
                    break;
                }
                var value = cache.getAdvancesSum(0, mid) - base;
                if (value > advance) {
                    right = mid - 1;
                } else if (value < advance) {
                    left = mid + 1;
                } else {
                    left = mid;
                    break;
                }
            }
            if (cache.getAdvancesSum(0, left) - base > advance) {
                left--;
            }
            left = Math.max(start, Math.min(end, left));
            return left;
        }
        return paint.findOffsetByRunAdvance(text, start, end, contextStart, contextEnd, isRtl, advance);
    }

    /**
     * Iterate the runs in the row
     *
     * @param reorderVisually {@code true} to reorder the elements visually. Otherwise, runs are consumed logically.
     */
    private void iterateRuns(RunElementsConsumer consumer, boolean reorderVisually) {
        ListPointers pointers = null;
        // Generally, reordering is not required
        IDirections dirs = reorderVisually && text.mayNeedBidi() ? new VisualDirections(directions) : directions;
        for (int i = 0; i < dirs.getRunCount(); i++) {
            int runEnd = dirs.getRunEnd(i);
            int runStart = dirs.getRunStart(i);
            int segmentStart = Math.max(runStart, textStart);
            int segmentEnd = Math.min(runEnd, textEnd);
            // We can not stop here because in multi-run text, directions are reordered and may not be logically continuous
            if (segmentStart >= segmentEnd) {
                continue;
            }
            pointers = seekStartIndices(segmentStart);
            if (!generateAndConsumeSingleRun(segmentStart, segmentEnd, dirs.isRunRtl(i), pointers, consumer)) {
                break;
            }
        }
        // handle trailing inline elements
        int currInlineIndex = pointers == null ? 0 : pointers.inlineElementIndex;
        List<RowElement> trailingInlineRun = new ArrayList<>();
        while (currInlineIndex < inlineElements.size() && getExpectedInlayHintColumn(inlineElements.get(currInlineIndex)) == textEnd) {
            var e = new RowElement();
            e.type = RowElementTypes.INLAY_HINT;
            e.displayColumnPosition = textEnd;
            e.inlayHint = inlineElements.get(currInlineIndex++);
            trailingInlineRun.add(e);
        }
        if (!trailingInlineRun.isEmpty()) {
            if (pointers == null) {
                pointers = seekStartIndices(textEnd);
            }
            pointers.inlineElementIndex = currInlineIndex;
            consumer.accept(trailingInlineRun, false, pointers);
        }
    }

    /**
     * Get the expected column position to render after.
     */
    private int getExpectedInlayHintColumn(InlayHint inlayHint) {
        int position = inlayHint.getColumn();
        if (inlayHint.getDisplaySide() == CharacterSide.RIGHT) {
            position++;
        }
        position = Math.min(position, textEnd);
        return position;
    }

    /**
     * Seek the start indices for {@code spans} and {@code inlineElements}
     */
    private ListPointers seekStartIndices(int segmentStart) {
        tmpSpan.setColumn(segmentStart);
        int spanIndex = Collections.binarySearch(spans, tmpSpan, SPAN_COMPARATOR);
        if (spanIndex < 0) {
            spanIndex = -(spanIndex + 1);
        }
        // spans is expected to have at least one element.
        if (spanIndex == spans.size()) {
            spanIndex--;
        }
        while (spanIndex > 0 && spans.get(spanIndex).getColumn() >= segmentStart) {
            spanIndex--;
        }
        int inlineIndex = 0;
        while (inlineIndex < inlineElements.size() && inlineElements.get(inlineIndex).getColumn() < segmentStart) {
            inlineIndex++;
        }
        return new ListPointers(spanIndex, inlineIndex);
    }

    /**
     * Generate elements in a unidirectional run, and consume them.
     */
    private boolean generateAndConsumeSingleRun(int segmentStart, int segmentEnd, boolean isRtl, ListPointers pointers, RunElementsConsumer consumer) {
        List<RowElement> runElements = new ArrayList<>();
        int lastEndIndex = segmentStart;
        while (true) {
            if (pointers.inlineElementIndex < inlineElements.size() && inlineElements.get(pointers.inlineElementIndex).getColumn() < segmentEnd) {
                var inlay = inlineElements.get(pointers.inlineElementIndex);
                var position = getExpectedInlayHintColumn(inlay);
                var element = new RowElement();
                if (lastEndIndex == position) {
                    pointers.inlineElementIndex++;
                    element.type = RowElementTypes.INLAY_HINT;
                    element.inlayHint = inlay;
                    element.displayColumnPosition = position;
                } else {
                    // lastEndIndex < position
                    element.type = RowElementTypes.TEXT;
                    element.startColumn = lastEndIndex;
                    element.endColumn = position;
                    element.isRtlText = isRtl;
                    lastEndIndex = position;
                }
                runElements.add(element);
            } else if (lastEndIndex < segmentEnd) {
                var element = new RowElement();
                element.type = RowElementTypes.TEXT;
                element.startColumn = lastEndIndex;
                element.endColumn = segmentEnd;
                element.isRtlText = isRtl;
                lastEndIndex = segmentEnd;
                runElements.add(element);
            } else {
                break;
            }
        }
        boolean result = consumer.accept(runElements, isRtl, pointers);
        int spansSize = spans.size();
        while (pointers.spanIndex + 1 < spansSize && spans.get(pointers.spanIndex + 1).getColumn() <= segmentEnd) {
            pointers.spanIndex++;
        }
        return result;
    }

    /**
     * Break the logical line into rows
     * @param width Max width of a row
     * @return At least one row is returned, even when the line is empty.
     */
    public List<WordwrapRow> breakText(int width, boolean antiWordBreaking) {
        List<WordwrapRow> rows = new ArrayList<>();
        var optimizer = antiWordBreaking ? WordBreaker.Factory.newInstance(text) : WordBreakerEmpty.INSTANCE;
        class TextBreaker implements RunElementsConsumer {
            /**
             * Current row to append elements. Maybe empty.
             */
            WordwrapRow currentRow = new WordwrapRow();
            /**
             * Width of current row
             */
            float currentWidth = 0f;

            @Override
            public boolean accept(List<RowElement> elements, boolean isRtl, ListPointers pointers) {
                for (var element : elements) {
                    if (element.type == RowElementTypes.TEXT) {
                        handleText(element);
                    } else if (element.type == RowElementTypes.INLAY_HINT) {
                        handleInlineElement(element);
                    }
                }
                return true;
            }

            void commitRow() {
                currentRow.rowWidth = currentWidth;
                rows.add(currentRow);
                currentWidth = 0f;
                currentRow = new WordwrapRow();
            }

            void handleText(RowElement e) {
                var advances = TemporaryFloatBuffer.obtain(e.endColumn - e.startColumn);
                float runWidth = getSingleRunAdvancesForBreaking(e.startColumn, e.endColumn, e.startColumn, e.endColumn, e.isRtlText, advances);

                // Easy case: the whole run can be placed on the row
                if (currentWidth + runWidth < width) {
                    if (currentRow.isEmpty) {
                        currentRow.setInitialRange(e.startColumn, e.endColumn);
                    } else {
                        currentRow.setEndColumn(e.endColumn);
                    }
                    currentWidth += runWidth;
                    TemporaryFloatBuffer.recycle(advances);
                    return;
                }

                // Split by grapheme boundary and possible line boundaries
                var limit = e.endColumn - e.startColumn;
                var offset = 0;
                while (offset < limit) {
                    int next = GraphemeBoundsBreaker.findGraphemeBreakPoint(advances, limit, (int) (width - currentWidth), offset);
                    if (next == offset) {
                        if (currentRow.isEmpty) {
                            // Force to break the text, though no space is available
                            next++;
                        } else {
                            // Switch to new row
                            commitRow();
                            continue;
                        }
                    }
                    // do anti-word-breaking
                    int beforeOptimization = next;
                    next = optimizer.getOptimizedBreakPoint(e.startColumn + offset, e.startColumn + next) - e.startColumn;
                    float advance = 0f;
                    for (int j = offset; j < next; j++) {
                        advance += advances[j];
                    }
                    if (currentRow.isEmpty) {
                        currentRow.setInitialRange(e.startColumn + offset, e.startColumn + next);
                    } else {
                        // It's okay the directly set the end, because text elements are yielded in logical order
                        currentRow.setEndColumn(e.startColumn + next);
                    }
                    currentWidth += advance;
                    if (beforeOptimization != next) {
                        // The row end is optimized, switch to new row
                        commitRow();
                    }
                    offset = next;
                }
                TemporaryFloatBuffer.recycle(advances);
            }

            void handleInlineElement(RowElement e) {
                var inlay = e.inlayHint;
                var renderer = params.getInlayHintRendererProvider().getInlayHintRendererForType(inlay.getType());
                float w = 0f;
                if (renderer != null) {
                    w = renderer.measure(inlay, paint, inlayHintRenderParams);
                    w = Math.max(0f, w);
                }
                if (currentRow.isEmpty || currentWidth + w > width) {
                    if (!currentRow.isEmpty) {
                        commitRow();
                    }
                    // we don't care if the new row can actually display the whole inlay hint
                    // because inlay hint can not split
                    currentRow.setInitialRange(e.displayColumnPosition, e.displayColumnPosition);
                    currentRow.addInlayHint(inlay);
                    currentWidth = w;
                } else {
                    currentRow.addInlayHint(inlay);
                    currentWidth += w;
                }
            }

            void appendTailIfNeeded() {
                if (!currentRow.isEmpty) {
                    commitRow();
                }
            }
        }
        var breaker = new TextBreaker();
        iterateRuns(breaker, false);
        // The line is empty, but there should be at least one row in the result
        if (rows.isEmpty() && breaker.currentRow.isEmpty) {
            breaker.currentRow.isEmpty = false;
            breaker.currentRow.startColumn = textStart;
            breaker.currentRow.endColumn = textEnd;
        }
        breaker.appendTailIfNeeded();
        return rows;
    }

    /**
     * The context for iterating in run elements.
     */
    private static class IteratingContext {
        /* for paint style update (avoid redundant style update native calls) */
        public long lastStyle = -1;
        /* for horizontal offset limiting */
        public float minOffset = 0f;
        public float maxOffset = Float.MAX_VALUE;
        /* for horizontal cursor offset seeking */
        public int targetCharOffset = -1;
        public float resultOffset = 0f;
        /* for horizontal char offset seeking */
        public float targetHorizontalOffset = -1f;
        public int resultCharOffset = -1;
        /* for background region iterating / text patching */
        public int startCharOffset;
        public int endCharOffset;
        /* for background region iterating */
        public RegionBuffer regionBuffer;
        /* for text patching */
        public boolean autoClip;
        public DrawTextConsumer drawTextConsumer;
        public Span currentSpan;
        /* for measure cache */
        public TextAdvancesCache advances;
    }

    /**
     * Check if cursor at given offset should be displayed in the text segment
     */
    private boolean checkCursorOffsetInSegment(int offset, int start, int end) {
        return (offset >= start && (offset < end || (offset == end && end == textEnd)));
    }

    /**
     * Clip text region to patch certain regions
     */
    private void clipRegionForPatchDrawing(float textOffset, float width, boolean italics, Canvas canvas) {
        if (!italics) {
            canvas.clipRect(textOffset, 0, textOffset + width, params.getRowHeight());
            return;
        }
        var path = new Path();
        var y = params.getTextBottom();
        path.moveTo(textOffset, y);
        path.lineTo(textOffset - RenderingConstants.TEXT_SKEW_X * y, 0f);
        path.lineTo(textOffset + width - RenderingConstants.TEXT_SKEW_X * y, 0f);
        path.lineTo(textOffset + width, y);
        path.close();
        canvas.clipPath(path);
    }

    /**
     * Draw a single function character
     */
    protected void drawFunctionCharacter(Canvas canvas, float offsetX, float width, char ch) {
        var paintGraph = params.getGraphPaint();
        var metricsGraph = params.getGraphMetrics();
        paintGraph.setTextAlign(android.graphics.Paint.Align.CENTER);
        float heightScaled = metricsGraph.descent - metricsGraph.ascent;
        float centerY = params.getRowHeight() / 2f;
        float baseline = centerY - heightScaled / 2f - metricsGraph.ascent;
        paintGraph.setColor(paint.getColor());
        canvas.drawText(FunctionCharacters.getNameForFunctionCharacter(ch), offsetX + width / 2f, baseline, paintGraph);
        paintGraph.setTextAlign(android.graphics.Paint.Align.LEFT);

        float actualWidth = paintGraph.measureText(FunctionCharacters.getNameForFunctionCharacter(ch));
        tmpRect.top = centerY - heightScaled / 2f;
        tmpRect.bottom = centerY + heightScaled / 2f;
        tmpRect.left = offsetX + width / 2f - actualWidth / 2f;
        tmpRect.right = offsetX + width / 2f + actualWidth / 2f;
        int color = paint.getColor();
        paint.setColor(params.getColorScheme().getColor(EditorColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE));
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        paint.setStrokeWidth(params.getRowHeight() * 0.05f);
        canvas.drawRoundRect(tmpRect, params.getRowHeight() * params.getRoundTextBackgroundFactor(), params.getRowHeight() * params.getRoundTextBackgroundFactor(), paint);
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setColor(color);
    }

    /**
     * Terminal function for a single-styled text run. Commit the given text region to Canvas.
     * This function also handles function characters rendering.
     */
    private void commitTextRunToCanvas(int paintStart, int paintEnd, int contextStart, int contextEnd, boolean isRtl,
                                       Canvas canvas, float offset, float width) {
        if (paint.isRenderFunctionCharacters()) {
            var chars = text.getBackingCharArray();
            int lastEnd = paintStart;
            float initOffset = offset + (isRtl ? width : 0f);
            float drawOffset = initOffset;
            for (int i = paintStart; i <= paintEnd; i++) {
                char ch = '\0';
                if (i == paintEnd || FunctionCharacters.isEditorFunctionChar(ch = chars[i])) {
                    // commit [lastEnd, i)
                    if (i - lastEnd > 0) {
                        if (isRtl) {
                            paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                        }
                        GraphicsCompat.drawTextRun(canvas, chars, lastEnd, i - lastEnd, contextStart, contextEnd - contextStart, drawOffset, params.getTextBaseline(), isRtl, paint);
                        if (isRtl) {
                            paint.setTextAlign(android.graphics.Paint.Align.LEFT);
                        }
                    }
                    if (i == paintEnd) {
                        break;
                    }
                    float chAdvance = paint.measureText(FunctionCharacters.getNameForFunctionCharacter(ch));
                    float advance = getRunAdvanceCacheable(i, paintStart, paintEnd, paintStart, paintEnd, isRtl);
                    drawFunctionCharacter(canvas, isRtl ? initOffset - advance - chAdvance : initOffset + advance, chAdvance, ch);
                    advance += chAdvance;
                    drawOffset = initOffset + (isRtl ? -advance : advance);
                    lastEnd = i;
                }
            }
        } else {
            GraphicsCompat.drawTextRun(canvas, text.getBackingCharArray(), paintStart, paintEnd - paintStart, contextStart, contextEnd - contextStart, offset, params.getTextBaseline(), isRtl, paint);
        }
    }

    private void commitTextRunToConsumer(int paintStart, int paintEnd, int contextStart, int contextEnd, boolean isRtl,
                                         Canvas canvas, float offset, float width, IteratingContext ctx) {
        ctx.drawTextConsumer.drawText(canvas, text.getBackingCharArray(), paintStart, paintEnd - paintStart, contextStart, contextEnd - contextStart, isRtl, offset, width, params, ctx.currentSpan);
    }

    /**
     * Truncate the text run to be committed, try to limit the committed part to be in the visible region
     */
    private void commitTextRunAutoTruncated(int paintStart, int paintEnd, int contextStart, int contextEnd, boolean isRtl,
                                            Canvas canvas, float offset, float width, IteratingContext ctx) {
        if (paintEnd - paintStart < MIN_AUTO_TRUNCATE_LENGTH || measureCache == null) {
            if (ctx.drawTextConsumer != null) {
                commitTextRunToConsumer(paintStart, paintEnd, contextStart, contextEnd, isRtl, canvas, offset, width, ctx);
            } else {
                commitTextRunToCanvas(paintStart, paintEnd, contextStart, contextEnd, isRtl, canvas, offset, width);
            }
        } else {
            float runAdvanceLeft = Math.max(0, ctx.minOffset - offset) - paint.getSpaceWidth();
            float runAdvanceRight = Math.min(width, ctx.maxOffset - offset) + paint.getSpaceWidth();
            int boundForLeft = findOffsetByAdvanceCacheable(paintStart, paintEnd, contextStart, contextEnd, isRtl, runAdvanceLeft);
            int boundForRight = findOffsetByAdvanceCacheable(paintStart, paintEnd, contextStart, contextEnd, isRtl, runAdvanceRight);
            int commitStart = Math.min(boundForLeft, boundForRight);
            int commitEnd = Math.max(boundForLeft, boundForRight);

            if (commitStart < commitEnd) {
                int commitContextStart = commitStart, commitContextEnd = commitEnd;
                var chars = text.getBackingCharArray();
                while (commitContextStart - 1 >= contextStart && chars[commitContextStart - 1] != ' '
                        && (commitContextEnd - commitContextStart) < MAX_CONTEXT_LENGTH) {
                    commitContextStart--;
                }
                while (commitContextEnd + 1 < contextEnd && chars[commitContextEnd] != ' '
                        && (commitContextEnd - commitContextStart) < MAX_CONTEXT_LENGTH) {
                    commitContextEnd++;
                }

                float advanceStart = measureAdvanceInRun(commitStart, paintStart, paintEnd, contextStart, contextEnd, isRtl);
                float advanceEnd = measureAdvanceInRun(commitEnd, paintStart, paintEnd, contextStart, contextEnd, isRtl);
                float newWidth = Math.abs(advanceStart - advanceEnd);
                float commitOffset = isRtl ? offset + width - advanceEnd : offset + advanceStart;
                if (ctx.drawTextConsumer != null) {
                    commitTextRunToConsumer(commitStart, commitEnd, contextStart, contextEnd, isRtl, canvas, commitOffset, newWidth, ctx);
                } else {
                    commitTextRunToCanvas(commitStart, commitEnd, contextStart, contextEnd, isRtl, canvas, commitOffset, newWidth);
                }
            }
        }
    }

    /**
     * Split the region with breakpoints from selection, and commit them separately to achieve
     * different text color in selected region
     */
    private void splitRegionsAndCommit(int paintStart, int paintEnd, boolean isRtl,
                                       Canvas canvas, float offset, float width, int color, IteratingContext ctx) {
        int selectionStart = Math.max(paintStart, Math.min(paintEnd, selectedStart));
        int selectionEnd = Math.max(paintStart, Math.min(paintEnd, selectedEnd));
        tmpIndices[0] = paintStart;
        tmpIndices[1] = paintEnd;
        tmpIndices[2] = selectionStart;
        tmpIndices[3] = selectionEnd;
        Arrays.sort(tmpIndices);
        float advance = 0f;
        for (int i = 0; i + 1 < tmpIndices.length; i++) {
            int commitStart = tmpIndices[i], commitEnd = tmpIndices[i + 1];
            if (commitStart == commitEnd) {
                continue;
            }
            if (commitStart >= selectionStart && commitEnd <= selectionEnd) {
                paint.setColor(params.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
            } else {
                paint.setColor(color);
            }
            float segmentWidth = getRunAdvanceCacheable(commitEnd, commitStart, commitEnd, paintStart, paintEnd, isRtl);
            if (isRtl) {
                commitTextRunAutoTruncated(commitStart, commitEnd, paintStart, paintEnd, true, canvas, offset + width - advance - segmentWidth, segmentWidth, ctx);
            } else {
                commitTextRunAutoTruncated(commitStart, commitEnd, paintStart, paintEnd, false, canvas, offset + advance, segmentWidth, ctx);
            }
            advance += segmentWidth;
        }
    }

    /**
     * Terminal handler for text element.
     */
    private float handleSingleStyledText(int paintStart, int paintEnd, boolean isRtl, Span span,
                                         Canvas canvas, float offset, IteratingContext ctx) {
        var paintGeneral = paint;

        // Apply font style if required
        if ((canvas != null && ctx.drawTextConsumer == null) || measureCache == null) {
            long styleBits = span.getStyleBits();
            if (span.getStyleBits() != ctx.lastStyle) {
                paintGeneral.setFakeBoldText(TextStyle.isBold(styleBits));
                if (TextStyle.isItalics(styleBits)) {
                    paintGeneral.setTextSkewX(RenderingConstants.TEXT_SKEW_X);
                } else {
                    paintGeneral.setTextSkewX(0);
                }
                ctx.lastStyle = styleBits;
            }
        }

        float[] advances = null;
        if (ctx.advances != null) {
            advances = TemporaryFloatBuffer.obtain(paintEnd - paintStart);
        }
        float width = getTextRunAdvancesCacheable(paintStart, paintEnd - paintStart,
                paintStart, paintEnd - paintStart, isRtl,
                advances, 0);
        if (ctx.advances != null && advances != null) {
            for (int i = paintStart; i < paintEnd; i++) {
                ctx.advances.setAdvanceAt(i, advances[i - paintStart]);
            }
            TemporaryFloatBuffer.recycle(advances);
        }
        if (checkCursorOffsetInSegment(ctx.targetCharOffset, paintStart, paintEnd)) {
            // Immediately stop the iteration
            ctx.maxOffset = 0f;
            float advance = getRunAdvanceCacheable(ctx.targetCharOffset, paintStart, paintEnd, paintStart, paintEnd, isRtl);
            if (isRtl) {
                ctx.resultOffset = offset + width - advance;
            } else {
                ctx.resultOffset = offset + advance;
            }
            return width;
        }

        if (ctx.targetHorizontalOffset != -1f) {
            float runOffset = ctx.targetHorizontalOffset - offset;
            if (isRtl) {
                runOffset = width - runOffset;
            }
            // Do some easy checks to avoid expensive native calls
            if (runOffset > width) {
                ctx.resultCharOffset = paintEnd;
            } else if (runOffset <= 0) {
                ctx.resultCharOffset = paintStart;
            } else {
                ctx.resultCharOffset = findOffsetByAdvanceCacheable(paintStart, paintEnd, paintStart, paintEnd, isRtl, runOffset);
            }
        }

        float regionLeft = -1f, regionRight = -1f;
        if (ctx.regionBuffer != null || ctx.drawTextConsumer != null) {
            int sharedTextStart = Math.max(paintStart, ctx.startCharOffset);
            int sharedTextEnd = Math.min(paintEnd, ctx.endCharOffset);
            if (sharedTextStart < sharedTextEnd) {
                if (sharedTextStart == paintStart && sharedTextEnd == paintEnd) {
                    regionLeft = offset;
                    regionRight = offset + width;
                } else {
                    float startAdvance = getRunAdvanceCacheable(sharedTextStart, paintStart, paintEnd, paintStart, paintEnd, isRtl);
                    float endAdvance = getRunAdvanceCacheable(sharedTextEnd, paintStart, paintEnd, paintStart, paintEnd, isRtl);
                    startAdvance = isRtl ? width - startAdvance : startAdvance;
                    endAdvance = isRtl ? width - endAdvance : endAdvance;
                    regionLeft = offset + Math.min(startAdvance, endAdvance);
                    regionRight = offset + Math.max(startAdvance, endAdvance);
                }
                if (ctx.regionBuffer != null) {
                    ctx.regionBuffer.commitRegion(regionLeft, regionRight);
                }
            }
        }

        // Check intersection for early return
        float sharedStart = Math.max(offset, ctx.minOffset);
        float sharedEnd = Math.min(offset + width, ctx.maxOffset);
        if (sharedStart >= sharedEnd) {
            return width;
        }

        if (canvas == null) {
            return width;
        }

        if (ctx.drawTextConsumer != null) {
            int sharedTextStart = Math.max(paintStart, ctx.startCharOffset);
            int sharedTextEnd = Math.min(paintEnd, ctx.endCharOffset);
            if (sharedTextStart >= sharedTextEnd) {
                return width;
            }
            if (ctx.autoClip) {
                canvas.save();
                clipRegionForPatchDrawing(regionLeft, regionRight - regionLeft, TextStyle.isItalics(span.getStyleBits()), canvas);
            }
            ctx.currentSpan = span;
            commitTextRunAutoTruncated(paintStart, paintEnd, paintStart, paintEnd, isRtl, canvas, offset, width, ctx);
            ctx.currentSpan = null;
            ctx.lastStyle = -1;
            if (ctx.autoClip) {
                canvas.restore();
            }

            // drawing virtually, we should not actually render to the canvas
            return width;
        }

        var paintOther = params.getMiscPaint();
        SpanExternalRenderer renderer = span.getSpanExt(SpanExtAttrs.EXT_EXTERNAL_RENDERER);

        // Invoke external renderer preDraw
        if (renderer != null && renderer.requirePreDraw()) {
            int saveCount = canvas.save();
            canvas.translate(offset, 0);
            canvas.clipRect(0f, params.getRowTop(), width, params.getRowHeight());
            try {
                renderer.draw(canvas, paintGeneral, params.getColorScheme(), true);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while invoking external renderer", e);
            }
            canvas.restoreToCount(saveCount);
        }

        int backgroundColor = RendererUtils.getBackgroundColor(span, params.getColorScheme());
        if (backgroundColor != 0 && paintStart != paintEnd) {
            tmpRect.set(offset, params.getRowTop(), offset + width, params.getRowBottom());
            paintGeneral.setColor(backgroundColor);
            canvas.drawRoundRect(tmpRect, params.getRowHeight() * params.getRoundTextBackgroundFactor(), params.getRowHeight() * params.getRoundTextBackgroundFactor(), paintGeneral);
        }

        // Draw text
        int foregroundColor = RendererUtils.getForegroundColor(span, params.getColorScheme());
        if (selectedStart >= selectedEnd || selectedStart >= textEnd || selectedEnd <= textStart
                || params.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED) == 0) {
            // Easy case when there is no selected text in region
            paintGeneral.setColor(foregroundColor);
            commitTextRunAutoTruncated(paintStart, paintEnd, paintStart, paintEnd, isRtl, canvas, offset, width, ctx);
        } else {
            splitRegionsAndCommit(paintStart, paintEnd, isRtl, canvas, offset, width, foregroundColor, ctx);
        }

        // Draw strikethrough
        if (TextStyle.isStrikeThrough(span.getStyle())) {
            var strikethroughColor = params.getColorScheme().getColor(EditorColorScheme.STRIKETHROUGH);
            paintOther.setColor(strikethroughColor == 0 ? paintGeneral.getColor() : strikethroughColor);
            canvas.drawLine(offset, params.getRowTop() + params.getRowHeight() / 2f, offset + width, params.getRowTop() + params.getRowHeight() / 2f, paintOther);
        }

        // Draw underline
        var underlineColor = span.getUnderlineColor();
        int underlineColorInt;
        if (underlineColor != null && (underlineColorInt = underlineColor.resolve(params.getColorScheme())) != 0) {
            tmpRect.bottom = params.getTextBottom();
            tmpRect.top = tmpRect.bottom - params.getTextHeight() * RenderingConstants.TEXT_UNDERLINE_WIDTH_FACTOR;
            tmpRect.left = offset;
            tmpRect.right = offset + width;
            paintGeneral.setColor(underlineColorInt);
            canvas.drawRect(tmpRect, paintGeneral);
        }

        // Invoke external renderer postDraw
        if (renderer != null && renderer.requirePostDraw()) {
            int saveCount = canvas.save();
            canvas.translate(offset, params.getRowTop());
            canvas.clipRect(0f, 0f, width, params.getRowHeight());
            try {
                renderer.draw(canvas, paintGeneral, params.getColorScheme(), false);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while invoking external renderer", e);
            }
            canvas.restoreToCount(saveCount);
        }
        return width;
    }

    /**
     * Split text in an unidirectional run with span boundaries
     */
    private float handleMultiStyledText(int start, int end, boolean isRtl, ListPointers pointers,
                                        Canvas canvas, float offset, IteratingContext ctx) {
        int spanIndex = pointers.spanIndex;
        int targetCharIndex = isRtl ? end - 1 : start;
        int spansSize = spans.size();
        while (spanIndex + 1 < spansSize && spans.get(spanIndex + 1).getColumn() <= targetCharIndex) {
            spanIndex++;
        }
        float localOffset = 0f;
        if (isRtl) {
            // spanIndex controls text starts at (end-1) or before
            int nextEnd = end;
            while (nextEnd > start) {
                boolean moveSpanIndex = true;
                int segmentStart;

                Span span = spans.get(spanIndex);
                if (spanIndex == 0) {
                    moveSpanIndex = false;
                    segmentStart = 0;
                } else {
                    segmentStart = span.getColumn();
                }
                segmentStart = Math.max(start, segmentStart);
                int segmentEnd = nextEnd;

                localOffset += handleSingleStyledText(segmentStart, segmentEnd, isRtl, span, canvas, offset + localOffset, ctx);

                if (moveSpanIndex) {
                    spanIndex--;
                }
                nextEnd = segmentStart;

                if (offset + localOffset > ctx.maxOffset) {
                    break;
                }
            }
        } else {
            int lastEnd = start;
            while (lastEnd < end) {
                boolean moveSpanIndex = true;
                int segmentEnd;
                if (spanIndex + 1 >= spansSize) {
                    moveSpanIndex = false;
                    segmentEnd = textEnd;
                } else {
                    segmentEnd = spans.get(spanIndex + 1).getColumn();
                }
                segmentEnd = Math.min(end, segmentEnd);
                int segmentStart = lastEnd;
                Span span = spans.get(spanIndex);

                localOffset += handleSingleStyledText(segmentStart, segmentEnd, isRtl, span, canvas, offset + localOffset, ctx);

                lastEnd = segmentEnd;
                if (moveSpanIndex) {
                    spanIndex++;
                }

                if (offset + localOffset > ctx.maxOffset) {
                    break;
                }
            }
        }
        return localOffset;
    }

    /**
     * Split text in an unidirectional run with tab.
     * Tab character is consumed as terminal text segment here.
     */
    private float handleSingleTextElement(RowElement e, ListPointers pointers,
                                          Canvas canvas, float offset, IteratingContext ctx) {
        var chars = text.getBackingCharArray();
        boolean isRtl = e.isRtlText;
        float localOffset = 0f;
        int lastEnd = isRtl ? e.endColumn : e.startColumn;
        int terminalIndex = isRtl ? (e.startColumn - 1) : e.endColumn;
        float tabWidth = params.getTabWidth() * paint.getSpaceWidth();
        for (int index = (isRtl ? e.endColumn - 1 : e.startColumn);
             isRtl ? (index >= terminalIndex) : (index <= terminalIndex);
             index += (isRtl ? -1 : 1)) {
            if (index == terminalIndex || chars[index] == '\t') {
                int regionStart = isRtl ? index + 1 : lastEnd;
                int regionEnd = isRtl ? lastEnd : index;
                localOffset += handleMultiStyledText(regionStart, regionEnd, isRtl, pointers, canvas, offset + localOffset, ctx);
                if (index != terminalIndex) {
                    // tab consumed here
                    // [index, index+1)
                    if (index == ctx.targetCharOffset || (index + 1 == ctx.targetCharOffset && index + 1 == textEnd)) {
                        float advance = index == ctx.targetCharOffset ? 0 : tabWidth;
                        if (isRtl) {
                            ctx.resultOffset = localOffset + tabWidth - advance;
                        } else {
                            ctx.resultOffset = localOffset + advance;
                        }
                        ctx.maxOffset = 0f;
                    }
                    if (ctx.regionBuffer != null && index >= ctx.startCharOffset && index < ctx.endCharOffset) {
                        ctx.regionBuffer.commitRegion(offset + localOffset, offset + localOffset + tabWidth);
                    }
                    if (ctx.advances != null) {
                        ctx.advances.setAdvanceAt(index, tabWidth);
                    }
                    if (ctx.drawTextConsumer != null && index >= ctx.startCharOffset && index < ctx.endCharOffset) {
                        ctx.drawTextConsumer.drawText(canvas, chars, index, 1, index, 1, isRtl, offset + localOffset, tabWidth, params, null);
                    }
                    // virtually drawn
                    localOffset += tabWidth;
                }
                lastEnd = isRtl ? index : index + 1;
                if (offset + localOffset > ctx.maxOffset) {
                    break;
                }
            }
        }
        return localOffset;
    }

    /**
     * Handle a single inline element in an unidirectional run
     */
    private float handleSingleInlineElement(RowElement e,
                                            Canvas canvas, float offset, IteratingContext ctx) {
        var inlay = e.inlayHint;
        var renderer = params.getInlayHintRendererProvider().getInlayHintRendererForType(inlay.getType());
        float w = 0f;
        if (renderer != null) {
            w = renderer.measure(inlay, paint, inlayHintRenderParams);
            w = Math.max(0f, w);
        }
        if (ctx.regionBuffer != null) {
            ctx.regionBuffer.commitPossibleInterval(offset, offset + w);
        }
        if (canvas == null || ctx.drawTextConsumer != null) {
            return w;
        }
        var regionLeft = offset;
        var regionRight = offset + w;
        var sharedStart = Math.max(regionLeft, ctx.minOffset);
        var sharedEnd = Math.min(regionRight, ctx.maxOffset);
        if (renderer != null && sharedStart < sharedEnd) {
            int saveCount = canvas.save();
            canvas.translate(offset, params.getRowTop());
            renderer.render(inlay, canvas, paint, inlayHintRenderParams, params.getColorScheme(), w);
            canvas.restoreToCount(saveCount);
            ctx.lastStyle = -1;
        }
        return w;
    }

    /**
     * Handle elements in a unidirectional run
     */
    private float handleMultiElementRun(List<RowElement> e, boolean isRtl, ListPointers pointers,
                                        Canvas canvas, float offset, IteratingContext ctx) {
        var visualElements = isRtl ? new ReversedListView<>(e) : e;
        float localOffset = 0f;
        for (var element : visualElements) {
            if (element.type == RowElementTypes.TEXT) {
                localOffset += handleSingleTextElement(element, pointers, canvas, offset + localOffset, ctx);
            } else if (element.type == RowElementTypes.INLAY_HINT) {
                localOffset += handleSingleInlineElement(element, canvas, offset + localOffset, ctx);
            }
            if (offset + localOffset > ctx.maxOffset) {
                break;
            }
        }
        return localOffset;
    }

    /**
     * Draw text into the given canvas. Text metrics information is provided by previously set params.
     * Text is rendered from horizontal offset 0 from left to right.
     * @param canvas A pre-translated canvas to draw text
     * @param minHorizontalOffset Min visible horizontal offset in text
     * @param maxHorizontalOffset Max visible horizontal offset in text
     * @return packed integer and float. The first value represents if the row end is drawn; the second represents the final horizontal offset.
     */
    public long draw(@NonNull Canvas canvas, float minHorizontalOffset, float maxHorizontalOffset) {
        var ctx = new IteratingContext();
        ctx.minOffset = minHorizontalOffset;
        ctx.maxOffset = maxHorizontalOffset;
        class DrawHandler implements RunElementsConsumer {

            float horizontalOffset = 0;

            boolean isExhausted = true;

            @Override
            public boolean accept(List<RowElement> e, boolean isRtl, ListPointers pointers) {
                float runWidth = handleMultiElementRun(e, isRtl, pointers, canvas, horizontalOffset, ctx);
                horizontalOffset += runWidth;
                return isExhausted = horizontalOffset < maxHorizontalOffset;
            }

        }
        var handler = new DrawHandler();
        iterateRuns(handler, true);
        return IntPair.packIntFloat(handler.isExhausted ? 1 : 0, handler.horizontalOffset);
    }

    /**
     * Get the horizontal offset of cursor at the given index
     */
    public float getCursorOffsetForIndex(int index) {
        var ctx = new IteratingContext();
        ctx.targetCharOffset = index;
        class CursorOffsetHandler implements RunElementsConsumer {

            float horizontalOffset = 0;

            @Override
            public boolean accept(List<RowElement> e, boolean isRtl, ListPointers pointers) {
                float runWidth = handleMultiElementRun(e, isRtl, pointers, null, horizontalOffset, ctx);
                horizontalOffset += runWidth;
                return ctx.maxOffset != 0f;
            }

        }
        var handler = new CursorOffsetHandler();
        iterateRuns(handler, true);
        return ctx.resultOffset;
    }

    /**
     * Get text index from the given cursor horizontal offset
     */
    public int getIndexForCursorOffset(float offset) {
        var ctx = new IteratingContext();
        ctx.targetHorizontalOffset = offset;
        ctx.maxOffset = offset;
        iterateRuns(new MaxOffsetIterationConsumer(ctx), true);
        return ctx.resultCharOffset == -1 ? textStart : ctx.resultCharOffset;
    }

    /**
     * Iterate over background regions. Visually consequent regions are merged into a single region.
     *
     * @param start                   Start of target background segment
     * @param end                     End of target background segment
     * @param allowLeadingBackground  Allow leading inline elements to be included
     * @param allowTrailingBackground Allow trailing inline elements to be included
     */
    public void iterateBackgroundRegions(int start, int end, boolean allowLeadingBackground, boolean allowTrailingBackground, @NonNull BackgroundRegionConsumer handler) {
        var ctx = new IteratingContext();
        ctx.startCharOffset = start;
        ctx.endCharOffset = end;
        ctx.regionBuffer = new RegionBuffer(ctx, handler, allowLeadingBackground, allowTrailingBackground);
        iterateRuns(new MaxOffsetIterationConsumer(ctx), true);
        ctx.regionBuffer.commitCurrentIfPresent();
    }

    /**
     * Iterate over terminal drawTextRun calls
     * @param start Start of target text segment
     * @param end End of target text segment
     * @param canvas A pre-translated canvas to clip
     * @param minHorizontalOffset Min visible horizontal offset in text
     * @param maxHorizontalOffset Max visible horizontal offset in text
     * @param autoClip Clip region outside the desired text segment
     */
    public void iterateDrawTextRegions(int start, int end, Canvas canvas,
                                       float minHorizontalOffset, float maxHorizontalOffset,
                                       boolean autoClip, @NonNull DrawTextConsumer consumer) {
        var ctx = new IteratingContext();
        ctx.startCharOffset = start;
        ctx.endCharOffset = end;
        ctx.minOffset = minHorizontalOffset;
        ctx.maxOffset = maxHorizontalOffset;
        ctx.autoClip = autoClip;
        ctx.drawTextConsumer = consumer;
        iterateRuns(new MaxOffsetIterationConsumer(ctx, canvas), true);
    }

    /**
     * Compute row width
     */
    public float computeRowWidth() {
        var ctx = new IteratingContext();
        var handler = new MaxOffsetIterationConsumer(ctx);
        iterateRuns(handler, true);
        return handler.horizontalOffset;
    }

    /**
     * Measure text advance in unidirectional run
     */
    public float measureAdvanceInRun(int offset, int start, int end,
                                     int contextStart, int contextEnd, boolean isRtl) {
        return getRunAdvanceCacheable(offset, start, end, contextStart, contextEnd, isRtl);
    }

    /**
     * Build measure of logical line for currently-set text range. A logical line maybe split into
     * multiple rows, so each time only one row is measured.
     * {@link #buildMeasureCacheTailor(TextAdvancesCache)} should always be called for each line.
     * @see #buildMeasureCacheTailor(TextAdvancesCache)
     * @param cache size must be bigger than text end
     */
    public void buildMeasureCacheStep(@NonNull TextAdvancesCache cache) {
        var ctx = new IteratingContext();
        ctx.advances = cache;
        iterateRuns(new MaxOffsetIterationConsumer(ctx), true);
    }

    /**
     * Finish the logical line measure cache building.
     * @see #buildMeasureCacheStep(TextAdvancesCache)
     * @param cache size must be bigger than text end
     */
    public void buildMeasureCacheTailor(@NonNull TextAdvancesCache cache) {
        cache.finishBuilding();
    }

    /**
     * Iteration consumer that stops when the horizontal offset exceeds the desired max offset
     */
    private class MaxOffsetIterationConsumer implements RunElementsConsumer {

        float horizontalOffset = 0f;
        IteratingContext ctx;
        Canvas canvas;

        MaxOffsetIterationConsumer(IteratingContext ctx) {
            this.ctx = ctx;
        }

        MaxOffsetIterationConsumer(IteratingContext ctx, Canvas canvas) {
            this(ctx);
            this.canvas = canvas;
        }

        @Override
        public boolean accept(List<RowElement> e, boolean isRtl, ListPointers pointers) {
            horizontalOffset += handleMultiElementRun(e, isRtl, pointers, canvas, horizontalOffset, ctx);
            return horizontalOffset < ctx.maxOffset;
        }

    }

    /**
     * Result of {@link #breakText(int, boolean)}
     */
    public static class WordwrapRow {
        boolean isEmpty = true;
        /**
         * Start column (inclusive)
         */
        public int startColumn;
        /**
         * End column (exclusive)
         */
        public int endColumn;
        /**
         * Inlay hints on the row, maybe null or empty
         */
        public List<InlayHint> inlayHints;
        public float rowWidth;

        void setInitialRange(int start, int end) {
            isEmpty = false;
            startColumn = start;
            endColumn = end;
        }

        void setEndColumn(int column) {
            if (isEmpty) {
                throw new IllegalStateException();
            }
            this.endColumn = column;
        }

        void addInlayHint(InlayHint inlayHint) {
            if (isEmpty) {
                throw new IllegalStateException();
            }
            if (inlayHints == null) {
                inlayHints = new ArrayList<>();
            }
            inlayHints.add(inlayHint);
        }
    }

    static class ListPointers {

        int spanIndex;
        int inlineElementIndex;

        public ListPointers(int spanIndex, int inlineElementIndex) {
            this.spanIndex = spanIndex;
            this.inlineElementIndex = inlineElementIndex;
        }

        @NonNull
        protected ListPointers copy() {
            return new ListPointers(spanIndex, inlineElementIndex);
        }
    }

    private static class RegionBuffer {
        private final static float EPS = 1e-6f;
        boolean isEmpty = true;
        float currentLeft;
        float currentRight;
        boolean hasPossibleInterval = false;
        float intervalLeft;
        float intervalRight;
        IteratingContext ctx;
        BackgroundRegionConsumer consumer;
        boolean allowLeadingBackground;
        boolean allowTrailingBackground;

        RegionBuffer(IteratingContext ctx, BackgroundRegionConsumer consumer, boolean allowLeadingBackground, boolean allowTrailingBackground) {
            this.ctx = ctx;
            this.consumer = consumer;
            this.allowLeadingBackground = allowLeadingBackground;
            this.allowTrailingBackground = allowTrailingBackground;
        }

        void commitRegion(float regionLeft, float regionRight) {
            if (isEmpty) {
                if (hasPossibleInterval && Math.abs(regionLeft - intervalRight) <= EPS) {
                    currentLeft = intervalLeft;
                } else {
                    currentLeft = regionLeft;
                }
                currentRight = regionRight;
                isEmpty = false;
                hasPossibleInterval = false;
                return;
            }
            if (!hasPossibleInterval && Math.abs(regionLeft - currentRight) <= EPS) {
                currentRight = regionRight;
                return;
            } else if (hasPossibleInterval && Math.abs(regionLeft - intervalRight) <= EPS) {
                currentRight = regionRight;
                hasPossibleInterval = false;
                return;
            }
            commitCurrentIfPresent();
            isEmpty = false;
            currentLeft = regionLeft;
            currentRight = regionRight;
        }

        void commitPossibleInterval(float regionLeft, float regionRight) {
            if (isEmpty && !allowLeadingBackground) {
                return;
            }
            if (hasPossibleInterval) {
                if (Math.abs(regionLeft - intervalRight) <= EPS) {
                    intervalRight = regionRight;
                } else {
                    hasPossibleInterval = false;
                }
            } else if (Math.abs(regionLeft - currentRight) <= EPS) {
                intervalLeft = regionLeft;
                intervalRight = regionRight;
                hasPossibleInterval = true;
            }
        }

        void commitCurrentIfPresent() {
            if (isEmpty) {
                return;
            }
            if (hasPossibleInterval && allowTrailingBackground) {
                currentRight = intervalRight;
            }
            if (!consumer.handleRegion(currentLeft, currentRight)) {
                ctx.maxOffset = 0f;
            }
            isEmpty = true;
            hasPossibleInterval = false;
        }
    }

    interface RunElementsConsumer {

        boolean accept(List<RowElement> e, boolean isRtl, ListPointers pointers);

    }

    public interface BackgroundRegionConsumer {

        /**
         * Handle a background region
         *
         * @return true to continue the iteration; false otherwise.
         */
        boolean handleRegion(float left, float right);

    }

    public interface DrawTextConsumer {

        /**
         * @param span may be null, when tab encountered.
         */
        void drawText(Canvas canvas, char[] text, int index, int count, int contextIndex, int contextCount, boolean isRtl,
                      float horizontalOffset, float width, TextRowParams params, Span span);

    }
}
