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

/**
 * This class is used to control cursor visibility
 *
 * @author Rose
 */
final class CursorBlink implements Runnable {

    final CodeEditor editor;
    long lastSelectionModificationTime = 0;
    int period;
    boolean visibility;
    boolean valid;
    private float[] buffer;

    CursorBlink(CodeEditor editor, int period) {
        visibility = true;
        this.editor = editor;
        this.period = period;
    }

    void setPeriod(int period) {
        this.period = period;
        if (period <= 0) {
            visibility = true;
            valid = false;
        } else {
            valid = true;
        }
    }

    void onSelectionChanged() {
        lastSelectionModificationTime = System.currentTimeMillis();
        visibility = true;
    }

    boolean isSelectionVisible() {
        buffer = editor.mLayout.getCharLayoutOffset(editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn(), buffer);
        return (buffer[0] >= editor.getOffsetY() && buffer[0] - editor.getRowHeight() <= editor.getOffsetY() + editor.getHeight()
                && buffer[1] >= editor.getOffsetX() && buffer[1] - 100f/* larger than a single character */ <= editor.getOffsetX() + editor.getWidth());
    }

    @Override
    public void run() {
        if (valid && period > 0) {
            if (System.currentTimeMillis() - lastSelectionModificationTime >= period * 2) {
                visibility = !visibility;
                if (!editor.getCursor().isSelected() && isSelectionVisible()) {
                    editor.invalidate();
                }
            }
            editor.postDelayed(this, period);
        } else {
            visibility = true;
        }
    }

}
