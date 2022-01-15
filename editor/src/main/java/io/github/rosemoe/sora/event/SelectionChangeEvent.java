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
package io.github.rosemoe.sora.event;

import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * This event happens when text is edited by the user, or the user click the view to change the
 * position of selection.
 *
 * Note that you should not change returned CharPosition objects because they are shared in an event
 * dispatch.
 */
public class SelectionChangeEvent extends Event {

    private final CharPosition left;
    private final CharPosition right;

    public SelectionChangeEvent(CodeEditor editor) {
        super(editor);
        var cursor = editor.getText().getCursor();
        left = cursor.left();
        right = cursor.right();
    }

    /**
     * Get the left selection's position
     */
    public CharPosition getLeft() {
        return left;
    }

    /**
     * Get the right selection's position
     */
    public CharPosition getRight() {
        return right;
    }

    /**
     * Checks whether text is selected
     */
    public boolean isSelected() {
        return left.index != right.index;
    }

}
