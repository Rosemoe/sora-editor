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
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Styles;

/**
 * 管理代码折叠状态与行可见性。
 *
 * <p>折叠区域基于 {@link Styles#blocksByStart}（即 {@link CodeBlock}）生成：每个 startLine 对应一个最外层 endLine。 折叠后隐藏
 * (startLine, endLine] 的所有行，但 startLine 自身仍保持可见。
 *  @author Ghost
 */
public final class FoldingManager {

  private static final String TAG = "SoraFolding";

  private final CodeEditor editor;

  /** key=startLine, value=endLine (inclusive) */
  private final SparseIntArray foldableEndsByStartLine = new SparseIntArray();

  /** key=startLine, value=true */
  private final SparseBooleanArray collapsedByStartLine = new SparseBooleanArray();

  /** 隐藏区间：key=startHiddenLine, value=endHiddenLine (inclusive)，按 key 升序且互不重叠 */
  private final SparseIntArray hiddenRanges = new SparseIntArray();

  private boolean mappingDirty = true;
  private int[] visibleLines = new int[0]; // visibleRow -> line
  private int[] lineToVisibleRow = new int[0]; // line -> visibleRow, -1 if hidden

  /**
   * Bumped whenever the visible line mapping may change (fold toggled, line shift, new styles,
   * etc.). Layouts can use this to invalidate cached row translations.
   */
  private long mappingVersion = 0;

  public FoldingManager(@NonNull CodeEditor editor) {
    this.editor = editor;
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

  /** 当前可见行映射的版本号，布局可用于判断是否需要重建可见行缓存。 */
  public long getMappingVersion() {
    return mappingVersion;
  }

  /**
   * 刷新可折叠区域集合，并清理失效的折叠状态。
   *
   * @return true 表示“隐藏区间”发生变化，需要刷新布局/滚动范围
   */
  public boolean onStylesUpdated(@Nullable Styles styles) {
    final int oldHiddenHash = hiddenRangesHash();

    foldableEndsByStartLine.clear();
    final int lineCount = editor.getLineCount();
    final int lastLine = Math.max(0, lineCount - 1);
    if (styles != null) {
      final List<CodeBlock> blocksByStart = styles.blocksByStart;
      if (blocksByStart != null) {
        for (int i = 0; i < blocksByStart.size(); i++) {
          final CodeBlock block = blocksByStart.get(i);
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
          final int oldEnd = foldableEndsByStartLine.get(startLine, -1);
          if (endLine > oldEnd) {
            foldableEndsByStartLine.put(startLine, endLine);
          }
        }
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

  public boolean isFoldableLine(int startLine) {
    return foldableEndsByStartLine.indexOfKey(startLine) >= 0
        && foldableEndsByStartLine.get(startLine) > startLine;
  }

  /**
   * 查找“包含指定行”的可折叠起始行（更偏向内层：startLine 越大越优先）。
   *
   * @return startLine，找不到返回 -1
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
      final int endLine = foldableEndsByStartLine.valueAt(i);
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
   * 查找“包含指定行”的已折叠起始行（更偏向内层：startLine 越大越优先）。
   *
   * @return startLine，找不到返回 -1
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
      final int endLine = foldableEndsByStartLine.get(startLine, -1);
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
      if (editor.getProps().foldingDebugLogEnabled) {
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
      if (editor.getProps().foldingDebugLogEnabled) {
        Log.d(TAG, "fold: startLine=" + startLine + " already collapsed");
      }
      return false;
    }
    collapsedByStartLine.put(startLine, true);
    rebuildHiddenRanges();
    mappingDirty = true;
    if (editor.getProps().foldingDebugLogEnabled) {
      Log.d(
          TAG,
          "fold: startLine="
              + startLine
              + " endLine="
              + foldableEndsByStartLine.get(startLine)
              + " hiddenRanges="
              + hiddenRanges.size());
    }
    return true;
  }

  public boolean unfold(int startLine) {
    if (!collapsedByStartLine.get(startLine)) {
      if (editor.getProps().foldingDebugLogEnabled) {
        Log.d(TAG, "unfold: startLine=" + startLine + " not collapsed");
      }
      return false;
    }
    collapsedByStartLine.delete(startLine);
    rebuildHiddenRanges();
    mappingDirty = true;
    if (editor.getProps().foldingDebugLogEnabled) {
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
      final int endLine = foldableEndsByStartLine.valueAt(i);
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
    final int idx = foldableEndsByStartLine.indexOfKey(startLine);
    if (idx < 0) {
      return null;
    }
    final int endLine = foldableEndsByStartLine.valueAt(idx);
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
      return Math.max(0, editor.getLineCount() - 1);
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

  /** 文本插入/删除后用于同步折叠状态的行号。 */
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
    final int lineCount = editor.getLineCount();
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
      final int endLine = foldableEndsByStartLine.get(startLine, -1);
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
