/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.widget;

import android.text.Editable;
import android.view.KeyEvent;

/**
 * Handles key events such SHIFT
 * @author Rosemoe
 */
public class KeyMetaStates extends android.text.method.MetaKeyKeyListener {

    private CodeEditor editor;
    /**
     * Dummy text used for Android original APIs
     */
    private Editable dest = Editable.Factory.getInstance().newEditable("");

    public KeyMetaStates(CodeEditor editor) {
        this.editor = editor;
    }

    public void onKeyDown(KeyEvent event) {
        super.onKeyDown(editor, dest, event.getKeyCode(), event);
    }

    public void onKeyUp(KeyEvent event) {
        super.onKeyUp(editor, dest, event.getKeyCode(), event);
    }

    public boolean isShiftPressed() {
        return getMetaState(dest, META_SHIFT_ON) != 0;
    }

    public void adjust() {
        adjustMetaAfterKeypress(dest);
    }

    public void clearMetaStates(int states) {
        clearMetaKeyState(editor, dest, states);
    }

}
