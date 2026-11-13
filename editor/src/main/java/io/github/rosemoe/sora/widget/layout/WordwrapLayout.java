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
package io.github.rosemoe.sora.widget.layout;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.graphics.TextRow;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Wordwrap layout for editor
 * <p>
 * This layout will not let character displayed outside the editor's width
 * <p>
 * However, using this can be power-costing because we will have to recreate this layout in various
 * conditions, such as when the line number increases and its width grows or when the text size has changed
 *
 * @author Rose
 */
public class WordwrapLayout extends AbstractLayout {

    /**
     * When measuring text in wordwrap mode, we must use the max possible width of the character sequence
     * so that no character will be invisible after its styles are applied on actual drawing.
     */

    private final static List<Span> sSpansForWordwrap = new ArrayList<>();

    static {
        sSpansForWordwrap.add(SpanFactory.obtainNoExt(0, TextStyle.makeStyle(0, 0, true, true, false)));
    }

    private final int width;
    private final float miniGraphWidth;
    private final boolean antiWordBreaking;
    private final boolean supportRtlRow;
    /**
     * Rows of all lines. Line numbers are maintained here even when the lines are hidden.
     */
    private List<RowRegion> rowTable;
    /**
     * Rows that are actually displayed. It aliases {@link #rowTable} while no folding is active
     * and is a filtered copy otherwise. Rebuilt lazily when {@link #visibleDirty} is set.
     *
     * @see #display()
     */
    private List<RowRegion> visibleTable;
    private boolean visibleDirty;
    /**
     * Bumped on every text edit, so results of an async break that started before the edit are discarded
     */
    private final AtomicInteger editCount = new AtomicInteger(0);

    public WordwrapLayout(@NonNull CodeEditor editor, @NonNull Content text, boolean antiWordBreaking, boolean supportRtlRow, @Nullable WordwrapLayout oldLayout, boolean clearCache) {
        super(editor, text);
        this.antiWordBreaking = antiWordBreaking;
        this.supportRtlRow = supportRtlRow;
        rowTable = oldLayout != null ? oldLayout.rowTable : new ArrayList<>();
        if (clearCache) {
            rowTable.clear();
        }
        visibleDirty = true;
        miniGraphWidth = (editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0 ?
                editor.getRenderer().getMiniGraphWidth() : 0f;
        width = editor.getWidth() - (int) (editor.measureTextRegionOffset() + editor.getTextPaint().measureText("a")) - (int) miniGraphWidth * 2;
        breakAllLines();
    }

    /**
     * Rows that are displayed on screen, i.e. rows of the lines that are not hidden by folding
     */
    private List<RowRegion> display() {
        if (visibleDirty || visibleTable == null) {
            rebuildVisible();
        }
        return visibleTable;
    }

    private void rebuildVisible() {
        if (rowTable == null) {
            visibleTable = null;
            visibleDirty = false;
            return;
        }
        var lineMap = getFoldedLineMap();
        if (lineMap.isEmpty()) {
            // No folding active, keep identity with the maintenance table
            visibleTable = rowTable;
        } else {
            var filtered = new ArrayList<RowRegion>(rowTable.size());
            for (int i = 0; i < rowTable.size(); i++) {
                var region = rowTable.get(i);
                if (!lineMap.isHidden(region.line)) {
                    filtered.add(region);
                }
            }
            visibleTable = filtered;
        }
        visibleDirty = false;
    }

    @Override
    public void onFoldingRegionsChanged() {
        visibleDirty = true;
    }

    private void breakAllLines() {
        var editCountLocal = editCount.get();
        var taskCount = Math.min(SUBTASK_COUNT, (int) Math.ceil((float) text.getLineCount() / MIN_LINE_COUNT_FOR_SUBTASK));
        var sizeEachTask = text.getLineCount() / taskCount;
        var monitor = new TaskMonitor(taskCount, (results, cancelledCount) -> {
            final var editor = this.editor;
            if (editor != null) {
                List<WordwrapResult> r2 = new ArrayList<>();
                for (Object result : results) {
                    // Cancelled tasks leave no result, and the edit check below then re-breaks
                    if (result != null) {
                        r2.add((WordwrapResult) result);
                    }
                }
                Collections.sort(r2);
                editor.postInLifecycle(() -> {
                    if (WordwrapLayout.this.editor != editor) {
                        // This layout could have been abandoned when waiting for Runnable execution
                        // See #307
                        return;
                    }
                    if (editCountLocal != editCount.get()) {
                        // Text was edited while breaking, so the results may not match it anymore
                        breakAllLines();
                        return;
                    }
                    if (rowTable != null) {
                        rowTable.clear();
                    } else {
                        rowTable = new ArrayList<>();
                    }
                    for (WordwrapResult wordwrapResult : r2) {
                        rowTable.addAll(wordwrapResult.regions);
                    }
                    visibleDirty = true;
                    editor.setLayoutBusy(false);
                    editor.getEventHandler().scrollBy(0, 0);
                });
            }
        });
        editor.setLayoutBusy(true);
        for (int i = 0; i < taskCount; i++) {
            var start = sizeEachTask * i;
            var end = i + 1 == taskCount ? (text.getLineCount() - 1) : (sizeEachTask * (i + 1) - 1);
            submitTask(new WordwrapAnalyzeTask(monitor, i, start, end, editCountLocal));
        }
    }

    /**
     * Find the first row of the given line in the given table
     */
    private int findRowIn(@NonNull List<RowRegion> table, int line) {
        int index;
        // Binary find line
        int left = 0, right = table.size();
        while (left <= right) {
            var mid = (left + right) / 2;
            if (mid < 0 || mid >= table.size()) {
                left = Math.max(0, Math.min(table.size() - 1, mid));
                break;
            }
            int value = table.get(mid).line;
            if (value < line) {
                left = mid + 1;
            } else if (value > line) {
                right = mid - 1;
            } else {
                left = mid;
                break;
            }
        }
        index = left;
        if (table.isEmpty()) {
            return 0;
        }
        index = Math.min(index, table.size() - 1);
        while (index > 0 && table.get(index).startColumn > 0) {
            index--;
        }
        return index;
    }

    private int findRow(int line) {
        return findRowIn(rowTable, line);
    }

    /**
     * Find the displayed row that contains the given position
     */
    public int findRow(int line, int column) {
        return findRowIn(display(), line, column);
    }

    private int findRowIn(@NonNull List<RowRegion> table, int line, int column) {
        if (table.isEmpty()) {
            return 0;
        }
        int row = findRowIn(table, line);
        while (row < table.size() && table.get(row).endColumn <= column && row + 1 < table.size() && table.get(row + 1).line == line) {
            row++;
        }
        return row;
    }

    private void breakLines(int startLine, int endLine) {
        int insertPosition = 0;
        while (insertPosition < rowTable.size()) {
            if (rowTable.get(insertPosition).line < startLine) {
                insertPosition++;
            } else {
                break;
            }
        }
        while (insertPosition < rowTable.size()) {
            int line = rowTable.get(insertPosition).line;
            if (line >= startLine && line <= endLine) {
                rowTable.remove(insertPosition);
            } else {
                break;
            }
        }
        List<RowRegion> newRegions = new ArrayList<>();
        for (int i = startLine; i <= endLine; i++) {
            newRegions.addAll(breakLine(i, text.getLine(i), null));
        }
        rowTable.addAll(insertPosition, newRegions);
        visibleDirty = true;
    }

    /**
     * Break a single line
     */
    private List<RowRegion> breakLine(int line, ContentLine sequence, Paint paint) {
        Paint p = paint;
        if (p == null) {
            p = new Paint(editor.isRenderFunctionCharacters());
            p.set(editor.getTextPaint());
        }
        var tr = new TextRow();
        var directions = text.getLineDirections(line);
        tr.set(sequence, 0, sequence.length(), sSpansForWordwrap, getInlayHints(line), directions, p, null, editor.getRenderer().createTextRowParams());

        boolean isRtlBased = false;
        if (supportRtlRow && sequence.mayNeedBidi()) {
            int minRunLevel = Integer.MAX_VALUE;
            for (int i = 0; i < directions.getRunCount(); i++) {
                minRunLevel = Math.min(minRunLevel, directions.getRunLevel(i));
            }
            if ((minRunLevel & 1) != 0) {
                isRtlBased = true;
            }
        }

        var rows = tr.breakText(width, antiWordBreaking);
        var results = new ArrayList<RowRegion>();
        for (var row : rows) {
            results.add(new RowRegion(line, row.startColumn, row.endColumn, row.inlayHints, row.rowWidth, isRtlBased));
        }
        return results;
    }

    @Override
    public void beforeReplace(@NonNull Content content) {
        // Intentionally empty
    }

    @Override
    public void afterInsert(@NonNull Content content, int startLine, int startColumn, int endLine, int endColumn, @NonNull CharSequence insertedContent) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent);
        editCount.incrementAndGet();
        // Update line numbers
        int delta = endLine - startLine;
        if (delta != 0) {
            for (int row = findRow(startLine + 1); row < rowTable.size(); row++) {
                rowTable.get(row).line += delta;
            }
        }
        // Re-break
        breakLines(startLine, endLine);
    }

    @Override
    public void afterDelete(@NonNull Content content, int startLine, int startColumn, int endLine, int endColumn, @NonNull CharSequence deletedContent) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent);
        editCount.incrementAndGet();
        int delta = endLine - startLine;
        if (delta != 0) {
            int startRow = findRow(startLine);
            while (startRow < rowTable.size()) {
                int line = rowTable.get(startRow).line;
                if (line >= startLine && line <= endLine) {
                    rowTable.remove(startRow);
                } else {
                    break;
                }
            }
            for (int row = findRow(endLine + 1); row < rowTable.size(); row++) {
                var region = rowTable.get(row);
                if (region.line >= endLine)
                    region.line -= delta;
            }
        }
        breakLines(startLine, startLine);
    }

    @Override
    public void destroyLayout() {
        super.destroyLayout();
        rowTable = null;
        visibleTable = null;
    }

    @NonNull
    @Override
    public Row getRowAt(int rowIndex) {
        if (rowTable.isEmpty()) {
            var line = getFoldedLineMap().displayToLine(rowIndex);
            var r = new Row();
            r.startColumn = 0;
            r.endColumn = text.getColumnCount(line);
            r.isLeadingRow = true;
            r.isTrailingRow = true;
            r.lineIndex = line;
            r.inlayHints = getInlayHints(line);
            return r;
        }
        var table = display();
        if (rowIndex >= table.size()) {
            rowIndex = table.size() - 1;
        }
        if (rowIndex < 0) {
            rowIndex = 0;
        }
        var region = table.get(rowIndex);
        var isLeadingRow = rowIndex <= 0 || table.get(rowIndex - 1).line != region.line;
        var isTrailingRow = rowIndex + 1 >= table.size() || table.get(rowIndex + 1).line != region.line;
        return region.toRow(isLeadingRow, isTrailingRow, width);
    }

    @Override
    public int getLineNumberForRow(int row) {
        if (rowTable.isEmpty()) {
            return getFoldedLineMap().displayToLine(row);
        }
        var table = display();
        if (table.isEmpty()) {
            return getFoldedLineMap().displayToLine(row);
        }
        return row >= table.size() ? table.get(table.size() - 1).line : table.get(row).line;
    }

    @NonNull
    @Override
    public RowIterator obtainRowIterator(int initialRow, @Nullable SparseArray<ContentLine> preloadedLines) {
        return rowTable.isEmpty() ? new LineBreakLayout.LineBreakLayoutRowItr(this, text, initialRow, preloadedLines) : new WordwrapLayoutRowItr(initialRow);
    }

    @Override
    public long getUpPosition(int line, int column) {
        if (rowTable.isEmpty()) {
            var lineMap = getFoldedLineMap();
            int target = line - 1;
            while (target >= 0 && lineMap.isHidden(target)) {
                target--;
            }
            if (target < 0) {
                return IntPair.pack(0, 0);
            }
            int c_column = text.getColumnCount(target);
            if (column > c_column) {
                column = c_column;
            }
            return IntPair.pack(target, column);
        }
        var table = display();
        int row = findRowIn(table, line, column);
        if (row > 0) {
            var offset = column - table.get(row).startColumn;
            var lastRow = table.get(row - 1);
            var max = lastRow.endColumn - lastRow.startColumn;
            offset = Math.min(offset, max);
            return IntPair.pack(lastRow.line, lastRow.startColumn + offset);
        }
        return IntPair.pack(0, 0);
    }

    @Override
    public long getDownPosition(int line, int column) {
        if (rowTable.isEmpty()) {
            int lineCount = text.getLineCount();
            var lineMap = getFoldedLineMap();
            int target = line + 1;
            while (target < lineCount && lineMap.isHidden(target)) {
                target++;
            }
            if (target >= lineCount) {
                return IntPair.pack(line, text.getColumnCount(line));
            } else {
                int c_column = text.getColumnCount(target);
                if (column > c_column) {
                    column = c_column;
                }
                return IntPair.pack(target, column);
            }
        }
        var table = display();
        int row = findRowIn(table, line, column);
        if (row + 1 < table.size()) {
            var offset = column - table.get(row).startColumn;
            var nextRow = table.get(row + 1);
            var max = nextRow.endColumn - nextRow.startColumn;
            offset = Math.min(offset, max);
            return IntPair.pack(nextRow.line, nextRow.startColumn + offset);
        } else {
            return IntPair.pack(line, text.getColumnCount(line));
        }
    }

    @Override
    public int getLayoutWidth() {
        return 0;
    }

    @Override
    public int getLayoutHeight() {
        if (rowTable.isEmpty()) {
            return editor.getRowHeight() * getFoldedLineMap().getVisibleLineCount();
        }
        return display().size() * editor.getRowHeight();
    }

    @Override
    public int getRowIndexForPosition(int index) {
        var pos = editor.getText().getIndexer().getCharPosition(index);
        var line = pos.line;
        if (rowTable.isEmpty()) {
            return getFoldedLineMap().lineToDisplay(line);
        }
        var column = pos.column;
        var table = display();
        int row = findRowIn(table, line);
        if (row < table.size()) {
            var region = table.get(row);
            if (region.line != line) {
                return 0;
            }
            while (region.startColumn < column && row + 1 < table.size()) {
                row++;
                region = table.get(row);
                if (region.line != line || region.startColumn > column) {
                    row--;
                    break;
                }
            }
            return row;
        }
        return 0;
    }

    @Override
    public void invalidateLines(StyleUpdateRange range) {
        var itr = range.lineIndexIterator(text.getLineCount() - 1);
        while (itr.hasNext()) {
            var line = itr.nextInt();
            breakLines(line, line);
        }
    }

    @NonNull
    @Override
    public VisualLocation getVisualPositionForLayoutOffset(float offsetX, float offsetY) {
        if (rowTable.isEmpty()) {
            int row = (int) (offsetY / editor.getRowHeight());
            int line = getFoldedLineMap().displayToLine(row);
            var tr = editor.getRenderer().createTextRow(row);
            var pos = tr.getElementPositionForCursorOffset(offsetX);
            return new VisualLocation(line, pos.textOffset, pos.element, pos.isInElementBounds);
        }
        var table = display();
        int row = (int) (offsetY / editor.getRowHeight());
        row = Math.max(0, Math.min(row, table.size() - 1));
        RowRegion region = table.get(row);
        if (region.startColumn != 0) {
            offsetX -= miniGraphWidth;
        }
        offsetX -= region.getRenderTranslateX(width);
        var tr = editor.getRenderer().createTextRow(row);
        var pos = tr.getElementPositionForCursorOffset(offsetX);
        return new VisualLocation(region.line, pos.textOffset, pos.element, pos.isInElementBounds);
    }

    @NonNull
    @Override
    public float[] getCharLayoutOffset(int line, int column, float[] dest) {
        if (dest == null || dest.length < 2) {
            dest = new float[2];
        }
        var lineMap = getFoldedLineMap();
        var visibleLine = lineMap.clampToVisible(line);
        if (rowTable.isEmpty()) {
            dest[0] = editor.getRowBottom(lineMap.lineToDisplay(visibleLine));
            var tr = editor.getRenderer().createTextRow(lineMap.lineToDisplay(visibleLine));
            dest[1] = tr.getCursorOffsetForIndex(Math.min(column, text.getColumnCount(visibleLine)));
            return dest;
        }
        var table = display();
        int row = findRowIn(table, visibleLine);
        if (row < table.size()) {
            RowRegion region = table.get(row);
            if (region.line != visibleLine) {
                dest[0] = dest[1] = 0;
                return dest;
            }
            while (region.startColumn < column && row + 1 < table.size()) {
                row++;
                region = table.get(row);
                if (region.line != visibleLine || region.startColumn > column) {
                    row--;
                    region = table.get(row);
                    break;
                }
            }
            dest[0] = editor.getRowBottom(row);
            var tr = editor.getRenderer().createTextRow(row);
            dest[1] = tr.getCursorOffsetForIndex(Math.min(column, region.endColumn));
            if (region.startColumn != 0) {
                dest[1] += miniGraphWidth;
            }
            dest[1] += region.getRenderTranslateX(width);
        } else {
            dest[0] = dest[1] = 0;
        }
        return dest;
    }

    @Override
    public int getRowCountForLine(int line) {
        if (rowTable.isEmpty()) {
            return 1;
        }
        if (getFoldedLineMap().isHidden(line)) {
            return 0;
        }
        var table = display();
        int row = findRowIn(table, line);
        int count = 0;
        while (row < table.size() && table.get(row).line == line) {
            count++;
            row++;
        }
        return count;
    }

    /**
     * Get soft breaks on the given line
     */
    public List<Integer> getSoftBreaksForLine(int line) {
        if (rowTable.isEmpty() || getFoldedLineMap().isHidden(line)) {
            return Collections.emptyList();
        }
        var table = display();
        int row = findRowIn(table, line);
        var list = new ArrayList<Integer>();
        while (row < table.size() && table.get(row).line == line) {
            var column = table.get(row).startColumn;
            if (column != 0) {
                list.add(column);
            }
            row++;
        }
        return list;
    }

    @Override
    public int getRowCount() {
        if (rowTable.isEmpty()) {
            return getFoldedLineMap().getVisibleLineCount();
        }
        return display().size();
    }

    static class RowRegion {

        final int startColumn;
        final int endColumn;
        List<InlayHint> inlayHints;
        int line;
        float rowWidth;
        boolean displayFromRight;

        RowRegion(int line, int start, int end, List<InlayHint> inlayHints, float rowWidth, boolean displayFromRight) {
            this.line = line;
            startColumn = start;
            endColumn = end;
            this.inlayHints = inlayHints;
            this.rowWidth = rowWidth;
            this.displayFromRight = displayFromRight;
        }

        public Row toRow(boolean isLeadingRow, boolean isTrailingRow, float layoutWidth) {
            var row = new Row();
            row.isLeadingRow = isLeadingRow;
            row.isTrailingRow = isTrailingRow;
            row.startColumn = startColumn;
            row.endColumn = endColumn;
            row.lineIndex = line;
            row.inlayHints = inlayHints == null ? Collections.emptyList() : inlayHints;
            row.renderTranslateX = getRenderTranslateX(layoutWidth);
            return row;
        }

        public float getRenderTranslateX(float layoutWidth) {
            return displayFromRight && layoutWidth > rowWidth ? layoutWidth - rowWidth : 0f;
        }

        @NonNull
        @Override
        public String toString() {
            return "RowRegion{" +
                    "startColumn=" + startColumn +
                    ", endColumn=" + endColumn +
                    ", line=" + line +
                    '}';
        }
    }

    private static class WordwrapResult implements Comparable<WordwrapResult> {

        int index;
        List<RowRegion> regions;

        public WordwrapResult(int idx, List<RowRegion> r) {
            index = idx;
            regions = r;
        }

        @Override
        public int compareTo(WordwrapResult wordwrapResult) {
            return Integer.compare(index, wordwrapResult.index);
        }
    }

    class WordwrapLayoutRowItr implements RowIterator {

        private final Row result;
        private final int initRow;
        private int currentRow;

        WordwrapLayoutRowItr(int initialRow) {
            initRow = currentRow = initialRow;
            result = new Row();
        }

        @NonNull
        @Override
        public Row next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var table = display();
            RowRegion region = table.get(currentRow);
            result.lineIndex = region.line;
            result.startColumn = region.startColumn;
            result.endColumn = region.endColumn;
            result.inlayHints = region.inlayHints == null ? Collections.emptyList() : region.inlayHints;
            result.isLeadingRow = currentRow <= 0 || table.get(currentRow - 1).line != region.line;
            result.isTrailingRow = currentRow + 1 >= table.size() || table.get(currentRow + 1).line != region.line;
            result.renderTranslateX = region.getRenderTranslateX(width);
            currentRow++;
            return result;
        }

        @Override
        public boolean hasNext() {
            var table = display();
            return currentRow >= 0 && currentRow < table.size();
        }

        @Override
        public void reset() {
            currentRow = initRow;
        }
    }
    private class WordwrapAnalyzeTask extends LayoutTask<WordwrapResult> {

        private final int start, end, id, editCountLocal;
        private final Paint paint;

        WordwrapAnalyzeTask(TaskMonitor monitor, int id, int start, int end, int editCountLocal) {
            super(monitor);
            this.start = start;
            this.id = id;
            this.end = end;
            this.editCountLocal = editCountLocal;
            paint = new Paint(editor.isRenderFunctionCharacters());
            paint.set(editor.getTextPaint());
            paint.onAttributeUpdate();
        }

        @Override
        protected WordwrapResult compute() {
            var list = new ArrayList<RowRegion>();
            text.runReadActionsOnLines(start, end, (int index, ContentLine line, Content.ContentLineConsumer2.AbortFlag abortFlag) -> {
                list.addAll(breakLine(index, line, paint));
                if (!shouldRun()) {
                    abortFlag.set = true;
                }
            });
            return new WordwrapResult(id, list);
        }

        @Override
        protected boolean shouldRun() {
            return super.shouldRun() && editCount.get() == editCountLocal;
        }

    }

}
