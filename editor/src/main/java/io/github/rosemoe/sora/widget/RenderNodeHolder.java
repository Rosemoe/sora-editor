/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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

import android.graphics.Canvas;
import android.graphics.RenderNode;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.styling.EmptyReader;
import io.github.rosemoe.sora.util.ArrayList;

/**
 * Hardware accelerated text render, which manages {@link RenderNode}
 * to speed up drawing process.
 *
 * @author Rosemoe
 */
@RequiresApi(29)
class RenderNodeHolder {

    private final CodeEditor editor;
    private final ArrayList<TextRenderNode> cache;
    private final Stack<TextRenderNode> pool = new Stack<>();

    public RenderNodeHolder(CodeEditor editor) {
        this.editor = editor;
        cache = new ArrayList<>(64);
    }

    private boolean shouldUpdateCache() {
        return !editor.isWordwrap() && editor.isHardwareAcceleratedDrawAllowed();
    }

    public boolean invalidateInRegion(@NonNull StyleUpdateRange range) {
        var res = false;
        var itr = cache.iterator();
        while (itr.hasNext()) {
            var element = itr.next();
            if (range.isInRange(element.line)) {
                itr.remove();
                element.renderNode.discardDisplayList();
                pool.push(element);
                res = true;
            }
        }
        return res;
    }

    public boolean invalidateInRegion(int startLine, int endLine) {
        var res = false;
        var itr = cache.iterator();
        while (itr.hasNext()) {
            var element = itr.next();
            if (element.line >= startLine) {
                itr.remove();
                element.renderNode.discardDisplayList();
                pool.push(element);
                res = true;
            }
        }
        return res;
    }

    /**
     * Called by editor when text style changes.
     * Such as text size/typeface.
     * Also called when wordwrap state changes from true to false
     */
    public void invalidate() {
        cache.forEach(node -> node.isDirty = true);
    }

    public TextRenderNode getNode(int line) {
        var size = cache.size();
        for (int i = 0; i < size; i++) {
            var node = cache.get(i);
            if (node.line == line) {
                Collections.swap(cache, 0, i);
                return node;
            }
        }
        var node = pool.isEmpty() ? new TextRenderNode(line) : pool.pop();
        node.line = line;
        node.isDirty = true;
        cache.add(0, node);
        return node;
    }

    public void keepCurrentInDisplay(int start, int end) {
        var itr = cache.iterator();
        while (itr.hasNext()) {
            var node = itr.next();
            if (node.line < start || node.line > end) {
                itr.remove();
                node.renderNode.discardDisplayList();
            }
        }
    }

    public int drawLineHardwareAccelerated(Canvas canvas, int line, float offsetX, float offsetY) {
        if (!canvas.isHardwareAccelerated()) {
            throw new UnsupportedOperationException("Only hardware-accelerated canvas can be used");
        }
        var styles = editor.getStyles();
        // It's safe to use row directly because the mode is non-wordwrap
        var node = getNode(line);
        if (node.needsRecord()) {
            var spans = styles == null ? null : styles.spans;
            var reader = spans == null ? new EmptyReader() : spans.read();
            try {
                reader.moveToLine(line);
            } catch (Exception e) {
                reader = new EmptyReader();
            }
            editor.getRenderer().updateLineDisplayList(node.renderNode, line, reader);
            try {
                reader.moveToLine(-1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            node.isDirty = false;
        }
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.drawRenderNode(node.renderNode);
        canvas.restore();
        return node.renderNode.getWidth();
    }

    public void afterInsert(int startLine, int endLine) {
        cache.forEach(node -> {
            if (node.line == startLine) {
                node.isDirty = true;
            } else if (node.line > startLine) {
                node.line += endLine - startLine;
            }
        });
    }

    public void afterDelete(int startLine, int endLine) {
        List<TextRenderNode> garbage = new ArrayList<>();
        cache.forEach(node -> {
            if (node.line == startLine) {
                node.isDirty = true;
            } else if (node.line > startLine && node.line <= endLine) {
                garbage.add(node);
                node.renderNode.discardDisplayList();
            } else if (node.line > endLine) {
                node.line -= endLine - startLine;
            }
        });
        cache.removeAll(garbage);
        pool.addAll(garbage);
    }

    protected static class TextRenderNode {

        /**
         * The target line of this node.
         * -1 for unavailable
         */
        public int line;
        public RenderNode renderNode;
        public boolean isDirty;

        public TextRenderNode(int line) {
            this.line = line;
            renderNode = new RenderNode("editorRenderNode");
            isDirty = true;
        }

        public boolean needsRecord() {
            return isDirty || !renderNode.hasDisplayList();
        }

    }
}
