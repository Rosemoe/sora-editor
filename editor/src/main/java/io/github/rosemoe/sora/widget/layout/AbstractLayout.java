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
package io.github.rosemoe.sora.widget.layout;

import java.util.List;

import io.github.rosemoe.sora.data.Span;
import io.github.rosemoe.sora.graphics.GraphicTextRow;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Base layout implementation of {@link Layout}.
 * It provides some convenient methods to editor instance and text measuring.
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

    protected List<Span> getSpans(int line) {
        return editor.getSpansForLine(line);
    }

    protected float[] orderedFindCharIndex(float targetOffset, ContentLine str, int line, int index, int end) {
        var gtr = GraphicTextRow.obtain();
        gtr.set(str, index, end, editor.getTabWidth(), getSpans(line), editor.getTextPaint());
        var res = gtr.findOffsetByAdvance(index, targetOffset);
        GraphicTextRow.recycle(gtr);
        return res;
    }

    protected float[] orderedFindCharIndex(float targetOffset, ContentLine str, int line) {
       return orderedFindCharIndex(targetOffset, str, line, 0, str.length());
    }

    @Override
    public void updateMeasureCaches(int startLine, int endLine, long timestamp) {
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
                if (line.length() <= 128 && line.timestamp < timestamp) {
                    var gtr = GraphicTextRow.obtain();
                    gtr.set(line, 0, line.length(), editor.getTabWidth(), getSpans(startLine), editor.getTextPaint());
                    gtr.buildMeasureCache();
                    GraphicTextRow.recycle(gtr);
                    line.timestamp = timestamp;
                } else {
                    line.widthCache = null;
                }
                startLine++;
            }
        }
    }

    public void updateMeasureCaches(int line1, int line2) {
        updateMeasureCaches(line1, line2, System.nanoTime());
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {
        updateMeasureCaches(startLine, startLine + 1);
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

}
