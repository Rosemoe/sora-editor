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
package io.github.rosemoe.sora.widget;

import static io.github.rosemoe.sora.util.Numbers.stringSize;
import static io.github.rosemoe.sora.widget.DirectAccessProps.CURSOR_LINE_BG_OVERLAP_CURSOR;
import static io.github.rosemoe.sora.widget.DirectAccessProps.CURSOR_LINE_BG_OVERLAP_CUSTOM;
import static io.github.rosemoe.sora.widget.DirectAccessProps.CURSOR_LINE_BG_OVERLAP_MIXED;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.MutableIntList;
import androidx.collection.MutableLongLongMap;
import androidx.collection.MutableLongObjectMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.graphics.BubbleHelper;
import io.github.rosemoe.sora.graphics.BufferedDrawPoints;
import io.github.rosemoe.sora.graphics.GraphicsCompat;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.graphics.TextRow;
import io.github.rosemoe.sora.graphics.TextRowParams;
import io.github.rosemoe.sora.lang.completion.snippet.SnippetItem;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.EmptyReader;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.Spans;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;
import io.github.rosemoe.sora.lang.styling.line.LineAnchorStyle;
import io.github.rosemoe.sora.lang.styling.line.LineBackground;
import io.github.rosemoe.sora.lang.styling.line.LineGutterBackground;
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon;
import io.github.rosemoe.sora.lang.styling.line.LineStyles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.bidi.Directions;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.LongArrayList;
import io.github.rosemoe.sora.util.MutableInt;
import io.github.rosemoe.sora.util.Numbers;
import io.github.rosemoe.sora.util.TemporaryCharBuffer;
import io.github.rosemoe.sora.widget.layout.Row;
import io.github.rosemoe.sora.widget.layout.RowIterator;
import io.github.rosemoe.sora.widget.rendering.RenderingConstants;
import io.github.rosemoe.sora.widget.rendering.TextAdvancesCache;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;
import io.github.rosemoe.sora.widget.style.DiagnosticIndicatorStyle;
import io.github.rosemoe.sora.widget.style.LineInfoPanelPosition;
import io.github.rosemoe.sora.widget.style.LineInfoPanelPositionMode;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;

public class EditorRenderer {

    private final static int[] PRESSED_DRAWABLE_STATE = new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled};
    private final static int[] DEFAULT_DRAWABLE_STATE = new int[]{android.R.attr.state_enabled};

    private static final String LOG_TAG = "EditorRenderer";
    private final static int[] sDiagnosticsColorMapping = {0, EditorColorScheme.PROBLEM_TYPO, EditorColorScheme.PROBLEM_WARNING, EditorColorScheme.PROBLEM_ERROR};
    protected final BufferedDrawPoints bufferedDrawPoints;
    protected final Paint paintGeneral;
    protected final Paint paintOther;
    protected final Rect viewRect;
    private final RectF tmpRect;
    private final Path tmpPath;
    protected final Paint paintGraph;
    private final RectF verticalScrollBarRect;
    private final RectF horizontalScrollBarRect;
    private final LongArrayList postDrawLineNumbers = new LongArrayList();
    private final MutableIntList postDrawCurrentLines = new MutableIntList();
    private final LongArrayList matchedPositions = new LongArrayList();
    private final MutableLongLongMap highlightPositions = new MutableLongLongMap();
    private final SparseArray<ContentLine> preloadedLines = new SparseArray<>();
    private final SparseArray<Directions> preloadedDirections = new SparseArray<>();
    private final CodeEditor editor;
    private final List<DiagnosticRegion> collectedDiagnostics = new ArrayList<>();
    protected List<CodeBlock> lastStuckLines;
    Paint.FontMetricsInt metricsText;
    @Nullable
    private Drawable horizontalScrollbarThumbDrawable;
    @Nullable
    private Drawable horizontalScrollbarTrackDrawable;
    @Nullable
    private Drawable verticalScrollbarThumbDrawable;
    @Nullable
    private Drawable verticalScrollbarTrackDrawable;
    private final Drawable lineBreakGraph;
    private final Drawable softwrapLeftGraph;
    private final Drawable softwrapRightGraph;
    private volatile long displayTimestamp;
    private Paint.FontMetricsInt metricsLineNumber;
    private Paint.FontMetricsInt metricsGraph;
    private int cachedGutterWidth;
    private Cursor cursor;
    protected ContentLine lineBuf;
    protected Content content;
    private volatile boolean renderingFlag;
    protected boolean forcedRecreateLayout;

    public EditorRenderer(@NonNull CodeEditor editor) {
        this.editor = editor;
        verticalScrollBarRect = new RectF();
        horizontalScrollBarRect = new RectF();

        bufferedDrawPoints = new BufferedDrawPoints();

        paintGeneral = new Paint(editor.isRenderFunctionCharacters());
        paintGeneral.setAntiAlias(true);
        paintOther = new Paint(false);
        paintOther.setStrokeWidth(this.editor.getDpUnit() * 1.8f);
        paintOther.setStrokeCap(Paint.Cap.ROUND);
        paintOther.setTypeface(Typeface.MONOSPACE);
        paintOther.setAntiAlias(true);
        paintGraph = new Paint(false);
        paintGraph.setAntiAlias(true);

        metricsText = paintGeneral.getFontMetricsInt();
        metricsLineNumber = paintOther.getFontMetricsInt();

        viewRect = new Rect();
        tmpRect = new RectF();
        tmpPath = new Path();

        lineBreakGraph = editor.getContext().getDrawable(R.drawable.line_break);
        softwrapLeftGraph = editor.getContext().getDrawable(R.drawable.softwrap_left);
        softwrapRightGraph = editor.getContext().getDrawable(R.drawable.softwrap_right);

        onEditorFullTextUpdate();
    }

    /**
     * Called when the editor text is changed by {@link CodeEditor#setText}
     */
    public void onEditorFullTextUpdate() {
        cursor = editor.getCursor();
        content = editor.getText();
    }

    public void draw(@NonNull Canvas canvas) {
        int saveCount = canvas.save();
        canvas.translate(editor.getOffsetX(), editor.getOffsetY());
        renderingFlag = true;
        try {
            drawView(canvas);
        } finally {
            renderingFlag = false;
        }
        canvas.restoreToCount(saveCount);
    }

    public void onSizeChanged(int width, int height) {
        viewRect.right = width;
        viewRect.bottom = height;
    }

    Paint getPaint() {
        return paintGeneral;
    }

    Paint getPaintOther() {
        return paintOther;
    }

    Paint getPaintGraph() {
        return paintGraph;
    }

    void setCachedLineNumberWidth(int width) {
        cachedGutterWidth = width;
    }

    public RectF getVerticalScrollBarRect() {
        return verticalScrollBarRect;
    }

    public RectF getHorizontalScrollBarRect() {
        return horizontalScrollBarRect;
    }

    public void setHorizontalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        horizontalScrollbarThumbDrawable = drawable;
    }

    @Nullable
    public Drawable getHorizontalScrollbarThumbDrawable() {
        return horizontalScrollbarThumbDrawable;
    }

    public void setHorizontalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        horizontalScrollbarTrackDrawable = drawable;
    }

    @Nullable
    public Drawable getHorizontalScrollbarTrackDrawable() {
        return horizontalScrollbarTrackDrawable;
    }

    public void setVerticalScrollbarThumbDrawable(@Nullable Drawable drawable) {
        this.verticalScrollbarThumbDrawable = drawable;
    }

    @Nullable
    public Drawable getVerticalScrollbarThumbDrawable() {
        return verticalScrollbarThumbDrawable;
    }

    public void setVerticalScrollbarTrackDrawable(@Nullable Drawable drawable) {
        verticalScrollbarTrackDrawable = drawable;
    }

    @Nullable
    public Drawable getVerticalScrollbarTrackDrawable() {
        return verticalScrollbarTrackDrawable;
    }

    public void setTextSizePxDirect(float size) {
        paintGeneral.setTextSizeWrapped(size);
        paintOther.setTextSize(size);
        paintGraph.setTextSize(size * editor.getProps().functionCharacterSizeFactor);
        metricsText = paintGeneral.getFontMetricsInt();
        metricsLineNumber = paintOther.getFontMetricsInt();
        metricsGraph = paintGraph.getFontMetricsInt();
        editor.getRenderContext().invalidateRenderNodes();
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
        paintGeneral.setTypefaceWrapped(typefaceText);
        metricsText = paintGeneral.getFontMetricsInt();
        editor.getRenderContext().invalidateRenderNodes();
        updateTimestamp();
        editor.createLayout();
        editor.invalidate();
    }

    public void setTypefaceLineNumber(Typeface typefaceLineNumber) {
        if (typefaceLineNumber == null) {
            typefaceLineNumber = Typeface.MONOSPACE;
        }
        paintOther.setTypeface(typefaceLineNumber);
        metricsLineNumber = paintOther.getFontMetricsInt();
        editor.invalidate();
    }

    public void setTextScaleX(float textScaleX) {
        paintGeneral.setTextScaleX(textScaleX);
        paintOther.setTextScaleX(textScaleX);
        onTextStyleUpdate();
    }

    public void setLetterSpacing(float letterSpacing) {
        paintGeneral.setLetterSpacing(letterSpacing);
        paintOther.setLetterSpacing(letterSpacing);
        onTextStyleUpdate();
    }

    protected void onTextStyleUpdate() {
        paintGeneral.setRenderFunctionCharacters(editor.isRenderFunctionCharacters());
        metricsGraph = paintGraph.getFontMetricsInt();
        metricsLineNumber = paintOther.getFontMetricsInt();
        metricsText = paintGeneral.getFontMetricsInt();
        editor.getRenderContext().invalidateRenderNodes();
        updateTimestamp();
        editor.createLayout();
        editor.invalidate();
    }

    /**
     * Update timestamp required for measuring cache
     */
    protected void updateTimestamp() {
        displayTimestamp = SystemClock.elapsedRealtimeNanos();
    }

    protected void prepareLine(int line) {
        lineBuf = getLine(line);
    }

    protected ContentLine getLine(int line) {
        if (!renderingFlag) {
            return getLineDirect(line);
        }
        var line2 = preloadedLines.get(line);
        if (line2 == null) {
            line2 = content.getLine(line);
            preloadedLines.put(line, line2);
        }
        return line2;
    }

    protected Directions getLineDirections(int line) {
        if (!renderingFlag) {
            return content.getLineDirections(line);
        }
        var line2 = preloadedDirections.get(line);
        if (line2 == null) {
            line2 = content.getLineDirections(line);
            preloadedDirections.put(line, line2);
        }
        return line2;
    }

    ContentLine getLineDirect(int line) {
        return content.getLine(line);
    }

    int getColumnCount(int line) {
        return getLine(line).length();
    }

    // draw methods

    @RequiresApi(29)
    public void updateLineDisplayList(RenderNode renderNode, int line, Spans.Reader spans) {
        float widthLine = drawSingleTextLine(null, line, 0f, 0f, spans, false);
        renderNode.setPosition(0, 0, (int) (widthLine + 0.5f), editor.getRowHeight());
        var canvas = renderNode.beginRecording();
        try {
            drawSingleTextLine(canvas, line, 0f, 0f, spans, false);
        } finally {
            renderNode.endRecording();
        }
    }

    @UnsupportedUserUsage
    public TextRow createTextRow(int rowIndex) {
        var styles = editor.getStyles();
        var spanMap = styles != null ? styles.spans : null;
        var spanReader = spanMap != null ? spanMap.read() : null;
        spanReader = spanReader == null ? EmptyReader.getInstance() : spanReader;
        var row = editor.getLayout().getRowAt(rowIndex);
        var line = content.getLine(row.lineIndex);
        TextRow tr = new TextRow();
        var cache = editor.getRenderContext().getCache().queryMeasureCache(row.lineIndex);
        var widths = cache != null && cache.getUpdateTimestamp() >= displayTimestamp ? cache.getWidths() : null;
        widths = widths != null && widths.getSize() > line.length() ? widths : null;
        tr.set(line, row.startColumn, row.endColumn, spanReader.getSpansOnLine(row.lineIndex), row.inlayHints, content.getLineDirections(row.lineIndex), paintGeneral, widths, createTextRowParams());
        applySelectedTextRange(tr, row.lineIndex);
        return tr;
    }

    private void applySelectedTextRange(TextRow tr, int lineIndex) {
        if (cursor.isSelected() && lineIndex >= cursor.getLeftLine() && lineIndex <= cursor.getRightLine()) {
            int startColInLine = lineIndex == cursor.getLeftLine() ? cursor.getLeftColumn() : 0;
            int endColInLine = lineIndex == cursor.getRightLine() ? cursor.getRightColumn() : lineBuf.length();
            startColInLine = Math.max(tr.getTextStart(), startColInLine);
            endColInLine = Math.min(tr.getTextEnd(), endColInLine);
            if (startColInLine < endColInLine) {
                tr.setSelectedRange(startColInLine, endColInLine);
            }
        }
    }

    protected float drawSingleTextLine(Canvas canvas, int line, float offsetX, float offsetY, Spans.Reader spans, boolean visibleOnly) {
        prepareLine(line);
        int columnCount = getColumnCount(line);
        if (spans == null || spans.getSpanCount() <= 0) {
            spans = EmptyReader.getInstance();
        }
        TextRow tr = new TextRow();
        var inlayHints = editor.getInlayHints();
        List<InlayHint> lineInlays = inlayHints == null ? Collections.emptyList() : inlayHints.getForLine(line);
        var cache = editor.getRenderContext().getCache().queryMeasureCache(line);
        var widths = cache != null && cache.getUpdateTimestamp() >= displayTimestamp ? cache.getWidths() : null;
        widths = widths != null && widths.getSize() > lineBuf.length() ? widths : null;
        tr.set(lineBuf, 0, columnCount, spans.getSpansOnLine(line), lineInlays, getLineDirections(line), paintGeneral, widths, createTextRowParams());
        applySelectedTextRange(tr, line);
        if (canvas != null) {
            canvas.save();
            canvas.translate(offsetX, editor.getRowTop(0) + offsetY);
            if (visibleOnly) {
                float visibleStart = Math.max(0f, -offsetX);
                float visibleEnd = Math.max(visibleStart, -offsetX + editor.getWidth());
                tr.draw(canvas, visibleStart, visibleEnd);
            } else {
                tr.draw(canvas, 0f, Float.MAX_VALUE);
            }
            canvas.restore();
        }
        return canvas == null ? tr.computeRowWidth() : 0f;
    }

    public boolean hasSideHintIcons() {
        Styles styles;
        if ((styles = editor.getStyles()) != null) {
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
        cursor.updateCache(editor.getFirstVisibleLine());

        EditorColorScheme color = editor.getColorScheme();
        drawColor(canvas, color.getColor(EditorColorScheme.WHOLE_BACKGROUND), viewRect);

        float lineNumberWidth = editor.measureLineNumber(); // include line number margin
        var sideIconWidth = hasSideHintIcons() ? editor.getRowHeight() : 0f;
        float offsetX = -editor.getOffsetX() + editor.measureTextRegionOffset();
        float textOffset = offsetX;

        var gutterWidth = (int) (lineNumberWidth + sideIconWidth + editor.getDividerWidth() + editor.getDividerMarginLeft() + editor.getDividerMarginRight());
        if (editor.isWordwrap()) {
            if (cachedGutterWidth == 0) {
                cachedGutterWidth = gutterWidth;
            } else if (cachedGutterWidth != gutterWidth && !editor.getEventHandler().isScaling) {
                cachedGutterWidth = gutterWidth;
                editor.postInLifecycle(editor::requestLayoutIfNeeded);
                editor.createLayout(false);
            } else if (forcedRecreateLayout) {
                editor.createLayout();
                editor.postInLifecycle(editor::requestLayoutIfNeeded);
            }
        } else {
            cachedGutterWidth = 0;
            if (forcedRecreateLayout) {
                editor.createLayout();
            }
        }
        forcedRecreateLayout = false;

        prepareLines(editor.getFirstVisibleLine(), editor.getLastVisibleLine());
        buildMeasureCacheForLines(editor.getFirstVisibleLine(), editor.getLastVisibleLine(), displayTimestamp, true);
        var stuckLines = getStuckCodeBlocks();

        if (cursor.isSelected()) {
            editor.getInsertHandleDescriptor().setEmpty();
        } else {
            editor.getLeftHandleDescriptor().setEmpty();
            editor.getRightHandleDescriptor().setEmpty();
        }

        boolean lineNumberNotPinned = editor.isLineNumberEnabled() && (editor.isWordwrap() || !editor.isLineNumberPinned());

        var postDrawLineNumbers = this.postDrawLineNumbers;
        postDrawLineNumbers.clear();
        var postDrawCurrentLines = this.postDrawCurrentLines;
        postDrawCurrentLines.clear();
        List<DrawCursorTask> postDrawCursor = new ArrayList<>(3);
        MutableInt firstLn = editor.isFirstLineNumberAlwaysVisible() && editor.isWordwrap() ? new MutableInt(-1) : null;

        canvas.save();
        float stuckLineBottom = getStuckLineBottom(stuckLines);
        canvas.clipRect(0, stuckLineBottom, editor.getWidth(), editor.getHeight());
        drawRows(canvas, textOffset, postDrawLineNumbers, postDrawCursor, postDrawCurrentLines, firstLn);
        patchHighlightedDelimiters(canvas, textOffset);
        drawDiagnosticIndicators(canvas, offsetX);
        canvas.restore();

        offsetX = -editor.getOffsetX();

        int currentLineNumber = cursor.isSelected() ? -1 : cursor.getLeftLine();

        if (lineNumberNotPinned) {
            drawLineNumberBackground(canvas, offsetX, lineNumberWidth + sideIconWidth + editor.getDividerMarginLeft(), color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));
            int lineNumberColor = editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER);
            int currentLineBgColor = editor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
            if (editor.getCursorAnimator().isRunning() && editor.isHighlightCurrentLine() && editor.isEditable()) {
                tmpRect.bottom = editor.getCursorAnimator().animatedLineBottom() - editor.getOffsetY();
                tmpRect.top = tmpRect.bottom - editor.getCursorAnimator().animatedLineHeight();
                tmpRect.left = 0;
                tmpRect.right = (int) (textOffset - editor.getDividerMarginRight());
                drawColor(canvas, currentLineBgColor, tmpRect);
            }

            canvas.save();
            canvas.clipRect(0, stuckLineBottom, editor.getWidth(), editor.getHeight());
            for (int i = 0; i < postDrawCurrentLines.count(); i++) {
                drawRowBackground(canvas, currentLineBgColor, postDrawCurrentLines.get(i), (int) (textOffset - editor.getDividerMarginRight()));
            }
            // User defined gutter background
            drawUserGutterBackground(canvas, (int) (textOffset - editor.getDividerMarginRight()));
            drawSideIcons(canvas, offsetX + lineNumberWidth);
            canvas.restore();

            drawDivider(canvas, offsetX + lineNumberWidth + sideIconWidth + editor.getDividerMarginLeft(), color.getColor(EditorColorScheme.LINE_DIVIDER));

            canvas.save();
            canvas.clipRect(0, stuckLineBottom, editor.getWidth(), editor.getHeight());
            if (firstLn != null && firstLn.value != -1) {
                int bottom = editor.getRowBottom(0);
                float y;
                if (postDrawLineNumbers.size() == 0 || editor.getRowTop(IntPair.getSecond(postDrawLineNumbers.get(0))) - editor.getOffsetY() > bottom) {
                    // Free to draw at first line
                    y = (editor.getRowBottom(0) + editor.getRowTop(0)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent;
                } else {
                    int row = IntPair.getSecond(postDrawLineNumbers.get(0));
                    y = (editor.getRowBottom(row - 1) + editor.getRowTop(row - 1)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.getOffsetY();
                }
                paintOther.setTextAlign(editor.getLineNumberAlign());
                paintOther.setColor(firstLn.value == currentLineNumber ? color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) : lineNumberColor);
                var text = Integer.toString(firstLn.value + 1);
                switch (editor.getLineNumberAlign()) {
                    case LEFT:
                        canvas.drawText(text, offsetX, y, paintOther);
                        break;
                    case RIGHT:
                        canvas.drawText(text, offsetX + lineNumberWidth, y, paintOther);
                        break;
                    case CENTER:
                        canvas.drawText(text, offsetX + (lineNumberWidth + editor.getDividerMarginLeft()) / 2f, y, paintOther);
                }
            }
            for (int i = 0; i < postDrawLineNumbers.size(); i++) {
                long packed = postDrawLineNumbers.get(i);
                drawLineNumber(canvas, IntPair.getFirst(packed), IntPair.getSecond(packed), offsetX, lineNumberWidth, IntPair.getFirst(packed) == currentLineNumber ? color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) : lineNumberColor);
            }
            canvas.restore();
        }

        if (editor.isBlockLineEnabled()) {
            canvas.save();
            canvas.clipRect(0, stuckLineBottom, editor.getWidth(), editor.getHeight());
            if (editor.isWordwrap()) {
                drawSideBlockLine(canvas);
            } else {
                drawBlockLines(canvas, textOffset);
            }
            canvas.restore();
        }

        if (!editor.getCursorAnimator().isRunning()) {
            for (var action : postDrawCursor) {
                action.execute(canvas);
            }
        } else {
            drawSelectionOnAnimation(canvas);
        }

        drawStuckLines(canvas, stuckLines, textOffset);

        if (editor.isLineNumberEnabled() && !lineNumberNotPinned) {
            drawLineNumberBackground(canvas, 0, lineNumberWidth + sideIconWidth + editor.getDividerMarginLeft(), color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));

            canvas.save();
            canvas.clipRect(0, stuckLineBottom, editor.getWidth(), editor.getHeight());
            int lineNumberColor = editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER);
            int currentLineBgColor = editor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
            if (editor.getCursorAnimator().isRunning() && editor.isHighlightCurrentLine() && editor.isEditable()) {
                tmpRect.bottom = editor.getCursorAnimator().animatedLineBottom() - editor.getOffsetY();
                tmpRect.top = tmpRect.bottom - editor.getCursorAnimator().animatedLineHeight();
                tmpRect.left = 0;
                tmpRect.right = (int) (textOffset - editor.getDividerMarginRight());
                drawColor(canvas, currentLineBgColor, tmpRect);
            }
            for (int i = 0; i < postDrawCurrentLines.count(); i++) {
                drawRowBackground(canvas, currentLineBgColor, postDrawCurrentLines.get(i), (int) (textOffset - editor.getDividerMarginRight() + editor.getOffsetX()));
            }
            drawUserGutterBackground(canvas, (int) (textOffset - editor.getDividerMarginRight() + editor.getOffsetX()));
            drawSideIcons(canvas, lineNumberWidth);
            canvas.restore();

            drawDivider(canvas, lineNumberWidth + sideIconWidth + editor.getDividerMarginLeft(), color.getColor(EditorColorScheme.LINE_DIVIDER));

            canvas.save();
            canvas.clipRect(0, stuckLineBottom, editor.getWidth(), editor.getHeight());
            for (int i = 0; i < postDrawLineNumbers.size(); i++) {
                long packed = postDrawLineNumbers.get(i);
                drawLineNumber(canvas, IntPair.getFirst(packed), IntPair.getSecond(packed), 0, lineNumberWidth, IntPair.getFirst(packed) == currentLineNumber ? color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) : lineNumberColor);
            }
            canvas.restore();
        }

        drawStuckLineNumbers(canvas, stuckLines, offsetX, lineNumberWidth, editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER));
        drawScrollBars(canvas);
        drawEdgeEffect(canvas);

        releasePreloadedData();
        lastStuckLines = stuckLines;
        drawFormatTip(canvas);
    }

    protected void drawUserGutterBackground(Canvas canvas, int right) {
        int firstVis = editor.getFirstVisibleLine(), lastVis = editor.getLastVisibleLine();
        for (int line = firstVis; line <= lastVis; line++) {
            var bg = getUserGutterBackgroundForLine(line);
            if (bg != null) {
                var bgColor = bg.resolve(editor.getColorScheme());
                var top = (int) (editor.getLayout().getCharLayoutOffset(line, 0)[0] / editor.getRowHeight()) - 1;
                var count = editor.getLayout().getRowCountForLine(line);
                for (int i = 0; i < count; i++) {
                    drawRowBackground(canvas, bgColor, top + i, right);
                }
            }
        }
    }

    protected void drawStuckLineNumbers(Canvas canvas, List<CodeBlock> candidates, float offset, float lineNumberWidth, int lineNumberColor) {
        if (candidates == null || candidates.isEmpty() || !editor.isLineNumberEnabled()) {
            return;
        }
        var cursor = editor.getCursor();
        var currentLine = cursor.isSelected() ? -1 : cursor.getLeftLine();
        canvas.save();
        var offsetY = editor.getOffsetY();
        canvas.translate(0, offsetY);
        for (int i = 0; i < candidates.size(); i++) {
            var block = candidates.get(i);
            var line = block.startLine;
            var bg = getUserGutterBackgroundForLine(line);
            var color = bg != null ? bg.resolve(editor.getColorScheme()) : 0;
            var bottomOffset = editor.getRowBottom(i);
            var endLineTop = editor.getRowTop(block.endLine) - editor.getOffsetY();
            var shouldTranslate = endLineTop < bottomOffset && endLineTop >= bottomOffset - editor.getRowHeight();
            if (shouldTranslate) {
                canvas.save();
                canvas.clipRect(0, editor.getRowTop(i) - offsetY, editor.getWidth(), editor.getHeight());
                canvas.translate(0, endLineTop - bottomOffset);
            }
            if (currentLine == line || color != 0) {
                tmpRect.top = editor.getRowTop(i) - offsetY;
                tmpRect.bottom = editor.getRowBottom(i) - offsetY - editor.getDpUnit();
                tmpRect.left = editor.isLineNumberPinned() ? 0 : offset;
                tmpRect.right = tmpRect.left + editor.measureTextRegionOffset();
                if (currentLine == line && editor.isHighlightCurrentLine())
                    drawColor(canvas, editor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE), tmpRect);
                if (color != 0)
                    drawColor(canvas, color, tmpRect);
            }
            drawLineNumber(canvas, line, i,
                    editor.isLineNumberPinned() ? 0 : offset, lineNumberWidth,
                    currentLine == line ? editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_CURRENT) : lineNumberColor);
            if (shouldTranslate) {
                canvas.restore();
            }
        }
        canvas.restore();
    }

    protected float getStuckLineBottom(List<CodeBlock> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0f;
        }
        float bottomOffset = 0f;
        var offsetLine = 0;
        var previousLine = -1;
        for (int i = 0; i < candidates.size(); i++) {
            var block = candidates.get(i);
            if (block.startLine > previousLine) {
                bottomOffset = editor.getRowBottom(offsetLine);
                var endLineTop = editor.getRowTop(block.endLine) - editor.getOffsetY();
                var shouldTranslate = endLineTop < bottomOffset && endLineTop >= bottomOffset - editor.getRowHeight();
                if (shouldTranslate) {
                    bottomOffset += endLineTop - bottomOffset;
                }
                previousLine = block.startLine;
                offsetLine++;
            }
        }
        return bottomOffset;
    }

    protected void drawStuckLines(Canvas canvas, List<CodeBlock> candidates, float offset) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        var styles = editor.getStyles();
        var spanMap = styles != null ? styles.spans : null;
        var spanReader = spanMap != null ? spanMap.read() : null;
        var previousLine = -1;
        var offsetLine = 0;
        var cursor = editor.getCursor();
        var currentLine = cursor.isSelected() ? -1 : cursor.getLeftLine();
        float bottomOffset = 0f;
        for (int i = 0; i < candidates.size(); i++) {
            var block = candidates.get(i);
            if (block.startLine > previousLine) {
                tmpRect.top = editor.getRowTop(offsetLine);
                bottomOffset = tmpRect.bottom = editor.getRowBottom(offsetLine);
                tmpRect.left = offset;
                tmpRect.right = editor.getWidth();
                var endLineTop = editor.getRowTop(block.endLine) - editor.getOffsetY();
                var shouldTranslate = endLineTop < tmpRect.bottom && endLineTop >= tmpRect.top;
                if (shouldTranslate) {
                    canvas.save();
                    canvas.clipRect(0, tmpRect.top, editor.getWidth(), editor.getHeight());
                    canvas.translate(0, endLineTop - tmpRect.bottom);
                    bottomOffset += endLineTop - tmpRect.bottom;
                }
                var colorId = EditorColorScheme.WHOLE_BACKGROUND;
                if (block.startLine == currentLine && editor.isHighlightCurrentLine()) {
                    colorId = EditorColorScheme.CURRENT_LINE;
                }
                drawColor(canvas, editor.getColorScheme().getColor(colorId), tmpRect);
                if (canvas.isHardwareAccelerated() && editor.isHardwareAcceleratedDrawAllowed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        && editor.getRenderContext().getRenderNodeHolder() != null && !editor.getEventHandler().isScaling &&
                        (editor.getProps().cacheRenderNodeForLongLines || getLine(block.startLine).length() < 128)) {
                    editor.getRenderContext().getRenderNodeHolder().drawLineHardwareAccelerated(canvas, block.startLine, offset, offsetLine * editor.getRowHeight());
                } else {
                    try {
                        if (spanReader != null) {
                            spanReader.moveToLine(block.startLine);
                        }
                        drawSingleTextLine(canvas, block.startLine, offset, offsetLine * editor.getRowHeight(), spanReader, true);
                    } finally {
                        if (spanReader != null) {
                            spanReader.moveToLine(-1);
                        }
                    }
                }
                previousLine = block.startLine;
                offsetLine++;
                if (shouldTranslate) {
                    canvas.restore();
                }
            }
        }
        if (bottomOffset > 0f) {
            tmpRect.top = bottomOffset - editor.getDpUnit();
            tmpRect.bottom = bottomOffset;
            tmpRect.left = 0;
            tmpRect.right = editor.getWidth();
            var shadow = (editor.getProps().stickyLineIndicator & DirectAccessProps.STICKY_LINE_INDICATOR_SHADOW) != 0;
            var showLine = (editor.getProps().stickyLineIndicator & DirectAccessProps.STICKY_LINE_INDICATOR_LINE) != 0;
            if (!shadow && !showLine) {
                return;
            }
            var lineColor = editor.getColorScheme().getColor(EditorColorScheme.STICKY_SCROLL_DIVIDER);
            showLine = lineColor != 0;
            if (shadow) {
                canvas.save();
                canvas.clipRect(0, showLine ? tmpRect.top : tmpRect.bottom, editor.getWidth(), editor.getHeight());
                paintGeneral.setShadowLayer(editor.getDpUnit() * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP, 0, 0, Color.BLACK);
            }
            var color = !showLine && shadow ? Color.BLACK : lineColor;
            drawColor(canvas, color, tmpRect);
            if (shadow) {
                paintGeneral.setShadowLayer(0, 0, 0, 0);
                canvas.restore();
            }
        }
    }

    protected void drawHardwrapMarker(Canvas canvas, float offset) {
        int column = editor.getProps().hardwrapColumn;
        if (!editor.isWordwrap() && column > 0) {
            tmpRect.left = offset + paintGeneral.measureText("a") * column;
            tmpRect.right = tmpRect.left + editor.getDpUnit() * 2f;
            tmpRect.top = 0f;
            tmpRect.bottom = viewRect.bottom;
            drawColor(canvas, editor.getColorScheme().getColor(EditorColorScheme.HARD_WRAP_MARKER), tmpRect);
        }
    }

    protected void drawSideIcons(Canvas canvas, float offset) {
        if (!hasSideHintIcons()) {
            return;
        }
        var row = editor.getFirstVisibleRow();
        var itr = editor.getLayout().obtainRowIterator(row);
        final var iconSizeFactor = editor.getProps().sideIconSizeFactor;
        var size = (int) (editor.getRowHeight() * iconSizeFactor);
        var offsetToLeftTop = (int) (editor.getRowHeight() * (1 - iconSizeFactor) / 2f);
        while (row <= editor.getLastVisibleRow() && itr.hasNext()) {
            var rowInf = itr.next();
            if (rowInf.isLeadingRow) {
                var hint = getLineStyle(rowInf.lineIndex, LineSideIcon.class);
                if (hint != null) {
                    var drawable = hint.getDrawable();
                    var rect = new Rect(0, 0, size, size);
                    rect.offsetTo((int) offset + offsetToLeftTop, editor.getRowTop(row) - editor.getOffsetY() + offsetToLeftTop);
                    drawable.setBounds(rect);
                    drawable.draw(canvas);
                }
            }
            row++;
        }
    }

    protected void drawFormatTip(Canvas canvas) {
        if (editor.isFormatting()) {
            String text = editor.getFormatTip();
            float baseline = editor.getRowBaseline(0);
            float rightX = editor.getWidth();
            paintGeneral.setColor(editor.getColorScheme().getColor(EditorColorScheme.TEXT_NORMAL));
            paintGeneral.setFakeBoldText(true);
            paintGeneral.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(text, rightX, baseline, paintGeneral);
            paintGeneral.setTextAlign(Paint.Align.LEFT);
            paintGeneral.setFakeBoldText(false);
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
            paintGeneral.setColor(color);
            canvas.drawRect(rect, paintGeneral);
        }
    }

    /**
     * Draw rect on screen in a round rectangle
     * Will not do anything if color is zero
     *
     * @param canvas Canvas to draw
     * @param color  Color of rect
     * @param rect   Rect to draw
     */
    protected void drawColorRound(Canvas canvas, int color, RectF rect) {
        if (color != 0) {
            paintGeneral.setColor(color);
            canvas.drawRoundRect(rect, rect.height() * RenderingConstants.ROUND_RECT_FACTOR, rect.height() * RenderingConstants.ROUND_RECT_FACTOR, paintGeneral);
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
            paintGeneral.setColor(color);
            canvas.drawRect(rect, paintGeneral);
        }
    }

    /**
     * Draw background for whole row
     */
    protected void drawRowBackground(Canvas canvas, int color, int row) {
        drawRowBackground(canvas, color, row, viewRect.right);
    }

    protected void drawRowBackground(Canvas canvas, int color, int row, int right) {
        tmpRect.top = editor.getRowTop(row) - editor.getOffsetY();
        tmpRect.bottom = editor.getRowBottom(row) - editor.getOffsetY();
        tmpRect.left = 0;
        tmpRect.right = right;
        drawColor(canvas, color, tmpRect);
    }

    /**
     * Draw single line number
     */
    protected void drawLineNumber(Canvas canvas, int line, int row, float offsetX, float width, int color) {
        if (width + offsetX <= 0) {
            return;
        }
        if (paintOther.getTextAlign() != editor.getLineNumberAlign()) {
            paintOther.setTextAlign(editor.getLineNumberAlign());
        }
        paintOther.setColor(color);
        // Line number center align to text center
        float y = (editor.getRowBottom(row) + editor.getRowTop(row)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.getOffsetY();

        var buffer = TemporaryCharBuffer.obtain(20);
        line++;
        int i = stringSize(line);
        Numbers.getChars(line, i, buffer);

        switch (editor.getLineNumberAlign()) {
            case LEFT:
                canvas.drawText(buffer, 0, i, offsetX, y, paintOther);
                break;
            case RIGHT:
                canvas.drawText(buffer, 0, i, offsetX + width, y, paintOther);
                break;
            case CENTER:
                canvas.drawText(buffer, 0, i, offsetX + (width + editor.getDividerMarginLeft()) / 2f, y, paintOther);
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
        tmpRect.bottom = editor.getHeight();
        tmpRect.top = 0;
        int offY = editor.getOffsetY();
        if (offY < 0) {
            tmpRect.bottom = tmpRect.bottom - offY;
            tmpRect.top = tmpRect.top - offY;
        }
        tmpRect.left = left;
        tmpRect.right = right;
        drawColor(canvas, color, tmpRect);
    }

    /**
     * Draw divider line
     *
     * @param canvas  Canvas to draw
     * @param offsetX End x of line number region
     * @param color   Color to draw divider
     */
    protected void drawDivider(Canvas canvas, float offsetX, int color) {
        boolean shadow = editor.isLineNumberPinned() && !editor.isWordwrap() && editor.getOffsetX() > 0;
        float right = offsetX + editor.getDividerWidth();
        if (right < 0) {
            return;
        }
        float left = Math.max(0f, offsetX);
        tmpRect.bottom = editor.getHeight();
        tmpRect.top = 0;
        int offY = editor.getOffsetY();
        if (offY < 0) {
            tmpRect.bottom = tmpRect.bottom - offY;
            tmpRect.top = tmpRect.top - offY;
        }
        tmpRect.left = left;
        tmpRect.right = right;
        if (shadow) {
            canvas.save();
            canvas.clipRect(tmpRect.left, tmpRect.top, editor.getWidth(), tmpRect.bottom);
            paintGeneral.setShadowLayer(Math.min(editor.getDpUnit() * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP, editor.getOffsetX()), 0, 0, Color.BLACK);
        }
        drawColor(canvas, color, tmpRect);
        if (shadow) {
            canvas.restore();
            paintGeneral.setShadowLayer(0, 0, 0, 0);
        }
    }

    private void prepareLines(int start, int end) {
        releasePreloadedData();
        content.runReadActionsOnLines(Math.max(0, start - 5), Math.min(content.getLineCount() - 1, end + 5), (int i, ContentLine line, Directions dirs) -> {
            preloadedLines.put(i, line);
            preloadedDirections.put(i, dirs);
        });
    }

    private void releasePreloadedData() {
        preloadedLines.clear();
        preloadedDirections.clear();
    }

    protected List<CodeBlock> getStuckCodeBlocks() {
        if (editor.isWordwrap() || !editor.getProps().stickyScroll) {
            return null;
        }
        Styles styles;
        int startLine = editor.getFirstVisibleLine();
        int offsetY = editor.getOffsetY(), rowHeight = editor.getRowHeight();
        List<CodeBlock> codeBlocks;
        if ((styles = editor.getStyles()) == null || (codeBlocks = styles.blocksByStart) == null) {
            return null;
        }
        int size = codeBlocks.size();
        List<CodeBlock> candidates = new ArrayList<>();
        var limit = editor.getProps().stickyScrollIterationLimit;
        var maxLine = content.getLineCount();
        for (int i = 0; i < size && i < limit; i++) {
            var block = codeBlocks.get(i);
            if (block == null || block.startLine > block.endLine ||
                    block.startLine > maxLine || block.endLine > maxLine ||
                    block.startLine < 0) {
                continue;
            }
            if (block.startLine > startLine) {
                break;
            }
            if (block.endLine > startLine && editor.getRowTop(block.startLine) - offsetY < 0) {
                candidates.add(block);
                startLine++;
                offsetY += rowHeight;
            }
        }
        var maxLines = editor.getProps().stickyScrollMaxLines;
        if (candidates.size() > maxLines) {
            if (maxLines <= 0) {
                return null;
            }
            if (editor.getProps().stickyScrollPreferInnerScope) {
                candidates = candidates.subList(candidates.size() - maxLines, candidates.size());
            } else {
                candidates = candidates.subList(0, maxLines);
            }
        }
        if (editor.getCursor().isSelected() && editor.getProps().stickyScrollAutoCollapse) {
            var limitLine = editor.getCursor().getLeftLine();
            var firstVis = editor.getFirstVisibleLine();
            int lastSelectionLine = editor.getCursor().getRightLine();
            if (lastSelectionLine >= firstVis) {
                while (!candidates.isEmpty() && firstVis + candidates.size() >= limitLine) {
                    candidates.remove(candidates.size() - 1);
                }
            }
        }
        return candidates;
    }

    private final LineStyles coordinateLine = new LineStyles(0);

    @Nullable
    protected LineStyles getLineStyles(int line) {
        Styles styles;
        List<LineStyles> lineStylesList;
        if ((styles = editor.getStyles()) == null || (lineStylesList = styles.lineStyles) == null) {
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

    @Nullable
    protected ResolvableColor getUserGutterBackgroundForLine(int line) {
        var bg = getLineStyle(line, LineGutterBackground.class);
        if (bg != null) {
            return bg.getColor();
        }
        return null;
    }

    /**
     * Draw current line background during animation
     */
    protected void drawAnimatedCurrentLineBackground(Canvas canvas, int currentLineBgColor) {
        tmpRect.bottom = editor.getCursorAnimator().animatedLineBottom() - editor.getOffsetY();
        tmpRect.top = tmpRect.bottom - editor.getCursorAnimator().animatedLineHeight();
        tmpRect.left = 0;
        tmpRect.right = viewRect.right;
        drawColor(canvas, currentLineBgColor, tmpRect);
    }

    public TextRowParams createTextRowParams() {
        return new TextRowParams(editor.getTabWidth(), getTextMetrics(), editor.getRowTopOfText(0),
                editor.getRowBottomOfText(0), editor.getRowHeightOfText(), editor.getRowBaseline(0),
                editor.getRowTop(0), editor.getRowBottom(0),
                editor.getRowHeight(), editor.getProps().roundTextBackgroundFactor,
                editor, editor.getColorScheme(), paintOther, paintGraph, metricsGraph);
    }

    /**
     * Draw rows with a {@link RowIterator}
     *
     * @param canvas              Canvas to draw
     * @param offset              Offset of text region start
     * @param postDrawLineNumbers Line numbers to be drawn later
     * @param postDrawCursor      Cursors to be drawn later
     */
    protected void drawRows(Canvas canvas, float offset, LongArrayList postDrawLineNumbers, List<DrawCursorTask> postDrawCursor, MutableIntList postDrawCurrentLines, MutableInt requiredFirstLn) {
        int firstVis = editor.getFirstVisibleRow();
        RowIterator rowIterator = editor.getLayout().obtainRowIterator(firstVis, preloadedLines);
        Spans spans = editor.getStyles() == null ? null : editor.getStyles().spans;
        var matchedPositions = this.matchedPositions;
        var highlightPositions = this.highlightPositions;
        matchedPositions.clear();
        highlightPositions.clear();
        int currentLine = cursor.isSelected() ? -1 : cursor.getLeftLine();
        int currentLineBgColor = editor.getColorScheme().getColor(EditorColorScheme.CURRENT_LINE);
        int currentRow = cursor.isSelected() ? -1 : editor.getLayout().getRowIndexForPosition(cursor.getLeft());
        int currentRowBorder = editor.getColorScheme().getColor(EditorColorScheme.CURRENT_ROW_BORDER);
        int lastPreparedLine = -1;
        int leadingWhitespaceEnd = 0;
        int trailingWhitespaceStart = 0;
        float circleRadius = 0f;
        float miniGraphWidth = editor.isWordwrap() && (editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0 ? getMiniGraphWidth() : 0f;
        var composingPosition = editor.inputConnection.composingText.isComposing() && editor.inputConnection.composingText.startIndex >= 0 && editor.inputConnection.composingText.startIndex < content.length() ? content.getIndexer().getCharPosition(editor.inputConnection.composingText.startIndex) : null;
        var composingLength = editor.inputConnection.composingText.endIndex - editor.inputConnection.composingText.startIndex;
        var draggingSelection = editor.getEventHandler().draggingSelection;
        if (editor.shouldInitializeNonPrintable()) {
            float spaceWidth = paintGeneral.getSpaceWidth();
            circleRadius = Math.min(editor.getRowHeight(), spaceWidth) * RenderingConstants.NON_PRINTABLE_CIRCLE_RADIUS_FACTOR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !editor.isWordwrap() && canvas.isHardwareAccelerated() && editor.isHardwareAcceleratedDrawAllowed()) {
            editor.getRenderContext().getRenderNodeHolder().keepCurrentInDisplay(firstVis, editor.getLastVisibleRow());
        }
        float offset2 = editor.getOffsetX() - editor.measureTextRegionOffset();

        // Step 1 - Draw background of rows

        // Pre-draw animated current line background
        if (editor.getCursorAnimator().isRunning() && editor.isHighlightCurrentLine() && editor.isEditable()
                && (editor.getProps().cursorLineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_CURSOR || editor.getProps().cursorLineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_MIXED)) {
            drawAnimatedCurrentLineBackground(canvas, currentLineBgColor);
        }
        // Draw custom line backgrounds & normal current line background
        for (int row = firstVis; row <= editor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
            Row rowInf = rowIterator.next();
            int line = rowInf.lineIndex;
            if (lastPreparedLine != line) {
                prepareLine(line);
                lastPreparedLine = line;
            }

            final var lineBgOverlapBehavior = editor.getProps().cursorLineBgOverlapBehavior;

            var drawCurrentLineBg = line == currentLine &&
                    !editor.getCursorAnimator().isRunning() &&
                    editor.isHighlightCurrentLine() &&
                    editor.isEditable();

            final var drawCustomLineBg = !drawCurrentLineBg
                    || (editor.getProps().drawCustomLineBgOnCurrentLine && lineBgOverlapBehavior != CURSOR_LINE_BG_OVERLAP_CUSTOM);

            var isOverlapping = false;

            if (drawCustomLineBg) {
                // Draw custom background
                var customBackground = getUserBackgroundForLine(line);
                if (customBackground != null) {
                    var color = customBackground.resolve(editor.getColorScheme());
                    if (line == currentLine) {
                        isOverlapping = true;
                    }

                    drawRowBackground(canvas, color, row);
                }
            }

            if (isOverlapping) {
                drawCurrentLineBg &= lineBgOverlapBehavior != CURSOR_LINE_BG_OVERLAP_CURSOR;
            }

            if (drawCurrentLineBg) {
                int commitCurrentLineBg = currentLineBgColor;
                if (isOverlapping && lineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_MIXED) {
                    // alpha = 0.5f = 0.5 * 255 = 128 = 0x80
                    commitCurrentLineBg = (commitCurrentLineBg & 0x00FFFFFF) | 0x80000000;
                }

                // Draw current line background
                drawRowBackground(canvas, commitCurrentLineBg, row);
                postDrawCurrentLines.add(row);
            }
        }
        // Post-draw animated current line background
        if (editor.getCursorAnimator().isRunning() && editor.isHighlightCurrentLine()
                && editor.getProps().cursorLineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_CUSTOM) {
            drawAnimatedCurrentLineBackground(canvas, currentLineBgColor);
        }
        rowIterator.reset();

        // Other system line background are drawn last
        for (int row = firstVis; row <= editor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
            Row rowInf = rowIterator.next();
            canvas.save();
            canvas.translate(rowInf.renderTranslateX, 0f);
            int line = rowInf.lineIndex;
            int columnCount = getColumnCount(line);
            if (lastPreparedLine != line) {
                editor.computeMatchedPositions(line, matchedPositions);
                editor.computeHighlightPositions(line, highlightPositions);
                prepareLine(line);
                lastPreparedLine = line;
            }
            float paintingOffset = -offset2;
            if (!rowInf.isLeadingRow)
                paintingOffset += miniGraphWidth;

            // Draw matched text background
            if (matchedPositions.size() > 0) {
                for (int i = 0; i < matchedPositions.size(); i++) {
                    var position = matchedPositions.get(i);
                    var start = IntPair.getFirst(position);
                    var end = IntPair.getSecond(position);
                    drawRowRegionBackground(canvas, row, start, end, rowInf.startColumn,
                            rowInf.endColumn, editor.getColorScheme().getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND),
                            editor.getColorScheme().getColor(EditorColorScheme.MATCHED_TEXT_BORDER));
                }
            }

            // Draw highlight text background
            if (highlightPositions.getSize() > 0) {
                int finalRow = row;
                highlightPositions.forEach((position, colorPair) -> {
                    var start = IntPair.getFirst(position);
                    var end = IntPair.getSecond(position);
                    drawRowRegionBackground(canvas, finalRow, start, end, rowInf.startColumn,
                            rowInf.endColumn, IntPair.getFirst(colorPair), IntPair.getSecond(colorPair));
                    return null;
                });
            }

            // Draw selected text background
            if (cursor.isSelected() && line >= cursor.getLeftLine() && line <= cursor.getRightLine()) {
                int selectionStart = 0;
                int selectionEnd = columnCount;
                if (line == cursor.getLeftLine()) {
                    selectionStart = cursor.getLeftColumn();
                }
                if (line == cursor.getRightLine()) {
                    selectionEnd = cursor.getRightColumn();
                }
                if (getColumnCount(line) == 0 && line != cursor.getRightLine()) {
                    tmpRect.top = getRowTopForBackground(row) - editor.getOffsetY();
                    tmpRect.bottom = getRowBottomForBackground(row) - editor.getOffsetY();
                    tmpRect.left = paintingOffset;
                    tmpRect.right = tmpRect.left + paintGeneral.getSpaceWidth() * 2;
                    drawRowBackgroundRectWithBorder(canvas, tmpRect,
                            editor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND),
                            editor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BORDER));
                } else if (selectionStart < selectionEnd) {
                    drawRowRegionBackground(canvas, row, selectionStart, selectionEnd, rowInf.startColumn, rowInf.endColumn,
                            editor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND),
                            editor.getColorScheme().getColor(EditorColorScheme.SELECTED_TEXT_BORDER));
                }
            }
            canvas.restore();

            // Draw current row border
            if (row == currentRow && currentRowBorder != 0) {
                tmpRect.top = editor.getRowTop(row) - editor.getOffsetY();
                tmpRect.bottom = editor.getRowBottom(row) - editor.getOffsetY();
                tmpRect.left = Math.max(0, -offset2);
                tmpRect.right = editor.getWidth();
                paintGeneral.setColor(currentRowBorder);
                paintGeneral.setStyle(android.graphics.Paint.Style.STROKE);
                paintGeneral.setStrokeWidth(editor.getDpUnit());
                canvas.drawRect(tmpRect, paintGeneral);
                paintGeneral.setStyle(android.graphics.Paint.Style.FILL);
            }
        }
        rowIterator.reset();

        // Background of snippets
        patchSnippetRegions(canvas, offset);

        // Hard wrap marker
        drawHardwrapMarker(canvas, offset);

        // Step 2 - Draw text and text decorations
        Spans.Reader reader = null;
        lastPreparedLine = -1;
        TextAdvancesCache lineCache = null;
        for (int row = firstVis; row <= editor.getLastVisibleRow() && rowIterator.hasNext(); row++) {
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
                var cache = editor.getRenderContext().getCache().queryMeasureCache(line);
                if (cache != null && cache.getUpdateTimestamp() == displayTimestamp && cache.getWidths() != null && cache.getWidths().getSize() > columnCount) {
                    lineCache = cache.getWidths();
                } else {
                    lineCache = null;
                }
                prepareLine(line);
                // Release old reader
                if (reader != null) {
                    try {
                        reader.moveToLine(-1);
                    } catch (Exception e) {
                        Log.w(LOG_TAG, "Failed to release SpanReader", e);
                    }
                }
                // Get new reader and lock
                // Note that we should hold the reader during the **text line** rendering process
                // Otherwise, the spans of that line can be changed during the inter rendering time
                // between two **rows** because the spans could have been changed concurrently
                // See #290
                reader = spans == null ? EmptyReader.getInstance() : spans.read();
                try {
                    reader.moveToLine(line);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Failed to read span", e);
                    reader = EmptyReader.getInstance();
                }
                if (reader.getSpanCount() == 0) {
                    // Unacceptable span count, use fallback reader
                    reader = EmptyReader.getInstance();
                }
                if (editor.shouldInitializeNonPrintable()) {
                    long positions = editor.findLeadingAndTrailingWhitespacePos(lineBuf);
                    leadingWhitespaceEnd = IntPair.getFirst(positions);
                    trailingWhitespaceStart = IntPair.getSecond(positions);
                }
            }

            // Get visible region on the line
            float paintingOffset = -offset2;
            float offsetCopy = offset2;

            paintingOffset += rowInf.renderTranslateX;
            offsetCopy -= rowInf.renderTranslateX;

            if (!rowInf.isLeadingRow) {
                if ((editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                    drawMiniGraph(canvas, offset, row, softwrapLeftGraph);
                    paintingOffset += miniGraphWidth;
                    offsetCopy -= miniGraphWidth;
                }
            }

            float backupOffset = paintingOffset;
            int nonPrintableFlags = editor.getNonPrintablePaintingFlags();

            // Draw text here
            if (!editor.isHardwareAcceleratedDrawAllowed()
                    || editor.getEventHandler().isScaling ||
                    !canvas.isHardwareAccelerated() || editor.isWordwrap() ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    || (rowInf.endColumn - rowInf.startColumn > 128 && !editor.getProps().cacheRenderNodeForLongLines) /* Save memory */) {
                // Draw without hardware acceleration
                TextRow tr = new TextRow();
                tr.set(lineBuf, rowInf.startColumn, rowInf.endColumn, reader.getSpansOnLine(line), rowInf.inlayHints, getLineDirections(line), paintGeneral, lineCache, createTextRowParams());
                applySelectedTextRange(tr, line);

                canvas.save();
                canvas.translate(-offsetCopy, editor.getRowTop(row) - editor.getOffsetY());
                // visible editor window: [offsetX, offsetX+editorWidth]
                // current row window region: [textRegionOffsetW+leftMiniGraphWidth, textRegionOffsetX+leftMiniGraphX+rowWidth]
                // shifted start at offsetX-(textRegionOffsetX+leftMiniGraphWidth)
                // visible in-row offset from max{offsetX, textRegionOffsetX+leftMiniGraphWidth} - (textRegionOffsetX+leftMiniGraphWidth)
                // to min{textRegionOffsetX+leftMiniGraphX+rowWidth, offsetX+editorWidth-(textRegionOffsetX+leftMiniGraphWidth)}
                float beginOffset = Math.max(0, offsetCopy);
                float endOffset = beginOffset + editor.getWidth();
                var result = tr.draw(canvas, beginOffset, endOffset);
                canvas.restore();

                var exhausted = IntPair.getFirst(result) == 1;
                paintingOffset += IntPair.getSecondAsFloat(result);

                // Draw hard wrap & soft wrap
                if (exhausted && rowInf.isTrailingRow && (nonPrintableFlags & CodeEditor.FLAG_DRAW_LINE_SEPARATOR) != 0) {
                    drawMiniGraph(canvas, paintingOffset, row, lineBreakGraph);
                } else if (!rowInf.isTrailingRow && editor.isWordwrap() && (nonPrintableFlags & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                    drawMiniGraph(canvas, paintingOffset, row, softwrapRightGraph);
                }
            } else {
                paintingOffset = offset + editor.getRenderContext().getRenderNodeHolder().drawLineHardwareAccelerated(canvas, line, offset, editor.getRowTop(row) - editor.getOffsetY());
                // Draw hard wrap
                if (rowInf.isTrailingRow && (nonPrintableFlags & CodeEditor.FLAG_DRAW_LINE_SEPARATOR) != 0) {
                    drawMiniGraph(canvas, paintingOffset, row, lineBreakGraph);
                }
            }

            // Recover the offset
            paintingOffset = backupOffset;

            // Draw non-printable characters
            if (circleRadius != 0f && (leadingWhitespaceEnd != columnCount || (nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE) != 0)) {
                TextRow tr = new TextRow();
                tr.set(lineBuf, rowInf.startColumn, rowInf.endColumn, reader.getSpansOnLine(line), rowInf.inlayHints, getLineDirections(line), paintGeneral, lineCache, createTextRowParams());
                canvas.save();
                canvas.translate(paintingOffset, editor.getRowTopOfText(row) - editor.getOffsetY());
                bufferedDrawPoints.setOffsets(paintingOffset, editor.getRowTopOfText(row) - editor.getOffsetY());
                float beginOffset = Math.max(0, paintingOffset);
                float endOffset = beginOffset + editor.getWidth();
                final var wsLeadingEnd = leadingWhitespaceEnd;
                final var wsTrailingStart = trailingWhitespaceStart;

                paintOther.setColor(editor.getColorScheme().getColor(EditorColorScheme.NON_PRINTABLE_CHAR));
                tr.iterateDrawTextRegions(rowInf.startColumn, rowInf.endColumn, canvas, beginOffset, endOffset, false,
                        (Canvas _canvas, char[] text, int index, int count, int contextIndex, int contextCount, boolean isRtl,
                         float horizontalOffset, float width, TextRowParams params, Span span) -> {
                            if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_LEADING) != 0) {
                                drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, 0, wsLeadingEnd);
                            }
                            if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_INNER) != 0) {
                                drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, wsLeadingEnd, wsTrailingStart);
                            }
                            if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING) != 0) {
                                drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, wsTrailingStart, columnCount);
                            }
                            if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION) != 0 && cursor.isSelected() && line >= cursor.getLeftLine() && line <= cursor.getRightLine()) {
                                int selectionStart = 0;
                                int selectionEnd = columnCount;
                                if (line == cursor.getLeftLine()) {
                                    selectionStart = cursor.getLeftColumn();
                                }
                                if (line == cursor.getRightLine()) {
                                    selectionEnd = cursor.getRightColumn();
                                }
                                if ((nonPrintableFlags & 0b1110) == 0) {
                                    drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, selectionStart, selectionEnd);
                                } else {
                                    if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_LEADING) == 0) {
                                        drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, selectionStart, Math.min(wsLeadingEnd, selectionEnd));
                                    }
                                    if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_INNER) == 0) {
                                        drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, Math.max(wsLeadingEnd, selectionStart), Math.min(wsTrailingStart, selectionEnd));
                                    }
                                    if ((nonPrintableFlags & CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING) == 0) {
                                        drawWhitespaces(_canvas, tr, text, index, count, contextIndex, contextCount, isRtl, horizontalOffset, width, Math.max(wsTrailingStart, selectionStart), selectionEnd);
                                    }
                                }
                            }
                        });
                canvas.restore();
                bufferedDrawPoints.setOffsets(0, 0);
            }

            // Draw composing text underline
            if (composingPosition != null && line == composingPosition.line) {
                int composingStart = composingPosition.column;
                int composingEnd = composingStart + composingLength;
                int paintStart = Math.min(Math.max(composingStart, rowInf.startColumn), rowInf.endColumn);
                int paintEnd = Math.min(Math.max(composingEnd, rowInf.startColumn), rowInf.endColumn);

                if (paintStart < paintEnd) {
                    TextRow tr = new TextRow();
                    tr.set(lineBuf, rowInf.startColumn, rowInf.endColumn, reader.getSpansOnLine(line), rowInf.inlayHints, content.getLineDirections(line), paintGeneral, lineCache, createTextRowParams());
                    tmpRect.top = editor.getRowBottom(row) - editor.getOffsetY();
                    tmpRect.bottom = tmpRect.top + editor.getRowHeight() * 0.06f;
                    var finalOffset = paintingOffset;
                    tr.iterateBackgroundRegions(paintStart, paintEnd, false, false, (left, right) -> {
                        tmpRect.left = finalOffset + left;
                        tmpRect.right = finalOffset + right;
                        if (tmpRect.left > 0f)
                            drawColor(canvas, editor.getColorScheme().getColor(EditorColorScheme.UNDERLINE), tmpRect);
                        return tmpRect.right < editor.getWidth();
                    });
                }
            }

            final var layout = editor.getLayout();
            // Draw cursors
            if (cursor.isSelected()) {
                if (cursor.getLeftLine() == line && isInside(cursor.getLeftColumn(), rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    float centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(cursor.getLeftLine(), cursor.getLeftColumn())[1] - editor.getOffsetX();
                    var type = content.isRtlAt(cursor.getLeftLine(), cursor.getLeftColumn()) ? SelectionHandleStyle.HANDLE_TYPE_RIGHT : SelectionHandleStyle.HANDLE_TYPE_LEFT;
                    var task = new DrawCursorTask(centerX, getRowBottomForBackground(row) - editor.getOffsetY(), type, editor.getLeftHandleDescriptor());
                    postDrawCursor.add(task);
                    applyBidiIndicatorAttrs(task, cursor.getLeftLine(), cursor.getLeftColumn());
                }
                if (cursor.getRightLine() == line && isInside(cursor.getRightColumn(), rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    float centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(cursor.getRightLine(), cursor.getRightColumn())[1] - editor.getOffsetX();
                    var type = content.isRtlAt(cursor.getRightLine(), cursor.getRightColumn()) ? SelectionHandleStyle.HANDLE_TYPE_LEFT : SelectionHandleStyle.HANDLE_TYPE_RIGHT;
                    var task = new DrawCursorTask(centerX, getRowBottomForBackground(row) - editor.getOffsetY(), type, editor.getRightHandleDescriptor());
                    postDrawCursor.add(task);
                    applyBidiIndicatorAttrs(task, cursor.getRightLine(), cursor.getRightColumn());
                }
            } else if (cursor.getLeftLine() == line && isInside(cursor.getLeftColumn(), rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                float centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(cursor.getLeftLine(), cursor.getLeftColumn())[1] - editor.getOffsetX();
                var task = new DrawCursorTask(centerX, getRowBottomForBackground(row) - editor.getOffsetY(), SelectionHandleStyle.HANDLE_TYPE_INSERT, editor.getInsertHandleDescriptor());
                postDrawCursor.add(task);
                applyBidiIndicatorAttrs(task, cursor.getLeftLine(), cursor.getLeftColumn());
            }
            // Draw dragging selection or selecting target
            if (draggingSelection != null) {
                if (draggingSelection.line == line && isInside(draggingSelection.column, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    float centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(draggingSelection.line, draggingSelection.column)[1] - editor.getOffsetX();
                    var task = new DrawCursorTask(centerX, getRowBottomForBackground(row) - editor.getOffsetY(), SelectionHandleStyle.HANDLE_TYPE_UNDEFINED, null);
                    postDrawCursor.add(task);
                    applyBidiIndicatorAttrs(task, draggingSelection.line, draggingSelection.column);
                }
            } else if (editor.isInMouseMode() && editor.isTextSelected()) {
                var target = editor.getSelectingTarget();
                if (target != null && target.line == line && isInside(target.column, rowInf.startColumn, rowInf.endColumn, rowInf.isTrailingRow)) {
                    float centerX = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(target.line, target.column)[1] - editor.getOffsetX();
                    var task = new DrawCursorTask(centerX, getRowBottomForBackground(row) - editor.getOffsetY(), SelectionHandleStyle.HANDLE_TYPE_UNDEFINED, null);
                    postDrawCursor.add(task);
                    applyBidiIndicatorAttrs(task, target.line, target.column);
                }
            }
        }

        // Release last used reader object
        if (reader != null) {
            try {
                reader.moveToLine(-1);
            } catch (Exception e) {
                Log.w(LOG_TAG, "Failed to release SpanReader", e);
            }
        }

        paintGeneral.setFakeBoldText(false);
        paintGeneral.setTextSkewX(0);
        paintOther.setStrokeWidth(circleRadius * 2);
        bufferedDrawPoints.commitPoints(canvas, paintOther);
    }

    private long getBidiIndicatorAttrs(int line, int column) {
        var lineDirections = getLineDirections(line);
        int count = lineDirections.getRunCount();
        if (count == 1) {
            // Simple LTR/RTL Run
            return IntPair.pack(0, lineDirections.isRunRtl(0) ? 1 : 0);
        }
        for (int i = 0; i < count; i++) {
            if (i + 1 == count || lineDirections.getRunStart(i) <= column && column < lineDirections.getRunEnd(i)) {
                return IntPair.pack(editor.getProps().showBidiDirectionIndicator ? 1 : 0, lineDirections.isRunRtl(i) ? 1 : 0);
            }
        }
        return IntPair.pack(0, 0);
    }

    private void applyBidiIndicatorAttrs(DrawCursorTask task, int line, int column) {
        var bidiAttrs = getBidiIndicatorAttrs(line, column);
        task.setBidiIndicatorRequired(IntPair.getFirst(bidiAttrs) == 1);
        task.setRightToLeft(IntPair.getSecond(bidiAttrs) == 1);
    }

    private void drawBidiSelectionIndicator(Canvas canvas, float x, float topY, float selectionHeight, boolean isRtl) {
        float height = selectionHeight * 0.2f;
        float deltaX = height * 0.866f; // sqrt(3)/ 2
        tmpPath.reset();
        tmpPath.moveTo(x, topY);
        tmpPath.lineTo(x + (isRtl ? -deltaX : deltaX), topY + height / 2f);
        tmpPath.lineTo(x, topY + height);
        tmpPath.close();
        canvas.drawPath(tmpPath, paintGeneral);
    }

    protected void drawDiagnosticIndicator(Canvas canvas, DiagnosticIndicatorStyle style, int i, float startX, float endX) {
        final float waveLength = editor.getDpUnit() * editor.getProps().indicatorWaveLength;
        final float amplitude = editor.getDpUnit() * editor.getProps().indicatorWaveAmplitude;
        final float waveWidth = editor.getDpUnit() * editor.getProps().indicatorWaveWidth;
        // Draw
        float centerY = editor.getRowBottom(i) - editor.getOffsetY();
        switch (style) {
            case WAVY_LINE: {
                var lineWidth = 0 - startX;
                var waveCount = (int) Math.ceil(lineWidth / waveLength);
                var phi = lineWidth < 0 ? 0f : (waveLength * waveCount - lineWidth);
                lineWidth = endX - startX;
                canvas.save();
                canvas.clipRect(startX, 0, endX, canvas.getHeight());
                canvas.translate(startX, centerY);
                tmpPath.reset();
                tmpPath.moveTo(0, 0);
                waveCount = (int) Math.ceil((phi + lineWidth) / waveLength);
                for (int j = 0; j < waveCount; j++) {
                    tmpPath.quadTo(waveLength * j + waveLength / 4, amplitude, waveLength * j + waveLength / 2, 0);
                    tmpPath.quadTo(waveLength * j + waveLength * 3 / 4, -amplitude, waveLength * j + waveLength, 0);
                }
                // Draw path
                paintOther.setStrokeWidth(waveWidth);
                paintOther.setStyle(Paint.Style.STROKE);
                canvas.drawPath(tmpPath, paintOther);
                canvas.restore();
                paintOther.setStyle(Paint.Style.FILL);
                break;
            }
            case LINE: {
                paintOther.setStrokeWidth(waveWidth);
                canvas.drawLine(startX, centerY, endX, centerY, paintOther);
                break;
            }
            case DOUBLE_LINE: {
                paintOther.setStrokeWidth(waveWidth / 3f);
                canvas.drawLine(startX, centerY, endX, centerY, paintOther);
                canvas.drawLine(startX, centerY - waveWidth, endX, centerY - waveWidth, paintOther);
                break;
            }
        }
    }

    protected void drawDiagnosticIndicators(Canvas canvas, float offset) {
        var diagnosticsContainer = editor.getDiagnostics();
        var style = editor.getDiagnosticIndicatorStyle();
        if (diagnosticsContainer != null && style != DiagnosticIndicatorStyle.NONE && style != null) {
            var text = content;
            var firstVisRow = editor.getFirstVisibleRow();
            var lastVisRow = editor.getLastVisibleRow();
            var firstIndex = text.getCharIndex(editor.getFirstVisibleLine(), 0);
            var lastLine = Math.min(text.getLineCount() - 1, editor.getLastVisibleLine() + 1);
            var lastIndex = text.getCharIndex(lastLine, 0) + text.getColumnCount(lastLine);
            diagnosticsContainer.queryInRegion(collectedDiagnostics, firstIndex, lastIndex);
            if (collectedDiagnostics.isEmpty()) {
                return;
            }
            var start = new CharPosition();
            var end = new CharPosition();
            var indexer = cursor.getIndexer();
            for (var region : collectedDiagnostics) {
                var startIndex = Math.max(firstIndex, region.startIndex);
                var endIndex = Math.min(lastIndex, region.endIndex);
                indexer.getCharPosition(startIndex, start);
                indexer.getCharPosition(endIndex, end);
                var startRow = editor.getLayout().getRowIndexForPosition(startIndex);
                var endRow = editor.getLayout().getRowIndexForPosition(endIndex);
                // Setup color
                var colorId = (region.severity >= 0 && region.severity <= 3) ? sDiagnosticsColorMapping[region.severity] : 0;
                if (colorId == 0) {
                    break;
                }
                paintOther.setColor(editor.getColorScheme().getColor(colorId));
                int visStartRow = Math.max(firstVisRow, startRow);
                int visEndRow = Math.min(lastVisRow, endRow);
                for (int i = visStartRow; i <= visEndRow; i++) {
                    var row = editor.getLayout().getRowAt(i);
                    var tr = createTextRow(i);
                    int startColumn = i == startRow ? start.column : row.startColumn;
                    int endColumn = i == endRow ? end.column : row.endColumn;
                    float finalOffset;
                    if (editor.isWordwrap() && !row.isLeadingRow && (editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                        finalOffset = offset + row.renderTranslateX + getMiniGraphWidth();
                    } else {
                        finalOffset = offset + row.renderTranslateX;
                    }
                    if (startColumn == endColumn) {
                        // Make it always visible
                        var startX = finalOffset + tr.getCursorOffsetForIndex(startColumn);
                        var endX = startX + paintGeneral.measureText("a");
                        drawDiagnosticIndicator(canvas, style, i, startX, endX);
                    } else {
                        final int rowIndex = i;
                        tr.iterateBackgroundRegions(startColumn, endColumn, false, false, (left, right) -> {
                            if (right > 0f)
                                drawDiagnosticIndicator(canvas, style, rowIndex, finalOffset + left, finalOffset + right);
                            return finalOffset + right < editor.getWidth();
                        });
                    }
                }
            }
        }
        collectedDiagnostics.clear();
    }

    /**
     * Draw non-printable characters
     */
    private void drawWhitespaces(Canvas canvas, TextRow tr, char[] chars, int index, int count, int contextIndex, int contextCount, boolean isRtl, float horizontalOffset, float width, int min, int max) {
        int paintStart = Math.max(index, Math.min(index + count, min));
        int paintEnd = Math.max(index, Math.min(index + count, max));

        if (paintStart < paintEnd) {
            float spaceWidth = paintGeneral.getSpaceWidth();
            float rowCenter = (editor.getRowHeightOfText() / 2f + editor.getRowTopOfText(0));
            float offset = isRtl ? horizontalOffset + width : horizontalOffset;
            while (paintStart < paintEnd) {
                char ch = chars[paintStart];
                int paintCount = 0;
                boolean paintLine = false;
                if (ch == ' ' || ch == '\t') {
                    float advance = tr.measureAdvanceInRun(paintStart, index, paintStart, contextIndex, contextIndex + contextCount, isRtl);
                    offset = isRtl ? horizontalOffset + width - advance : horizontalOffset + advance;
                }
                if (ch == ' ') {
                    paintCount = 1;
                } else if (ch == '\t') {
                    if ((editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_TAB_SAME_AS_SPACE) != 0) {
                        paintCount = editor.getTabWidth();
                    } else {
                        paintLine = true;
                    }
                }
                for (int i = 0; i < paintCount; i++) {
                    float charStartOffset = offset + spaceWidth * i;
                    float charEndOffset = charStartOffset + spaceWidth;
                    float centerOffset = (charStartOffset + charEndOffset) / 2f;
                    if (isRtl) {
                        centerOffset -= spaceWidth;
                    }
                    bufferedDrawPoints.drawPoint(centerOffset, rowCenter);
                }
                if (paintLine) {
                    var charWidth = editor.getTabWidth() * spaceWidth;
                    float delta = charWidth * 0.05f;
                    float rtlDelta = isRtl ? -charWidth : 0f;
                    canvas.drawLine(offset + delta + rtlDelta, rowCenter, offset + charWidth + rtlDelta - delta, rowCenter, paintOther);
                }

                if (ch == ' ' || ch == '\t') {
                    float charWidth = (ch == ' ' ? spaceWidth : spaceWidth * editor.getTabWidth());
                    offset += isRtl ? -charWidth : charWidth;
                }
                paintStart++;
            }
        }
    }

    public float getMiniGraphWidth() {
        float height = editor.getRowHeightOfText() * editor.getProps().miniMarkerSizeFactor;
        var graph = editor.getContext().getDrawable(R.drawable.line_break);
        if (graph == null) {
            return 0;
        }
        int w = graph.getIntrinsicWidth(), h = graph.getIntrinsicHeight();
        if (w <= 0 || h <= 0 || height <= 0) {
            return 0f;
        }
        return height * ((float) w / h);
    }

    /**
     * Draw small characters as graph
     */
    protected void drawMiniGraph(Canvas canvas, float offset, int row, Drawable graph) {
        float graphBottom = row == -1 ? (editor.getRowBottomOfText(0)) : (editor.getRowBottomOfText(row) - editor.getOffsetY());
        float height = editor.getRowHeightOfText() * editor.getProps().miniMarkerSizeFactor;
        if (height <= 0 || graph == null) {
            return;
        }
        int w = graph.getIntrinsicWidth(), h = graph.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float width = height * ((float) w / h);
        graph.setColorFilter(editor.getColorScheme().getColor(EditorColorScheme.NON_PRINTABLE_CHAR), PorterDuff.Mode.SRC_ATOP);
        graph.setBounds((int) offset, (int) (graphBottom - height), (int) (offset + width), (int) graphBottom);
        graph.draw(canvas);
    }

    protected int getRowTopForBackground(int row) {
        if (!editor.getProps().textBackgroundWrapTextOnly) {
            return editor.getRowTop(row);
        } else {
            return editor.getRowTopOfText(row);
        }
    }

    protected int getRowBottomForBackground(int row) {
        if (!editor.getProps().textBackgroundWrapTextOnly) {
            return editor.getRowBottom(row);
        } else {
            return editor.getRowBottomOfText(row);
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
    protected void drawRowRegionBackground(Canvas canvas, int row, int highlightStart, int highlightEnd, int rowStart, int rowEnd, int color, int borderColor) {
        highlightStart = Math.max(highlightStart, rowStart);
        highlightEnd = Math.min(highlightEnd, rowEnd);
        if (highlightStart < highlightEnd) {
            tmpRect.top = getRowTopForBackground(row) - editor.getOffsetY();
            tmpRect.bottom = getRowBottomForBackground(row) - editor.getOffsetY();
            float offset = editor.measureTextRegionOffset() - editor.getOffsetX();
            if (editor.isWordwrap() && !editor.getLayout().getRowAt(row).isLeadingRow && (editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0) {
                offset += getMiniGraphWidth();
            }
            float finalOffset = offset;
            var tr = createTextRow(row);
            var width = editor.getWidth();
            tr.iterateBackgroundRegions(highlightStart, highlightEnd, false, false, (left, right) -> {
                tmpRect.left = finalOffset + left;
                tmpRect.right = finalOffset + right;
                if (tmpRect.right < 0 || tmpRect.left > width) {
                    return false;
                }
                drawRowBackgroundRectWithBorder(canvas, tmpRect, color, borderColor);
                return true;
            });
        }
    }

    protected void drawRowBackgroundRectWithBorder(Canvas canvas, RectF rect, int backgroundColor, int borderColor) {
        paintGeneral.setColor(backgroundColor);
        drawRowBackgroundRect(canvas, rect);
        if (borderColor == 0) {
            return;
        }
        paintGeneral.setColor(borderColor);
        paintGeneral.setStyle(android.graphics.Paint.Style.STROKE);
        paintGeneral.setStrokeWidth(editor.getTextBorderWidth());
        drawRowBackgroundRect(canvas, rect);
        paintGeneral.setStyle(android.graphics.Paint.Style.FILL);
    }

    protected void drawRowBackgroundRect(Canvas canvas, RectF rect) {
        drawRowBackgroundRect(canvas, rect, paintGeneral);
    }

    protected void drawRowBackgroundRect(Canvas canvas, RectF rect, Paint p) {
        if (editor.getProps().enableRoundTextBackground) {
            canvas.drawRoundRect(rect, editor.getRowHeight() * editor.getProps().roundTextBackgroundFactor, editor.getRowHeight() * editor.getProps().roundTextBackgroundFactor, p);
        } else {
            canvas.drawRect(rect, p);
        }
    }

    /**
     * Is inside the region
     *
     * @param index Index to test
     * @param start Start of region
     * @param end   End of region
     * @return true if cursor should be drawn in this row
     */
    private boolean isInside(int index, int start, int end, boolean isLastRow) {
        // Due not to draw duplicate cursors for a single one
        if (index == end && !isLastRow) {
            return false;
        }
        return index >= start && index <= end;
    }

    public long getTimestamp() {
        return displayTimestamp;
    }

    public android.graphics.Paint.FontMetricsInt getLineNumberMetrics() {
        return metricsLineNumber;
    }

    public android.graphics.Paint.FontMetricsInt getTextMetrics() {
        return metricsText;
    }

    /**
     * Draw effect of edges
     *
     * @param canvas The canvas to draw
     */
    protected void drawEdgeEffect(Canvas canvas) {
        boolean postDraw = false;
        var verticalEdgeEffect = editor.getVerticalEdgeEffect();
        var horizontalEdgeEffect = editor.getHorizontalEdgeEffect();
        if (!verticalEdgeEffect.isFinished()) {
            boolean bottom = editor.getEventHandler().glowTopOrBottom;
            if (bottom) {
                canvas.save();
                canvas.translate(-editor.getMeasuredWidth(), editor.getMeasuredHeight());
                canvas.rotate(180, editor.getMeasuredWidth(), 0);
            }
            postDraw = verticalEdgeEffect.draw(canvas);
            if (bottom) {
                canvas.restore();
            }
        }
        if (editor.isWordwrap()) {
            horizontalEdgeEffect.finish();
        }
        if (!horizontalEdgeEffect.isFinished()) {
            canvas.save();
            boolean right = editor.getEventHandler().glowLeftOrRight;
            if (right) {
                canvas.rotate(90);
                canvas.translate(0, -editor.getMeasuredWidth());
            } else {
                canvas.translate(0, editor.getMeasuredHeight());
                canvas.rotate(-90);
            }
            postDraw = horizontalEdgeEffect.draw(canvas) || postDraw;
            canvas.restore();
        }
        var scroller = editor.getScroller();
        if (scroller.isOverScrolled()) {
            if (verticalEdgeEffect.isFinished() && (scroller.getCurrY() < 0 || scroller.getCurrY() > editor.getScrollMaxY())) {
                editor.getEventHandler().glowTopOrBottom = scroller.getCurrY() >= editor.getScrollMaxY();
                verticalEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
                postDraw = true;
            }
            if (horizontalEdgeEffect.isFinished() && (scroller.getCurrX() < 0 || scroller.getCurrX() > editor.getScrollMaxX())) {
                editor.getEventHandler().glowLeftOrRight = scroller.getCurrX() >= editor.getScrollMaxX();
                horizontalEdgeEffect.onAbsorb((int) scroller.getCurrVelocity());
                postDraw = true;
            }
        }
        if (postDraw) {
            editor.postInvalidate();
        }
    }

    /**
     * Draw code block lines on screen
     *
     * @param canvas  The canvas to draw
     * @param offsetX The start x offset for text
     */
    protected void drawBlockLines(Canvas canvas, float offsetX) {
        final var styles = editor.getStyles();
        List<CodeBlock> blocks = styles == null ? null : styles.blocks;
        var indentMode = styles != null && styles.isIndentCountMode();
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        int first = editor.getFirstVisibleRow();
        int last = editor.getLastVisibleRow();
        boolean mark = false;
        int invalidCount = 0;
        int maxCount = styles.getSuppressSwitch();
        int mm = editor.binarySearchEndBlock(first, blocks);
        if (mm == -1) {
            mm = 0;
        }
        int cursorIdx = editor.getCurrentCursorBlock();
        for (int curr = mm; curr < blocks.size(); curr++) {
            CodeBlock block = blocks.get(curr);
            if (block == null) {
                continue;
            }
            if (CodeEditor.hasVisibleRegion(block.startLine, block.endLine, first, last)) {
                try {
                    var lineContent = getLine(block.endLine);
                    float offsetEnd = indentMode ? paintGeneral.getSpaceWidth() * block.endColumn : createTextRow(block.endLine).getCursorOffsetForIndex(Math.min(block.endColumn, lineContent.length()));
                    lineContent = getLine(block.startLine);
                    float offsetStart = indentMode ? paintGeneral.getSpaceWidth() * block.startColumn : createTextRow(block.startLine).getCursorOffsetForIndex(Math.min(block.startColumn, lineContent.length()));
                    float offset = Math.min(offsetEnd, offsetStart);
                    float centerX = offset + offsetX;
                    tmpRect.top = Math.max(0, editor.getRowBottom(block.startLine) - editor.getOffsetY());
                    tmpRect.bottom = Math.min(editor.getHeight(), (block.toBottomOfEndLine ? editor.getRowBottom(block.endLine) : editor.getRowTop(block.endLine)) - editor.getOffsetY());
                    tmpRect.left = centerX - editor.getDpUnit() * editor.getBlockLineWidth() / 2;
                    tmpRect.right = centerX + editor.getDpUnit() * editor.getBlockLineWidth() / 2;
                    drawColor(canvas, editor.getColorScheme().getColor(curr == cursorIdx ? EditorColorScheme.BLOCK_LINE_CURRENT : EditorColorScheme.BLOCK_LINE), tmpRect);
                } catch (IndexOutOfBoundsException e) {
                    // Ignored
                    // Because the exception usually occurs when the content is changed.
                }
                mark = true;
            } else if (mark) {
                if (invalidCount >= maxCount) break;
                invalidCount++;
            }
        }
    }

    protected void drawSideBlockLine(Canvas canvas) {
        if (!editor.getProps().drawSideBlockLine) {
            return;
        }
        List<CodeBlock> blocks = editor.getStyles() == null ? null : editor.getStyles().blocks;
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        var current = editor.getCurrentCursorBlock();
        if (current >= 0 && current < blocks.size()) {
            var block = blocks.get(current);
            var layout = editor.getLayout();
            try {
                float top = layout.getCharLayoutOffset(block.startLine, block.startColumn)[0] - editor.getRowHeight() - editor.getOffsetY();
                float bottom = layout.getCharLayoutOffset(block.endLine, block.endColumn)[0] - editor.getOffsetY();
                float left = editor.measureLineNumber();
                float right = left + editor.getDividerMarginLeft();
                float center = (left + right) / 2 - editor.getOffsetX();
                paintGeneral.setColor(editor.getColorScheme().getColor(EditorColorScheme.SIDE_BLOCK_LINE));
                paintGeneral.setStrokeWidth(editor.getDpUnit() * editor.getBlockLineWidth());
                canvas.drawLine(center, top, center, bottom, paintGeneral);
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
        verticalScrollBarRect.setEmpty();
        horizontalScrollBarRect.setEmpty();
        if (!editor.getEventHandler().shouldDrawScrollBarForTouch() && !(editor.isInMouseMode() && editor.getProps().mouseModeAlwaysShowScrollbars)) {
            return;
        }
        var percentage = editor.getEventHandler().getScrollBarFadeOutPercentageForTouch();
        if (editor.isInMouseMode() && editor.getProps().mouseModeAlwaysShowScrollbars) {
            percentage = 0f;
        }
        var size = editor.getDpUnit() * RenderingConstants.SCROLLBAR_WIDTH_DIP;
        if (editor.isHorizontalScrollBarEnabled() && !editor.isWordwrap() && editor.getScrollMaxX() > editor.getWidth() * 3 / 4) {
            canvas.save();
            canvas.translate(0f, size * percentage);

            drawScrollBarTrackHorizontal(canvas);
            drawScrollBarHorizontal(canvas);

            canvas.restore();
        }
        if (editor.isVerticalScrollBarEnabled() && editor.getScrollMaxY() > editor.getHeight() / 2) {
            canvas.save();
            canvas.translate(size * percentage, 0f);

            drawScrollBarTrackVertical(canvas);
            drawScrollBarVertical(canvas);

            canvas.restore();
        }
    }

    /**
     * Draw vertical scroll bar track
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarTrackVertical(Canvas canvas) {
        if (editor.getEventHandler().holdVerticalScrollBar()) {
            tmpRect.right = editor.getWidth();
            tmpRect.left = editor.getWidth() - editor.getDpUnit() * RenderingConstants.SCROLLBAR_WIDTH_DIP;
            tmpRect.top = 0;
            tmpRect.bottom = editor.getHeight();
            if (verticalScrollbarTrackDrawable != null) {
                verticalScrollbarTrackDrawable.setBounds((int) tmpRect.left, (int) tmpRect.top, (int) tmpRect.right, (int) tmpRect.bottom);
                verticalScrollbarTrackDrawable.draw(canvas);
            } else {
                drawColor(canvas, editor.getColorScheme().getColor(EditorColorScheme.SCROLL_BAR_TRACK), tmpRect);
            }
        }
    }

    /**
     * Draw vertical scroll bar
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarVertical(Canvas canvas) {
        int height = editor.getHeight();
        float all = editor.getScrollMaxY() + height;
        float length = Math.max(height / all * height, editor.getDpUnit() * RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP);
        float topY = editor.getOffsetY() * 1.0f / editor.getScrollMaxY() * (height - length);
        if (editor.getEventHandler().holdVerticalScrollBar()) {
            drawLineInfoPanel(canvas, topY, length);
        }
        tmpRect.right = editor.getWidth();
        tmpRect.left = editor.getWidth() - editor.getDpUnit() * RenderingConstants.SCROLLBAR_WIDTH_DIP;
        tmpRect.top = topY;
        tmpRect.bottom = topY + length;
        verticalScrollBarRect.set(tmpRect);
        if (verticalScrollbarThumbDrawable != null) {
            verticalScrollbarThumbDrawable.setState(editor.getEventHandler().holdVerticalScrollBar() ? PRESSED_DRAWABLE_STATE : DEFAULT_DRAWABLE_STATE);
            verticalScrollbarThumbDrawable.setBounds((int) tmpRect.left, (int) tmpRect.top, (int) tmpRect.right, (int) tmpRect.bottom);
            verticalScrollbarThumbDrawable.draw(canvas);
        } else {
            drawColor(canvas, editor.getColorScheme().getColor(editor.getEventHandler().holdVerticalScrollBar() ? EditorColorScheme.SCROLL_BAR_THUMB_PRESSED : EditorColorScheme.SCROLL_BAR_THUMB), tmpRect);
        }
    }

    /**
     * Draw line number panel
     *
     * @param canvas Canvas to draw
     * @param topY   The y at the top of the vertical scrollbar
     * @param length The length of vertical scrollbar
     */
    protected void drawLineInfoPanel(Canvas canvas, float topY, float length) {
        if (!editor.isDisplayLnPanel()) {
            return;
        }
        int mode = editor.getLnPanelPositionMode();
        int position = editor.getLnPanelPosition();
        String text = editor.getLineNumberTipTextProvider().getCurrentText(editor);
        float backupSize = paintGeneral.getTextSize();
        paintGeneral.setTextSize(editor.getLineInfoTextSize());
        Paint.FontMetricsInt backupMetrics = metricsText;
        metricsText = paintGeneral.getFontMetricsInt();
        float expand = editor.getDpUnit() * 8;
        float textWidth = paintGeneral.measureText(text);
        float baseline;
        float textOffset = 0f;
        if (mode == LineInfoPanelPositionMode.FIXED) {
            tmpRect.top = editor.getHeight() / 2f - editor.getRowHeight() / 2f - expand;
            tmpRect.bottom = editor.getHeight() / 2f + editor.getRowHeight() / 2f + expand;
            tmpRect.left = editor.getWidth() / 2f - textWidth / 2f - expand;
            tmpRect.right = editor.getWidth() / 2f + textWidth / 2f + expand;
            baseline = editor.getHeight() / 2f + 2 * expand;
            float offset = 10 * editor.getDpUnit();
            if (position != LineInfoPanelPosition.CENTER) {
                if ((position | LineInfoPanelPosition.TOP) == position) {
                    tmpRect.top = offset;
                    tmpRect.bottom = offset + editor.getRowHeight() + 2 * expand;
                    baseline = offset + editor.getRowBaseline(0) + expand;
                }
                if ((position | LineInfoPanelPosition.BOTTOM) == position) {
                    tmpRect.top = editor.getHeight() - offset - 2 * expand - editor.getRowHeight();
                    tmpRect.bottom = editor.getHeight() - offset;
                    baseline = editor.getHeight() - editor.getRowHeight() + editor.getRowBaseline(0) - offset - expand;
                }
                if ((position | LineInfoPanelPosition.LEFT) == position) {
                    tmpRect.left = offset;
                    tmpRect.right = offset + 2 * expand + textWidth;
                }
                if ((position | LineInfoPanelPosition.RIGHT) == position) {
                    tmpRect.right = editor.getWidth() - offset;
                    tmpRect.left = editor.getWidth() - offset - expand * 2 - textWidth;
                }
            }
            drawColorRound(canvas, editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL), tmpRect);
        } else {
            float[] radii = null;
            tmpRect.right = editor.getWidth() - 30 * editor.getDpUnit();
            tmpRect.left = editor.getWidth() - 30 * editor.getDpUnit() - expand * 2 - textWidth;
            if (position == LineInfoPanelPosition.TOP) {
                tmpRect.top = topY;
                tmpRect.bottom = topY + editor.getRowHeight() + 2 * expand;
                baseline = topY + editor.getRowBaseline(0) + expand;
                radii = new float[8];
                for (int i = 0; i < 8; i++) {
                    if (i != 5)
                        radii[i] = tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR;
                }
            } else if (position == LineInfoPanelPosition.BOTTOM) {
                tmpRect.top = topY + length - editor.getRowHeight() - 2 * expand;
                tmpRect.bottom = topY + length;
                baseline = topY + length - editor.getRowBaseline(0) / 2f;
                radii = new float[8];
                for (int i = 0; i < 8; i++) {
                    if (i != 3)
                        radii[i] = tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR;
                }
            } else {
                float centerY = topY + length / 2f;
                tmpRect.top = centerY - editor.getRowHeight() / 2f - expand;
                tmpRect.bottom = centerY + editor.getRowHeight() / 2f + expand;
                baseline = centerY - editor.getRowHeight() / 2f + editor.getRowBaseline(0);
            }
            if (radii != null) {
                tmpPath.reset();
                tmpPath.addRoundRect(tmpRect, radii, Path.Direction.CW);
            } else {
                tmpRect.offset(-expand, 0f);
                tmpRect.right += expand;
                textOffset = -expand / 2f;
                BubbleHelper.buildBubblePath(tmpPath, tmpRect);
            }
            paintGeneral.setColor(editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL));
            canvas.drawPath(tmpPath, paintGeneral);
        }
        float centerX = (tmpRect.left + tmpRect.right) / 2 + textOffset;
        paintGeneral.setColor(editor.getColorScheme().getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT));
        paintGeneral.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, centerX, baseline, paintGeneral);
        paintGeneral.setTextAlign(Paint.Align.LEFT);
        paintGeneral.setTextSize(backupSize);
        metricsText = backupMetrics;
    }

    /**
     * Draw horizontal scroll bar track
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarTrackHorizontal(Canvas canvas) {
        if (editor.getEventHandler().holdHorizontalScrollBar()) {
            tmpRect.set(0, editor.getHeight() - editor.getDpUnit() * RenderingConstants.SCROLLBAR_WIDTH_DIP, editor.getWidth(), editor.getHeight());
            if (horizontalScrollbarTrackDrawable != null) {
                horizontalScrollbarTrackDrawable.setBounds((int) tmpRect.left, (int) tmpRect.top, (int) tmpRect.right, (int) tmpRect.bottom);
                horizontalScrollbarTrackDrawable.draw(canvas);
            } else {
                drawColor(canvas, editor.getColorScheme().getColor(EditorColorScheme.SCROLL_BAR_TRACK), tmpRect);
            }
        }
    }

    protected void patchSnippetRegions(Canvas canvas, float textOffset) {
        var controller = editor.getSnippetController();
        if (controller.isInSnippet()) {
            var editing = controller.getEditingTabStop();
            if (editing != null) {
                patchTextRegionWithColor(canvas, textOffset, editing.getStartIndex(), editing.getEndIndex(), 0, editor.getColorScheme().getColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING), 0);
            }
            for (SnippetItem snippetItem : controller.getEditingRelatedTabStops()) {
                patchTextRegionWithColor(canvas, textOffset, snippetItem.getStartIndex(), snippetItem.getEndIndex(), 0, editor.getColorScheme().getColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED), 0);
            }
            for (SnippetItem snippetItem : controller.getInactiveTabStops()) {
                patchTextRegionWithColor(canvas, textOffset, snippetItem.getStartIndex(), snippetItem.getEndIndex(), 0, editor.getColorScheme().getColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE), 0);
            }
        }
    }

    protected void patchHighlightedDelimiters(Canvas canvas, float textOffset) {
        if (editor.inputConnection.composingText.isComposing() || !editor.getProps().highlightMatchingDelimiters || editor.getCursor().isSelected()) {
            return;
        }
        var paired = editor.styleDelegate.getFoundBracketPair();
        if (paired != null) {
            var color = editor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND);
            var backgroundColor = editor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND);
            var underlineColor = editor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE);
            var borderColor = editor.getColorScheme().getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER);
            var borderWidth = editor.getTextBorderWidth();
            if (isInvalidTextBounds(paired.leftIndex, paired.leftLength) || isInvalidTextBounds(paired.rightIndex, paired.rightLength)) {
                // Index out of bounds
                return;
            }

            boolean continuous = paired.leftIndex + paired.leftLength == paired.rightIndex;
            if (color != 0 || underlineColor != 0) {
                if (continuous) {
                    patchTextRegionWithColor(canvas, textOffset, paired.leftIndex, paired.rightIndex + paired.rightLength, color, backgroundColor, underlineColor);
                } else {
                    patchTextRegionWithColor(canvas, textOffset, paired.leftIndex, paired.leftIndex + paired.leftLength, color, backgroundColor, underlineColor);
                    patchTextRegionWithColor(canvas, textOffset, paired.rightIndex, paired.rightIndex + paired.rightLength, color, backgroundColor, underlineColor);
                }
                backgroundColor = 0;
            }
            if (backgroundColor != 0 || (borderColor != 0 && borderWidth > 0)) {
                if (continuous) {
                    patchTextBackgroundRegions(canvas, textOffset, paired.leftIndex, paired.rightIndex + paired.rightLength, backgroundColor, borderWidth, borderColor);
                } else {
                    patchTextBackgroundRegions(canvas, textOffset, paired.leftIndex, paired.leftIndex + paired.leftLength, backgroundColor, borderWidth, borderColor);
                    patchTextBackgroundRegions(canvas, textOffset, paired.rightIndex, paired.rightIndex + paired.rightLength, backgroundColor, borderWidth, borderColor);
                }
            }
        }
    }

    protected boolean isInvalidTextBounds(int index, int length) {
        return (index < 0 || length < 0 || index + length > content.length());
    }

    protected void patchTextRegionWithColor(Canvas canvas, float textOffset, int start, int end, int color, int backgroundColor, int underlineColor) {
        paintGeneral.setColor(color);
        paintOther.setStrokeWidth(editor.getRowHeightOfText() * RenderingConstants.MATCHING_DELIMITERS_UNDERLINE_WIDTH_FACTOR);

        var useBoldStyle = editor.getProps().boldMatchingDelimiters;
        paintGeneral.setStyle(useBoldStyle ? Paint.Style.FILL_AND_STROKE : Paint.Style.FILL);
        paintGeneral.setFakeBoldText(useBoldStyle);

        patchTextRegions(canvas, textOffset, start, end, (Canvas canvasLocal, char[] text, int index, int count, int contextIndex, int contextCount, boolean isRtl,
                                                          float horizontalOffset, float width, TextRowParams params, Span span) -> {
            if (span == null) {
                return;
            }
            if (backgroundColor != 0) {
                tmpRect.top = getRowTopForBackground(0);
                tmpRect.bottom = getRowBottomForBackground(0);
                tmpRect.left = horizontalOffset;
                tmpRect.right = horizontalOffset + width;
                paintOther.setColor(backgroundColor);
                drawRowBackgroundRect(canvas, tmpRect, paintOther);
            }
            long style = span.getStyle();
            if (color != 0) {
                paintGeneral.setTextSkewX(TextStyle.isItalics(style) ? RenderingConstants.TEXT_SKEW_X : 0f);
                paintGeneral.setStrikeThruText(TextStyle.isStrikeThrough(style));
                GraphicsCompat.drawTextRun(canvas, text, index, count, contextIndex, contextCount, horizontalOffset, params.getTextBaseline(), isRtl, paintGeneral);
            }
            if (underlineColor != 0) {
                paintOther.setColor(underlineColor);
                var bottom = params.getTextBottom() - params.getTextHeight() * 0.05f;
                canvas.drawLine(horizontalOffset, bottom, horizontalOffset + width, bottom, paintOther);
            }
        }, null);
        paintGeneral.setStyle(Paint.Style.FILL);
        paintGeneral.setFakeBoldText(false);
        paintGeneral.setTextSkewX(0f);
        paintGeneral.setStrikeThruText(false);
    }

    protected void patchTextBackgroundRegions(Canvas canvas, float textOffset, int start, int end, int backgroundColor, float borderWidth, int borderColor) {
        if (backgroundColor == 0 && (borderWidth <= 0 || borderColor == 0)) {
            return;
        }
        patchTextRegions(canvas, textOffset, start, end, null, (float left, float right) -> {
            if (textOffset + left < 0) {
                return true;
            }
            tmpRect.top = getRowTopForBackground(0);
            tmpRect.bottom = getRowBottomForBackground(0);
            tmpRect.left = left;
            tmpRect.right = right;
            if (backgroundColor != 0) {
                paintOther.setColor(backgroundColor);
                drawRowBackgroundRect(canvas, tmpRect, paintOther);
            }
            if (borderWidth > 0 && borderColor != 0) {
                paintOther.setStyle(android.graphics.Paint.Style.STROKE);
                paintOther.setColor(borderColor);
                paintOther.setStrokeWidth(borderWidth);
                drawRowBackgroundRect(canvas, tmpRect, paintOther);
                paintOther.setStyle(android.graphics.Paint.Style.FILL);
            }
            return textOffset + right > editor.getWidth();
        });
    }


    protected void patchTextRegions(Canvas canvas, float textOffset, int start, int end,
                                    @Nullable TextRow.DrawTextConsumer patch,
                                    @Nullable TextRow.BackgroundRegionConsumer bgPatch) {
        if (patch == null && bgPatch == null) {
            return;
        }
        var firstVisRow = editor.getFirstVisibleRow();
        var lastVisRow = editor.getLastVisibleRow();

        var layout = editor.getLayout();
        var startRow = layout.getRowIndexForPosition(start);
        var endRow = layout.getRowIndexForPosition(end);
        var posStart = cursor.getIndexer().getCharPosition(start);
        var posEnd = cursor.getIndexer().getCharPosition(end);
        var itr = layout.obtainRowIterator(startRow, preloadedLines);
        for (int i = startRow; i <= endRow && itr.hasNext(); i++) {
            var row = itr.next();
            if (!(firstVisRow <= i && i <= lastVisRow)) {
                continue;
            }
            var startOnRow = (i == startRow ? posStart.column : row.startColumn);
            var endOnRow = (i == endRow ? posEnd.column : row.endColumn);
            var tr = createTextRow(i);
            float horizontalOffset = textOffset;
            if ((editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0 && !row.isLeadingRow) {
                horizontalOffset += getMiniGraphWidth();
            }
            float minHorizontalOffset = Math.max(0, -horizontalOffset);
            float maxHorizontalOffset = minHorizontalOffset + editor.getWidth();
            canvas.save();
            canvas.translate(horizontalOffset + row.renderTranslateX, editor.getRowTop(i) - editor.getOffsetY());
            if (bgPatch != null) {
                tr.iterateBackgroundRegions(startOnRow, endOnRow, false, false, bgPatch);
            }
            if (patch != null) {
                tr.iterateDrawTextRegions(startOnRow, endOnRow, canvas, minHorizontalOffset, maxHorizontalOffset, true, patch);
            }
            canvas.restore();
        }
    }

    protected void drawSelectionOnAnimation(Canvas canvas) {
        if (!editor.isEditable()) {
            return;
        }
        tmpRect.bottom = editor.getCursorAnimator().animatedY() - editor.getOffsetY();
        tmpRect.top = tmpRect.bottom - (editor.getProps().textBackgroundWrapTextOnly ? editor.getRowHeightOfText() : editor.getRowHeight());
        float centerX = editor.getCursorAnimator().animatedX() - editor.getOffsetX();
        tmpRect.left = centerX - editor.getInsertSelectionWidth() / 2;
        tmpRect.right = centerX + editor.getInsertSelectionWidth() / 2;
        drawColor(canvas, editor.getColorScheme().getColor(EditorColorScheme.SELECTION_INSERT), tmpRect);
        var bidiAttrs = getBidiIndicatorAttrs(cursor.getLeftLine(), cursor.getLeftColumn());
        if (IntPair.getFirst(bidiAttrs) == 1) {
            drawBidiSelectionIndicator(canvas, centerX, tmpRect.top, tmpRect.height(), IntPair.getSecond(bidiAttrs) == 1);
        }
        if (editor.getEventHandler().shouldDrawInsertHandle() && !editor.isInMouseMode()) {
            editor.getHandleStyle().draw(canvas, SelectionHandleStyle.HANDLE_TYPE_INSERT, centerX, tmpRect.bottom, editor.getRowHeight(), editor.getColorScheme().getColor(EditorColorScheme.SELECTION_HANDLE), editor.getInsertHandleDescriptor());
        }
    }

    /**
     * Draw horizontal scroll bar
     *
     * @param canvas Canvas to draw
     */
    protected void drawScrollBarHorizontal(Canvas canvas) {
        int page = editor.getWidth();
        float all = editor.getScrollMaxX();
        float length = page / (all + editor.getWidth()) * editor.getWidth();
        float minLength = RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP * editor.getDpUnit();
        if (length <= minLength) length = minLength;
        float leftX = editor.getOffsetX() / all * (editor.getWidth() - length);
        tmpRect.top = editor.getHeight() - editor.getDpUnit() * RenderingConstants.SCROLLBAR_WIDTH_DIP;
        tmpRect.bottom = editor.getHeight();
        tmpRect.right = leftX + length;
        tmpRect.left = leftX;
        horizontalScrollBarRect.set(tmpRect);
        if (horizontalScrollbarThumbDrawable != null) {
            horizontalScrollbarThumbDrawable.setState(editor.getEventHandler().holdHorizontalScrollBar() ? PRESSED_DRAWABLE_STATE : DEFAULT_DRAWABLE_STATE);
            horizontalScrollbarThumbDrawable.setBounds((int) tmpRect.left, (int) tmpRect.top, (int) tmpRect.right, (int) tmpRect.bottom);
            horizontalScrollbarThumbDrawable.draw(canvas);
        } else {
            drawColor(canvas, editor.getColorScheme().getColor(editor.getEventHandler().holdHorizontalScrollBar() ? EditorColorScheme.SCROLL_BAR_THUMB_PRESSED : EditorColorScheme.SCROLL_BAR_THUMB), tmpRect);
        }
    }

    // BEGIN Measure-------------------------------------

    /**
     * Build measure cache for the given lines, if the timestamp indicates that it is outdated.
     */
    protected void buildMeasureCacheForLines(int startLine, int endLine, long timestamp, boolean useCachedContent) {
        var text = content;
        var context = editor.getRenderContext();
        while (startLine <= endLine && startLine < text.getLineCount()) {
            var line = useCachedContent ? getLine(startLine) : getLineDirect(startLine);
            var cache = editor.getRenderContext().getCache().getOrCreateMeasureCache(startLine);
            if (cache.getUpdateTimestamp() < timestamp) {
                var forced = false;
                if (cache.getWidths() == null || cache.getWidths().getSize() < line.length()) {
                    cache.setWidths(new TextAdvancesCache(Math.max(line.length() + 8, 90)));
                    forced = true;
                }
                var spans = editor.getSpansForLine(startLine);
                var hash = Objects.hash(spans, line.length(), editor.getTabWidth(),
                        paintGeneral.getFlags(), paintGeneral.getTextSize(), paintGeneral.getTextScaleX(),
                        paintGeneral.getLetterSpacing(), paintGeneral.getFontFeatureSettings(), paintGeneral.getTypeface().hashCode());
                if (context.getCache().getStyleHash(startLine) != hash || forced) {
                    context.getCache().setStyleHash(startLine, hash);
                    // Build cache here
                    var beginRowIndex = editor.layout.getRowIndexForPosition(text.getCharIndex(startLine, 0));
                    var itr = editor.layout.obtainRowIterator(beginRowIndex);
                    var tr = new TextRow();
                    var lineText = text.getLine(startLine);
                    var directions = text.getLineDirections(startLine);
                    int requiredSize = lineText.length() + 10;
                    var widths = cache.getWidths();
                    if (widths == null || widths.getSize() < requiredSize) {
                        widths = new TextAdvancesCache(requiredSize);
                        cache.setWidths(widths);
                    }
                    while (itr.hasNext()) {
                        var row = itr.next();
                        if (row.lineIndex != startLine) {
                            break;
                        }
                        tr.set(lineText, row.startColumn, row.endColumn, spans, row.inlayHints, directions, paintGeneral, null, createTextRowParams());
                        tr.buildMeasureCacheStep(widths);
                    }
                    tr.setRange(0, lineText.length());
                    tr.buildMeasureCacheTailor(widths);
                    cache.setUpdateTimestamp(timestamp);
                }
            }
            startLine++;
        }
    }

    protected void buildMeasureCacheForLines(int startLine, int endLine) {
        buildMeasureCacheForLines(startLine, endLine, displayTimestamp, false);
    }

    protected float getRowWidth(int row) {
        return createTextRow(row).computeRowWidth();
    }

    // END Measure---------------------------------------


    protected class DrawCursorTask {

        private final static SelectionHandleStyle.HandleDescriptor TMP_DESC = new SelectionHandleStyle.HandleDescriptor();

        protected float x;
        protected float y;
        protected int handleType;
        protected SelectionHandleStyle.HandleDescriptor descriptor;
        protected boolean isBidiIndicatorRequired;
        protected boolean isRightToLeft;


        public DrawCursorTask(float x, float y, int handleType, SelectionHandleStyle.HandleDescriptor descriptor) {
            this.x = x;
            this.y = y;
            this.handleType = handleType;
            this.descriptor = descriptor;
        }

        public void setBidiIndicatorRequired(boolean bidiIndicatorRequired) {
            isBidiIndicatorRequired = bidiIndicatorRequired;
        }

        public void setRightToLeft(boolean rightToLeft) {
            isRightToLeft = rightToLeft;
        }

        private int getActualHandleType() {
            if (isRightToLeft && handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT) {
                return SelectionHandleStyle.HANDLE_TYPE_RIGHT;
            }
            if (isRightToLeft && handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) {
                return SelectionHandleStyle.HANDLE_TYPE_LEFT;
            }
            return handleType;
        }

        private boolean drawSelForLeftRight() {
            return ((handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)
                    && editor.getProps().showSelectionWhenSelected && !editor.isInMouseMode());
        }

        private boolean drawSelForInsert() {
            return (!(handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)
                    && (editor.getCursorBlink().visibility || editor.getEventHandler().holdInsertHandle() || editor.isInLongSelect()));
        }

        private boolean isSelForLongSelect() {
            return editor.isInLongSelect() && !(handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT
                    || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT);
        }

        protected void execute(Canvas canvas) {
            // Hide cursors (API level 31)
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                if (editor.inputConnection.imeConsumingInput || !editor.isFocused()) {
                    return;
                }
            }
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && !editor.isEditable()) {
                return;
            }
            var descriptor = this.descriptor == null ? TMP_DESC : this.descriptor;
            // Follow the thumb or stick to text row
            if (!descriptor.position.isEmpty()) {
                if (!editor.isStickyTextSelection()) {
                    if (editor.getEventHandler().getTouchedHandleType() == getActualHandleType()
                            && handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && editor.getEventHandler().isHandleMoving()) {
                        x = editor.getEventHandler().motionX + (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER ? descriptor.position.width() : 0) * (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT ? 1 : -1);
                        y = editor.getEventHandler().motionY - descriptor.position.height() * 2 / 3f;
                    }
                }
            }

            if (drawSelForLeftRight() || drawSelForInsert() || handleType == SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                float startY = y - (editor.getProps().textBackgroundWrapTextOnly ? editor.getRowHeightOfText() : editor.getRowHeight());
                float stopY = y;
                paintGeneral.setColor(editor.getColorScheme().getColor(EditorColorScheme.SELECTION_INSERT));
                paintGeneral.setStrokeWidth(editor.getInsertSelectionWidth());
                paintGeneral.setStyle(android.graphics.Paint.Style.STROKE);
                if (isSelForLongSelect()) {
                    paintGeneral.setPathEffect(new DashPathEffect(new float[]{(stopY - startY) / 8f, (stopY - startY) / 8f}, (stopY - startY) / 16f));
                    paintGeneral.setStrokeWidth(editor.getInsertSelectionWidth() * 1.5f);
                }
                canvas.drawLine(x, startY, x, stopY, paintGeneral);
                paintGeneral.setStyle(android.graphics.Paint.Style.FILL);
                paintGeneral.setPathEffect(null);
                if (drawSelForInsert() && isBidiIndicatorRequired) {
                    // Draw a flag for LTR/RTL mixed row
                    float height = (stopY - startY);
                    drawBidiSelectionIndicator(canvas, x, startY, height, isRightToLeft);
                }
            }
            var handleType = this.handleType;
            // Hide insert handle conditionally
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && (editor.isInLongSelect() || !editor.getEventHandler().shouldDrawInsertHandle())) {
                handleType = SelectionHandleStyle.HANDLE_TYPE_UNDEFINED;
            }
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && !editor.isInMouseMode() /* hide if mouse inside */) {
                editor.getHandleStyle().draw(canvas, handleType, x, y, editor.getRowHeight(), editor.getColorScheme().getColor(EditorColorScheme.SELECTION_HANDLE), descriptor);
                if (descriptor == TMP_DESC) {
                    descriptor.setEmpty();
                }
            } else {
                descriptor.setEmpty();
            }
        }
    }
}
