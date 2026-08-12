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

import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Styles;

/**
 * Manages code folding state and line visibility.
 *
 * <p>Foldable regions are derived directly from {@link Styles#blocks}. On every refresh, all
 * distinct end lines are merged per start line (a single start line may have several
 * nested/sibling foldable regions; all of them are kept, none are overwritten). Folding a region
 * hides every line in {@code (startLine, endLine]}, while {@code startLine} itself stays visible.
 *
 * <p>This class only depends on {@link LineCountProvider}, not the whole {@link CodeEditor}, so it
 * can be constructed and exercised independently of the widget.
 *
 * @author Ghost
 */
public final class FoldingManager {

    private static final String TAG = "SoraFolding";
    private static final int[] EMPTY_ENDS = new int[0];

    private final LineCountProvider lineCountProvider;
    private final BooleanSupplier debugLogEnabled;

    /** key=startLine, value=sorted (ascending), distinct end lines of every region starting there */
    private final SparseArray<int[]> foldableEndsByStartLine = new SparseArray<>();

    /** key=startLine, value=true */
    private final SparseBooleanArray collapsedByStartLine = new SparseBooleanArray();

    /** Hidden ranges: key=startHiddenLine, value=endHiddenLine (inclusive), sorted ascending by key and non-overlapping */
    private final SparseIntArray hiddenRanges = new SparseIntArray();

    private boolean mappingDirty = true;
    private int[] visibleLines = new int[0]; // visibleRow -> line
    private int[] lineToVisibleRow = new int[0]; // line -> visibleRow, -1 if hidden

    /**
     * Bumped whenever the visible line mapping may change (fold toggled, line shift, new styles,
     * etc.). Layouts can use this to invalidate cached row translations.
     */
    private long mappingVersion = 0;

    public FoldingManager(@NonNull LineCountProvider lineCountProvider, @NonNull BooleanSupplier debugLogEnabled) {
        this.lineCountProvider = lineCountProvider;
        this.debugLogEnabled = debugLogEnabled;
    }

    public void resetForNewText() {
        foldableEndsByStartLine.clear();
        collapsedByStartLine.clear();
        hiddenRanges.clear();
        mappingDirty = true;
        visibleLines = new int[0];
        lineToVisibleRow = new int[0];
        mappingVersion++;
    }

    /** Current version of the visible line mapping; layouts can use this to decide whether their cached row translation needs rebuilding. */
    public long getMappingVersion() {
        return mappingVersion;
    }

    /**
     * Refreshes the set of foldable regions and clears out collapsed states that no longer have
     * a matching region.
     *
     * @return true if the hidden ranges changed and the layout/scroll range needs to be refreshed
     */
    public boolean onStylesUpdated(@Nullable Styles styles) {
        final int oldHiddenHash = hiddenRangesHash();

        foldableEndsByStartLine.clear();
        final int lineCount = lineCountProvider.getLineCount();
        final int lastLine = Math.max(0, lineCount - 1);
        if (styles != null && styles.blocks != null) {
            final Map<Integer, TreeSet<Integer>> gathered = new HashMap<>();
            final List<CodeBlock> blocks = styles.blocks;
            for (int i = 0; i < blocks.size(); i++) {
                final CodeBlock block = blocks.get(i);
                if (block == null) {
                    continue;
                }
                int startLine = block.startLine;
                if (startLine < 0 || startLine > lastLine) {
                    continue;
                }
                int endLine = block.endLine;
                if (endLine > lastLine) {
                    endLine = lastLine;
                }
                if (endLine <= startLine) {
                    continue;
                }
                gathered.computeIfAbsent(startLine, k -> new TreeSet<>()).add(endLine);
            }
            for (Map.Entry<Integer, TreeSet<Integer>> entry : gathered.entrySet()) {
                final TreeSet<Integer> ends = entry.getValue();
                final int[] array = new int[ends.size()];
                int idx = 0;
                for (int end : ends) {
                    array[idx++] = end;
                }
                foldableEndsByStartLine.put(entry.getKey(), array);
            }
        }

        // Remove collapsed states for non-existing foldable regions
        for (int i = collapsedByStartLine.size() - 1; i >= 0; i--) {
            final int startLine = collapsedByStartLine.keyAt(i);
            if (foldableEndsByStartLine.indexOfKey(startLine) < 0) {
                collapsedByStartLine.delete(startLine);
            }
        }

        rebuildHiddenRanges();
        mappingDirty = true;
        return oldHiddenHash != hiddenRangesHash();
    }

    /**
     * Returns the endLine of every foldable region starting on the given line (ascending,
     * distinct). A single line may have several nested/sibling foldable regions. Returns an
     * empty array if none exist.
     */
    @NonNull
    public int[] getFoldableEndLines(int startLine) {
        final int[] ends = foldableEndsByStartLine.get(startLine);
        return ends == null ? EMPTY_ENDS : Arrays.copyOf(ends, ends.length);
    }

    /** The endLine of the "default" (outermost) foldable region starting on this line, or -1 if there is none. */
    private int outermostEndLine(int startLine) {
        final int[] ends = foldableEndsByStartLine.get(startLine);
        if (ends == null || ends.length == 0) {
            return -1;
        }
        return ends[ends.length - 1];
    }

    public boolean isFoldableLine(int startLine) {
        return outermostEndLine(startLine) > startLine;
    }

    /**
     * Finds the foldable start line that contains the given line (prefers inner regions: a
     * larger startLine wins).
     *
     * @return the startLine, or -1 if none is found
     */
    public int findFoldableStartLineForLine(int line) {
        if (foldableEndsByStartLine.size() == 0) {
            return -1;
        }
        int idx = foldableEndsByStartLine.indexOfKey(line);
        if (idx < 0) {
            idx = ~idx - 1;
        }
        for (int i = idx; i >= 0; i--) {
            final int startLine = foldableEndsByStartLine.keyAt(i);
            final int endLine = outermostEndLine(startLine);
            if (startLine < line && endLine >= line) {
                return startLine;
            }
            if (startLine == line && endLine > line) {
                return startLine;
            }
        }
        return -1;
    }

    /**
     * Finds the collapsed start line that contains the given line (prefers inner regions: a
     * larger startLine wins).
     *
     * @return the startLine, or -1 if none is found
     */
    public int findCollapsedStartLineForLine(int line) {
        if (collapsedByStartLine.size() == 0) {
            return -1;
        }
        int idx = collapsedByStartLine.indexOfKey(line);
        if (idx < 0) {
            idx = ~idx - 1;
        }
        for (int i = idx; i >= 0; i--) {
            final int startLine = collapsedByStartLine.keyAt(i);
            final int endLine = outermostEndLine(startLine);
            if (endLine <= startLine) {
                continue;
            }
            if (startLine < line && endLine >= line) {
                return startLine;
            }
            if (startLine == line) {
                return startLine;
            }
        }
        return -1;
    }

    public boolean isCollapsed(int startLine) {
        return collapsedByStartLine.get(startLine);
    }

    public boolean fold(int startLine) {
        if (!isFoldableLine(startLine)) {
            if (debugLogEnabled.getAsBoolean()) {
                Log.d(
                        TAG,
                        "fold: startLine="
                                + startLine
                                + " not foldable (foldables="
                                + foldableEndsByStartLine.size()
                                + ")");
            }
            return false;
        }
        if (collapsedByStartLine.get(startLine)) {
            if (debugLogEnabled.getAsBoolean()) {
                Log.d(TAG, "fold: startLine=" + startLine + " already collapsed");
            }
            return false;
        }
        collapsedByStartLine.put(startLine, true);
        rebuildHiddenRanges();
        mappingDirty = true;
        if (debugLogEnabled.getAsBoolean()) {
            Log.d(
                    TAG,
                    "fold: startLine="
                            + startLine
                            + " endLine="
                            + outermostEndLine(startLine)
                            + " hiddenRanges="
                            + hiddenRanges.size());
        }
        return true;
    }

    public boolean unfold(int startLine) {
        if (!collapsedByStartLine.get(startLine)) {
            if (debugLogEnabled.getAsBoolean()) {
                Log.d(TAG, "unfold: startLine=" + startLine + " not collapsed");
            }
            return false;
        }
        collapsedByStartLine.delete(startLine);
        rebuildHiddenRanges();
        mappingDirty = true;
        if (debugLogEnabled.getAsBoolean()) {
            Log.d(TAG, "unfold: startLine=" + startLine + " hiddenRanges=" + hiddenRanges.size());
        }
        return true;
    }

    public boolean toggle(int startLine) {
        return isCollapsed(startLine) ? unfold(startLine) : fold(startLine);
    }

    public void unfoldAll() {
        if (collapsedByStartLine.size() == 0) {
            return;
        }
        collapsedByStartLine.clear();
        hiddenRanges.clear();
        mappingDirty = true;
        mappingVersion++;
    }

    public boolean foldAll() {
        if (foldableEndsByStartLine.size() == 0) {
            return false;
        }
        boolean changed = false;
        for (int i = 0; i < foldableEndsByStartLine.size(); i++) {
            final int startLine = foldableEndsByStartLine.keyAt(i);
            final int endLine = outermostEndLine(startLine);
            if (endLine > startLine && !collapsedByStartLine.get(startLine)) {
                collapsedByStartLine.put(startLine, true);
                changed = true;
            }
        }
        if (changed) {
            rebuildHiddenRanges();
            mappingDirty = true;
        }
        return changed;
    }

    @Nullable
    public FoldRegion getFoldRegion(int startLine) {
        final int endLine = outermostEndLine(startLine);
        if (endLine <= startLine) {
            return null;
        }
        return new FoldRegion(startLine, endLine, isCollapsed(startLine));
    }

    public boolean isLineHidden(int line) {
        if (hiddenRanges.size() == 0) {
            return false;
        }
        int idx = hiddenRanges.indexOfKey(line);
        if (idx >= 0) {
            return true;
        }
        idx = ~idx - 1;
        if (idx < 0) {
            return false;
        }
        return line <= hiddenRanges.valueAt(idx);
    }

    public int getVisibleRowCount() {
        ensureLineMappings();
        return visibleLines.length;
    }

    public int getLineForVisibleRow(int visibleRow) {
        ensureLineMappings();
        if (visibleRow < 0) {
            return 0;
        }
        if (visibleRow >= visibleLines.length) {
            return Math.max(0, lineCountProvider.getLineCount() - 1);
        }
        return visibleLines[visibleRow];
    }

    public int getVisibleRowForLine(int line) {
        ensureLineMappings();
        if (line < 0) {
            return 0;
        }
        if (line >= lineToVisibleRow.length) {
            return visibleLines.length == 0 ? 0 : visibleLines.length - 1;
        }
        final int res = lineToVisibleRow[line];
        if (res >= 0) {
            return res;
        }
        if (visibleLines.length == 0) {
            return 0;
        }
        final int idx = Arrays.binarySearch(visibleLines, line);
        final int insertionPoint = idx >= 0 ? idx : -idx - 1;
        return Math.max(0, Math.min(visibleLines.length - 1, insertionPoint - 1));
    }

    /** Shifts fold state line numbers to stay in sync after a text insertion/deletion. */
    public void onLineShift(int anchorLine, int deltaLines, int deletedEndLine) {
        if (deltaLines == 0) {
            mappingDirty = true;
            return;
        }
        // Shift collapsed keys
        final SparseBooleanArray newCollapsed = new SparseBooleanArray(collapsedByStartLine.size());
        for (int i = 0; i < collapsedByStartLine.size(); i++) {
            final int key = collapsedByStartLine.keyAt(i);
            if (deltaLines < 0) {
                // deletion: remove states inside deleted range [anchorLine, deletedEndLine]
                if (key >= anchorLine && key <= deletedEndLine) {
                    continue;
                }
            }
            int newKey = key;
            if (key > anchorLine) {
                newKey = key + deltaLines;
            }
            if (newKey >= 0) {
                newCollapsed.put(newKey, true);
            }
        }
        collapsedByStartLine.clear();
        for (int i = 0; i < newCollapsed.size(); i++) {
            collapsedByStartLine.put(newCollapsed.keyAt(i), true);
        }

        // foldable regions will be rebuilt from Styles soon; keep current but mark dirty
        rebuildHiddenRanges();
        mappingDirty = true;
    }

    private void ensureLineMappings() {
        if (!mappingDirty) {
            return;
        }
        final int lineCount = lineCountProvider.getLineCount();
        if (lineCount <= 0) {
            visibleLines = new int[0];
            lineToVisibleRow = new int[0];
            mappingDirty = false;
            return;
        }

        int visibleCount = 0;
        for (int line = 0; line < lineCount; line++) {
            if (!isLineHidden(line)) {
                visibleCount++;
            }
        }

        final int[] newVisibleLines = new int[visibleCount];
        final int[] newLineToVisible = new int[lineCount];
        for (int i = 0; i < lineCount; i++) {
            newLineToVisible[i] = -1;
        }
        int vi = 0;
        for (int line = 0; line < lineCount; line++) {
            if (!isLineHidden(line)) {
                newVisibleLines[vi] = line;
                newLineToVisible[line] = vi;
                vi++;
            }
        }

        visibleLines = newVisibleLines;
        lineToVisibleRow = newLineToVisible;
        mappingDirty = false;
    }

    private void rebuildHiddenRanges() {
        hiddenRanges.clear();
        mappingVersion++;
        if (collapsedByStartLine.size() == 0) {
            return;
        }
        int currentStart = -1;
        int currentEnd = -1;
        for (int i = 0; i < collapsedByStartLine.size(); i++) {
            final int startLine = collapsedByStartLine.keyAt(i);
            final int endLine = outermostEndLine(startLine);
            if (endLine <= startLine) {
                continue;
            }
            final int hideStart = startLine + 1;
            final int hideEnd = endLine;
            if (hideStart > hideEnd) {
                continue;
            }
            if (currentStart < 0) {
                currentStart = hideStart;
                currentEnd = hideEnd;
                continue;
            }
            if (hideStart <= currentEnd + 1) {
                currentEnd = Math.max(currentEnd, hideEnd);
            } else {
                hiddenRanges.put(currentStart, currentEnd);
                currentStart = hideStart;
                currentEnd = hideEnd;
            }
        }
        if (currentStart >= 0) {
            hiddenRanges.put(currentStart, currentEnd);
        }
    }

    private int hiddenRangesHash() {
        int h = 17;
        for (int i = 0; i < hiddenRanges.size(); i++) {
            h = 31 * h + hiddenRanges.keyAt(i);
            h = 31 * h + hiddenRanges.valueAt(i);
        }
        return h;
    }

    public static final class FoldRegion {
        public final int startLine;
        public final int endLine;
        public final boolean collapsed;

        public FoldRegion(int startLine, int endLine, boolean collapsed) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.collapsed = collapsed;
        }
    }
}