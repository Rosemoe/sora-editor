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
package io.github.rosemoe.sora.lang.styling.inlayHint;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;

public class InlayHintManager {

    private final static Comparator<InlayHint> COLUMN_COMPARATOR = (a, b) -> Integer.compare(a.getPosition().column, b.getPosition().column);

    private final TreeSet<LineInlayHint> lines;
    private final LineInlayHint E = new LineInlayHint(0);
    private SparseUpdateRange layoutRange;

    public InlayHintManager() {
        lines = new TreeSet<>();
        layoutRange = new SparseUpdateRange();
    }

    private LineInlayHint getLine(int line, boolean createIfNotFound) {
        E.setLine(line);
        var result = lines.ceiling(E);
        result = (result == null || result.getLine() != line) ? null : result;
        if (createIfNotFound && result == null) {
            result = new LineInlayHint(line);
            lines.add(result);
        }
        return result;
    }

    public synchronized void addInlayHint(@NonNull InlayHint hint) {
        var line = hint.getPosition().line;
        var list = getLine(line, true).getHints();
        list.add(hint);
        Collections.sort(list, COLUMN_COMPARATOR);
        layoutRequired(line);
    }

    public synchronized void removeInlayHint(@NonNull InlayHint hint) {
        var line = hint.getPosition().line;
        var list = getLine(line, false);
        if (list != null && list.getHints().remove(hint)) {
            layoutRequired(line);
        }
    }

    private void layoutRequired(int line) {
        layoutRange.addLine(line);
    }

    public synchronized void removeInlayHintsOn(int line) {
        var list = getLine(line, false);
        if (list != null && list.getHints().size() > 0) {
            list.getHints().clear();
            layoutRequired(line);
        }
    }

    public synchronized List<InlayHint> getInlayHintsOn(int line) {
        var list = getLine(line, false);
        if (list != null && list.getHints().size() > 0) {
            return new ArrayList<>(list.getHints());
        }
        return null;
    }

    @NonNull
    public synchronized StyleUpdateRange getUpdatedRange() {
        var current = layoutRange;
        layoutRange = new SparseUpdateRange();
        return current;
    }


    public synchronized void afterInsert(Content content, CharPosition start, CharPosition end) {
        int length = end.index - start.index;
        E.setLine(start.line);
        for (var line : lines.tailSet(E)) {
            if (line.getLine() == start.line) {
                var itr = line.getHints().iterator();
                while (itr.hasNext()) {
                    var element = itr.next();
                    if (element.getPosition().column >= start.column) {
                        if (end.line == start.line) {
                            element.getPosition().column += end.column - start.column;
                            element.getPosition().index += end.column - start.column;
                        } else {
                            itr.remove();
                        }
                    }
                }
            } else if (line.getLine() > start.line) {
                line.setLine(line.getLine() + (end.line - start.line));
                for (InlayHint hint : line.getHints()) {
                    hint.getPosition().line += (end.line - start.line);
                    hint.getPosition().index += length;
                }
            }
        }
    }

    public synchronized void afterDelete(Content content, CharPosition start, CharPosition end) {
        E.setLine(start.line);
        int length = end.index - start.index;
        var setItr = lines.tailSet(E).iterator();
        while (setItr.hasNext()) {
            var line = setItr.next();
            if (line.getLine() == start.line) {
                var itr = line.getHints().iterator();
                while (itr.hasNext()) {
                    var element = itr.next();
                    if (element.getPosition().column >= start.column) {
                        itr.remove();
                    }
                }
            } else if (line.getLine() > start.line && line.getLine() <= end.line) {
                setItr.remove();
            } else if (line.getLine() > start.line) {
                line.setLine(line.getLine() - (end.line - start.line));
                for (InlayHint hint : line.getHints()) {
                    hint.getPosition().line -= (end.line - start.line);
                    hint.getPosition().index -= length;
                }
            }
        }
    }
}
