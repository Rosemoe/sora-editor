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

import android.graphics.Paint;

import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.text.FontCache;

/**
 * Base layout implementation of {@link Layout}
 * This class has basic methods for its subclasses to measure texts
 *
 * @author Rose
 */
abstract class AbstractLayout implements Layout {

    protected CodeEditor editor;
    protected Content text;
    protected Paint shadowPaint;
    protected FontCache fontCache;

    public AbstractLayout(CodeEditor editor, Content text) {
        this.editor = editor;
        this.text = text;
        shadowPaint = new Paint(editor.getTextPaint());
        fontCache = new FontCache();
    }

    protected float measureText(CharSequence text, int start, int end) {
        int tabCount = 0;
        end = Math.min(text.length(), end);
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == '\t') {
                tabCount++;
            }
        }
        float extraWidth = fontCache.measureChar(' ', shadowPaint) * editor.getTabWidth() - fontCache.measureChar('\t', shadowPaint);
        return fontCache.measureText(text, start, end, shadowPaint) + tabCount * extraWidth;
    }

    protected float[] orderedFindCharIndex(float targetOffset, CharSequence str, int index, int end) {
        float width = 0f;
        while (index < end && width < targetOffset) {
            float single = fontCache.measureChar(str.charAt(index), shadowPaint);
            if (str.charAt(index) == '\t') {
                single = editor.getTabWidth() * fontCache.measureChar(' ', shadowPaint);
            }
            width += single;
            index++;
        }
        return new float[]{index, width};
    }

    protected float[] orderedFindCharIndex(float targetOffset, CharSequence str) {
        float width = 0f;
        int index = 0;
        int length = str.length();
        while (index < length && width < targetOffset) {
            float single = fontCache.measureChar(str.charAt(index), shadowPaint);
            if (str.charAt(index) == '\t') {
                single = editor.getTabWidth() * fontCache.measureChar(' ', shadowPaint);
            }
            width += single;
            index++;
        }
        return new float[]{index, width};
    }

    @Override
    public void destroyLayout() {
        editor = null;
        text = null;
        shadowPaint = null;
        fontCache = null;
    }

}
