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

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.github.rosemoe.sora.lang.styling.inlayHint.CharacterSide;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;
import io.github.rosemoe.sora.lang.styling.util.PointAnchoredContainer;

/**
 * This class represents a 'row' in editor.
 * Editor uses this to draw rows
 *
 * @author Rosemoe
 */
public class Row {

    /**
     * The index in lines
     * But not row index
     */
    public int lineIndex;

    /**
     * Whether this row is a start of a line
     * Editor will draw line number to left of this row to indicate this
     */
    public boolean isLeadingRow;

    /**
     * Start index in target line
     */
    public int startColumn;

    /**
     * End index in target line
     */
    public int endColumn;

    public List<InlayHint> inlayHints;

    private AbstractLayout layout;

    public Row(@NonNull AbstractLayout layout) {
        this.layout = layout;
    }

    public Iterator<RowElement> elements() {
        return new Iterator<>() {
            final List<InlayHint> inlays = layout.getInlayHints(lineIndex);
            int inlayIndex = 0;
            int lastEndIndex = startColumn;
            final int columnCount = layout.text.getColumnCount(lineIndex);
            final RowElement element = new RowElement();

            {
                Collections.sort(inlays, (a, b) -> {
                    int res = PointAnchoredContainer.Companion.getComparator().compare(a, b);
                    if (res == 0) {
                        return Integer.compare(a.getDisplaySide().ordinal(), b.getDisplaySide().ordinal());
                    }
                    return res;
                });
                while (inlayIndex < inlays.size() && inlays.get(inlayIndex).getColumn() < startColumn) {
                    inlayIndex++;
                }
            }

            private int getExpectedInlayColumn() {
                var inlay = inlays.get(inlayIndex);
                var position = inlay.getColumn();
                if (inlay.getDisplaySide() == CharacterSide.RIGHT) {
                    position = Math.min(columnCount, position + 1);
                }
                return position;
            }

            @Override
            public boolean hasNext() {
                return lastEndIndex < endColumn || (inlayIndex < inlays.size() && getExpectedInlayColumn() <= endColumn);
            }

            @Override
            public RowElement next() {
                if (hasNext()) {
                    if (inlayIndex < inlays.size() && getExpectedInlayColumn() <= endColumn) {
                        var inlay = inlays.get(inlayIndex);
                        var position = getExpectedInlayColumn();
                        if (lastEndIndex == position) {
                            inlayIndex++;
                            element.type = RowElementTypes.INLAY_HINT;
                            element.inlayHint = inlay;
                        } else {
                            // lastEndIndex < position
                            element.type = RowElementTypes.TEXT;
                            element.startColumn = lastEndIndex;
                            element.endColumn = position;
                            lastEndIndex = position;
                        }
                    } else if (lastEndIndex < endColumn) {
                        element.type = RowElementTypes.TEXT;
                        element.startColumn = lastEndIndex;
                        element.endColumn = endColumn;
                        lastEndIndex = endColumn;
                    }
                    return element;
                }
                return null;
            }
        };
    }

}
