/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
 *    Copyright (C) 2020-2021  Rosemoe
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

import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Base layout implementation of {@link Layout}
 * This class has basic methods for its subclasses to measure texts
 *
 * @author Rose
 */
public abstract class AbstractLayout implements Layout {

    protected CodeEditor editor;
    protected Content text;

    public AbstractLayout(CodeEditor editor, Content text) {
        this.editor = editor;
        this.text = text;
        updateMeasureCaches(0, text == null ? 0 : text.getLineCount());
    }

    protected float measureText(CharSequence text, int start, int end) {
        return editor.measureText(text, start, end - start);
    }

    protected float[] orderedFindCharIndex(float targetOffset, ContentLine str, int index, int end) {
        return editor.findFirstVisibleChar(-targetOffset, index, end, 0, str);
    }

    protected float[] orderedFindCharIndex(float targetOffset, ContentLine str) {
       return orderedFindCharIndex(targetOffset, str, 0, str.length());
    }

    private void updateMeasureCaches(int startLine, int endLine) {
        if (text == null) {
            return;
        }
        if (text.getLineCount() > 10000) {
            // Disable the cache if text is too large
            while (startLine <= endLine && startLine < text.getLineCount()) {
                text.getLine(startLine).widthCache = null;
                startLine++;
            }
        } else {
            while (startLine <= endLine && startLine < text.getLineCount()) {
                ContentLine line = text.getLine(startLine);
                // Do not create cache for long lines
                if (line.length() < 128) {
                    if (line.widthCache == null) {
                        line.widthCache = new float[128];
                    }
                    editor.getTextPaint().getTextWidths(line.value, 0, line.length(), line.widthCache);
                } else {
                    line.widthCache = null;
                }
                startLine++;
            }
        }
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {
        updateMeasureCaches(startLine, endLine);
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent) {
        updateMeasureCaches(startLine, endLine);
    }

    @Override
    public void destroyLayout() {
        editor = null;
        text = null;
    }

    @Override
    public void updateCache(int startLine, int endLine) {
        updateMeasureCaches(0, text == null ? 0 : text.getLineCount());
    }
}
