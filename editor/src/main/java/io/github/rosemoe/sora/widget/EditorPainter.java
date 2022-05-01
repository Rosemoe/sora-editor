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
package io.github.rosemoe.sora.widget;

import static io.github.rosemoe.sora.util.Numbers.stringSize;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_LINE_SEPARATOR;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_INNER;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_LEADING;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING;
import static io.github.rosemoe.sora.widget.CodeEditor.SCALE_MINI_GRAPH;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.util.MutableInt;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.graphics.BufferedDrawPoints;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.EmptyReader;
import io.github.rosemoe.sora.lang.styling.ExternalRenderer;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Spans;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.text.AndroidBidi;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.Numbers;
import io.github.rosemoe.sora.util.TemporaryCharBuffer;
import io.github.rosemoe.sora.widget.layout.Row;
import io.github.rosemoe.sora.widget.layout.RowIterator;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;

public class EditorPainter {

    private static final String LOG_TAG = EditorPainter.class.getSimpleName();

    private HwAcceleratedRenderer mRenderer;
    private final BufferedDrawPoints mDrawPoints;

    private long mTimestamp;

    private final Paint mPaint;
    private final Paint mPaintOther;
    private final Rect mViewRect;
    private final RectF mRect;
    private final Path mPath;
    private final Paint mPaintGraph;
    private final RectF mVerticalScrollBar;
    private final RectF mHorizontalScrollBar;

    private final CodeEditor mEditor;
    private Paint.FontMetricsInt mLineNumberMetrics;
    private Paint.FontMetricsInt mGraphMetrics;

    private int mCachedLineNumberWidth;
    private android.graphics.Paint.FontMetricsInt mTextMetrics;
    
    private Cursor mCursor;

    public EditorPainter(@NonNull CodeEditor editor) {
        mEditor = editor;
        mVerticalScrollBar = new RectF();
        mHorizontalScrollBar = new RectF();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mRenderer = new HwAcceleratedRenderer(editor);
        }
        mDrawPoints = new BufferedDrawPoints();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaintOther = new Paint();
        mPaintOther.setStrokeWidth(mEditor.getDpUnit() * 1.8f);
        mPaintOther.setStrokeCap(Paint.Cap.ROUND);
        mPaintOther.setTypeface(Typeface.MONOSPACE);
        mPaintOther.setAntiAlias(true);
        mPaintGraph = new Paint();
        mPaintGraph.setAntiAlias(true);

        mTextMetrics = mPaint.getFontMetricsInt();
        mLineNumberMetrics = mPaintOther.getFontMetricsInt();

        mViewRect = new Rect();
        mRect = new RectF();
        mPath = new Path();
        
        notifyFullTextUpdate();
    }
    
    public void notifyFullTextUpdate() {
        mCursor = mEditor.getCursor();
    }

    public void draw(@NonNull Canvas canvas) {
        drawView(canvas);
    }

    public void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        mViewRect.right = width;
        mViewRect.bottom = height;
    }

    Paint getPaint() {
        return mPaint;
    }

    Paint getPaintOther() {
        return mPaintOther;
    }

    Paint getPaintGraph() {
        return mPaintGraph;
    }

    android.graphics.Paint.FontMetricsInt getTextMetrics() {
        return mTextMetrics;
    }

    int getCachedLineNumberWidth() {
        return mCachedLineNumberWidth;
    }

    void setCachedLineNumberWidth(int width) {
        mCachedLineNumberWidth = width;
    }

    public RectF getVerticalScrollBarRect() {
        return mVerticalScrollBar;
    }

    public RectF getHorizontalScrollBarRect() {
        return mHorizontalScrollBar;
    }

    public void setTextSizePxDirect(float size) {
        mPaint.setTextSizeWrapped(size);
        mPaintOther.setTextSize(size);
        mPaintGraph.setTextSize(size * SCALE_MINI_GRAPH);
        mTextMetrics = mPaint.getFontMetricsInt();
        mLineNumberMetrics = mPaintOther.getFontMetricsInt();
        mGraphMetrics = mPaintGraph.getFontMetricsInt();
        invalidateHwRenderer();
        updateTimestamp();
    }

    /**
     * Set text's typeface
     *
     * @param typefaceText New typeface
     */
    public void setTypefaceText(Typeface typefaceText) {
        if (typefaceText == null) {
            typefaceText = Typeface.DEFAULT;
        }
        mPaint.setTypefaceWrapped(typefaceText);
        mTextMetrics = mPaint.getFontMetricsInt();
        invalidateHwRenderer();
        updateTimestamp();
        mEditor.createLayout();
        mEditor.invalidate();
    }

    public void setTypefaceLineNumber(Typeface typefaceLineNumber) {
        if (typefaceLineNumber == null) {
            typefaceLineNumber = Typeface.MONOSPACE;
        }
        mPaintOther.setTypeface(typefaceLineNumber);
        mLineNumberMetrics = mPaintOther.getFontMetricsInt();
        mEditor.invalidate();
    }

    /**
     * Update timestamp required for measuring cache
     */
    protected void updateTimestamp() {
        mTimestamp = System.nanoTime();
    }

    /**
     * Invalidate the whole hardware-accelerated renderer
     */
    public void invalidateHwRenderer() {
        if (mRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mRenderer.invalidate();
        }
    }

    /**
     * Invalidate the region in hardware-accelerated renderer
     */
    void invalidateChanged(int startLine, int endLine) {
        if (mRenderer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mCursor != null) {
            if (mRenderer.invalidateInRegion(startLine, Integer.MAX_VALUE)) {
                mEditor.invalidate();
            }
        }
    }

    /**
     * Invalidate the cursor region in hardware-accelerated renderer
     */
    void invalidateInCursor() {
        invalidateChanged(mCursor.getLeftLine(), mCursor.getRightLine());
    }


    @RequiresApi(29)
    protected void updateLineDisplayList(RenderNode renderNode, int line, Spans.Reader spans) {
        final float waveLength = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveLength;
        final float amplitude = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveAmplitude;
        final float waveWidth = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveWidth;
        mEditor.prepareLine(line);
        /*if (bidiBuffer == null || bidiBuffer.length != mEditor.getLineBuffer().length()) {
            bidiBuffer = new char[mEditor.getLineBuffer().length()];
            bidiLevels = new byte[mEditor.getLineBuffer().length()];
        }
        mEditor.getLineBuffer().getChars(0, mEditor.getLineBuffer().length(), bidiBuffer, 0);
        var dir = AndroidBidi.bidi(AndroidBidi.DIR_LEFT_TO_RIGHT, bidiBuffer, bidiLevels);
        var dirs = AndroidBidi.directions(dir, bidiLevels, 0, bidiBuffer, 0, bidiBuffer.length);*/
        int columnCount = mEditor.getText().getColumnCount(line);
        float widthLine = mEditor.measureText(mEditor.getLineBuffer(), 0, columnCount, line) + mEditor.getDpUnit() * 20;
        renderNode.setPosition(0, 0, (int) widthLine, mEditor.getRowHeight() + (int) amplitude);
        Canvas canvas = renderNode.beginRecording();
        if (spans == null) {
            spans = new EmptyReader();
        }
        int spanOffset = 0;
        float paintingOffset = 0;
        int row = 0;
        float phi = 0f;
        Span span = spans.getSpanAt(spanOffset);
        // Draw by spans
        long lastStyle = 0;
        while (columnCount > span.column) {
            int spanEnd = spanOffset + 1 >= spans.getSpanCount() ? columnCount : spans.getSpanAt(spanOffset + 1).column;
            spanEnd = Math.min(columnCount, spanEnd);
            int paintStart = span.column;
            int paintEnd = Math.min(columnCount, spanEnd);
            float width = mEditor.measureText(mEditor.getLineBuffer(), paintStart, paintEnd - paintStart, line);
            ExternalRenderer renderer = span.renderer;

            // Invoke external renderer preDraw
            if (renderer != null && renderer.requirePreDraw()) {
                int saveCount = canvas.save();
                canvas.translate(paintingOffset, mEditor.getRowTop(row));
                canvas.clipRect(0f, 0f, width, mEditor.getRowHeight());
                try {
                    renderer.draw(canvas, mPaint, mEditor.getColorScheme(), true);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error while invoking external renderer", e);
                }
                canvas.restoreToCount(saveCount);
            }

            // Apply font style
            long styleBits = span.getStyleBits();
            if (span.getStyleBits() != lastStyle) {
                mPaint.setFakeBoldText(TextStyle.isBold(styleBits));
                if (TextStyle.isItalics(styleBits)) {
                    mPaint.setTextSkewX(-0.2f);
                } else {
                    mPaint.setTextSkewX(0);
                }
                lastStyle = styleBits;
            }

            int backgroundColorId = span.getBackgroundColorId();
            if (backgroundColorId != 0) {
                if (paintStart != paintEnd) {
                    mRect.top = mEditor.getRowTop(row);
                    mRect.bottom = mEditor.getRowBottom(row);
                    mRect.left = paintingOffset;
                    mRect.right = mRect.left + width;
                    mPaint.setColor(mEditor.getColorScheme().getColor(backgroundColorId));
                    canvas.drawRoundRect(mRect, mEditor.getRowHeight() * 0.13f, mEditor.getRowHeight() * 0.13f, mPaint);
                }
            }


            // Draw text
            drawRegionTextDirectional(canvas, null, paintingOffset, mEditor.getRowBaseline(row), line, paintStart, paintEnd, span.column, spanEnd, columnCount, mEditor.getColorScheme().getColor(span.getForegroundColorId()));

            // Draw strikethrough
            if ((span.problemFlags & Span.FLAG_DEPRECATED) != 0 || TextStyle.isStrikeThrough(span.style)) {
                mPaintOther.setColor(Color.BLACK);
                canvas.drawLine(paintingOffset, mEditor.getRowTop(row) + mEditor.getRowHeight() / 2f, paintingOffset + width, mEditor.getRowTop(row) + mEditor.getRowHeight() / 2f, mPaintOther);
            }

            // Draw underline
            if (span.underlineColor != 0) {
                mRect.bottom = mEditor.getRowBottom(row) - mEditor.getDpUnit() * 1;
                mRect.top = mRect.bottom - mEditor.getRowHeight() * 0.08f;
                mRect.left = paintingOffset;
                mRect.right = paintingOffset + width;
                drawColor(canvas, span.underlineColor, mRect);
            }

            // Draw issue curly underline
            if (waveWidth > 0 && span.problemFlags > 0 && Integer.highestOneBit(span.problemFlags) != Span.FLAG_DEPRECATED) {
                int color = 0;
                switch (Integer.highestOneBit(span.problemFlags)) {
                    case Span.FLAG_ERROR:
                        color = mEditor.getColorScheme().getColor(EditorColorScheme.PROBLEM_ERROR);
                        break;
                    case Span.FLAG_WARNING:
                        color = mEditor.getColorScheme().getColor(EditorColorScheme.PROBLEM_WARNING);
                        break;
                    case Span.FLAG_TYPO:
                        color = mEditor.getColorScheme().getColor(EditorColorScheme.PROBLEM_TYPO);
                        break;
                }
                if (color != 0 && span.column >= 0 && spanEnd - span.column >= 0) {
                    // Start and end X offset
                    float startOffset = mEditor.measureText(mEditor.getLineBuffer(), 0, span.column, line);
                    float lineWidth = mEditor.measureText(mEditor.getLineBuffer(), Math.max(0, span.column), spanEnd - span.column, line) + phi;
                    float centerY = mEditor.getRowBottom(row);
                    // Clip region due not to draw outside the horizontal region
                    canvas.save();
                    canvas.clipRect(paintingOffset, 0, paintingOffset + width , canvas.getHeight());
                    canvas.translate(startOffset - phi, centerY);
                    // Draw waves
                    mPath.reset();
                    mPath.moveTo(0, 0);
                    int waveCount = (int) Math.ceil(lineWidth / waveLength);
                    for (int i = 0; i < waveCount; i++) {
                        mPath.quadTo(waveLength * i + waveLength / 4, amplitude, waveLength * i + waveLength / 2, 0);
                        mPath.quadTo(waveLength * i + waveLength * 3 / 4, -amplitude, waveLength * i + waveLength, 0);
                    }
                    phi = waveLength - (waveCount * waveLength - lineWidth);
                    // Draw path
                    mPaintOther.setStrokeWidth(mEditor.getDpUnit() * waveWidth);
                    mPaintOther.setStyle(Paint.Style.STROKE);
                    mPaintOther.setColor(color);
                    canvas.drawPath(mPath, mPaintOther);
                    canvas.restore();
                }
                mPaintOther.setStyle(Paint.Style.FILL);
            } else {
                phi = 0f;
            }

            // Invoke external renderer postDraw
            if (renderer != null && renderer.requirePostDraw()) {
                int saveCount = canvas.save();
                canvas.translate(paintingOffset, mEditor.getRowTop(row));
                canvas.clipRect(0f, 0f, width, mEditor.getRowHeight());
                try {
                    renderer.draw(canvas, mPaint, mEditor.getColorScheme(), false);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error while invoking external renderer", e);
                }
                canvas.restoreToCount(saveCount);
            }

            paintingOffset += width;

            if (paintEnd == columnCount) {
                break;
            }
            spanOffset++;
            if (spanOffset < spans.getSpanCount()) {
                span = spans.getSpanAt(spanOffset);
            } else {
                spanOffset--;
            }
        }
        renderNode.endRecording();
        mPaint.setTextSkewX(0);
        mPaint.setFakeBoldText(false);
    }

    // draw methods

    /**
     * Paint the view on given Canvas
     *
     * @param canvas Canvas you want to draw
     */
    public void drawView(Canvas canvas) {
        if (mEditor.isFormatting()) {
            String text = "Formatting your code...";
            float centerY = mEditor.getHeight() / 2f;
            drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL), mRect);
            float baseline = centerY - mEditor.getRowHeight() / 2f + mEditor.getRowBaseline(0);
            float centerX = mEditor.getWidth() / 2f;
            mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT));
            mPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, centerX, baseline, mPaint);
            mPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }
        mCursor.updateCache(mEditor.getFirstVisibleLine());

        EditorColorScheme color = mEditor.getColorScheme();
        drawColor(canvas, color.getColor(EditorColorScheme.WHOLE_BACKGROUND), mViewRect);

        float lineNumberWidth = mEditor.measureLineNumber();
        float offsetX = -mEditor.getOffsetX() + mEditor.measureTextRegionOffset();
        float textOffset = offsetX;

        if (mEditor.isWordwrap()) {
            if (mCachedLineNumberWidth == 0) {
                mCachedLineNumberWidth = (int) lineNumberWidth;
            } else if (mCachedLineNumberWidth != (int) lineNumberWidth && !mEditor.getEventHandler().isScaling) {
                mCachedLineNumberWidth = (int) lineNumberWidth;
                mEditor.createLayout();
            }
        } else {
            mCachedLineNumberWidth = 0;
        }

        mEditor.buildMeasureCacheForLines(mEditor.getFirstVisibleLine(), mEditor.getLastVisibleLine(), mTimestamp);

        if (mCursor.isSelected()) {
            mEditor.getInsertHandleDescriptor().setEmpty();
        }
        if (!mCursor.isSelected()) {
            mEditor.getLeftHandleDescriptor().setEmpty();
            mEditor.getRightHandleDescriptor().setEmpty();
        }

        boolean lineNumberNotPinned = mEditor.isLineNumberEnabled() && (mEditor.isWordwrap() || !mEditor.isLineNumberPinned());

        LongArrayList postDrawLineNumbers = mEditor.getPostDrawLineNumbers();
        postDrawLineNumbers.clear();
        LongArrayList postDrawCurrentLines = mEditor.getPostDrawCurrentLines();
        postDrawCurrentLines.clear();
        List<DrawCursorTask> postDrawCursor = new ArrayList<>(3);
        MutableInt firstLn = mEditor.isFirstLineNumberAlwaysVisible() && mEditor.isWordwrap() ? new MutableInt(-1) : null;

        drawRows(canvas, textOffset, postDrawLineNumbers, postDrawCursor, postDrawCurrentLines, firstLn);

        offsetX = -mEditor.getOffsetX();

        if (lineNumberNotPinned) {
            drawLineNumberBackground(canvas, offsetX, lineNumberWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));
            int lineNumberColor = mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER);
            int currentLineBgColor = mEditor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
            if (mEditor.getCursorAnimator().isRunning()) {
                mRect.bottom = (float) mEditor.getCursorAnimator().animatorBgBottom.getAnimatedValue() - mEditor.getOffsetY();
                mRect.top = mRect.bottom - (float) mEditor.getCursorAnimator().animatorBackground.getAnimatedValue();
                mRect.left = 0;
                mRect.right = (int) (textOffset - mEditor.getDividerMargin());
                drawColor(canvas, currentLineBgColor, mRect);
            }
            for (int i = 0; i < postDrawCurrentLines.size(); i++) {
                drawRowBackground(canvas, currentLineBgColor, (int) postDrawCurrentLines.get(i), (int) (textOffset - mEditor.getDividerMargin()));
            }
            drawDivider(canvas, offsetX + lineNumberWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_DIVIDER));
            if (firstLn != null && firstLn.value != -1) {
                int bottom = mEditor.getRowBottom(0);
                float y;
                if (postDrawLineNumbers.size() == 0 || mEditor.getRowTop(IntPair.getSecond(postDrawLineNumbers.get(0))) - mEditor.getOffsetY() > bottom) {
                    // Free to draw at first line
                    y = (mEditor.getRowBottom(0) + mEditor.getRowTop(0)) / 2f - (mLineNumberMetrics.descent - mLineNumberMetrics.ascent) / 2f - mLineNumberMetrics.ascent;
                } else {
                    int row = IntPair.getSecond(postDrawLineNumbers.get(0));
                    y = (mEditor.getRowBottom(row - 1) + mEditor.getRowTop(row - 1)) / 2f - (mLineNumberMetrics.descent - mLineNumberMetrics.ascent) / 2f - mLineNumberMetrics.ascent - mEditor.getOffsetY();
                }
                mPaintOther.setTextAlign(mEditor.getLineNumberAlign());
                mPaintOther.setColor(lineNumberColor);
                switch (mEditor.getLineNumberAlign()) {
                    case LEFT:
                        canvas.drawText(Integer.toString(firstLn.value + 1), offsetX, y, mPaintOther);
                        break;
                    case RIGHT:
                        canvas.drawText(Integer.toString(firstLn.value + 1), offsetX + lineNumberWidth, y, mPaintOther);
                        break;
                    case CENTER:
                        canvas.drawText(Integer.toString(firstLn.value + 1), offsetX + (lineNumberWidth + mEditor.getDividerMargin()) / 2f, y, mPaintOther);
                }
            }
            for (int i = 0; i < postDrawLineNumbers.size(); i++) {
                long packed = postDrawLineNumbers.get(i);
                drawLineNumber(canvas, IntPair.getFirst(packed), IntPair.getSecond(packed), offsetX, lineNumberWidth, lineNumberColor);
            }
        }

        if (mEditor.isBlockLineEnabled()) {
            if (mEditor.isWordwrap()) {
                drawSideBlockLine(canvas);
            } else {
                drawBlockLines(canvas, textOffset);
            }
        }

        if (!mEditor.getCursorAnimator().isRunning()) {
            for (var action : postDrawCursor) {
                action.execute(canvas);
            }
        } else {
            drawSelectionOnAnimation(canvas);
        }

        if (mEditor.isLineNumberEnabled() && !lineNumberNotPinned) {
            drawLineNumberBackground(canvas, 0, lineNumberWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));
            int lineNumberColor = mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER);
            int currentLineBgColor = mEditor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
            if (mEditor.getCursorAnimator().isRunning()) {
                mRect.bottom = (float) mEditor.getCursorAnimator().animatorBgBottom.getAnimatedValue() - mEditor.getOffsetY();
                mRect.top = mRect.bottom - (float) mEditor.getCursorAnimator().animatorBackground.getAnimatedValue();
                mRect.left = 0;
                mRect.right = (int) (textOffset - mEditor.getDividerMargin());
                drawColor(canvas, currentLineBgColor, mRect);
            }
            for (int i = 0; i < postDrawCurrentLines.size(); i++) {
                drawRowBackground(canvas, currentLineBgColor, (int) postDrawCurrentLines.get(i), (int) (textOffset - mEditor.getDividerMargin() + mEditor.getOffsetX()));
            }
            drawDivider(canvas, lineNumberWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_DIVIDER));
            for (int i = 0; i < postDrawLineNumbers.size(); i++) {
                long packed = postDrawLineNumbers.get(i);
                drawLineNumber(canvas, IntPair.getFirst(packed), IntPair.getSecond(packed), 0, lineNumberWidth, lineNumberColor);
            }
        }

        drawScrollBars(canvas);
        drawEdgeEffect(canvas);

        mEditor.rememberDisplayedLines();
    }

    /**
     * Draw rect on screen
     * Will not do anything if color is zero
     *
     * @param canvas Canvas to draw
     * @param color  Color of rect
     * @param rect   Rect to draw
     */
    protected void drawColor(Canvas canvas, int color, RectF rect) {
        if (color != 0) {
            mPaint.setColor(color);
            canvas.drawRect(rect, mPaint);
        }
    }

    /**
     * Draw rect on screen
     * Will not do anything if color is zero
     *
     * @param canvas Canvas to draw
     * @param color  Color of rect
     * @param rect   Rect to draw
     */
    protected void drawColor(Canvas canvas, int color, Rect rect) {
        if (color != 0) {
            mPaint.setColor(color);
            canvas.drawRect(rect, mPaint);
        }
    }

    /**
     * Draw background for whole row
     */
    protected void drawRowBackground(Canvas canvas, int color, int row) {
        drawRowBackground(canvas, color, row, mViewRect.right);
    }

    protected void drawRowBackground(Canvas canvas, int color, int row, int right) {
        mRect.top = mEditor.getRowTop(row) - mEditor.getOffsetY();
        mRect.bottom = mEditor.getRowBottom(row) - mEditor.getOffsetY();
        mRect.left = 0;
        mRect.right = right;
        drawColor(canvas, color, mRect);
    }

    /**
     * Draw single line number
     */
    protected void drawLineNumber(Canvas canvas, int line, int row, float offsetX, float width, int color) {
        if (width + offsetX <= 0) {
            return;
        }
        if (mPaintOther.getTextAlign() != mEditor.getLineNumberAlign()) {
            mPaintOther.setTextAlign(mEditor.getLineNumberAlign());
        }
        mPaintOther.setColor(color);
        // Line number center align to text center
        float y = (mEditor.getRowBottom(row) + mEditor.getRowTop(row)) / 2f - (mLineNumberMetrics.descent - mLineNumberMetrics.ascent) / 2f - mLineNumberMetrics.ascent - mEditor.getOffsetY();

        var buffer = TemporaryCharBuffer.obtain(20);
        line++;
        int i = stringSize(line);
        Numbers.getChars(line, i, buffer);

        switch (mEditor.getLineNumberAlign()) {
            case LEFT:
                canvas.drawText(buffer, 0, i, offsetX, y, mPaintOther);
                break;
            case RIGHT:
                canvas.drawText(buffer, 0, i, offsetX + width, y, mPaintOther);
                break;
            case CENTER:
                canvas.drawText(buffer, 0, i, offsetX + (width + mEditor.getDividerMargin()) / 2f, y, mPaintOther);
        }
        TemporaryCharBuffer.recycle(buffer);
    }

    /**
     * Draw line number background
     *
     * @param canvas  Canvas to draw
     * @param offsetX Start x of line number region
     * @param width   Width of line number region
     * @param color   Color of line number background
     */
    protected void drawLineNumberBackground(Canvas canvas, float offsetX, float width, int color) {
        float right = offsetX + width;
        if (right < 0) {
            return;
        }
        float left = Math.max(0f, offsetX);
        mRect.bottom = mEditor.getHeight();
        mRect.top = 0;
        int offY = mEditor.getOffsetY();
        if (offY < 0) {
            mRect.bottom = mRect.bottom - offY;
            mRect.top = mRect.top - offY;
        }
        mRect.left = left;
        mRect.right = right;
        drawColor(canvas, color, mRect);
    }

    /**
     * Draw divider line
     *
     * @param canvas  Canvas to draw
     * @param offsetX End x of line number region
     * @param color   Color to draw divider
     */
    protected void drawDivider(Canvas canvas, float offsetX, int color) {
        boolean shadow = mEditor.isLineNumberPinned() && !mEditor.isWordwrap() && mEditor.getOffsetX() > 0;
        float right = offsetX + mEditor.getDividerWidth();
        if (right < 0) {
            return;
        }
        float left = Math.max(0f, offsetX);
        mRect.bottom = mEditor.getHeight();
        mRect.top = 0;
        int offY = mEditor.getOffsetY();
        if (offY < 0) {
            mRect.bottom = mRect.bottom - offY;
            mRect.top = mRect.top - offY;
        }
        mRect.left = left;
        mRect.right = right;
        if (shadow) {
            canvas.save();
            canvas.clipRect(mRect.left, mRect.top, mEditor.getWidth(), mRect.bottom);
            mPaint.setShadowLayer(Math.min(mEditor.getDpUnit() * 8, mEditor.getOffsetX()), 0, 0, Color.BLACK);
        }
        drawColor(canvas, color, mRect);
        if (shadow) {
            canvas.restore();
            mPaint.setShadowLayer(0, 0, 0, 0);
        }
    }

    /**
     * Draw rows with a {@link RowIterator}
     *
     * @param canvas              Canvas to draw
     * @param offset              Offset of text region start
     * @param postDrawLineNumbers Line numbers to be drawn later
     * @param postDrawCursor      Cursors to be drawn later
     */
    protected void drawRows(Canvas canvas, float offset, LongArrayList postDrawLineNumbers, List<DrawCursorTask> postDrawCursor, LongArrayList postDrawCurrentLines, MutableInt requiredFirstLn) {
        int firstVis = mEditor.getFirstVisibleRow();
        final float waveLength = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveLength;
        final float amplitude = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveAmplitude;
        final float waveWidth = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveWidth;
        RowIterator rowIterator = mEditor.getLayout().obtainRowIterator(firstVis);
        Spans spans = mEditor.getStyles() == null ? null : mEditor.getStyles().spans;
        var matchedPositions = new LongArrayList();
        int currentLine = mCursor.isSelected() ? -1 : mCursor.getLeftLine();
        int currentLineBgColor = mEditor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
        int lastPreparedLine = -1;
        int spanOffset = 0;
        int leadingWhitespaceEnd = 0;
        int trailingWhitespaceStart = 0;
        float circleRadius = 0f;
        if (mEditor.shouldInitializeNonPrintable()) {
            float spaceWidth = mPaint.getSpaceWidth();
            float maxD = Math.min(mEditor.getRowHeight(), spaceWidth);
            maxD *= 0.25f;
            circleRadius = maxD / 2;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !mEditor.isWordwrap() && canvas.isHardwareAccelerated() && mEditor.isHardwareAcceleratedDrawAllowed()) {
            mRenderer.keepCurrentInDisplay(firstVis, mEditor.getLastVisibleRow());
        }
        float offset2 = mEditor.getOffsetX() - mEditor.measureTextRegionOffset();
        float offset3 = offset2 - mEditor.getDpUnit() * 15;

        // Step 1 - Draw background of rows
        for (int row = firstVis; row <= mEditor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
            Row rowInf = rowIterator.next();
            int line = rowInf.lineIndex;
            int columnCount = mEditor.getText().getColumnCount(line);
            if (lastPreparedLine != line) {
                mEditor.computeMatchedPositions(line, matchedPositions);
                mEditor.prepareLine(line);
                lastPreparedLine = line;
            }
            // Get visible region on the line
            float[] charPos = mEditor.findFirstVisibleChar(offset3, rowInf.startColumn, rowInf.endColumn, mEditor.getLineBuffer(), line);
            int firstVisibleChar = (int) charPos[0];
            float paintingOffset = charPos[1] - offset2;
            int lastVisibleChar = (int) mEditor.findFirstVisibleChar(offset2 + mEditor.getWidth() - offset3, firstVisibleChar + 1, rowInf.endColumn, mEditor.getLineBuffer(), line)[0];

            // Draw current line background
            if (line == currentLine && !mEditor.getCursorAnimator().isRunning()) {
                drawRowBackground(canvas, currentLineBgColor, row);
                postDrawCurrentLines.add(row);
            }

            // Draw matched text background
            if (matchedPositions.size() > 0) {
                for (int i = 0;i < matchedPositions.size();i++) {
                    var position = matchedPositions.get(i);
                    var start = IntPair.getFirst(position);
                    var end = IntPair.getSecond(position);
                    drawRowRegionBackground(canvas, paintingOffset, row, firstVisibleChar, lastVisibleChar, start, end, mEditor.getColorScheme().getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND), line);
                }
            }

            // Draw selected text background
            if (mCursor.isSelected() && line >= mCursor.getLeftLine() && line <= mCursor.getRightLine()) {
                int selectionStart = 0;
                int selectionEnd = columnCount;
                if (line == mCursor.getLeftLine()) {
                    selectionStart = mCursor.getLeftColumn();
                }
                if (line == mCursor.getRightLine()) {
                    selectionEnd = mCursor.getRightColumn();
                }
                if (mEditor.getText().getColumnCount(line) == 0 && line != mCursor.getRightLine()) {
                    mRect.top = mEditor.getRowTop(row) - mEditor.getOffsetY();
                    mRect.bottom = mEditor.getRowBottom(row) - mEditor.getOffsetY();
                    mRect.left = paintingOffset;
                    mRect.right = mRect.left + mPaint.getSpaceWidth() * 2;
                    mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND));
                    canvas.drawRoundRect(mRect, mEditor.getRowHeight() * 0.13f, mEditor.getRowHeight() * 0.13f, mPaint);
                } else {
                    drawRowRegionBackground(canvas, paintingOffset, row, firstVisibleChar, lastVisibleChar, selectionStart, selectionEnd, mEditor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND), line);
                }
            }
        }
        rowIterator.reset();

        // Draw current line background on animation
        if (mEditor.getCursorAnimator().isRunning()) {
            mRect.bottom = (float) mEditor.getCursorAnimator().animatorBgBottom.getAnimatedValue() - mEditor.getOffsetY();
            mRect.top = mRect.bottom - (float) mEditor.getCursorAnimator().animatorBackground.getAnimatedValue();
            mRect.left = 0;
            mRect.right = mViewRect.right;
            drawColor(canvas, currentLineBgColor, mRect);
        }

        // Step 2 - Draw text and text decorations
        long lastStyle = 0;
        for (int row = firstVis; row <= mEditor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
            Row rowInf = rowIterator.next();
            int line = rowInf.lineIndex;
            ContentLine contentLine = mEditor.getText().getLine(line);
            int columnCount = contentLine.length();
            if (row == firstVis && requiredFirstLn != null) {
                requiredFirstLn.value = line;
            } else if (rowInf.isLeadingRow) {
                postDrawLineNumbers.add(IntPair.pack(line, row));
            }

            // Prepare data
            if (lastPreparedLine != line) {
                lastPreparedLine = line;
                mEditor.prepareLine(line);
                spanOffset = 0;
                if (mEditor.shouldInitializeNonPrintable()) {
                    long positions = mEditor.findLeadingAndTrailingWhitespacePos(mEditor.getLineBuffer());
                    leadingWhitespaceEnd = IntPair.getFirst(positions);
                    trailingWhitespaceStart = IntPair.getSecond(positions);
                }
            }

            // Get visible region on the line
            float[] charPos = mEditor.findFirstVisibleChar(offset3, rowInf.startColumn, rowInf.endColumn, mEditor.getLineBuffer(), line);
            int firstVisibleChar = (int) charPos[0];
            float paintingOffset = charPos[1] - offset2;
            int lastVisibleChar = (int) mEditor.findFirstVisibleChar(offset2 + mEditor.getWidth() - offset3, firstVisibleChar + 1, rowInf.endColumn, mEditor.getLineBuffer(), line)[0];

            float backupOffset = paintingOffset;

            // Draw text here
            if (!mEditor.isHardwareAcceleratedDrawAllowed() || !canvas.isHardwareAccelerated() || mEditor.isWordwrap() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (rowInf.endColumn - rowInf.startColumn > 256 && !mEditor.getProps().cacheRenderNodeForLongLines) /* Save memory */) {
                // Draw without hardware acceleration
                // Get spans
                var reader = spans == null ? new EmptyReader() : spans.read();
                try {
                    reader.moveToLine(line);
                } catch (Exception e) {
                    reader = new EmptyReader();
                }
                // Seek for first span
                float phi = 0f;
                while (spanOffset + 1 < reader.getSpanCount()) {
                    if (reader.getSpanAt(spanOffset + 1).column <= firstVisibleChar) {
                        // Update phi
                        Span span = reader.getSpanAt(spanOffset);
                        if (span.problemFlags > 0 && Integer.highestOneBit(span.problemFlags) != Span.FLAG_DEPRECATED) {
                            float lineWidth;
                            int spanEnd = Math.min(rowInf.endColumn, reader.getSpanAt(spanOffset + 1).column);
                            if (mEditor.isWordwrap()) {
                                lineWidth = mEditor.measureText(mEditor.getLineBuffer(), Math.max(firstVisibleChar, span.column), spanEnd - Math.max(firstVisibleChar, span.column), line) + phi;
                            } else {
                                lineWidth = mEditor.measureText(mEditor.getLineBuffer(), span.column, spanEnd - span.column, line) + phi;
                            }
                            int waveCount = (int) Math.ceil(lineWidth / waveLength);
                            phi = waveLength - (waveCount * waveLength - lineWidth);
                        } else {
                            phi = 0f;
                        }
                        spanOffset++;
                    } else {
                        break;
                    }
                }
                Span span = reader.getSpanAt(spanOffset);
                // Draw by spans
                while (lastVisibleChar > span.column) {
                    int spanEnd = spanOffset + 1 >= reader.getSpanCount() ? columnCount : reader.getSpanAt(spanOffset + 1).column;
                    spanEnd = Math.min(columnCount, spanEnd);
                    int paintStart = Math.max(firstVisibleChar, span.column);
                    if (paintStart >= columnCount) {
                        break;
                    }
                    int paintEnd = Math.min(lastVisibleChar, spanEnd);
                    if (paintStart > paintEnd) {
                        break;
                    }
                    float width = mEditor.measureText(mEditor.getLineBuffer(), paintStart, paintEnd - paintStart, line);
                    ExternalRenderer renderer = span.renderer;

                    // Invoke external renderer preDraw
                    if (renderer != null && renderer.requirePreDraw()) {
                        int saveCount = canvas.save();
                        canvas.translate(paintingOffset, mEditor.getRowTop(row) - mEditor.getOffsetY());
                        canvas.clipRect(0f, 0f, width, mEditor.getRowHeight());
                        try {
                            renderer.draw(canvas, mPaint, mEditor.getColorScheme(), true);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error while invoking external renderer", e);
                        }
                        canvas.restoreToCount(saveCount);
                    }

                    // Apply font style
                    long styleBits = span.getStyleBits();
                    if (span.getStyleBits() != lastStyle) {
                        mPaint.setFakeBoldText(TextStyle.isBold(styleBits));
                        if (TextStyle.isItalics(styleBits)) {
                            mPaint.setTextSkewX(-0.2f);
                        } else {
                            mPaint.setTextSkewX(0);
                        }
                        lastStyle = styleBits;
                    }

                    int backgroundColorId = span.getBackgroundColorId();
                    if (backgroundColorId != 0) {
                        if (paintStart != paintEnd) {
                            mRect.top = mEditor.getRowTop(row) - mEditor.getOffsetY();
                            mRect.bottom = mEditor.getRowBottom(row) - mEditor.getOffsetY();
                            mRect.left = paintingOffset;
                            mRect.right = mRect.left + width;
                            mPaint.setColor(mEditor.getColorScheme().getColor(backgroundColorId));
                            canvas.drawRoundRect(mRect, mEditor.getRowHeight() * 0.13f, mEditor.getRowHeight() * 0.13f, mPaint);
                        }
                    }

                    // Draw text
                    drawRegionText(canvas, paintingOffset, mEditor.getRowBaseline(row) - mEditor.getOffsetY(), line, paintStart, paintEnd, span.column, spanEnd, false, columnCount, mEditor.getColorScheme().getColor(span.getForegroundColorId()));

                    // Draw strikethrough
                    if ((span.problemFlags & Span.FLAG_DEPRECATED) != 0 || TextStyle.isStrikeThrough(styleBits)) {
                        mPaintOther.setColor(Color.BLACK);
                        canvas.drawLine(paintingOffset, mEditor.getRowTop(row) + mEditor.getRowHeight() / 2f - mEditor.getOffsetY(),
                                paintingOffset + width, mEditor.getRowTop(row) + mEditor.getRowHeight() / 2f - mEditor.getOffsetY(), mPaintOther);
                    }

                    // Draw underline
                    if (span.underlineColor != 0) {
                        mRect.bottom = mEditor.getRowBottom(row) - mEditor.getOffsetY() - mEditor.getDpUnit() * 1;
                        mRect.top = mRect.bottom - mEditor.getRowHeight() * 0.08f;
                        mRect.left = paintingOffset;
                        mRect.right = paintingOffset + width;
                        drawColor(canvas, span.underlineColor, mRect);
                    }

                    // Draw issue curly underline
                    if (waveWidth > 0 && span.problemFlags > 0 && Integer.highestOneBit(span.problemFlags) != Span.FLAG_DEPRECATED) {
                        int color = 0;
                        switch (Integer.highestOneBit(span.problemFlags)) {
                            case Span.FLAG_ERROR:
                                color = mEditor.getColorScheme().getColor(EditorColorScheme.PROBLEM_ERROR);
                                break;
                            case Span.FLAG_WARNING:
                                color = mEditor.getColorScheme().getColor(EditorColorScheme.PROBLEM_WARNING);
                                break;
                            case Span.FLAG_TYPO:
                                color = mEditor.getColorScheme().getColor(EditorColorScheme.PROBLEM_TYPO);
                                break;
                        }
                        if (color != 0 && span.column >= 0 && spanEnd - span.column >= 0) {
                            // Start and end X offset
                            float startOffset;
                            float lineWidth;
                            if (mEditor.isWordwrap()) {
                                startOffset = mEditor.measureTextRegionOffset() + mEditor.measureText(mEditor.getLineBuffer(), firstVisibleChar, Math.max(0, span.column - firstVisibleChar), line) - mEditor.getOffsetX();
                                lineWidth = mEditor.measureText(mEditor.getLineBuffer(), Math.max(firstVisibleChar, span.column), spanEnd - Math.max(firstVisibleChar, span.column), line) + phi;
                            } else {
                                startOffset = mEditor.measureTextRegionOffset() + mEditor.measureText(mEditor.getLineBuffer(), 0, span.column, line) - mEditor.getOffsetX();
                                lineWidth = mEditor.measureText(mEditor.getLineBuffer(), span.column, spanEnd - span.column, line) + phi;
                            }
                            float centerY = mEditor.getRowBottom(row) - mEditor.getOffsetY();
                            // Clip region due not to draw outside the horizontal region
                            canvas.save();
                            canvas.clipRect(paintingOffset, 0, paintingOffset + width, canvas.getHeight());
                            canvas.translate(startOffset - phi, centerY);
                            // Draw waves
                            mPath.reset();
                            mPath.moveTo(0, 0);
                            int waveCount = (int) Math.ceil(lineWidth / waveLength);
                            for (int i = 0; i < waveCount; i++) {
                                mPath.quadTo(waveLength * i + waveLength / 4, amplitude, waveLength * i + waveLength / 2, 0);
                                mPath.quadTo(waveLength * i + waveLength * 3 / 4, -amplitude, waveLength * i + waveLength, 0);
                            }
                            phi = waveLength - (waveCount * waveLength - lineWidth);
                            // Draw path
                            mPaintOther.setStrokeWidth(waveWidth);
                            mPaintOther.setStyle(Paint.Style.STROKE);
                            mPaintOther.setColor(color);
                            canvas.drawPath(mPath, mPaintOther);
                            canvas.restore();
                        }
                        mPaintOther.setStyle(Paint.Style.FILL);
                    } else {
                        phi = 0f;
                    }

                    // Invoke external renderer postDraw
                    if (renderer != null && renderer.requirePostDraw()) {
                        int saveCount = canvas.save();
                        canvas.translate(paintingOffset, mEditor.getRowTop(row) - mEditor.getOffsetY());
                        canvas.clipRect(0f, 0f, width, mEditor.getRowHeight());
                        try {
                            renderer.draw(canvas, mPaint, mEditor.getColorScheme(), false);
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error while invoking external renderer", e);
                        }
                        canvas.restoreToCount(saveCount);
                    }

                    paintingOffset += width;

                    if (paintEnd == lastVisibleChar) {
                        break;
                    }
                    spanOffset++;
                    if (spanOffset < reader.getSpanCount()) {
                        span = reader.getSpanAt(spanOffset);
                    } else {
                        spanOffset--;
                        //break;
                    }
                }
                try {
                    reader.moveToLine(-1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                paintingOffset = offset + mRenderer.drawLineHardwareAccelerated(canvas, line, offset) - mEditor.getDpUnit() * 20;
                lastVisibleChar = columnCount;
            }

            int nonPrintableFlags = mEditor.getNonPrintablePaintingFlags();
            // Draw hard wrap
            if (lastVisibleChar == columnCount && (nonPrintableFlags & FLAG_DRAW_LINE_SEPARATOR) != 0) {
                drawMiniGraph(canvas, paintingOffset, row, "\u21B5");
            }

            // Recover the offset
            paintingOffset = backupOffset;

            // Draw non-printable characters
            if (circleRadius != 0f && (leadingWhitespaceEnd != columnCount || (nonPrintableFlags & FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE) != 0)) {
                if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_LEADING) != 0) {
                    drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, 0, leadingWhitespaceEnd);
                }
                if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_INNER) != 0) {
                    drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, leadingWhitespaceEnd, trailingWhitespaceStart);
                }
                if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_TRAILING) != 0) {
                    drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, trailingWhitespaceStart, columnCount);
                }
                if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_IN_SELECTION) != 0 && mCursor.isSelected() && line >= mCursor.getLeftLine() && line <= mCursor.getRightLine()) {
                    int selectionStart = 0;
                    int selectionEnd = columnCount;
                    if (line == mCursor.getLeftLine()) {
                        selectionStart = mCursor.getLeftColumn();
                    }
                    if (line == mCursor.getRightLine()) {
                        selectionEnd = mCursor.getRightColumn();
                    }
                    if ((nonPrintableFlags & 0b1110) == 0) {
                        drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, selectionStart, selectionEnd);
                    } else {
                        if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_LEADING) == 0) {
                            drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, selectionStart, Math.min(leadingWhitespaceEnd, selectionEnd));
                        }
                        if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_INNER) == 0) {
                            drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, Math.max(leadingWhitespaceEnd, selectionStart), Math.min(trailingWhitespaceStart, selectionEnd));
                        }
                        if ((nonPrintableFlags & FLAG_DRAW_WHITESPACE_TRAILING) == 0) {
                            drawWhitespaces(canvas, paintingOffset, line, row, firstVisibleChar, lastVisibleChar, Math.max(trailingWhitespaceStart, selectionStart), selectionEnd);
                        }
                    }
                }
            }

            // Draw composing text underline
            if (line == mEditor.mConnection.mComposingLine) {
                int composingStart = mEditor.mConnection.mComposingStart;
                int composingEnd = mEditor.mConnection.mComposingEnd;
                int paintStart = Math.min(Math.max(composingStart, firstVisibleChar), lastVisibleChar);
                int paintEnd = Math.min(Math.max(composingEnd, firstVisibleChar), lastVisibleChar);
                if (paintStart != paintEnd) {
                    mRect.top = mEditor.getRowBottom(row) - mEditor.getOffsetY();
                    mRect.bottom = mRect.top + mEditor.getRowHeight() * 0.06f;
                    mRect.left = paintingOffset + mEditor.measureText(mEditor.getLineBuffer(), firstVisibleChar, paintStart - firstVisibleChar, line);
                    mRect.right = mRect.left + mEditor.measureText(mEditor.getLineBuffer(), paintStart, paintEnd - paintStart, line);
                    drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.UNDERLINE), mRect);
                }
            }

            // Draw cursors
            if (mCursor.isSelected()) {
                if (mCursor.getLeftLine() == line && isInside(mCursor.getLeftColumn(), firstVisibleChar, lastVisibleChar, line)) {
                    float centerX = paintingOffset + mEditor.measureText(mEditor.getLineBuffer(), firstVisibleChar, mCursor.getLeftColumn() - firstVisibleChar, line);
                    postDrawCursor.add(new DrawCursorTask(centerX, mEditor.getRowBottom(row) - mEditor.getOffsetY(), SelectionHandleStyle.HANDLE_TYPE_LEFT, mEditor.getLeftHandleDescriptor()));
                }
                if (mCursor.getRightLine() == line && isInside(mCursor.getRightColumn(), firstVisibleChar, lastVisibleChar, line)) {
                    float centerX = paintingOffset + mEditor.measureText(mEditor.getLineBuffer(), firstVisibleChar, mCursor.getRightColumn() - firstVisibleChar, line);
                    postDrawCursor.add(new DrawCursorTask(centerX, mEditor.getRowBottom(row) - mEditor.getOffsetY(), SelectionHandleStyle.HANDLE_TYPE_RIGHT, mEditor.getRightHandleDescriptor()));
                }
            } else if (mCursor.getLeftLine() == line && isInside(mCursor.getLeftColumn(), firstVisibleChar, lastVisibleChar, line)) {
                float centerX = paintingOffset + mEditor.measureText(mEditor.getLineBuffer(), firstVisibleChar, mCursor.getLeftColumn() - firstVisibleChar, line);
                postDrawCursor.add(new DrawCursorTask(centerX, mEditor.getRowBottom(row) - mEditor.getOffsetY(), mEditor.getEventHandler().shouldDrawInsertHandle() ? SelectionHandleStyle.HANDLE_TYPE_INSERT : SelectionHandleStyle.HANDLE_TYPE_UNDEFINED, mEditor.getInsertHandleDescriptor()));
            }
        }

        mPaint.setFakeBoldText(false);
        mPaint.setTextSkewX(0);
        mPaintOther.setStrokeWidth(circleRadius * 2);
        mDrawPoints.commitPoints(canvas, mPaintOther);
    }

    /**
     * Draw non-printable characters
     */
    protected void drawWhitespaces(Canvas canvas, float offset, int line, int row, int rowStart, int rowEnd, int min, int max) {
        int paintStart = Math.max(rowStart, Math.min(rowEnd, min));
        int paintEnd = Math.max(rowStart, Math.min(rowEnd, max));
        mPaintOther.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.NON_PRINTABLE_CHAR));

        if (paintStart < paintEnd) {
            float spaceWidth = mPaint.getSpaceWidth();
            float rowCenter = (mEditor.getRowTop(row) + mEditor.getRowBottom(row)) / 2f - mEditor.getOffsetY();
            offset += mEditor.measureText(mEditor.getLineBuffer(), rowStart, paintStart - rowStart, line);
            var chars = mEditor.getLineBuffer().value;
            var lastPos = paintStart;
            while (paintStart < paintEnd) {
                char ch = chars[paintStart];
                int paintCount = 0;
                boolean paintLine = false;
                if (ch == ' ' || ch == '\t') {
                    offset += mEditor.measureText(mEditor.getLineBuffer(), lastPos, paintStart - lastPos, line);
                }
                if (ch == ' ') {
                    paintCount = 1;
                } else if (ch == '\t') {
                    if ((mEditor.getNonPrintablePaintingFlags() & FLAG_DRAW_TAB_SAME_AS_SPACE) != 0) {
                        paintCount = mEditor.getTabWidth();
                    } else {
                        paintLine = true;
                    }
                }
                for (int i = 0; i < paintCount; i++) {
                    float charStartOffset = offset + spaceWidth * i;
                    float charEndOffset = charStartOffset + spaceWidth;
                    float centerOffset = (charStartOffset + charEndOffset) / 2f;
                    mDrawPoints.drawPoint(centerOffset, rowCenter);
                }
                if (paintLine) {
                    var charWidth = mEditor.getTabWidth() * spaceWidth;
                    float delta = charWidth * 0.05f;
                    canvas.drawLine(offset + delta, rowCenter, offset + charWidth - delta, rowCenter, mPaintOther);
                }

                if (ch == ' ' || ch == '\t') {
                    offset += (ch == ' ' ? spaceWidth : spaceWidth * mEditor.getTabWidth());
                    lastPos = paintStart + 1;
                }
                paintStart++;
            }
        }
    }

    /**
     * Draw small characters as graph
     */
    protected void drawMiniGraph(Canvas canvas, float offset, int row, String graph) {
        // Draw
        mPaintGraph.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.NON_PRINTABLE_CHAR));
        float baseline = mEditor.getRowBottom(row) - mEditor.getOffsetY() - mGraphMetrics.descent;
        canvas.drawText(graph, 0, graph.length(), offset, baseline, mPaintGraph);
    }

    /**
     * Draw background of a text region
     *
     * @param canvas         Canvas to draw
     * @param paintingOffset Paint offset x on canvas
     * @param row            The row index
     * @param firstVis       First visible character
     * @param lastVis        Last visible character
     * @param highlightStart Region start
     * @param highlightEnd   Region end
     * @param color          Color of background
     */
    protected void drawRowRegionBackground(Canvas canvas, float paintingOffset, int row, int firstVis, int lastVis, int highlightStart, int highlightEnd, int color, int line) {
        int paintStart = Math.min(Math.max(firstVis, highlightStart), lastVis);
        int paintEnd = Math.min(Math.max(firstVis, highlightEnd), lastVis);
        if (paintStart != paintEnd) {
            mRect.top = mEditor.getRowTop(row) - mEditor.getOffsetY();
            mRect.bottom = mEditor.getRowBottom(row) - mEditor.getOffsetY();
            mRect.left = paintingOffset + mEditor.measureText(mEditor.getLineBuffer(), firstVis, paintStart - firstVis, line);
            mRect.right = mRect.left + mEditor.measureText(mEditor.getLineBuffer(), paintStart, paintEnd - paintStart, line);
            mPaint.setColor(color);
            canvas.drawRoundRect(mRect, mEditor.getRowHeight() * 0.13f, mEditor.getRowHeight() * 0.13f, mPaint);
        }
    }

    /**
     * Draw text region with highlighting selected text
     *
     * @param canvas      Canvas to draw
     * @param offsetX     Start paint offset x on canvas
     * @param baseline    Baseline on canvas
     * @param line        Drawing line index
     * @param startIndex  Start index to paint
     * @param endIndex    Index of end character to paint
     * @param columnCount Column count of line
     * @param color       Color of normal text in this region
     */
    protected void drawRegionText(Canvas canvas, float offsetX, float baseline, int line, int startIndex, int endIndex, int contextStart, int contextEnd, boolean isRtl, int columnCount, int color) {
        boolean hasSelectionOnLine = mCursor.isSelected() && line >= mCursor.getLeftLine() && line <= mCursor.getRightLine();
        int selectionStart = 0;
        int selectionEnd = columnCount;
        int contextCount = contextEnd - contextStart;
        if (line == mCursor.getLeftLine()) {
            selectionStart = mCursor.getLeftColumn();
        }
        if (line == mCursor.getRightLine()) {
            selectionEnd = mCursor.getRightColumn();
        }
        mPaint.setColor(color);
        if (hasSelectionOnLine && mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED) != 0) {
            if (endIndex <= selectionStart || startIndex >= selectionEnd) {
                drawText(canvas, mEditor.getLineBuffer(), startIndex, endIndex - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
            } else {
                if (startIndex <= selectionStart) {
                    if (endIndex >= selectionEnd) {
                        //Three regions
                        //startIndex - selectionStart
                        drawText(canvas, mEditor.getLineBuffer(), startIndex, selectionStart - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                        float deltaX = mEditor.measureText(mEditor.getLineBuffer(), startIndex, selectionStart - startIndex, line);
                        //selectionStart - selectionEnd
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mEditor.getLineBuffer(), selectionStart, selectionEnd - selectionStart, contextStart, contextCount, isRtl, offsetX + deltaX, baseline, line);
                        deltaX += mEditor.measureText(mEditor.getLineBuffer(), selectionStart, selectionEnd - selectionStart, line);
                        //selectionEnd - endIndex
                        mPaint.setColor(color);
                        drawText(canvas, mEditor.getLineBuffer(), selectionEnd, endIndex - selectionEnd, contextStart, contextCount, isRtl, offsetX + deltaX, baseline, line);
                    } else {
                        //Two regions
                        //startIndex - selectionStart
                        drawText(canvas, mEditor.getLineBuffer(), startIndex, selectionStart - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                        //selectionStart - endIndex
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mEditor.getLineBuffer(), selectionStart, endIndex - selectionStart, contextStart, contextCount, isRtl, offsetX + mEditor.measureText(mEditor.getLineBuffer(), startIndex, selectionStart - startIndex, line), baseline, line);
                    }
                } else {
                    //selectionEnd > startIndex > selectionStart
                    if (endIndex > selectionEnd) {
                        //Two regions
                        //selectionEnd - endIndex
                        drawText(canvas, mEditor.getLineBuffer(), selectionEnd, endIndex - selectionEnd, contextStart, contextCount, isRtl, offsetX + mEditor.measureText(mEditor.getLineBuffer(), startIndex, selectionEnd - startIndex, line), baseline, line);
                        //startIndex - selectionEnd
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mEditor.getLineBuffer(), startIndex, selectionEnd - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                    } else {
                        //One region
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mEditor.getLineBuffer(), startIndex, endIndex - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                    }
                }
            }
        } else {
            drawText(canvas, mEditor.getLineBuffer(), startIndex, endIndex - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
        }
    }

    protected void drawRegionTextDirectional(Canvas canvas, AndroidBidi.Directions dirs, float offsetX, float baseline, int line, int startIndex, int endIndex, int contextStart, int contextEnd, int columnCount, int color) {
        // TODO
        /* float accumulatedWidth = 0f;
        for (int i = 0; i < dirs.getRunCount(); i++) {
            int paintStart = Math.max(dirs.getRunStart(i), startIndex);
            int paintEnd = Math.min(dirs.getRunStart(i) + dirs.getRunLength(i), endIndex);
            if (startIndex < endIndex) {
                drawRegionText(canvas, offsetX + accumulatedWidth, baseline, line, paintStart, paintEnd, contextStart, contextEnd, dirs.isRunRtl(i), columnCount, color);
                accumulatedWidth += measureText(mEditor.getLineBuffer(), paintStart, paintEnd, line);
            }
        }*/
        drawRegionText(canvas, offsetX, baseline, line, startIndex, endIndex, contextStart, contextEnd, false, columnCount, color);
    }

    /**
     * Draw text on the given position
     *
     * @param canvas Canvas to draw
     * @param line   Source of characters
     * @param index  The index in array
     * @param count  Count of characters
     * @param offX   Offset x for paint
     * @param offY   Offset y for paint(baseline)
     */
    @SuppressLint("NewApi")
    protected void drawText(Canvas canvas, ContentLine line, int index, int count, int contextStart, int contextCount, boolean isRtl, float offX, float offY, int lineNumber) {
        int end = index + count;
        var src = line.value;
        int st = index;
        for (int i = index; i < end; i++) {
            if (src[i] == '\t') {
                //canvas.drawText(src, st, i - st, offX, offY, mPaint);
                canvas.drawTextRun(src, st, i - st, contextStart, contextCount, offX, offY, isRtl, mPaint);
                offX = offX + mEditor.measureText(line, st, i - st + 1, lineNumber);
                st = i + 1;
            }
        }
        if (st < end) {
            canvas.drawTextRun(src, st, end - st, contextStart, contextCount, offX, offY, isRtl, mPaint);
            //canvas.drawText(src, st, end - st, offX, offY, mPaint);
        }
    }

    /**
     * Is inside the region
     *
     * @param index Index to test
     * @param start Start of region
     * @param end   End of region
     * @param line  Checking line
     * @return true if cursor should be drawn in this row
     */
    private boolean isInside(int index, int start, int end, int line) {
        // Due not to draw duplicate cursors for a single one
        if (index == end && mEditor.getText().getLine(line).length() != end) {
            return false;
        }
        return index >= start && index <= end;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    class DrawCursorTask {

        float x;
        float y;
        int handleType;
        SelectionHandleStyle.HandleDescriptor descriptor;

        public DrawCursorTask(float x, float y, int handleType, SelectionHandleStyle.HandleDescriptor descriptor) {
            this.x = x;
            this.y = y;
            this.handleType = handleType;
            this.descriptor = descriptor;
        }

        void execute(Canvas canvas) {
            // Hide cursors (API level 31)
            if (mEditor.mConnection.mImeConsumingInput) {
                return;
            }
            // Follow the thumb
            if (!descriptor.position.isEmpty()) {
                if ((mEditor.getEventHandler().holdInsertHandle() && handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT)
                        || (mEditor.getEventHandler().mSelHandleType == EditorTouchEventHandler.SelectionHandle.LEFT && handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT)
                        || (mEditor.getEventHandler().mSelHandleType == EditorTouchEventHandler.SelectionHandle.RIGHT && handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)) {
                    x = mEditor.getEventHandler().mMotionX + (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER ? descriptor.position.width() : 0) * (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT ? 1 : -1);
                    y = mEditor.getEventHandler().mMotionY - descriptor.position.height() * 2 / 3f;
                }
            }

            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED || mEditor.getCursorBlink().visibility || mEditor.getEventHandler().holdInsertHandle()) {
                mRect.top = y - mEditor.getRowHeight();
                mRect.bottom = y;
                mRect.left = x - mEditor.getInsertSelectionWidth() / 2f;
                mRect.right = x + mEditor.getInsertSelectionWidth() / 2f;
                drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.SELECTION_INSERT), mRect);
            }
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                mEditor.getHandleStyle().draw(canvas, handleType, x, y, mEditor.getRowHeight(), mEditor.getColorScheme().getColor(EditorColorScheme.SELECTION_HANDLE), descriptor);
            } else if (descriptor != null) {
                descriptor.setEmpty();
            }
        }
    }

    /**
     * Draw effect of edges
     *
     * @param canvas The canvas to draw
     */
    protected void drawEdgeEffect(Canvas canvas) {
        boolean postDraw = false;
        MaterialEdgeEffect verticalEdgeEffect = mEditor.getVerticalEdgeEffect();
        MaterialEdgeEffect horizontalEdgeEffect = mEditor.getHorizontalEdgeEffect();
        if (!verticalEdgeEffect.isFinished()) {
            boolean bottom = mEditor.getEventHandler().topOrBottom;
            if (bottom) {
                canvas.save();
                canvas.translate(-mEditor.getMeasuredWidth(), mEditor.getMeasuredHeight());
                canvas.rotate(180, mEditor.getMeasuredWidth(), 0);
            }
            postDraw = verticalEdgeEffect.draw(canvas);
            if (bottom) {
                canvas.restore();
            }
        }
        if (mEditor.isWordwrap()) {
            horizontalEdgeEffect.finish();
        }
        if (!horizontalEdgeEffect.isFinished()) {
            canvas.save();
            boolean right = mEditor.getEventHandler().leftOrRight;
            if (right) {
                canvas.rotate(90);
                canvas.translate(0, -mEditor.getMeasuredWidth());
            } else {
                canvas.translate(0, mEditor.getMeasuredHeight());
                canvas.rotate(-90);
            }
            postDraw = horizontalEdgeEffect.draw(canvas) || postDraw;
            canvas.restore();
        }
        OverScroller scroller = mEditor.getScroller();
        if (scroller.isOverScrolled()) {
            if (verticalEdgeEffect.isFinished() && (scroller.getCurrY() < 0 || scroller.getCurrY() > mEditor.getScrollMaxY())) {
                mEditor.getEventHandler().topOrBottom = scroller.getCurrY() >= mEditor.getScrollMaxY();
                verticalEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
                postDraw = true;
            }
            if (horizontalEdgeEffect.isFinished() && (scroller.getCurrX() < 0 || scroller.getCurrX() > mEditor.getScrollMaxX())) {
                mEditor.getEventHandler().leftOrRight = scroller.getCurrX() >= mEditor.getScrollMaxX();
                horizontalEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
                postDraw = true;
            }
        }
        if (postDraw) {
            mEditor.postInvalidate();
        }
    }

    /**
     * Draw code block lines on screen
     *
     * @param canvas  The canvas to draw
     * @param offsetX The start x offset for text
     */
    protected void drawBlockLines(Canvas canvas, float offsetX) {
        List<CodeBlock> blocks = mEditor.getStyles() == null ? null : mEditor.getStyles().blocks;
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        int first = mEditor.getFirstVisibleRow();
        int last = mEditor.getLastVisibleRow();
        boolean mark = false;
        int invalidCount = 0;
        int maxCount = Integer.MAX_VALUE;
        if (mEditor.getStyles() != null) {
            maxCount = mEditor.getStyles().getSuppressSwitch();
        }
        int mm = mEditor.binarySearchEndBlock(first, blocks);
        int cursorIdx = mEditor.getCurrentCursorBlock();
        for (int curr = mm; curr < blocks.size(); curr++) {
            CodeBlock block = blocks.get(curr);
            if (CodeEditor.hasVisibleRegion(block.startLine, block.endLine, first, last)) {
                try {
                    var lineContent = mEditor.getText().getLine(block.endLine);
                    float offset1 = mEditor.measureText(lineContent, 0, Math.min(block.endColumn, lineContent.length()), block.endLine);
                    lineContent = mEditor.getText().getLine(block.startLine);
                    float offset2 = mEditor.measureText(lineContent, 0, Math.min(block.startColumn, lineContent.length()), block.startLine);
                    float offset = Math.min(offset1, offset2);
                    float centerX = offset + offsetX;
                    mRect.top = Math.max(0, mEditor.getRowBottom(block.startLine) - mEditor.getOffsetY());
                    mRect.bottom = Math.min(mEditor.getHeight(), (block.toBottomOfEndLine ? mEditor.getRowBottom(block.endLine) : mEditor.getRowTop(block.endLine)) - mEditor.getOffsetY());
                    mRect.left = centerX - mEditor.getDpUnit() * mEditor.getBlockLineWidth() / 2;
                    mRect.right = centerX + mEditor.getDpUnit() * mEditor.getBlockLineWidth() / 2;
                    drawColor(canvas, mEditor.getColorScheme().getColor(curr == cursorIdx ? EditorColorScheme.BLOCK_LINE_CURRENT : EditorColorScheme.BLOCK_LINE), mRect);
                } catch (IndexOutOfBoundsException e) {
                    //Ignored
                    //Because the exception usually occurs when the content changed.
                }
                mark = true;
            } else if (mark) {
                if (invalidCount >= maxCount)
                    break;
                invalidCount++;
            }
        }
    }

    protected void drawSideBlockLine(Canvas canvas) {
        if (!mEditor.getProps().drawSideBlockLine) {
            return;
        }
        List<CodeBlock> blocks = mEditor.getStyles() == null ? null : mEditor.getStyles().blocks;
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        var current = mEditor.getCurrentCursorBlock();
        if (current >= 0 && current < blocks.size()) {
            var block = blocks.get(current);
            var layout = mEditor.getLayout();
            try {
                float top = layout.getCharLayoutOffset(block.startLine, block.startColumn)[0] - mEditor.getRowHeight() - mEditor.getOffsetY();
                float bottom = layout.getCharLayoutOffset(block.endLine, block.endColumn)[0] - mEditor.getOffsetY();
                float left = mEditor.measureLineNumber();
                float right = left + mEditor.getDividerMargin();
                float center = (left + right) / 2 - mEditor.getOffsetX();
                mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.SIDE_BLOCK_LINE));
                mPaint.setStrokeWidth(mEditor.getDpUnit() / 2f);
                canvas.drawLine(center, top, center, bottom, mPaint);
            } catch (IndexOutOfBoundsException e) {
                //ignored
            }
        }
    }

    /**
     * Draw scroll bars and tracks
     *
     * @param canvas The canvas to draw
     */
    protected void drawScrollBars(Canvas canvas) {
        mVerticalScrollBar.setEmpty();
        mHorizontalScrollBar.setEmpty();
        if (!mEditor.getEventHandler().shouldDrawScrollBar()) {
            return;
        }
        if (mEditor.isVerticalScrollBarEnabled() && mEditor.getScrollMaxY() > mEditor.getHeight() / 2) {
            drawScrollBarTrackVertical(canvas);
            drawScrollBarVertical(canvas);
        }
        if (mEditor.isHorizontalScrollBarEnabled() && !mEditor.isWordwrap() && mEditor.getScrollMaxX() > mEditor.getWidth() * 3 / 4) {
            drawScrollBarTrackHorizontal(canvas);
            drawScrollBarHorizontal(canvas);
        }
    }

    /**
     * Draw vertical scroll bar track
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarTrackVertical(Canvas canvas) {
        if (mEditor.getEventHandler().holdVerticalScrollBar()) {
            mRect.right = mEditor.getWidth();
            mRect.left = mEditor.getWidth() - mEditor.getDpUnit() * 10;
            mRect.top = 0;
            mRect.bottom = mEditor.getHeight();
            drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.SCROLL_BAR_TRACK), mRect);
        }
    }

    /**
     * Draw vertical scroll bar
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarVertical(Canvas canvas) {
        int height = mEditor.getHeight();
        float all = mEditor.getLayout().getLayoutHeight() + height / 2f;
        float length = height / all * height;
        float topY;
        if (length < mEditor.getDpUnit() * 30) {
            length = mEditor.getDpUnit() * 30;
            topY = (mEditor.getOffsetY() + height / 2f) / all * (height - length);
        } else {
            topY = mEditor.getOffsetY() / all * height;
        }
        if (mEditor.getEventHandler().holdVerticalScrollBar()) {
            float centerY = topY + length / 2f;
            drawLineInfoPanel(canvas, centerY, mRect.left - mEditor.getDpUnit() * 5);
        }
        mRect.right = mEditor.getWidth();
        mRect.left = mEditor.getWidth() - mEditor.getDpUnit() * 10;
        mRect.top = topY;
        mRect.bottom = topY + length;
        mVerticalScrollBar.set(mRect);
        drawColor(canvas, mEditor.getColorScheme().getColor(mEditor.getEventHandler().holdVerticalScrollBar()
                ? EditorColorScheme.SCROLL_BAR_THUMB_PRESSED
                : EditorColorScheme.SCROLL_BAR_THUMB), mRect);
    }

    /**
     * Draw line number panel
     *
     * @param canvas  Canvas to draw
     * @param centerY The center y on screen for the panel
     * @param rightX  The right x on screen for the panel
     */
    protected void drawLineInfoPanel(Canvas canvas, float centerY, float rightX) {
        if (!mEditor.isDisplayLnPanel()) {
            return;
        }
        String text = mEditor.getLnTip() + (1 + mEditor.getFirstVisibleLine());
        float backupSize = mPaint.getTextSize();
        mPaint.setTextSize(mEditor.getLineInfoTextSize());
        Paint.FontMetricsInt backupMetrics = mTextMetrics;
        mTextMetrics = mPaint.getFontMetricsInt();
        float expand = mEditor.getDpUnit() * 3;
        float textWidth = mPaint.measureText(text);
        mRect.top = centerY - mEditor.getRowHeight() / 2f - expand;
        mRect.bottom = centerY + mEditor.getRowHeight() / 2f + expand;
        mRect.right = rightX;
        mRect.left = rightX - expand * 2 - textWidth;
        drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL), mRect);
        float baseline = centerY - mEditor.getRowHeight() / 2f + mEditor.getRowBaseline(0);
        float centerX = (mRect.left + mRect.right) / 2;
        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT));
        mPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, centerX, baseline, mPaint);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setTextSize(backupSize);
        mTextMetrics = backupMetrics;
    }

    /**
     * Draw horizontal scroll bar track
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarTrackHorizontal(Canvas canvas) {
        if (mEditor.getEventHandler().holdHorizontalScrollBar()) {
            mRect.top = mEditor.getHeight() - mEditor.getDpUnit() * 10;
            mRect.bottom = mEditor.getHeight();
            mRect.right = mEditor.getWidth();
            mRect.left = 0;
            drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.SCROLL_BAR_TRACK), mRect);
        }
    }

    protected void drawSelectionOnAnimation(Canvas canvas) {
        mRect.bottom = (float) mEditor.getCursorAnimator().animatorY.getAnimatedValue() - mEditor.getOffsetY();
        mRect.top = mRect.bottom - mEditor.getRowHeight();
        float centerX = (float) mEditor.getCursorAnimator().animatorX.getAnimatedValue() - mEditor.getOffsetX();
        mRect.left = centerX - mEditor.getInsertSelectionWidth() / 2;
        mRect.right = centerX + mEditor.getInsertSelectionWidth() / 2;
        drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.SELECTION_INSERT), mRect);
        if (mEditor.getEventHandler().shouldDrawInsertHandle()) {
            mEditor.getHandleStyle().draw(canvas, SelectionHandleStyle.HANDLE_TYPE_INSERT, centerX, mRect.bottom, mEditor.getRowHeight(), mEditor.getColorScheme().getColor(EditorColorScheme.SELECTION_HANDLE), mEditor.getInsertHandleDescriptor());
        }
    }

    /**
     * Draw horizontal scroll bar
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarHorizontal(Canvas canvas) {
        int page = mEditor.getWidth();
        float all = mEditor.getScrollMaxX();
        float length = page / (all + mEditor.getWidth()) * mEditor.getWidth();
        float leftX = mEditor.getOffsetX() / all * (mEditor.getWidth() - length);
        mRect.top = mEditor.getHeight() - mEditor.getDpUnit() * 10;
        mRect.bottom = mEditor.getHeight();
        mRect.right = leftX + length;
        mRect.left = leftX;
        mHorizontalScrollBar.set(mRect);
        drawColor(canvas, mEditor.getColorScheme().getColor(mEditor.getEventHandler().holdHorizontalScrollBar() ? EditorColorScheme.SCROLL_BAR_THUMB_PRESSED : EditorColorScheme.SCROLL_BAR_THUMB), mRect);
    }
}
