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
package io.github.rosemoe.sora.widget.rendering;

import static io.github.rosemoe.sora.util.Numbers.stringSize;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

import java.util.Objects;

import io.github.rosemoe.sora.util.Numbers;
import io.github.rosemoe.sora.util.TemporaryCharBuffer;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class EditorRenderer {

    private final RenderContext[] contextCache = new RenderContext[4];
    private final CodeEditor editor;

    public EditorRenderer(@NonNull CodeEditor editor) {
        this.editor = Objects.requireNonNull(editor);
    }

    @NonNull
    private synchronized RenderContext obtainContext() {
        for (int i = 0; i < contextCache.length; i++) {
            if (contextCache[i] != null) {
                var context = contextCache[i];
                contextCache[i] = null;
                return context;
            }
        }
        return new RenderContext();
    }

    private synchronized void recycleContext(@NonNull RenderContext context) {
        for (int i = 0; i < contextCache.length; i++) {
            if (contextCache[i] == null) {
                contextCache[i] = context;
                context.canvas = null;
                context.textPaint = context.otherPaint = context.graphPaint = null;
                context.height = context.width = context.scrollX = context.scrollY = context.firstVisibleRow = context.lastVisibleRow = 0;
                context.paintingOffset = 0;
                context.lineNumberMetrics = null;
            }
        }
    }

    /**
     * Render the editor on the given canvas
     */
    public void render(@NonNull Canvas canvas) {
        long t = System.currentTimeMillis();
        var context = createRenderContext(canvas);
        renderProcedures(context);
        recycleContext(context);
        android.util.Log.d("Renderer", "t = " + (System.currentTimeMillis() - t));
    }

    public RenderContext createRenderContext(@NonNull Canvas canvas) {
        var context = obtainContext();
        context.canvas = canvas;
        context.scrollX = editor.getOffsetX();
        context.scrollY = editor.getOffsetY();
        context.width = editor.getWidth();
        context.height = editor.getHeight();
        context.paintingOffset = 0f;
        context.textPaint = editor.getTextPaint();
        context.otherPaint = editor.getOtherPaint();
        context.graphPaint = editor.getGraphPaint();

        context.firstVisibleRow = editor.getFirstVisibleRow();
        context.lastVisibleRow = editor.getLastVisibleRow();

        context.lineNumberMetrics = editor.getLineNumberMetrics();
        return context;
    }

    protected void renderProcedures(RenderContext context) {
        renderBackground(context);

        renderGutterFrame(context);
        renderCurrentLineBackground(context);
        renderLineNumbers(context);

        renderTextBackground(context);
        //renderText(context);
    }

    protected int getColor(int type) {
        return editor.getColorScheme().getColor(type);
    }

    protected void drawRect(RenderContext context, float l, float t, float r, float b, int color) {
        if (color == 0) {
            return;
        }
        context.otherPaint.setColor(color);
        context.canvas.drawRect(l, t, r, b, context.otherPaint);
    }

    /**
     * Render the background color of view
     */
    protected void renderBackground(RenderContext context) {
        drawRect(context, 0, 0, context.width, context.height, getColor(EditorColorScheme.WHOLE_BACKGROUND));
    }

    /**
     * Render line number background and divider
     */
    protected void renderGutterFrame(RenderContext context) {
        var lineNumberWidth = editor.measureLineNumber();
        var margin = editor.getDividerMargin();
        var dividerWidth = editor.getDividerWidth();
        var offset = editor.isLineNumberPinned() ? 0f : -context.scrollX;
        var enabled = editor.isLineNumberEnabled();
        if (enabled) {
            context.paintingOffset = -context.scrollX + lineNumberWidth + margin * 2 + dividerWidth;

            drawRect(context, offset, 0, offset + lineNumberWidth + margin, context.height,
                    getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND));

            drawRect(context, offset + lineNumberWidth + margin, 0,
                    offset + margin + lineNumberWidth + dividerWidth, context.height,
                    getColor(EditorColorScheme.LINE_DIVIDER));
        } else {
            context.paintingOffset = -context.scrollX;
        }
    }

    protected void renderCurrentLineBackground(RenderContext context) {
        var animator = editor.getCursorAnimator();
        float bottom = 0;
        float height = 0;
        var draw = false;
        if (animator.isRunning()) {
            bottom = animator.animatedLineBottom();
            height = animator.animatedLineHeight();
            draw = true;
        } else {
            var cursor = editor.getCursor();
            var layout = editor.getLayout();
            if (!cursor.isSelected()) {
                var line = cursor.getLeftLine();
                height = layout.getRowCountForLine(line) * editor.getRowHeight();
                bottom = layout.getCharLayoutOffset(line, editor.getText().getColumnCount(line))[0];
                draw = true;
            }
        }

        if (draw) {
            drawRect(context, 0, -context.scrollY + bottom - height, context.width, -context.scrollY + bottom, getColor(EditorColorScheme.CURRENT_LINE));
        }
    }

    protected void renderLineNumbers(RenderContext context) {
        var lineNumberWidth = editor.measureLineNumber();
        var margin = editor.getDividerMargin();
        var offset = editor.isLineNumberPinned() ? 0f : -context.scrollX;
        var enabled = editor.isLineNumberEnabled();
        if (enabled) {
            var itr = editor.getLayout().obtainRowIterator(context.firstVisibleRow);
            var row = context.firstVisibleRow;
            var color = getColor(EditorColorScheme.LINE_NUMBER);
            while (itr.hasNext() && row <= context.lastVisibleRow) {
                var rowInfo = itr.next();
                if (rowInfo.isLeadingRow
                        || (row == context.firstVisibleRow && editor.isFirstLineNumberAlwaysVisible())) {
                    drawLineNumber(context, rowInfo.lineIndex, row, offset, lineNumberWidth, color);
                }
                row ++;
            }
        }
    }

    /**
     * Draw single line number
     */
    protected void drawLineNumber(RenderContext context, int line, int row, float offsetX, float width, int color) {
        if (width + offsetX <= 0) {
            return;
        }
        var canvas = context.canvas;
        var paint = context.otherPaint;
        if (paint.getTextAlign() != editor.getLineNumberAlign()) {
            paint.setTextAlign(editor.getLineNumberAlign());
        }
        paint.setColor(color);
        // Line number center align to text center
        float y = (editor.getRowBottom(row) + editor.getRowTop(row)) / 2f - (context.lineNumberMetrics.descent - context.lineNumberMetrics.ascent) / 2f - context.lineNumberMetrics.ascent - editor.getOffsetY();

        var buffer = TemporaryCharBuffer.obtain(20);
        line++;
        int i = stringSize(line);
        Numbers.getChars(line, i, buffer);

        switch (editor.getLineNumberAlign()) {
            case LEFT:
                canvas.drawText(buffer, 0, i, offsetX, y, paint);
                break;
            case RIGHT:
                canvas.drawText(buffer, 0, i, offsetX + width, y, paint);
                break;
            case CENTER:
                canvas.drawText(buffer, 0, i, offsetX + (width + editor.getDividerMargin()) / 2f, y, paint);
        }
        TemporaryCharBuffer.recycle(buffer);
    }

    protected void renderTextBackground(RenderContext context) {

    }

    protected void renderText(RenderContext context) {

    }

    protected void renderWhitespaces(RenderContext context) {

    }

    protected void renderErrorIndicators(RenderContext context) {

    }

    protected void renderCodeBlocks(RenderContext context) {

    }

    protected void renderScrollbars(RenderContext context) {

    }

    protected void renderEdgeEffects(RenderContext context) {

    }

}
