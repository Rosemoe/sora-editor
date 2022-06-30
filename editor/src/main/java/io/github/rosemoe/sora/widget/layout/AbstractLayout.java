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
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.rosemoe.sora.graphics.GraphicTextRow;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Base layout implementation of {@link Layout}.
 * It provides some convenient methods to editor instance and text measuring.
 *
 * @author Rosemoe
 */
public abstract class AbstractLayout implements Layout {

    protected CodeEditor editor;
    protected Content text;
    protected static final int SUBTASK_COUNT = 8;
    protected static final int MIN_LINE_COUNT_FOR_SUBTASK = 3000;
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>(128));

    public AbstractLayout(CodeEditor editor, Content text) {
        this.editor = editor;
        this.text = text;
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
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {

    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent) {

    }

    @Override
    public void destroyLayout() {
        editor = null;
        text = null;
    }

    protected void submitTask(LayoutTask<?> task) {
        executor.submit(task);
    }

    protected static class TaskMonitor {

        private final int taskCount;
        private final Object[] results;
        private int completedCount = 0;
        private final Callback callback;

        public TaskMonitor(int totalTask, Callback callback) {
            taskCount = totalTask;
            results = new Object[totalTask];
            this.callback = callback;
        }

        public synchronized void reportCompleted(Object result) {
            results[completedCount++] = result;
            if (completedCount == taskCount) {
                callback.onCompleted(results);
            }
        }

        public interface Callback {
            void onCompleted(Object[] results);
        }

    }

    protected abstract class LayoutTask<T> implements Runnable {
        private final TaskMonitor monitor;

        protected LayoutTask(TaskMonitor monitor) {
            this.monitor = monitor;
        }

        protected boolean shouldRun() {
            return editor != null;
        }

        @Override
        public void run() {
            if (shouldRun()) {
                var result = compute();
                monitor.reportCompleted(result);
            }
        }

        protected abstract T compute();
    }

}
