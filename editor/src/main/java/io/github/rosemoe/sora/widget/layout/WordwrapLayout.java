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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

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
    private List<RowRegion> rowTable;

    /**
     * 折叠启用时可见行 -> rowTable 下标 的映射缓存，避免每次访问都线性扫描。
     */
    private int[] visibleRowToTableRow = new int[0];
    private int[] tableRowToVisibleRow = new int[0];
    private long lastMappingVersion = -1;
    private long lastRowTableVersion = -1;
    private long rowTableVersion = 0;

    public WordwrapLayout(@NonNull CodeEditor editor, @NonNull Content text, boolean antiWordBreaking, boolean supportRtlRow, @Nullable WordwrapLayout oldLayout, boolean clearCache) {
        super(editor, text);
        this.antiWordBreaking = antiWordBreaking;
        this.supportRtlRow = supportRtlRow;
        rowTable = oldLayout != null ? oldLayout.rowTable : new ArrayList<>();
        if (clearCache) {
            rowTable.clear();
        }
        miniGraphWidth = (editor.getNonPrintablePaintingFlags() & CodeEditor.FLAG_DRAW_SOFT_WRAP) != 0 ?
                editor.getRenderer().getMiniGraphWidth() : 0f;
        width = editor.getWidth() - (int) (editor.measureTextRegionOffset() + editor.getTextPaint().measureText("a")) - (int) miniGraphWidth * 2;
        breakAllLines();
    }

    private void breakAllLines() {
        var taskCount = Math.min(SUBTASK_COUNT, (int) Math.ceil((float) text.getLineCount() / MIN_LINE_COUNT_FOR_SUBTASK));
        var sizeEachTask = text.getLineCount() / taskCount;
        var monitor = new TaskMonitor(taskCount, (results, cancelledCount) -> {
            final var editor = this.editor;
            if (editor != null) {
                List<WordwrapResult> r2 = new ArrayList<>();
                for (Object result : results) {
                    r2.add((WordwrapResult) result);
                }
                Collections.sort(r2);
                editor.postInLifecycle(() -> {
                    if (WordwrapLayout.this.editor != editor) {
                        // This layout could have been abandoned when waiting for Runnable execution
                        // See #307
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
                    rowTableVersion++;
                    editor.setLayoutBusy(false);
                    editor.getEventHandler().scrollBy(0, 0);
                });
            }
        });
        editor.setLayoutBusy(true);
        for (int i = 0; i < taskCount; i++) {
            var start = sizeEachTask * i;
            var end = i + 1 == taskCount ? (text.getLineCount() - 1) : (sizeEachTask * (i + 1) - 1);
            submitTask(new WordwrapAnalyzeTask(monitor, i, start, end));
        }
    }

    private boolean isFoldingEnabled() {
        return editor != null && editor.isFoldingEnabled();
    }

    private void ensureFoldingCache() {
        if (!isFoldingEnabled() || rowTable == null || rowTable.isEmpty()) {
            return;
        }
        final long mappingVersion = editor.getFoldingManager().getMappingVersion();
        if (lastMappingVersion == mappingVersion && lastRowTableVersion == rowTableVersion) {
            return;
        }
        lastMappingVersion = mappingVersion;
        lastRowTableVersion = rowTableVersion;
        final int size = rowTable.size();
        final int[] tableToVisible = new int[size];
        Arrays.fill(tableToVisible, -1);
        int visibleCount = 0;
        for (int i = 0; i < size; i++) {
            final int line = rowTable.get(i).line;
            if (!editor.isLineHiddenByFolding(line)) {
                tableToVisible[i] = visibleCount++;
            }
        }
        final int[] visibleToTable = new int[visibleCount];
        int vi = 0;
        for (int i = 0; i < size; i++) {
            if (tableToVisible[i] >= 0) {
                visibleToTable[vi++] = i;
            }
        }
        tableRowToVisibleRow = tableToVisible;
        visibleRowToTableRow = visibleToTable;
    }

    private int findRow(int line) {
        int index;
        // Binary find line
        int left = 0, right = rowTable.size();
        while (left <= right) {
            var mid = (left + right) / 2;
            if (mid < 0 || mid >= rowTable.size()) {
                left = Math.max(0, Math.min(rowTable.size() - 1, mid));
                break;
            }
            int value = rowTable.get(mid).line;
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
        while (index > 0 && rowTable.get(index).startColumn > 0) {
            index--;
        }
        return index;
    }

    public int findRow(int line, int column) {
        int row = findRow(line);
        while (rowTable.get(row).endColumn <= column && row + 1 < rowTable.size() && rowTable.get(row + 1).line == line) {
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
        rowTableVersion++;
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
    }

    @NonNull
    @Override
    public Row getRowAt(int rowIndex) {
        if (rowTable.isEmpty()) {
            var r = new Row();
            final int line = isFoldingEnabled() ? editor.getFoldingManager().getLineForVisibleRow(rowIndex) : rowIndex;
            r.startColumn = 0;
            r.endColumn = text.getColumnCount(line);
            r.isLeadingRow = true;
            r.isTrailingRow = true;
            r.lineIndex = line;
            r.inlayHints = getInlayHints(line);
            return r;
        }
        if (isFoldingEnabled()) {
            ensureFoldingCache();
            if (rowIndex < 0) {
                rowIndex = 0;
            } else if (rowIndex >= visibleRowToTableRow.length) {
                rowIndex = Math.max(0, visibleRowToTableRow.length - 1);
            }
            if (visibleRowToTableRow.length == 0) {
                var r = new Row();
                r.startColumn = 0;
                r.endColumn = text.getColumnCount(0);
                r.isLeadingRow = true;
                r.isTrailingRow = true;
                r.lineIndex = 0;
                r.inlayHints = getInlayHints(0);
                return r;
            }
            final int tableIndex = visibleRowToTableRow[rowIndex];
            final RowRegion region = rowTable.get(tableIndex);
            final boolean isLeadingRow = rowIndex <= 0 || rowTable.get(visibleRowToTableRow[rowIndex - 1]).line != region.line;
            final boolean isTrailingRow = rowIndex + 1 >= visibleRowToTableRow.length || rowTable.get(visibleRowToTableRow[rowIndex + 1]).line != region.line;
            return region.toRow(isLeadingRow, isTrailingRow, width);
        }
        var region = rowTable.get(rowIndex);
        var isLeadingRow = rowIndex <= 0 || rowTable.get(rowIndex - 1).line != region.line;
        var isTrailingRow = rowIndex + 1 >= rowTable.size() || rowTable.get(rowIndex + 1).line != region.line;
        return rowTable.get(rowIndex).toRow(isLeadingRow, isTrailingRow, width);
    }

    @Override
    public int getLineNumberForRow(int row) {
        if (rowTable.isEmpty()) {
            if (isFoldingEnabled()) {
                return editor.getFoldingManager().getLineForVisibleRow(row);
            }
            return Math.max(0, Math.min(row, text.getLineCount() - 1));
        }
        if (isFoldingEnabled()) {
            ensureFoldingCache();
            if (visibleRowToTableRow.length == 0) {
                return 0;
            }
            row = Math.max(0, Math.min(row, visibleRowToTableRow.length - 1));
            return rowTable.get(visibleRowToTableRow[row]).line;
        }
        return row >= rowTable.size() ? rowTable.get(rowTable.size() - 1).line : rowTable.get(row).line;
    }

    @NonNull
    @Override
    public RowIterator obtainRowIterator(int initialRow, @Nullable SparseArray<ContentLine> preloadedLines) {
        return rowTable.isEmpty() ? new LineBreakLayout.LineBreakLayoutRowItr(this, editor, text, initialRow, preloadedLines) : new WordwrapLayoutRowItr(initialRow);
    }

    @Override
    public long getUpPosition(int line, int column) {
        if (rowTable.isEmpty()) {
            if (line - 1 < 0) {
                return IntPair.pack(0, 0);
            }
            int prev = line - 1;
            while (prev > 0 && editor.isLineHiddenByFolding(prev)) {
                prev--;
            }
            int c_column = text.getColumnCount(prev);
            if (column > c_column) {
                column = c_column;
            }
            return IntPair.pack(prev, column);
        }
        int row = findRow(line, column);
        if (isFoldingEnabled()) {
            ensureFoldingCache();
            int vr = row >= 0 && row < tableRowToVisibleRow.length ? tableRowToVisibleRow[row] : -1;
            if (vr < 0) {
                vr = editor.getFoldingManager().getVisibleRowForLine(line);
            }
            if (vr > 0) {
                final var lastRow = rowTable.get(visibleRowToTableRow[vr - 1]);
                var offset = column - rowTable.get(row).startColumn;
                var max = lastRow.endColumn - lastRow.startColumn;
                offset = Math.min(offset, max);
                return IntPair.pack(lastRow.line, lastRow.startColumn + offset);
            }
            return IntPair.pack(0, 0);
        }
        if (row > 0) {
            var offset = column - rowTable.get(row).startColumn;
            var lastRow = rowTable.get(row - 1);
            var max = lastRow.endColumn - lastRow.startColumn;
            offset = Math.min(offset, max);
            return IntPair.pack(lastRow.line, lastRow.startColumn + offset);
        }
        return IntPair.pack(0, 0);
    }

    @Override
    public long getDownPosition(int line, int column) {
        if (rowTable.isEmpty()) {
            int c_line = text.getLineCount();
            int next = line + 1;
            while (next < c_line && editor.isLineHiddenByFolding(next)) {
                next++;
            }
            if (next >= c_line) {
                return IntPair.pack(line, text.getColumnCount(line));
            }
            int c_column = text.getColumnCount(next);
            if (column > c_column) {
                column = c_column;
            }
            return IntPair.pack(next, column);
        }
        int row = findRow(line, column);
        if (isFoldingEnabled()) {
            ensureFoldingCache();
            int vr = row >= 0 && row < tableRowToVisibleRow.length ? tableRowToVisibleRow[row] : -1;
            if (vr < 0) {
                vr = editor.getFoldingManager().getVisibleRowForLine(line);
            }
            if (vr + 1 < visibleRowToTableRow.length) {
                final var nextRow = rowTable.get(visibleRowToTableRow[vr + 1]);
                var offset = column - rowTable.get(row).startColumn;
                var max = nextRow.endColumn - nextRow.startColumn;
                offset = Math.min(offset, max);
                return IntPair.pack(nextRow.line, nextRow.startColumn + offset);
            }
            return IntPair.pack(line, text.getColumnCount(line));
        }
        if (row + 1 < rowTable.size()) {
            var offset = column - rowTable.get(row).startColumn;
            var nextRow = rowTable.get(row + 1);
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
        return getRowCount() * editor.getRowHeight();
    }

    @Override
    public int getRowIndexForPosition(int index) {
        var pos = editor.getText().getIndexer().getCharPosition(index);
        var line = pos.line;
        if (rowTable.isEmpty()) {
            return isFoldingEnabled() ? editor.getFoldingManager().getVisibleRowForLine(line) : line;
        }
        var column = pos.column;
        int row = findRow(line);
        if (row < rowTable.size()) {
            var region = rowTable.get(row);
            if (region.line != line) {
                return isFoldingEnabled() ? editor.getFoldingManager().getVisibleRowForLine(line) : 0;
            }
            while (region.startColumn < column && row + 1 < rowTable.size()) {
                row++;
                region = rowTable.get(row);
                if (region.line != line || region.startColumn > column) {
                    row--;
                    break;
                }
            }
            if (isFoldingEnabled()) {
                ensureFoldingCache();
                final int vr = row >= 0 && row < tableRowToVisibleRow.length ? tableRowToVisibleRow[row] : -1;
                return vr >= 0 ? vr : editor.getFoldingManager().getVisibleRowForLine(line);
            }
            return row;
        }
        return isFoldingEnabled() ? editor.getFoldingManager().getVisibleRowForLine(line) : 0;
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
            int row = Math.max((int) (offsetY / editor.getRowHeight()), 0);
            if (isFoldingEnabled()) {
                row = Math.max(0, Math.min(row, editor.getFoldingManager().getVisibleRowCount() - 1));
            }
            final int line = isFoldingEnabled() ? editor.getFoldingManager().getLineForVisibleRow(row) : Math.min(text.getLineCount() - 1, row);
            if (line < 0) {
                return new VisualLocation(0, 0, null, false);
            }
            var tr = editor.getRenderer().createTextRow(row);
            var pos = tr.getElementPositionForCursorOffset(offsetX);
            return new VisualLocation(line, pos.textOffset, pos.element, pos.isInElementBounds);
        }
        int row = (int) (offsetY / editor.getRowHeight());
        if (isFoldingEnabled()) {
            ensureFoldingCache();
            row = Math.max(0, Math.min(row, visibleRowToTableRow.length - 1));
            if (visibleRowToTableRow.length == 0) {
                return new VisualLocation(0, 0, null, false);
            }
            RowRegion region = rowTable.get(visibleRowToTableRow[row]);
            if (region.startColumn != 0) {
                offsetX -= miniGraphWidth;
            }
            offsetX -= region.getRenderTranslateX(width);
            var tr = editor.getRenderer().createTextRow(row);
            var pos = tr.getElementPositionForCursorOffset(offsetX);
            return new VisualLocation(region.line, pos.textOffset, pos.element, pos.isInElementBounds);
        }
        row = Math.max(0, Math.min(row, rowTable.size() - 1));
        RowRegion region = rowTable.get(row);
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
        if (rowTable.isEmpty()) {
            final int row = isFoldingEnabled() ? editor.getFoldingManager().getVisibleRowForLine(line) : line;
            dest[0] = editor.getRowBottom(row);
            var tr = editor.getRenderer().createTextRow(row);
            dest[1] = tr.getCursorOffsetForIndex(column);
            return dest;
        }
        int row = findRow(line);
        if (row < rowTable.size()) {
            RowRegion region = rowTable.get(row);
            if (region.line != line) {
                dest[0] = dest[1] = 0;
                return dest;
            }
            while (region.startColumn < column && row + 1 < rowTable.size()) {
                row++;
                region = rowTable.get(row);
                if (region.line != line || region.startColumn > column) {
                    row--;
                    region = rowTable.get(row);
                    break;
                }
            }
            final int visibleRow;
            if (isFoldingEnabled()) {
                ensureFoldingCache();
                final int vr = row >= 0 && row < tableRowToVisibleRow.length ? tableRowToVisibleRow[row] : -1;
                visibleRow = vr >= 0 ? vr : editor.getFoldingManager().getVisibleRowForLine(line);
            } else {
                visibleRow = row;
            }
            dest[0] = editor.getRowBottom(visibleRow);
            var tr = editor.getRenderer().createTextRow(visibleRow);
            dest[1] = tr.getCursorOffsetForIndex(column);
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
        if (editor.isLineHiddenByFolding(line)) {
            return 0;
        }
        if (rowTable.isEmpty()) {
            return 1;
        }
        int row = findRow(line);
        int count = 0;
        while (row < rowTable.size() && rowTable.get(row).line == line) {
            count++;
            row++;
        }
        return count;
    }

    /**
     * Get soft breaks on the given line
     */
    public List<Integer> getSoftBreaksForLine(int line) {
        if (rowTable.isEmpty()) {
            return Collections.emptyList();
        }
        int row = findRow(line);
        var list = new ArrayList<Integer>();
        while (row < rowTable.size() && rowTable.get(row).line == line) {
            var column = rowTable.get(row).startColumn;
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
            if (isFoldingEnabled()) {
                return editor.getFoldingManager().getVisibleRowCount();
            }
            return text.getLineCount();
        }
        if (isFoldingEnabled()) {
            ensureFoldingCache();
            return visibleRowToTableRow.length;
        }
        return rowTable.size();
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

        private int tableRowForCurrent() {
            if (isFoldingEnabled()) {
                ensureFoldingCache();
                return visibleRowToTableRow[currentRow];
            }
            return currentRow;
        }

        private int rowCount() {
            return getRowCount();
        }

        private boolean sameLineAsNeighbor(int tableRow, int delta) {
            final int neighbor;
            if (isFoldingEnabled()) {
                neighbor = visibleRowToTableRow[currentRow + delta];
            } else {
                neighbor = tableRow + delta;
            }
            return rowTable.get(neighbor).line == rowTable.get(tableRow).line;
        }

        @NonNull
        @Override
        public Row next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final int tableRow = tableRowForCurrent();
            RowRegion region = rowTable.get(tableRow);
            result.lineIndex = region.line;
            result.startColumn = region.startColumn;
            result.endColumn = region.endColumn;
            result.inlayHints = region.inlayHints == null ? Collections.emptyList() : region.inlayHints;
            result.isLeadingRow = currentRow <= 0 || !sameLineAsNeighbor(tableRow, -1);
            result.isTrailingRow = currentRow + 1 >= rowCount() || !sameLineAsNeighbor(tableRow, 1);
            result.renderTranslateX = region.getRenderTranslateX(width);
            currentRow++;
            return result;
        }

        @Override
        public boolean hasNext() {
            if (isFoldingEnabled()) {
                ensureFoldingCache();
                return currentRow >= 0 && currentRow < visibleRowToTableRow.length;
            }
            return currentRow >= 0 && currentRow < rowTable.size();
        }

        @Override
        public void reset() {
            currentRow = initRow;
        }
    }

    private class WordwrapAnalyzeTask extends LayoutTask<WordwrapResult> {

        private final int start, end, id;
        private final Paint paint;

        WordwrapAnalyzeTask(TaskMonitor monitor, int id, int start, int end) {
            super(monitor);
            this.start = start;
            this.id = id;
            this.end = end;
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
    }

}
