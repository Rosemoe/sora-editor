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

import static io.github.rosemoe.sora.graphics.GraphicCharacter.*;
import static io.github.rosemoe.sora.util.Numbers.stringSize;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_LINE_SEPARATOR;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_INNER;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_LEADING;
import static io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING;

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
import android.util.SparseArray;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.graphics.BufferedDrawPoints;
import io.github.rosemoe.sora.graphics.GraphicTextRow;
import io.github.rosemoe.sora.graphics.GraphicsConstants;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.EmptyReader;
import io.github.rosemoe.sora.lang.styling.ExternalRenderer;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Spans;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor;
import io.github.rosemoe.sora.lang.styling.line.LineAnchorStyle;
import io.github.rosemoe.sora.lang.styling.line.LineBackground;
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon;
import io.github.rosemoe.sora.lang.styling.line.LineStyles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.UnicodeIterator;
import io.github.rosemoe.sora.text.bidi.Directions;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.Numbers;
import io.github.rosemoe.sora.util.TemporaryCharBuffer;
import io.github.rosemoe.sora.widget.layout.Row;
import io.github.rosemoe.sora.widget.layout.RowIterator;
import io.github.rosemoe.sora.widget.layout.WordwrapLayout;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;

public class EditorRenderer {

    private static final String LOG_TAG = "EditorPainter";
    private final static int[] sDiagnosticsColorMapping = {
            0,
            EditorColorScheme.PROBLEM_TYPO,
            EditorColorScheme.PROBLEM_WARNING,
            EditorColorScheme.PROBLEM_ERROR
    };
    private final BufferedDrawPoints mDrawPoints;
    private final Paint mPaint;
    private final Paint mPaintOther;
    private final Rect mViewRect;
    private final RectF mRect;
    private final Path mPath;
    private final Paint mPaintGraph;
    private final RectF mVerticalScrollBar;
    private final RectF mHorizontalScrollBar;
    private final LongArrayList mPostDrawLineNumbers = new LongArrayList();
    private final LongArrayList mPostDrawCurrentLines = new LongArrayList();
    private final LongArrayList mMatchedPositions = new LongArrayList();
    private final SparseArray<ContentLine> mPreloadedLines = new SparseArray<>();
    private final SparseArray<Directions> mPreloadedDirections = new SparseArray<>();
    private final CodeEditor mEditor;
    private final List<DiagnosticRegion> mCollectedDiagnostics = new ArrayList<>();
    Paint.FontMetricsInt mTextMetrics;
    private RenderNodeHolder mRenderNodeHolder;
    private long mTimestamp;
    private Paint.FontMetricsInt mLineNumberMetrics;
    private Paint.FontMetricsInt mGraphMetrics;
    private int mCachedGutterWidth;
    private Cursor mCursor;
    protected ContentLine mBuffer;
    protected Content mContent;
    private boolean mRendering;
    protected boolean basicDisplayMode;

    public EditorRenderer(@NonNull CodeEditor editor) {
        mEditor = editor;
        mVerticalScrollBar = new RectF();
        mHorizontalScrollBar = new RectF();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mRenderNodeHolder = new RenderNodeHolder(editor);
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

    public boolean isBasicDisplayMode() {
        return basicDisplayMode;
    }

    public void notifyFullTextUpdate() {
        mCursor = mEditor.getCursor();
        mContent = mEditor.getText();
    }

    public void draw(@NonNull Canvas canvas) {
        mRendering = true;
        try {
            drawView(canvas);
        } finally {
            mRendering = false;
        }
    }

    public void onSizeChanged(int width, int height) {
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

    void setCachedLineNumberWidth(int width) {
        mCachedGutterWidth = width;
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
        mPaintGraph.setTextSize(size * mEditor.getProps().miniMarkerSizeFactor);
        mTextMetrics = mPaint.getFontMetricsInt();
        mLineNumberMetrics = mPaintOther.getFontMetricsInt();
        mGraphMetrics = mPaintGraph.getFontMetricsInt();
        invalidateRenderNodes();
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
        invalidateRenderNodes();
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

    public void setTextScaleX(float textScaleX) {
        mPaint.setTextScaleX(textScaleX);
        mPaintOther.setTextScaleX(textScaleX);
        onTextStyleUpdate();
    }

    public void setLetterSpacing(float letterSpacing) {
        mPaint.setLetterSpacing(letterSpacing);
        mPaintOther.setLetterSpacing(letterSpacing);
        onTextStyleUpdate();
    }

    protected void onTextStyleUpdate() {
        mLineNumberMetrics = mPaintOther.getFontMetricsInt();
        mTextMetrics = mPaint.getFontMetricsInt();
        invalidateRenderNodes();
        updateTimestamp();
        mEditor.createLayout();
        mEditor.invalidate();
    }

    /**
     * Update timestamp required for measuring cache
     */
    protected void updateTimestamp() {
        mTimestamp = System.nanoTime();
    }

    protected void prepareLine(int line) {
        mBuffer = getLine(line);
    }

    ContentLine getLine(int line) {
        if (!mRendering) {
            return getLineDirect(line);
        }
        var line2 = mPreloadedLines.get(line);
        if (line2 == null) {
            line2 = mContent.getLine(line);
            mPreloadedLines.put(line, line2);
        }
        return line2;
    }

    Directions getLineDirections(int line) {
        if (!mRendering) {
            return mContent.getLineDirections(line);
        }
        var line2 = mPreloadedDirections.get(line);
        if (line2 == null) {
            line2 = mContent.getLineDirections(line);
            mPreloadedDirections.put(line, line2);
        }
        return line2;
    }

    ContentLine getLineDirect(int line) {
        return mContent.getLine(line);
    }

    int getColumnCount(int line) {
        return getLine(line).length();
    }

    /**
     * Invalidate the whole hardware-accelerated renderer
     */
    public void invalidateRenderNodes() {
        if (mRenderNodeHolder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mRenderNodeHolder.invalidate();
        }
    }

    /**
     * Invalidate the region in hardware-accelerated renderer
     */
    public void invalidateChanged(int startLine) {
        if (mRenderNodeHolder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mCursor != null) {
            if (mRenderNodeHolder.invalidateInRegion(startLine, Integer.MAX_VALUE)) {
                mEditor.invalidate();
            }
        }
    }

    /**
     * Invalidate the cursor region in hardware-accelerated renderer
     */
    public void invalidateInCursor() {
        invalidateChanged(mCursor.getLeftLine());
    }

    // draw methods

    @RequiresApi(29)
    protected void updateLineDisplayList(RenderNode renderNode, int line, Spans.Reader spans) {
        prepareLine(line);
        int columnCount = getColumnCount(line);
        float widthLine = measureText(mBuffer, line, 0, columnCount) + mEditor.getDpUnit() * 20;
        renderNode.setPosition(0, 0, (int) (widthLine + mPaintGraph.measureText("↵") * 1.5f), mEditor.getRowHeight());
        Canvas canvas = renderNode.beginRecording();
        if (spans == null) {
            spans = new EmptyReader();
        }
        int spanOffset = 0;
        float paintingOffset = 0;
        int row = 0;
        Span span = spans.getSpanAt(spanOffset);
        // Draw by spans
        long lastStyle = 0;
        while (columnCount > span.column) {
            int spanEnd = spanOffset + 1 >= spans.getSpanCount() ? columnCount : spans.getSpanAt(spanOffset + 1).column;
            spanEnd = Math.min(columnCount, spanEnd);
            int paintStart = span.column;
            int paintEnd = Math.min(columnCount, spanEnd);
            float width = measureText(mBuffer, line, paintStart, paintEnd - paintStart);
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
                    mPaint.setTextSkewX(GraphicsConstants.TEXT_SKEW_X);
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
                    canvas.drawRoundRect(mRect, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mPaint);
                }
            }


            // Draw text
            drawRegionTextDirectional(canvas, paintingOffset, mEditor.getRowBaseline(row), line, paintStart, paintEnd, span.column, spanEnd, columnCount, mEditor.getColorScheme().getColor(span.getForegroundColorId()));

            // Draw strikethrough
            if (TextStyle.isStrikeThrough(span.style)) {
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

        int nonPrintableFlags = mEditor.getNonPrintablePaintingFlags();
        // Draw hard wrap
        if ((nonPrintableFlags & FLAG_DRAW_LINE_SEPARATOR) != 0) {
            drawMiniGraph(canvas, paintingOffset, -1, "↵");
        }
        renderNode.endRecording();
        mPaint.setTextSkewX(0);
        mPaint.setFakeBoldText(false);
    }

    public boolean hasSideHintIcons() {
        Styles styles;
        if ((styles = mEditor.getStyles()) != null) {
            if (styles.styleTypeCount != null) {
                var count = styles.styleTypeCount.get(LineSideIcon.class);
                if (count == null) {
                    return false;
                }
                return count.value > 0;
            }
        }
        return false;
    }

    /**
     * Paint the view on given Canvas
     *
     * @param canvas Canvas you want to draw
     */
    public void drawView(Canvas canvas) {
        mCursor.updateCache(mEditor.getFirstVisibleLine());

        EditorColorScheme color = mEditor.getColorScheme();
        drawColor(canvas, color.getColor(EditorColorScheme.WHOLE_BACKGROUND), mViewRect);

        float lineNumberWidth = mEditor.measureLineNumber();
        var sideIconWidth = hasSideHintIcons() ? mEditor.getRowHeight() : 0f;
        float offsetX = -mEditor.getOffsetX() + mEditor.measureTextRegionOffset();
        float textOffset = offsetX;

        var gutterWidth = (int) (lineNumberWidth + sideIconWidth + mEditor.getDividerWidth() + mEditor.getDividerMargin() * 2);
        if (mEditor.isWordwrap()) {
            if (mCachedGutterWidth == 0) {
                mCachedGutterWidth = gutterWidth;
            } else if (mCachedGutterWidth != gutterWidth && !mEditor.getEventHandler().isScaling) {
                mCachedGutterWidth = gutterWidth;
                mEditor.createLayout();
            }
        } else {
            mCachedGutterWidth = 0;
        }

        prepareLines(mEditor.getFirstVisibleLine(), mEditor.getLastVisibleLine());
        buildMeasureCacheForLines(mEditor.getFirstVisibleLine(), mEditor.getLastVisibleLine(), mTimestamp, true);

        if (mCursor.isSelected()) {
            mEditor.getInsertHandleDescriptor().setEmpty();
        } else {
            mEditor.getLeftHandleDescriptor().setEmpty();
            mEditor.getRightHandleDescriptor().setEmpty();
        }

        boolean lineNumberNotPinned = mEditor.isLineNumberEnabled() && (mEditor.isWordwrap() || !mEditor.isLineNumberPinned());

        var postDrawLineNumbers = mPostDrawLineNumbers;
        postDrawLineNumbers.clear();
        var postDrawCurrentLines = mPostDrawCurrentLines;
        postDrawCurrentLines.clear();
        List<DrawCursorTask> postDrawCursor = new ArrayList<>(3);
        MutableInt firstLn = mEditor.isFirstLineNumberAlwaysVisible() && mEditor.isWordwrap() ? new MutableInt(-1) : null;

        drawRows(canvas, textOffset, postDrawLineNumbers, postDrawCursor, postDrawCurrentLines, firstLn);
        patchHighlightedDelimiters(canvas, textOffset);
        drawDiagnosticIndicators(canvas, offsetX);

        offsetX = -mEditor.getOffsetX();

        if (lineNumberNotPinned) {
            drawLineNumberBackground(canvas, offsetX, lineNumberWidth + sideIconWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));
            int lineNumberColor = mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER);
            int currentLineBgColor = mEditor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
            if (mEditor.getCursorAnimator().isRunning()) {
                mRect.bottom = mEditor.getCursorAnimator().animatedLineBottom() - mEditor.getOffsetY();
                mRect.top = mRect.bottom - mEditor.getCursorAnimator().animatedLineHeight();
                mRect.left = 0;
                mRect.right = (int) (textOffset - mEditor.getDividerMargin());
                drawColor(canvas, currentLineBgColor, mRect);
            }
            for (int i = 0; i < postDrawCurrentLines.size(); i++) {
                drawRowBackground(canvas, currentLineBgColor, (int) postDrawCurrentLines.get(i), (int) (textOffset - mEditor.getDividerMargin()));
            }
            drawSideIcons(canvas, offsetX + lineNumberWidth);
            drawDivider(canvas, offsetX + lineNumberWidth + sideIconWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_DIVIDER));
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
                var text = Integer.toString(firstLn.value + 1);
                switch (mEditor.getLineNumberAlign()) {
                    case LEFT:
                        canvas.drawText(text, offsetX, y, mPaintOther);
                        break;
                    case RIGHT:
                        canvas.drawText(text, offsetX + lineNumberWidth, y, mPaintOther);
                        break;
                    case CENTER:
                        canvas.drawText(text, offsetX + (lineNumberWidth + mEditor.getDividerMargin()) / 2f, y, mPaintOther);
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
            drawLineNumberBackground(canvas, 0, lineNumberWidth + sideIconWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));
            int lineNumberColor = mEditor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER);
            int currentLineBgColor = mEditor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
            if (mEditor.getCursorAnimator().isRunning()) {
                mRect.bottom = mEditor.getCursorAnimator().animatedLineBottom() - mEditor.getOffsetY();
                mRect.top = mRect.bottom - mEditor.getCursorAnimator().animatedLineHeight();
                mRect.left = 0;
                mRect.right = (int) (textOffset - mEditor.getDividerMargin());
                drawColor(canvas, currentLineBgColor, mRect);
            }
            for (int i = 0; i < postDrawCurrentLines.size(); i++) {
                drawRowBackground(canvas, currentLineBgColor, (int) postDrawCurrentLines.get(i), (int) (textOffset - mEditor.getDividerMargin() + mEditor.getOffsetX()));
            }
            drawSideIcons(canvas, lineNumberWidth);
            drawDivider(canvas, lineNumberWidth + sideIconWidth + mEditor.getDividerMargin(), color.getColor(EditorColorScheme.LINE_DIVIDER));
            for (int i = 0; i < postDrawLineNumbers.size(); i++) {
                long packed = postDrawLineNumbers.get(i);
                drawLineNumber(canvas, IntPair.getFirst(packed), IntPair.getSecond(packed), 0, lineNumberWidth, lineNumberColor);
            }
        }

        drawScrollBars(canvas);
        drawEdgeEffect(canvas);

        mEditor.rememberDisplayedLines();
        releasePreloadedData();
        drawFormatTip(canvas);
    }

    protected void drawSideIcons(Canvas canvas, float offset) {
        var row = mEditor.getFirstVisibleRow();
        var itr = mEditor.getLayout().obtainRowIterator(row);
        final var iconSizeFactor = mEditor.getProps().sideIconSizeFactor;
        var size = (int) (mEditor.getRowHeight() * iconSizeFactor);
        var offsetToLeftTop = (int) (mEditor.getRowHeight() * (1 - iconSizeFactor) / 2f);
        while (row <= mEditor.getLastVisibleRow() && itr.hasNext()) {
            var rowInf = itr.next();
            if (rowInf.isLeadingRow) {
                var hint = getLineStyle(rowInf.lineIndex, LineSideIcon.class);
                if (hint != null) {
                    var drawable = hint.getDrawable();
                    var rect = new Rect(0, 0, size, size);
                    rect.offsetTo((int) offset + offsetToLeftTop, mEditor.getRowTop(row) - mEditor.getOffsetY() + offsetToLeftTop);
                    drawable.setBounds(rect);
                    drawable.draw(canvas);
                }
            }
            row++;
        }
    }

    protected void drawFormatTip(Canvas canvas) {
        if (mEditor.isFormatting()) {
            String text = mEditor.getFormatTip();
            float baseline = mEditor.getRowBaseline(0);
            float rightX = mEditor.getWidth();
            mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_NORMAL));
            mPaint.setFakeBoldText(true);
            mPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(text, rightX, baseline, mPaint);
            mPaint.setTextAlign(Paint.Align.LEFT);
            mPaint.setFakeBoldText(false);
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

    private void prepareLines(int start, int end) {
        releasePreloadedData();
        mContent.runReadActionsOnLines(Math.max(0, start - 5), Math.min(mContent.getLineCount() - 1, end + 5), (int i, ContentLine line, Directions dirs) -> {
            mPreloadedLines.put(i, line);
            mPreloadedDirections.put(i, dirs);
        });
    }

    private void releasePreloadedData() {
        mPreloadedLines.clear();
        mPreloadedDirections.clear();
    }

    private final LineStyles coordinateLine = new LineStyles(0);

    @Nullable
    protected LineStyles getLineStyles(int line) {
        Styles styles;
        List<LineStyles> lineStylesList;
        if ((styles = mEditor.getStyles()) == null || (lineStylesList = styles.lineStyles) == null) {
            return null;
        }
        coordinateLine.setLine(line);
        var index = Collections.binarySearch(lineStylesList, coordinateLine);
        if (index >= 0 && index < lineStylesList.size()) {
            return lineStylesList.get(index);
        }
        return null;
    }

    @Nullable
    protected <T extends LineAnchorStyle> T getLineStyle(int line, Class<T> type) {
        var lineStyles = getLineStyles(line);
        if (lineStyles != null) {
            return lineStyles.findOne(type);
        }
        return null;
    }

    @Nullable
    protected ResolvableColor getUserBackgroundForLine(int line) {
        var bg = getLineStyle(line, LineBackground.class);
        if (bg != null) {
            return bg.getColor();
        }
        return null;
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
        RowIterator rowIterator = mEditor.getLayout().obtainRowIterator(firstVis, mPreloadedLines);
        Spans spans = mEditor.getStyles() == null ? null : mEditor.getStyles().spans;
        var matchedPositions = mMatchedPositions;
        matchedPositions.clear();
        int currentLine = mCursor.isSelected() ? -1 : mCursor.getLeftLine();
        int currentLineBgColor = mEditor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
        int lastPreparedLine = -1;
        int spanOffset = 0;
        int leadingWhitespaceEnd = 0;
        int trailingWhitespaceStart = 0;
        float circleRadius = 0f;
        var composingPosition = mEditor.mConnection.composingText.isComposing() ? mContent.getIndexer().getCharPosition(mEditor.mConnection.composingText.startIndex) : null;
        var composingLength = mEditor.mConnection.composingText.endIndex - mEditor.mConnection.composingText.startIndex;
        if (mEditor.shouldInitializeNonPrintable()) {
            float spaceWidth = mPaint.getSpaceWidth();
            circleRadius = Math.min(mEditor.getRowHeight(), spaceWidth) * 0.125f;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !mEditor.isWordwrap() && canvas.isHardwareAccelerated() && mEditor.isHardwareAcceleratedDrawAllowed()) {
            mRenderNodeHolder.keepCurrentInDisplay(firstVis, mEditor.getLastVisibleRow());
        }
        float offset2 = mEditor.getOffsetX() - mEditor.measureTextRegionOffset();
        float offset3 = offset2 - mEditor.getDpUnit() * 15;

        // Step 1 - Draw background of rows
        for (int row = firstVis; row <= mEditor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
            Row rowInf = rowIterator.next();
            int line = rowInf.lineIndex;
            int columnCount = getColumnCount(line);
            if (lastPreparedLine != line) {
                mEditor.computeMatchedPositions(line, matchedPositions);
                prepareLine(line);
                lastPreparedLine = line;
            }
            // Get visible region on the line
            float[] charPos = findDesiredVisibleChar(offset3, line, rowInf.startColumn, rowInf.endColumn);
            float paintingOffset = charPos[1] - offset2;

            var drawCurrentLineBg = line == currentLine && !mEditor.getCursorAnimator().isRunning() && mEditor.isEditable();
            if (!drawCurrentLineBg || mEditor.getProps().drawCustomLineBgOnCurrentLine) {
                // Draw custom background
                var customBackground = getUserBackgroundForLine(line);
                if (customBackground != null) {
                    var color = customBackground.resolve(mEditor);
                    drawRowBackground(canvas, color, row);
                }
            }
            if (drawCurrentLineBg) {
                // Draw current line background
                drawRowBackground(canvas, currentLineBgColor, row);
                postDrawCurrentLines.add(row);
            }

            // Draw matched text background
            if (matchedPositions.size() > 0) {
                for (int i = 0; i < matchedPositions.size(); i++) {
                    var position = matchedPositions.get(i);
                    var start = IntPair.getFirst(position);
                    var end = IntPair.getSecond(position);
                    drawRowRegionBackground(canvas, row, line, start, end, mEditor.getColorScheme().getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND));
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
                if (getColumnCount(line) == 0 && line != mCursor.getRightLine()) {
                    mRect.top = mEditor.getRowTop(row) - mEditor.getOffsetY();
                    mRect.bottom = mEditor.getRowBottom(row) - mEditor.getOffsetY();
                    mRect.left = paintingOffset;
                    mRect.right = mRect.left + mPaint.getSpaceWidth() * 2;
                    mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND));
                    canvas.drawRoundRect(mRect, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mPaint);
                } else {
                    drawRowRegionBackground(canvas, row, line, selectionStart, selectionEnd, mEditor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND));
                }
            }
        }
        rowIterator.reset();

        // Draw current line background on animation
        if (mEditor.getCursorAnimator().isRunning()) {
            mRect.bottom = mEditor.getCursorAnimator().animatedLineBottom() - mEditor.getOffsetY();
            mRect.top = mRect.bottom - mEditor.getCursorAnimator().animatedLineHeight();
            mRect.left = 0;
            mRect.right = mViewRect.right;
            drawColor(canvas, currentLineBgColor, mRect);
        }

        // Step 2 - Draw text and text decorations
        long lastStyle = 0;
        for (int row = firstVis; row <= mEditor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
            Row rowInf = rowIterator.next();
            int line = rowInf.lineIndex;
            ContentLine contentLine = getLine(line);
            int columnCount = contentLine.length();
            if (row == firstVis && requiredFirstLn != null) {
                requiredFirstLn.value = line;
            } else if (rowInf.isLeadingRow) {
                postDrawLineNumbers.add(IntPair.pack(line, row));
            }

            // Prepare data
            if (lastPreparedLine != line) {
                lastPreparedLine = line;
                prepareLine(line);
                spanOffset = 0;
                if (mEditor.shouldInitializeNonPrintable()) {
                    long positions = mEditor.findLeadingAndTrailingWhitespacePos(mBuffer);
                    leadingWhitespaceEnd = IntPair.getFirst(positions);
                    trailingWhitespaceStart = IntPair.getSecond(positions);
                }
            }

            // Get visible region on the line
            float[] charPos = findDesiredVisibleChar(offset3, line, rowInf.startColumn, rowInf.endColumn);
            int firstVisibleChar = (int) charPos[0];
            float paintingOffset = charPos[1] - offset2;
            int lastVisibleChar = (int) findDesiredVisibleChar(mEditor.getWidth() - paintingOffset, line, firstVisibleChar, rowInf.endColumn, rowInf.startColumn, true)[0];

            float backupOffset = paintingOffset;
            int nonPrintableFlags = mEditor.getNonPrintablePaintingFlags();

            // Draw text here
            if (!mEditor.isHardwareAcceleratedDrawAllowed() || !canvas.isHardwareAccelerated() || mEditor.isWordwrap() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (rowInf.endColumn - rowInf.startColumn > 256 && !mEditor.getProps().cacheRenderNodeForLongLines) /* Save memory */) {
                // Draw without hardware acceleration
                // Get spans
                var reader = spans == null ? new EmptyReader() : spans.read();
                try {
                    reader.moveToLine(line);
                } catch (Exception e) {
                    e.printStackTrace();
                    reader = new EmptyReader();
                }
                // Seek for first span
                while (spanOffset + 1 < reader.getSpanCount()) {
                    if (reader.getSpanAt(spanOffset + 1).column <= firstVisibleChar) {
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
                    paintStart = Math.max(0, paintStart);
                    if (paintStart >= columnCount) {
                        break;
                    }
                    int paintEnd = Math.min(lastVisibleChar, spanEnd);
                    paintEnd = Math.min(columnCount, paintEnd);
                    if (paintStart > paintEnd) {
                        break;
                    }
                    float width = measureText(mBuffer, line, paintStart, paintEnd - paintStart);
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
                            mPaint.setTextSkewX(GraphicsConstants.TEXT_SKEW_X);
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
                            canvas.drawRoundRect(mRect, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mPaint);
                        }
                    }

                    // Draw text
                    drawRegionTextDirectional(canvas, paintingOffset, mEditor.getRowBaseline(row) - mEditor.getOffsetY(), line, paintStart, paintEnd, span.column, spanEnd, columnCount, mEditor.getColorScheme().getColor(span.getForegroundColorId()));

                    // Draw strikethrough
                    if (TextStyle.isStrikeThrough(styleBits)) {
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

                    if (paintEnd >= lastVisibleChar || paintEnd >= columnCount) {
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

                // Draw hard wrap
                if (lastVisibleChar == columnCount && (nonPrintableFlags & FLAG_DRAW_LINE_SEPARATOR) != 0) {
                    drawMiniGraph(canvas, paintingOffset, row, "↵");
                }
            } else {
                paintingOffset = offset + mRenderNodeHolder.drawLineHardwareAccelerated(canvas, line, offset) - mEditor.getDpUnit() * 20;
                lastVisibleChar = columnCount;
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
            if (composingPosition != null && line == composingPosition.line) {
                int composingStart = composingPosition.column;
                int composingEnd = composingStart + composingLength;
                int paintStart = Math.min(Math.max(composingStart, firstVisibleChar), lastVisibleChar);
                int paintEnd = Math.min(Math.max(composingEnd, firstVisibleChar), lastVisibleChar);
                if (paintStart < paintEnd) {
                    mRect.top = mEditor.getRowBottom(row) - mEditor.getOffsetY();
                    mRect.bottom = mRect.top + mEditor.getRowHeight() * 0.06f;
                    mRect.left = paintingOffset + measureText(mBuffer, line, firstVisibleChar, paintStart - firstVisibleChar);
                    mRect.right = mRect.left + measureText(mBuffer, line, paintStart, paintEnd - paintStart);
                    drawColor(canvas, mEditor.getColorScheme().getColor(EditorColorScheme.UNDERLINE), mRect);
                }
            }

            final var layout = mEditor.getLayout();
            // Draw cursors
            if (mCursor.isSelected()) {
                if (mCursor.getLeftLine() == line && isInside(mCursor.getLeftColumn(), rowInf.startColumn, rowInf.endColumn, line)) {
                    float centerX = mEditor.measureTextRegionOffset() + layout.getCharLayoutOffset(mCursor.getLeftLine(), mCursor.getLeftColumn())[1] - mEditor.getOffsetX();
                    var type = mContent.isRtlAt(mCursor.getLeftLine(), mCursor.getLeftColumn()) ? SelectionHandleStyle.HANDLE_TYPE_RIGHT : SelectionHandleStyle.HANDLE_TYPE_LEFT;
                    postDrawCursor.add(new DrawCursorTask(centerX, getRowBottomForBackground(row) - mEditor.getOffsetY(), type, mEditor.getLeftHandleDescriptor()));
                }
                if (mCursor.getRightLine() == line && isInside(mCursor.getRightColumn(), rowInf.startColumn, rowInf.endColumn, line)) {
                    float centerX = mEditor.measureTextRegionOffset() + layout.getCharLayoutOffset(mCursor.getRightLine(), mCursor.getRightColumn())[1] - mEditor.getOffsetX();
                    var type = mContent.isRtlAt(mCursor.getRightLine(), mCursor.getRightColumn()) ? SelectionHandleStyle.HANDLE_TYPE_LEFT : SelectionHandleStyle.HANDLE_TYPE_RIGHT;
                    postDrawCursor.add(new DrawCursorTask(centerX, getRowBottomForBackground(row) - mEditor.getOffsetY(), type, mEditor.getRightHandleDescriptor()));
                }
            } else if (mCursor.getLeftLine() == line && isInside(mCursor.getLeftColumn(), rowInf.startColumn, rowInf.endColumn, line)) {
                float centerX = mEditor.measureTextRegionOffset() + layout.getCharLayoutOffset(mCursor.getLeftLine(), mCursor.getLeftColumn())[1] - mEditor.getOffsetX();
                postDrawCursor.add(new DrawCursorTask(centerX, getRowBottomForBackground(row) - mEditor.getOffsetY(), mEditor.getEventHandler().shouldDrawInsertHandle() ? SelectionHandleStyle.HANDLE_TYPE_INSERT : SelectionHandleStyle.HANDLE_TYPE_UNDEFINED, mEditor.getInsertHandleDescriptor()));
            }
        }

        mPaint.setFakeBoldText(false);
        mPaint.setTextSkewX(0);
        mPaintOther.setStrokeWidth(circleRadius * 2);
        mDrawPoints.commitPoints(canvas, mPaintOther);
    }

    protected void drawDiagnosticIndicators(Canvas canvas, float offset) {
        var diagnosticsContainer = mEditor.getDiagnostics();
        var style = mEditor.getDiagnosticIndicatorStyle();
        if (diagnosticsContainer != null && style != DiagnosticIndicatorStyle.NONE && style != null) {
            var text = mContent;
            var firstVisRow = mEditor.getFirstVisibleRow();
            var lastVisRow = mEditor.getLastVisibleRow();
            var firstIndex = text.getCharIndex(mEditor.getFirstVisibleLine(), 0);
            var lastIndex = text.getCharIndex(Math.min(text.getLineCount() - 1, mEditor.getLastVisibleLine()), 0);
            diagnosticsContainer.queryInRegion(mCollectedDiagnostics, firstIndex, lastIndex);
            if (mCollectedDiagnostics.isEmpty()) {
                return;
            }
            final float waveLength = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveLength;
            final float amplitude = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveAmplitude;
            final float waveWidth = mEditor.getDpUnit() * mEditor.getProps().indicatorWaveWidth;
            var start = new CharPosition();
            var end = new CharPosition();
            var indexer = mCursor.getIndexer();
            for (var region : mCollectedDiagnostics) {
                var startIndex = Math.max(firstIndex, region.startIndex);
                var endIndex = Math.min(lastIndex, region.endIndex);
                indexer.getCharPosition(startIndex, start);
                indexer.getCharPosition(endIndex, end);
                var startRow = mEditor.getLayout().getRowIndexForPosition(startIndex);
                var endRow = mEditor.getLayout().getRowIndexForPosition(endIndex);
                // Setup color
                var colorId = (region.severity >= 0 && region.severity <= 3) ? sDiagnosticsColorMapping[region.severity] : 0;
                if (colorId == 0) {
                    break;
                }
                mPaintOther.setColor(mEditor.getColorScheme().getColor(colorId));
                startRow = Math.max(firstVisRow, startRow);
                endRow = Math.min(lastVisRow, endRow);
                for (int i = startRow; i <= endRow; i++) {
                    var row = mEditor.getLayout().getRowAt(i);
                    var startX = 0f;
                    if (i == startRow) {
                        startX = measureText(getLine(row.lineIndex), row.lineIndex, row.startColumn, start.column - row.startColumn);
                    }
                    float endX;
                    if (i != endRow) {
                        endX = measureText(getLine(row.lineIndex), row.lineIndex, row.startColumn, row.endColumn - row.startColumn);
                    } else {
                        endX = measureText(getLine(row.lineIndex), row.lineIndex, row.startColumn, end.column - row.startColumn);
                    }
                    startX += offset;
                    endX += offset;
                    // Make it always visible
                    if (Math.abs(startX - endX) < 1e-2) {
                        endX = startX + mPaint.measureText("a");
                    }
                    if (endX > 0 && startX < mEditor.getWidth()) {
                        // Draw
                        float centerY = mEditor.getRowBottom(i) - mEditor.getOffsetY();
                        switch (style) {
                            case WAVY_LINE: {
                                var lineWidth = 0 - startX;
                                var waveCount = (int) Math.ceil(lineWidth / waveLength);
                                var phi = lineWidth < 0 ? 0f : (waveLength * waveCount - lineWidth);
                                lineWidth = endX - startX;
                                canvas.save();
                                canvas.clipRect(startX, 0, endX, canvas.getHeight());
                                canvas.translate(startX, centerY);
                                mPath.reset();
                                mPath.moveTo(0, 0);
                                waveCount = (int) Math.ceil((phi + lineWidth) / waveLength);
                                for (int j = 0; j < waveCount; j++) {
                                    mPath.quadTo(waveLength * j + waveLength / 4, amplitude, waveLength * j + waveLength / 2, 0);
                                    mPath.quadTo(waveLength * j + waveLength * 3 / 4, -amplitude, waveLength * j + waveLength, 0);
                                }
                                // Draw path
                                mPaintOther.setStrokeWidth(waveWidth);
                                mPaintOther.setStyle(Paint.Style.STROKE);
                                canvas.drawPath(mPath, mPaintOther);
                                canvas.restore();
                                mPaintOther.setStyle(Paint.Style.FILL);
                                break;
                            }
                            case LINE: {
                                mPaintOther.setStrokeWidth(waveWidth);
                                canvas.drawLine(startX, centerY, endX, centerY, mPaintOther);
                                break;
                            }
                            case DOUBLE_LINE: {
                                mPaintOther.setStrokeWidth(waveWidth / 3f);
                                canvas.drawLine(startX, centerY, endX, centerY, mPaintOther);
                                canvas.drawLine(startX, centerY - waveWidth, endX, centerY - waveWidth, mPaintOther);
                                break;
                            }
                        }
                    }
                }
            }
        }
        mCollectedDiagnostics.clear();
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
            offset += measureText(mBuffer, line, rowStart, paintStart - rowStart);
            var chars = mBuffer.value;
            var lastPos = paintStart;
            while (paintStart < paintEnd) {
                char ch = chars[paintStart];
                int paintCount = 0;
                boolean paintLine = false;
                if (ch == ' ' || ch == '\t') {
                    offset += measureText(mBuffer, line, lastPos, paintStart - lastPos);
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
        float baseline = row == -1 ? (mEditor.getRowBottom(0) - mGraphMetrics.descent) : (mEditor.getRowBottom(row) - mEditor.getOffsetY() - mGraphMetrics.descent);
        canvas.drawText(graph, 0, graph.length(), offset, baseline, mPaintGraph);
    }

    protected int getRowTopForBackground(int row) {
        if (!mEditor.getProps().textBackgroundWrapTextOnly) {
            return mEditor.getRowTop(row);
        } else {
            return mEditor.getRowTopOfText(row);
        }
    }

    protected int getRowBottomForBackground(int row) {
        if (!mEditor.getProps().textBackgroundWrapTextOnly) {
            return mEditor.getRowBottom(row);
        } else {
            return mEditor.getRowBottomOfText(row);
        }
    }

    /**
     * Draw background of a text region
     *
     * @param canvas         Canvas to draw
     * @param row            The row index
     * @param highlightStart Region start
     * @param highlightEnd   Region end
     * @param color          Color of background
     */
    protected void drawRowRegionBackground(Canvas canvas, int row, int line, int highlightStart, int highlightEnd, int color) {
        if (highlightStart != highlightEnd) {
            mRect.top = getRowTopForBackground(row) - mEditor.getOffsetY();
            mRect.bottom = getRowBottomForBackground(row) - mEditor.getOffsetY();
            var dirs = mContent.getLineDirections(line);
            var empty = true;
            var layout = mEditor.getLayout();
            mPaint.setColor(color);
            float paintingOffset = mEditor.measureTextRegionOffset() - mEditor.getOffsetX();
            for (int i = 0; i < dirs.getRunCount(); i++) {
                int sharedStart = Math.max(highlightStart, dirs.getRunStart(i));
                int sharedEnd = Math.min(highlightEnd, dirs.getRunEnd(i));
                if (dirs.getRunStart(i) >= highlightEnd) {
                    break;
                }
                if (sharedStart >= sharedEnd) {
                    continue;
                }
                var left = paintingOffset + layout.getCharLayoutOffset(line, sharedStart)[1];
                var right = paintingOffset + layout.getCharLayoutOffset(line, sharedEnd)[1];
                if (left > right) {
                    var tmp = left;
                    left = right;
                    right = tmp;
                }
                if (empty) {
                    mRect.left = left;
                    mRect.right = right;
                    empty = false;
                } else {
                    if (Math.abs(left - mRect.right) < 1e-2) {
                        mRect.right = right;
                    } else if (Math.abs(right - mRect.left) < 1e-2) {
                        mRect.left = left;
                    } else {
                        drawRowBackgroundRect(canvas, mRect);
                        mRect.left = left;
                        mRect.right = right;
                    }
                }
            }
            if (!empty) {
                drawRowBackgroundRect(canvas, mRect);
            }
        }
    }

    protected void drawRowBackgroundRect(Canvas canvas, RectF rect) {
        if (mEditor.getProps().enableRoundTextBackground) {
            canvas.drawRoundRect(rect, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mPaint);
        } else {
            canvas.drawRect(rect, mPaint);
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
                drawText(canvas, mBuffer, startIndex, endIndex - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
            } else {
                if (startIndex <= selectionStart) {
                    if (endIndex >= selectionEnd) {
                        //Three regions
                        //startIndex - selectionStart
                        drawText(canvas, mBuffer, startIndex, selectionStart - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                        float deltaX = measureText(mBuffer, line, startIndex, selectionStart - startIndex);
                        //selectionStart - selectionEnd
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mBuffer, selectionStart, selectionEnd - selectionStart, contextStart, contextCount, isRtl, offsetX + deltaX, baseline, line);
                        deltaX += measureText(mBuffer, line, selectionStart, selectionEnd - selectionStart);
                        //selectionEnd - endIndex
                        mPaint.setColor(color);
                        drawText(canvas, mBuffer, selectionEnd, endIndex - selectionEnd, contextStart, contextCount, isRtl, offsetX + deltaX, baseline, line);
                    } else {
                        //Two regions
                        //startIndex - selectionStart
                        drawText(canvas, mBuffer, startIndex, selectionStart - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                        //selectionStart - endIndex
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mBuffer, selectionStart, endIndex - selectionStart, contextStart, contextCount, isRtl, offsetX + measureText(mBuffer, line, startIndex, selectionStart - startIndex), baseline, line);
                    }
                } else {
                    //selectionEnd > startIndex > selectionStart
                    if (endIndex > selectionEnd) {
                        //Two regions
                        //selectionEnd - endIndex
                        drawText(canvas, mBuffer, selectionEnd, endIndex - selectionEnd, contextStart, contextCount, isRtl, offsetX + measureText(mBuffer, line, startIndex, selectionEnd - startIndex), baseline, line);
                        //startIndex - selectionEnd
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mBuffer, startIndex, selectionEnd - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                    } else {
                        //One region
                        mPaint.setColor(mEditor.getColorScheme().getColor(EditorColorScheme.TEXT_SELECTED));
                        drawText(canvas, mBuffer, startIndex, endIndex - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
                    }
                }
            }
        } else {
            drawText(canvas, mBuffer, startIndex, endIndex - startIndex, contextStart, contextCount, isRtl, offsetX, baseline, line);
        }
    }

    protected void drawRegionTextDirectional(Canvas canvas, float offsetX, float baseline, int line, int startIndex, int endIndex, int contextStart, int contextEnd, int columnCount, int color) {
        var directions = mContent.getLineDirections(line);
        var width = 0f;
        for (int i = 0; i < directions.getRunCount(); i++) {
            int sharedStart = Math.max(directions.getRunStart(i), startIndex);
            int sharedEnd = Math.min(directions.getRunEnd(i), endIndex);
            if (sharedEnd > sharedStart) {
                drawRegionText(canvas, offsetX + width, baseline, line, sharedStart, sharedEnd, contextStart, contextEnd, directions.isRunRtl(i), columnCount, color);
            }
            if (i + 1 < directions.getRunCount())
                width += measureText(getLine(line), line, sharedStart, sharedEnd - sharedStart);
        }
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
    protected void drawText(Canvas canvas, ContentLine line, int index, int count, int contextStart, int contextCount, boolean isRtl, float offX, float offY, int lineNumber) {
        int end = index + count;
        var src = line.value;
        int st = index;
        for (int i = index; i < end; i++) {
            if (src[i] == '\t') {
                drawTextRunDirect(canvas, src, st, i - st, contextStart, contextCount, offX, offY, isRtl);
                offX = offX + measureText(line, lineNumber, st, i - st + 1);
                st = i + 1;
            }
        }
        if (st < end) {
            drawTextRunDirect(canvas, src, st, end - st, contextStart, contextCount, offX, offY, isRtl);
        }
    }

    @SuppressLint("NewApi")
    protected void drawTextRunDirect(Canvas canvas, char[] src, int index, int count, int contextStart, int contextCount, float offX, float offY, boolean isRtl) {
        if (basicDisplayMode) {
            int charCount;
            for (int i = 0; i < count; i += charCount) {
                charCount = 1;
                if (Character.isHighSurrogate(src[index + i]) && i + 1 < count && Character.isLowSurrogate(src[index + i + 1])) {
                    charCount = 2;
                }
                canvas.drawText(src, index + i, charCount, offX, offY, mPaint);
                offX += mPaint.myGetTextRunAdvances(src, index + i, charCount, index + i, charCount, false, null, 0, true);
            }
        } else {
            canvas.drawTextRun(src, index, count, contextStart, contextCount, offX, offY, isRtl, mPaint);
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
        if (index == end && mContent.getLine(line).length() != end) {
            return false;
        }
        return index >= start && index <= end;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public android.graphics.Paint.FontMetricsInt getLineNumberMetrics() {
        return mLineNumberMetrics;
    }

    /**
     * Draw effect of edges
     *
     * @param canvas The canvas to draw
     */
    protected void drawEdgeEffect(Canvas canvas) {
        boolean postDraw = false;
        var verticalEdgeEffect = mEditor.getVerticalEdgeEffect();
        var horizontalEdgeEffect = mEditor.getHorizontalEdgeEffect();
        if (!verticalEdgeEffect.isFinished()) {
            boolean bottom = mEditor.getEventHandler().glowTopOrBottom;
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
            boolean right = mEditor.getEventHandler().glowLeftOrRight;
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
                mEditor.getEventHandler().glowTopOrBottom = scroller.getCurrY() >= mEditor.getScrollMaxY();
                verticalEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
                postDraw = true;
            }
            if (horizontalEdgeEffect.isFinished() && (scroller.getCurrX() < 0 || scroller.getCurrX() > mEditor.getScrollMaxX())) {
                mEditor.getEventHandler().glowLeftOrRight = scroller.getCurrX() >= mEditor.getScrollMaxX();
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
                    var lineContent = getLine(block.endLine);
                    float offset1 = measureText(lineContent, block.endLine, 0, Math.min(block.endColumn, lineContent.length()));
                    lineContent = getLine(block.startLine);
                    float offset2 = measureText(lineContent, block.startLine, 0, Math.min(block.startColumn, lineContent.length()));
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
                mPaint.setStrokeWidth(mEditor.getDpUnit() * mEditor.getBlockLineWidth());
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
        if (length < mEditor.getDpUnit() * 60) {
            length = mEditor.getDpUnit() * 60;
            topY = (mEditor.getOffsetY()) / all * (height - length);
        } else {
            topY = mEditor.getOffsetY() / all * height;
        }
        if (mEditor.getEventHandler().holdVerticalScrollBar()) {
            float centerY = topY + length / 2f;
            drawLineInfoPanel(canvas, centerY, mRect.left - mEditor.getDpUnit() * 5);
        }
        mRect.right = mEditor.getWidth();
        mRect.left = mEditor.getWidth() - mEditor.getDpUnit() * 13;
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

    protected void patchHighlightedDelimiters(Canvas canvas, float textOffset) {
        if (mEditor.mConnection.composingText.isComposing() || !mEditor.getProps().highlightMatchingDelimiters) {
            return;
        }
        var paired = mEditor.mStyleDelegate.getFoundBracketPair();
        if (paired != null) {
            var color = mEditor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND);
            var backgroundColor = mEditor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND);
            if (isInvalidTextBounds(paired.leftIndex, paired.leftLength) || isInvalidTextBounds(paired.rightIndex, paired.rightLength)) {
                // Index out of bounds
                return;
            }

            patchTextRegionWithColor(canvas, textOffset, paired.leftIndex, paired.leftIndex + paired.leftLength, color, backgroundColor);
            patchTextRegionWithColor(canvas, textOffset, paired.rightIndex, paired.rightIndex + paired.rightLength, color, backgroundColor);
        }
    }

    protected boolean isInvalidTextBounds(int index, int length) {
        return (index < 0 || length < 0 || index + length > mContent.length());
    }

    protected void patchTextRegionWithColor(Canvas canvas, float textOffset, int start, int end, int color, int backgroundColor) {
        mPaint.setColor(color);
        var underlineColor = mEditor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE);
        mPaintOther.setStrokeWidth(mEditor.getRowHeightOfText() * 0.1f);
        mPaint.setStyle(android.graphics.Paint.Style.FILL_AND_STROKE);
        mPaint.setFakeBoldText(true);
        patchTextRegions(canvas, textOffset, getTextRegionPositions(start, end), (canvasLocal, horizontalOffset, row, line, startCol, endCol, style) -> {
            if (backgroundColor != 0) {
                mRect.top = getRowTopForBackground(row) - mEditor.getOffsetY();
                mRect.bottom = getRowBottomForBackground(row) - mEditor.getOffsetY();
                mRect.left = 0;
                mRect.right = mEditor.getWidth();
                mPaintOther.setColor(backgroundColor);
                if (mEditor.getProps().enableRoundTextBackground) {
                    canvas.drawRoundRect(mRect, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mEditor.getRowHeight() * mEditor.getProps().roundTextBackgroundFactor, mPaintOther);
                } else {
                    canvas.drawRect(mRect, mPaintOther);
                }
            }
            if (color != 0) {
                mPaint.setTextSkewX(TextStyle.isItalics(style) ? GraphicsConstants.TEXT_SKEW_X : 0f);
                mPaint.setStrikeThruText(TextStyle.isStrikeThrough(style));
                drawText(canvas, getLine(line), startCol, endCol - startCol, startCol, endCol - startCol, false, horizontalOffset, mEditor.getRowBaseline(row) - mEditor.getOffsetY(), line);
            }
            if (underlineColor != 0) {
                mPaintOther.setColor(underlineColor);
                var bottom = mEditor.getRowBottomOfText(row) - mEditor.getOffsetY() - mEditor.getRowHeightOfText() * 0.05f;
                canvas.drawLine(0, bottom, mEditor.getWidth(), bottom, mPaintOther);
            }
        });
        mPaint.setStyle(android.graphics.Paint.Style.FILL);
        mPaint.setFakeBoldText(false);
        mPaint.setTextSkewX(0f);
        mPaint.setStrikeThruText(false);
    }

    protected List<TextDisplayPosition> getTextRegionPositions(int start, int end) {
        var layout = mEditor.getLayout();
        var startRow = layout.getRowIndexForPosition(start);
        var endRow = layout.getRowIndexForPosition(end);
        var posStart = mCursor.getIndexer().getCharPosition(start);
        var posEnd = mCursor.getIndexer().getCharPosition(end);
        var itr = layout.obtainRowIterator(startRow, mPreloadedLines);
        var list = new ArrayList<TextDisplayPosition>();
        for (int i = startRow; i <= endRow && itr.hasNext(); i++) {
            var row = itr.next();
            var startOnRow = (i == startRow ? posStart.column : row.startColumn);
            var endOnRow = (i == endRow ? posEnd.column : row.endColumn);
            var position = new TextDisplayPosition();
            list.add(position);
            position.row = i;
            var line = mContent.getLine(row.lineIndex);
            position.left = measureText(line, row.lineIndex, row.startColumn, startOnRow - row.startColumn);
            position.right = position.left + measureText(line, row.lineIndex, startOnRow, endOnRow - startOnRow);
            position.startColumn = startOnRow;
            position.endColumn = endOnRow;
            position.line = row.lineIndex;
            position.rowStart = row.startColumn;
        }
        System.out.println(list);
        return list;
    }

    protected void patchTextRegions(Canvas canvas, float textOffset, List<TextDisplayPosition> positions, @NonNull PatchDraw patch) {
        var styles = mEditor.getStyles();
        var spans = styles != null ? styles.getSpans() : null;
        var reader = spans != null ? spans.read() : new EmptyReader();
        var firstVisRow = mEditor.getFirstVisibleRow();
        var lastVisRow = mEditor.getLastVisibleRow();
        for (var position : positions) {
            if (!(firstVisRow <= position.row && position.row <= lastVisRow)) {
                continue;
            }
            // First, get the line
            var line = position.line;
            try {
                reader.moveToLine(line);
            } catch (Exception e) {
                Log.e(LOG_TAG, "patchTextRegions: Unable to get spans", e);
                break;
            }
            var startCol = position.startColumn;
            var endCol = position.endColumn;
            var lineText = getLine(line);
            var column = lineText.length();
            canvas.save();
            var horizontalOffset = textOffset;
            var first = true;
            // Find spans to draw
            Span nextSpan = null;
            int spanCount = reader.getSpanCount();
            for (int i = 0; i < spanCount; i++) {
                Span span;
                if (nextSpan == null) {
                    span = reader.getSpanAt(i);
                } else {
                    span = nextSpan;
                }
                nextSpan = i + 1 == spanCount ? null : reader.getSpanAt(i + 1);
                var spanStart = Math.max(span.column, position.rowStart);
                var sharedStart = Math.max(startCol, spanStart);
                var spanEnd = nextSpan == null ? column : nextSpan.column;
                if (spanEnd <= position.startColumn) {
                    continue;
                }
                var sharedEnd = Math.min(endCol, spanEnd);
                if (sharedEnd - sharedStart > 0) {
                    // Clip canvas to patch the requested region
                    if (first) {
                        first = false;
                        horizontalOffset += measureText(lineText, line, position.rowStart, spanStart - position.rowStart);
                        if (TextStyle.isItalics(span.getStyleBits())) {
                            var path = new Path();
                            var y = mEditor.getRowBottomOfText(position.row) - mEditor.getOffsetY();
                            path.moveTo(textOffset + position.left, y);
                            path.lineTo(textOffset + position.left - GraphicsConstants.TEXT_SKEW_X * y, 0f);
                            path.lineTo(mEditor.getWidth(), 0f);
                            path.lineTo(mEditor.getWidth(), mEditor.getHeight());
                            path.close();
                            canvas.clipPath(path);
                        } else {
                            canvas.clipRect(textOffset + position.left, 0, mEditor.getWidth(), mEditor.getHeight());
                        }
                    }
                    if (spanEnd >= endCol || i + 1 >= reader.getSpanCount()) {
                        if (TextStyle.isItalics(span.getStyleBits())) {
                            var path = new Path();
                            var y = mEditor.getRowBottomOfText(position.row) - mEditor.getOffsetY();
                            path.moveTo(textOffset + position.right, y);
                            path.lineTo(textOffset + position.right - GraphicsConstants.TEXT_SKEW_X * y, 0f);
                            path.lineTo(0, 0f);
                            path.lineTo(0, mEditor.getHeight());
                            path.close();
                            canvas.clipPath(path);
                        } else {
                            canvas.clipRect(0, 0, textOffset + position.right, mEditor.getHeight());
                        }
                    }
                    // Patch the text
                    patch.draw(canvas, horizontalOffset, position.row, line, spanStart, spanEnd, span.style);
                }
                if (spanEnd >= endCol) {
                    break;
                }
                horizontalOffset += measureText(lineText, line, spanStart, spanEnd - spanStart);
            }
            canvas.restore();
        }
        try {
            reader.moveToLine(-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void drawSelectionOnAnimation(Canvas canvas) {
        mRect.bottom = mEditor.getCursorAnimator().animatedY() - mEditor.getOffsetY();
        mRect.top = mRect.bottom - (mEditor.getProps().textBackgroundWrapTextOnly ? mEditor.getRowHeightOfText() : mEditor.getRowHeight());
        float centerX = mEditor.getCursorAnimator().animatedX() - mEditor.getOffsetX();
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

    // BEGIN Measure-------------------------------------

    @UnsupportedUserUsage
    public float[] findDesiredVisibleChar(float target, int lineIndex, int start, int end) {
        return findDesiredVisibleChar(target, lineIndex, start, end, start, false);
    }

    /**
     * Find first visible character
     */
    @UnsupportedUserUsage
    public float[] findDesiredVisibleChar(float target, int lineIndex, int start, int end, int contextStart, boolean forLast) {
        if (start >= end) {
            return new float[]{end, 0};
        }
        var line = getLine(lineIndex);
        if (line.widthCache != null && line.timestamp < mTimestamp) {
            buildMeasureCacheForLines(lineIndex, lineIndex, mTimestamp, false);
        }
        var gtr = GraphicTextRow.obtain(basicDisplayMode);
        gtr.set(mContent, lineIndex, contextStart, end, mEditor.getTabWidth(), line.widthCache == null ? mEditor.getSpansForLine(lineIndex) : null, mPaint);
        if (mEditor.getLayout() instanceof WordwrapLayout && line.widthCache == null) {
            gtr.setSoftBreaks(((WordwrapLayout) mEditor.getLayout()).getSoftBreaksForLine(lineIndex));
        }
        var res = gtr.findOffsetByAdvance(start, target);

        // Do some additional work here

        var offset = (int) res[0];
        // Check RTL context
        var rtl = false;
        int runIndex = -1;
        Directions dirs = null;
        if (line.mayNeedBidi()) {
            dirs = mContent.getLineDirections(lineIndex);
            if (offset == line.length()) {
                runIndex = dirs.getRunCount() - 1;
            } else {
                for (int i = 0; i < dirs.getRunCount(); i++) {
                    if (offset >= dirs.getRunStart(i) && offset < dirs.getRunEnd(i)) {
                        runIndex = i;
                        rtl = dirs.isRunRtl(i);
                        break;
                    }
                }
            }
        }

        // Find actual desired position
        if (rtl) {
            if (forLast) {
                offset = dirs.getRunEnd(runIndex);
            } else {
                offset = dirs.getRunStart(runIndex);
            }
        } else {
            if (forLast) {
                if (offset + 1 < end) {
                    var itr = new UnicodeIterator(line, offset + 1, end);
                    int codePoint;
                    var first = true;
                    while ((codePoint = itr.nextCodePoint()) != 0) {
                        if (isCombiningCharacter(codePoint) || first) {
                            offset = itr.getEndIndex();
                            first = false;
                        } else {
                            break;
                        }
                    }
                }
            } else {
                if (offset < end) {
                    var chars = line.getRawData();
                    while (offset > start) {
                        char ch = chars[offset];
                        if (Character.isLowSurrogate(ch)) {
                            if (offset - 1 >= start) {
                                if (isCombiningCharacter(Character.toCodePoint(chars[offset - 1], ch))) {
                                    offset -= 2;
                                } else if (isCombiningCharacter(ch)) {
                                    offset -= 1;
                                } else {
                                    break;
                                }
                            }
                        } else if (Character.isHighSurrogate(ch) || isCombiningCharacter(ch)) {
                            offset -= 1;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        if (!rtl && !forLast && offset > start) {
            // Try to combine one character again
            var chars = line.getRawData();
            if (Character.isLowSurrogate(chars[offset - 1])) {
                if (offset - 1 > start && !couldBeEmojiPart(Character.toCodePoint(chars[offset - 2], chars[offset - 1]))) {
                    offset -= 2;
                }
            } else if (!Character.isHighSurrogate(chars[offset - 1])) {
                offset -= 1;
            }
        }
        offset = Math.min(end, Math.max(start, offset));
        res = new float[]{offset, gtr.measureText(start, offset)};

        GraphicTextRow.recycle(gtr);
        return res;
    }

    /**
     * Find first visible character
     */
    @UnsupportedUserUsage
    public float[] findFirstVisibleCharForWordwrap(float target, int lineIndex, int start, int end, int contextStart, Paint paint) {
        if (start >= end) {
            return new float[]{end, 0};
        }
        var gtr = GraphicTextRow.obtain(basicDisplayMode);
        if (mEditor.defSpans.size() == 0) {
            mEditor.defSpans.add(Span.obtain(0, EditorColorScheme.TEXT_NORMAL));
        }
        gtr.set(mContent, lineIndex, contextStart, end, mEditor.getTabWidth(), mEditor.defSpans, paint);
        gtr.disableCache();
        var res = gtr.findOffsetByAdvance(start, target);
        GraphicTextRow.recycle(gtr);
        return res;
    }

    /**
     * Build measure cache for the given lines, if the timestamp indicates that it is outdated.
     */
    protected void buildMeasureCacheForLines(int startLine, int endLine, long timestamp, boolean useCachedContent) {
        var text = mContent;
        while (startLine <= endLine && startLine < text.getLineCount()) {
            var line = useCachedContent ? getLine(startLine) : getLineDirect(startLine);
            if (line.timestamp < timestamp) {
                var gtr = GraphicTextRow.obtain(basicDisplayMode);
                var forced = false;
                if (line.widthCache == null || line.widthCache.length < line.length()) {
                    line.widthCache = mEditor.obtainFloatArray(Math.max(line.length() + 8, 90), useCachedContent);
                    forced = true;
                }
                var spans = mEditor.getSpansForLine(startLine);
                gtr.set(text, startLine, 0, line.length(), mEditor.getTabWidth(), spans, mPaint);
                var softBreaks = (mEditor.mLayout instanceof WordwrapLayout) ? ((WordwrapLayout) mEditor.mLayout).getSoftBreaksForLine(startLine) : null;
                gtr.setSoftBreaks(softBreaks);
                var hash = Objects.hash(spans, line.length(), mEditor.getTabWidth(), basicDisplayMode, softBreaks, mPaint.getFlags(), mPaint.getTextSize(), mPaint.getTextScaleX(), mPaint.getLetterSpacing());
                if (line.styleHash != hash || forced) {
                    gtr.buildMeasureCache();
                    line.styleHash = hash;
                }
                GraphicTextRow.recycle(gtr);
                line.timestamp = timestamp;
            }
            startLine++;
        }
    }

    protected void buildMeasureCacheForLines(int startLine, int endLine) {
        buildMeasureCacheForLines(startLine, endLine, mTimestamp, false);
    }

    /**
     * Measure text width with editor's text paint
     *
     * @param text  Source string
     * @param index Start index in array
     * @param count Count of characters
     * @return The width measured
     */
    @UnsupportedUserUsage
    public float measureText(ContentLine text, int line, int index, int count) {
        var gtr = GraphicTextRow.obtain(basicDisplayMode);
        List<Span> spans = mEditor.defSpans;
        if (text.widthCache == null) {
            spans = mEditor.getSpansForLine(line);
        }
        gtr.set(text, getLineDirections(line), 0, text.length(), mEditor.getTabWidth(), spans, mPaint);
        if (mEditor.mLayout instanceof WordwrapLayout && text.widthCache == null) {
            gtr.setSoftBreaks(((WordwrapLayout) mEditor.mLayout).getSoftBreaksForLine(line));
        }
        var res = gtr.measureText(index, index + count);
        GraphicTextRow.recycle(gtr);
        return res;
    }

    // END Measure---------------------------------------

    protected interface PatchDraw {

        void draw(Canvas canvas, float horizontalOffset, int row, int line, int start, int end, long style);

    }

    private static class TextDisplayPosition {
        int row, startColumn, endColumn, line, rowStart;
        float left;
        float right;

        @Override
        @NonNull
        public String toString() {
            return "TextDisplayPosition{" +
                    "row=" + row +
                    ", startColumn=" + startColumn +
                    ", endColumn=" + endColumn +
                    ", line=" + line +
                    ", rowStart=" + rowStart +
                    ", left=" + left +
                    ", right=" + right +
                    '}';
        }
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
            if (mEditor.mConnection.imeConsumingInput || !mEditor.hasFocus()) {
                return;
            }
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && !mEditor.isEditable()) {
                return;
            }
            // Follow the thumb or stick to text row
            if (!descriptor.position.isEmpty()) {
                boolean isInsertHandle = mEditor.getEventHandler().holdInsertHandle() && handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT;
                boolean isLeftHandle = mEditor.getEventHandler().selHandleType == EditorTouchEventHandler.SelectionHandle.LEFT && handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT;
                boolean isRightHandle = mEditor.getEventHandler().selHandleType == EditorTouchEventHandler.SelectionHandle.RIGHT && handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT;
                if (!mEditor.isStickyTextSelection()) {
                    if (isInsertHandle || isLeftHandle || isRightHandle) {
                        x = mEditor.getEventHandler().motionX + (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER ? descriptor.position.width() : 0) * (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT ? 1 : -1);
                        y = mEditor.getEventHandler().motionY - descriptor.position.height() * 2 / 3f;
                    }
                }
            }

            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED || mEditor.getCursorBlink().visibility || mEditor.getEventHandler().holdInsertHandle()) {
                mRect.top = y - (mEditor.getProps().textBackgroundWrapTextOnly ? mEditor.getRowHeightOfText() : mEditor.getRowHeight());
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
}
