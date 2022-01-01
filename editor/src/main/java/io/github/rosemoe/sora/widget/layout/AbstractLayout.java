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

import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.data.Span;
import io.github.rosemoe.sora.graphics.GraphicTextRow;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.EditorColorScheme;

/**
 * Base layout implementation of {@link Layout}
 * This class has basic methods for its subclasses to measure texts
 *
 * @author Rose
 */
public abstract class AbstractLayout implements Layout {

    protected CodeEditor editor;
    protected Content text;
    protected List<Span> defSpans = new ArrayList<>(2);

    public AbstractLayout(CodeEditor editor, Content text) {
        this.editor = editor;
        this.text = text;
        defSpans.add(Span.obtain(0, EditorColorScheme.TEXT_NORMAL));
        updateMeasureCaches(0, text == null ? 0 : text.getLineCount());
    }

    protected List<Span> getSpans(int line) {
        var spanMap = editor.getTextAnalyzeResult().getSpanMap();
        return line < spanMap.size() ? spanMap.get(line) : defSpans;
    }

    protected float[] orderedFindCharIndex(float targetOffset, ContentLine str, int line, int index, int end) {
        var gtr = GraphicTextRow.obtain();
        gtr.set(str, index, end, editor.getTabWidth(), getSpans(line), editor.getTextPaint());
        return gtr.findOffsetByAdvance(index, targetOffset);
    }

    protected float[] orderedFindCharIndex(float targetOffset, ContentLine str, int line) {
       return orderedFindCharIndex(targetOffset, str, line, 0, str.length());
    }

    private void updateMeasureCaches(int startLine, int endLine) {
        //Temporarily disabled
        if (true) {
            return;
        }
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
