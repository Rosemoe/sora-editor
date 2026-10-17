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
package io.github.rosemoe.sora.lang.analysis;

import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.lang.styling.CodeBlock;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanFactory;
import io.github.rosemoe.sora.lang.styling.Spans;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.util.BaseAnalyzeManager;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * Asynchronous base implementation of {@link IncrementalAnalyzeManager}
 * <p>
 * {@inheritDoc}
 *
 * @author Rosemoe
 */
public abstract class AsyncIncrementalAnalyzeManager<S, T> extends BaseAnalyzeManager implements IncrementalAnalyzeManager<S, T> {

    private final static int MSG_BASE = 11451400;
    private final static int MSG_INIT = MSG_BASE + 1;
    private final static int MSG_MOD = MSG_BASE + 2;
    private final static int MSG_TOKENIZE = MSG_BASE + 3;
    private static int sThreadId = 0;
    private volatile LooperThread thread;
    private volatile long runCount;
    private final boolean useShallowCopy;

    private static boolean useShallowCopyByDefault = false;

    private static boolean updateStylesDuringAnalysis = true;

    /**
     * Use shallow copy for initial text copying. Memory usage will be much lower than full copy at the beginning.
     * <p>
     * As the text is modified, the memory usage will finally go up to the same usage of full copy when all lines are edited.
     * <p>
     * This function is experimental, and is disabled by default.
     */
    @Experimental
    public static void setUseShallowCopyByDefault(boolean useShallowCopy) {
        useShallowCopyByDefault = useShallowCopy;
    }

    /**
     * @see #setUseShallowCopyByDefault(boolean)
     */
    public static boolean isUseShallowCopyByDefault() {
        return useShallowCopyByDefault;
    }

    /**
     * Update styles during initial styles analysis. This is useful for long files to show analyzed
     * lines quickly, instead of showing the result after initial analysis.
     */
    public static void setUpdateStylesDuringAnalysis(boolean sendStylesAsAnalysis) {
        AsyncIncrementalAnalyzeManager.updateStylesDuringAnalysis = sendStylesAsAnalysis;
    }

    public static boolean isUpdateStylesDuringAnalysis() {
        return updateStylesDuringAnalysis;
    }

    public AsyncIncrementalAnalyzeManager() {
        this(isUseShallowCopyByDefault());
    }

    public AsyncIncrementalAnalyzeManager(boolean useShallowCopy) {
        this.useShallowCopy = useShallowCopy;
    }

    private synchronized static int nextThreadId() {
        sThreadId++;
        return sThreadId;
    }

    /**
     * Run the given code block only when the receiver is currently non-null
     */
    protected void withReceiver(@NonNull ReceiverConsumer consumer) {
        var r = getReceiver();
        if (r != null) {
            consumer.accept(r);
        }
    }

    /**
     * Called on the analyzer thread whenever the shadow document or its spans move.
     *
     * @param styles       the styles being built
     * @param startLine    first line of the affected range, inclusive
     * @param endLine      last line of the affected range, inclusive
     * @param modification the edit that was applied, or {@code null} while initializing and between
     *                     continuation batches
     * @param spansReady   {@code false} before initial tokenization, with empty spans, or right after
     *                     a shadow edit, while affected lines still carry the previous spans;
     *                     {@code true} once those lines have been tokenized. The range reported
     *                     with {@code true} is one batch, so further batches may follow.
     */
    protected void onAnalysisUpdate(Styles styles, int startLine, int endLine,
                                    @Nullable TextModification modification, boolean spansReady) {
    }

    protected final boolean isCurrentAnalyzerThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void insert(@NonNull CharPosition start, @NonNull CharPosition end, @NonNull CharSequence insertedText) {
        if (thread != null) {
            increaseRunCount();
            thread.offerMessage(MSG_MOD, new TextModification(IntPair.pack(start.line, start.column), IntPair.pack(end.line, end.column), insertedText, getContentRef().getReference().getDocumentVersion()));
        }
    }

    @Override
    public void delete(@NonNull CharPosition start, @NonNull CharPosition end, @NonNull CharSequence deletedText) {
        if (thread != null) {
            increaseRunCount();
            thread.offerMessage(MSG_MOD, new TextModification(IntPair.pack(start.line, start.column), IntPair.pack(end.line, end.column), null, getContentRef().getReference().getDocumentVersion()));
        }
    }

    @Override
    public void rerun() {
        if (thread != null) {
            if (thread.isAlive()) {
                thread.interrupt();
                thread.abort = true;
            }
            thread = null;
        }
        var ref = getContentRef();
        if (ref != null) {
            final var text = ref.getReference().copyText(false, useShallowCopy);
            text.setUndoEnabled(false);
            thread = new LooperThread();
            thread.documentVersion = ref.getReference().getDocumentVersion();
            thread.setName("AsyncAnalyzer-" + nextThreadId());
            thread.offerMessage(MSG_INIT, text);
            increaseRunCount();
            sendNewStyles(null);
            thread.start();
        }
    }

    @Override
    public LineTokenizeResult<S, T> getState(int line) {
        final var thread = this.thread;
        if (thread == Thread.currentThread()) {
            if (line >= 0 && line < thread.states.size()) {
                return thread.states.get(line);
            }
            return null;
        }
        throw new SecurityException("Can not get state from non-analytical or abandoned thread");
    }

    @Override
    public void onAbandonState(S state) {

    }

    @Override
    public void onAddState(S state) {

    }

    private synchronized void increaseRunCount() {
        runCount++;
    }

    @Override
    public void destroy() {
        if (thread != null) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
            thread.abort = true;
        }
        thread = null;
        super.destroy();
    }

    private void sendNewStyles(Styles styles) {
        final var r = getReceiver();
        if (r != null) {
            r.setStyles(this, styles);
        }
    }

    private void sendUpdate(Styles styles, int startLine, int endLine) {
        final var r = getReceiver();
        if (r != null) {
            r.updateStyles(this, styles, new SequenceUpdateRange(startLine, endLine));
        }
    }

    /**
     * Compute code blocks
     *
     * @param text The text. can be safely accessed.
     */
    public abstract List<CodeBlock> computeBlocks(Content text, CodeBlockAnalyzeDelegate delegate);

    public Styles getManagedStyles() {
        var thread = Thread.currentThread();
        if (thread.getClass() != AsyncIncrementalAnalyzeManager.LooperThread.class) {
            throw new IllegalThreadStateException();
        }
        return ((AsyncIncrementalAnalyzeManager<?, ?>.LooperThread) thread).styles;
    }

    public Content getManagedContent() {
        var thread = Thread.currentThread();
        if (thread.getClass() != AsyncIncrementalAnalyzeManager.LooperThread.class) {
            throw new IllegalThreadStateException();
        }
        return ((AsyncIncrementalAnalyzeManager<?, ?>.LooperThread) thread).shadowed;
    }

    /** Source-document version represented by the current analyzer message (not queued edits). */
    protected long getManagedDocumentVersion() {
        if (Thread.currentThread() != thread) {
            throw new IllegalThreadStateException("Abandoned or non-analyzer thread");
        }
        return thread.documentVersion;
    }

    private static class LockedSpans implements Spans {

        private static final String LOG_TAG = "LockedSpans";

        private final Lock lock;
        private final List<Line> lines;

        public LockedSpans() {
            lines = new ArrayList<>(128);
            lock = new ReentrantLock();
        }

        @Override
        public void adjustOnDelete(CharPosition start, CharPosition end) {

        }

        @Override
        public void adjustOnInsert(CharPosition start, CharPosition end) {

        }

        @Override
        public int getLineCount() {
            return lines.size();
        }

        @Override
        public Reader read() {
            return new ReaderImpl();
        }

        @Override
        public Modifier modify() {
            return new ModifierImpl();
        }

        @Override
        public boolean supportsModify() {
            return true;
        }

        private static class Line {

            public Lock lock = new ReentrantLock();

            public List<Span> spans;

            public Line(List<Span> s) {
                spans = s;
            }

        }

        private class ReaderImpl implements Spans.Reader {

            private Line line;

            public void moveToLine(int line) {
                if (line < 0 || line >= lines.size()) {
                    if (this.line != null) {
                        this.line.lock.unlock();
                    }
                    this.line = null;
                } else {
                    if (this.line != null) {
                        this.line.lock.unlock();
                    }
                    var locked = false;
                    try {
                        locked = lock.tryLock(100, TimeUnit.MICROSECONDS);
                    } catch (InterruptedException e) {
                        Log.w(LOG_TAG, "failed to acquire the lock", e);
                        Thread.currentThread().interrupt();
                    }
                    if (locked) {
                        try {
                            var obj = lines.get(line);
                            if (obj.lock.tryLock()) {
                                this.line = obj;
                            } else {
                                this.line = null;
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        this.line = null;
                    }
                }
            }

            @Override
            public int getSpanCount() {
                return line == null ? 1 : line.spans.size();
            }

            @Override
            public Span getSpanAt(int index) {
                return line == null ? SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL) : line.spans.get(index);
            }

            @Override
            public List<Span> getSpansOnLine(int line) {
                var spans = new ArrayList<Span>();
                var locked = false;
                try {
                    locked = lock.tryLock(1, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Log.w(LOG_TAG, "failed to acquire the lock", e);
                }
                if (locked) {
                    Line obj = null;
                    try {
                        if (line < lines.size()) {
                            obj = lines.get(line);
                        }
                    } finally {
                        lock.unlock();
                    }
                    if (obj != null && obj.lock.tryLock()) {
                        try {
                            return Collections.unmodifiableList(obj.spans);
                        } finally {
                            obj.lock.unlock();
                        }
                    } else {
                        spans.add(getSpanAt(0));
                    }
                } else {
                    spans.add(getSpanAt(0));
                }
                return spans;
            }
        }

        private class ModifierImpl implements Modifier {

            @Override
            public void setSpansOnLine(int line, List<Span> spans) {
                lock.lock();
                try {
                    while (lines.size() <= line) {
                        var list = new ArrayList<Span>();
                        list.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL));
                        lines.add(new Line(list));
                    }
                    var obj = lines.get(line);
                    obj.lock.lock();
                    try {
                        obj.spans = spans;
                    } finally {
                        obj.lock.unlock();
                    }
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void addLineAt(int line, List<Span> spans) {
                lock.lock();
                try {
                    lines.add(line, new Line(spans));
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void deleteLineAt(int line) {
                lock.lock();
                try {
                    var obj = lines.get(line);
                    obj.lock.lock();
                    try {
                        lines.remove(line);
                    } finally {
                        obj.lock.unlock();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

    }

    public static class TextModification {

        /** Where the edit begins, valid in both the old and the new revision. */
        public final long start;
        /** End of the replaced range in the old revision. */
        public final long oldEnd;
        /** End of the replacement in the new revision. */
        public final long newEnd;
        /** Inserted text, or {@code null} for a deletion. */
        public final CharSequence changedText;
        public final long documentVersion;

        TextModification(long start, long end, @Nullable CharSequence text, long documentVersion) {
            this.documentVersion = documentVersion;
            this.start = start;
            this.changedText = text;
            // A deletion replaces `start..end` with nothing, an insertion replaces `start` with
            // `start..end`. Both cases therefore collapse one of the two ends onto `start`.
            boolean deletion = text == null;
            this.oldEnd = deletion ? end : start;
            this.newEnd = deletion ? start : end;
        }
    }

    /**
     * Helper class for analyzing code block
     */
    public class CodeBlockAnalyzeDelegate {

        private final LooperThread thread;
        int suppressSwitch;

        CodeBlockAnalyzeDelegate(@NonNull LooperThread lp) {
            thread = lp;
        }

        public void setSuppressSwitch(int suppressSwitch) {
            this.suppressSwitch = suppressSwitch;
        }

        void reset() {
            suppressSwitch = Integer.MAX_VALUE;
        }

        public boolean isCancelled() {
            return thread.myRunCount != runCount || thread.abort || thread.isInterrupted();
        }

        public boolean isNotCancelled() {
            return !isCancelled();
        }

    }

    private final class LooperThread extends Thread {

        private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        volatile boolean abort;
        Content shadowed;
        long myRunCount;
        long documentVersion;
        // Dirty tokenization survives messages; a long state propagation must not delay new edits.
        int tokenizeStart = -1;
        int tokenizeEnd = -1;
        boolean tokenizeQueued;

        List<LineTokenizeResult<S, T>> states = new ArrayList<>();
        Styles styles;
        LockedSpans spans;
        CodeBlockAnalyzeDelegate delegate = new CodeBlockAnalyzeDelegate(this);

        public void offerMessage(int what, @Nullable Object obj) {
            var msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            offerMessage(msg);
        }

        public void offerMessage(@NonNull Message msg) {
            // Result ignored: capacity is enough as it is INT_MAX
            //noinspection ResultOfMethodCallIgnored
            messageQueue.offer(msg);
        }

        private void initialize() {
            styles = new Styles(spans = new LockedSpans());
            // Publish text-derived models before the first token is available.
            AsyncIncrementalAnalyzeManager.this.onAnalysisUpdate(
                    styles, 0, shadowed.getLineCount() - 1, null, false);
            S state = getInitialState();
            var mdf = spans.modify();
            for (int i = 0; i < shadowed.getLineCount() && !abort && !isInterrupted(); i++) {
                var line = shadowed.getLine(i);
                var result = tokenizeLine(line, state, i);
                state = result.state;
                var spans = result.spans != null ? result.spans : generateSpansForLine(result);
                states.add(result.clearSpans());
                onAddState(result.state);
                mdf.addLineAt(i, spans);
                if (isUpdateStylesDuringAnalysis() && i > 0 && i % 1000 == 0 && !abort) {
                    var tmpStyles = new Styles();
                    tmpStyles.spans = styles.spans;
                    sendNewStyles(tmpStyles);
                }
            }
            if (abort || isInterrupted() || thread != this) return;
            AsyncIncrementalAnalyzeManager.this.onAnalysisUpdate(
                    styles, 0, shadowed.getLineCount() - 1, null, true);
            // Token-aware models can replace their initial approximation without waiting for folds.
            sendUpdate(styles, 0, shadowed.getLineCount() - 1);
            styles.blocks = computeBlocks(shadowed, delegate);
            styles.setSuppressSwitch(delegate.suppressSwitch);
            styles.finishBuilding();

            if (!abort && thread == this) {
                sendNewStyles(styles);
            }
        }

        public boolean handleMessage(@NonNull Message msg) {
            try {
                myRunCount = runCount;
                delegate.reset();
                // Whether this message leaves a tokenization pass to run, and what it resumes from.
                boolean tokenizeAfter = false;
                switch (msg.what) {
                    case MSG_INIT:
                        shadowed = (Content) msg.obj;
                        if (!abort && !isInterrupted()) {
                            initialize();
                        }
                        break;
                    // A text edit. Apply it to the shadow document, mark the lines it touched as
                    // pending, and tokenize them.
                    case MSG_MOD: {
                        if (abort || isInterrupted()) break;
                        var mod = (TextModification) msg.obj;
                        documentVersion = mod.documentVersion;
                        int startLine = IntPair.getFirst(mod.start);
                        int oldEndLine = IntPair.getFirst(mod.oldEnd);
                        int newEndLine = IntPair.getFirst(mod.newEnd);
                        if (mod.changedText == null) {
                            shadowed.delete(startLine, IntPair.getSecond(mod.start),
                                    oldEndLine, IntPair.getSecond(mod.oldEnd));
                        } else {
                            shadowed.insert(startLine, IntPair.getSecond(mod.start), mod.changedText);
                        }
                        AsyncIncrementalAnalyzeManager.this.onAnalysisUpdate(
                                styles, startLine, newEndLine, mod, false);
                        sendUpdate(styles, startLine, newEndLine);

                        var modifier = spans.modify();
                        if (oldEndLine > startLine) {
                            // Lines the edit replaced lose their states. The line ranges below shrink
                            // to fit, so every removal happens at the same index, pulling the next
                            // line into its place.
                            var replaced = states.subList(startLine + 1, oldEndLine + 1);
                            for (var entry : replaced) {
                                if (entry != null) onAbandonState(entry.state);
                            }
                            replaced.clear();
                            for (int line = startLine + 1; line <= oldEndLine; line++) {
                                modifier.deleteLineAt(startLine + 1);
                            }
                        }
                        // Lines the edit created start out as placeholders. A null state means "not
                        // tokenized yet"; the pass below replaces it.
                        for (int line = startLine + 1; line <= newEndLine; line++) {
                            states.add(line, null);
                            modifier.addLineAt(line, Collections.singletonList(
                                    SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL)));
                        }
                        // Carry the unfinished tokenization frontier across this edit. A frontier
                        // inside the replaced range collapses onto the edit: its start onto the
                        // edit's start, its end onto the edit's end. One after the edit shifts by
                        // the line delta.
                        int lineDelta = newEndLine - oldEndLine;
                        if (tokenizeStart >= startLine) {
                            tokenizeStart = tokenizeStart <= oldEndLine ? startLine : tokenizeStart + lineDelta;
                        }
                        if (tokenizeEnd >= startLine) {
                            tokenizeEnd = tokenizeEnd <= oldEndLine ? newEndLine : tokenizeEnd + lineDelta;
                        }
                        // The frontier must reach the edit, and it must not converge before the
                        // previous frontier, whose suffix it had not validated yet.
                        tokenizeEnd = Math.max(tokenizeEnd, tokenizeStart);
                        tokenizeStart = tokenizeStart < 0 ? startLine : Math.min(tokenizeStart, startLine);
                        // Joining lines makes the following line's incoming state depend on this edit.
                        tokenizeEnd = Math.max(tokenizeEnd, newEndLine + (oldEndLine > startLine ? 1 : 0));
                        tokenizeAfter = true;
                        break;
                    }
                    // An earlier pass yielded before converging; continue from its frontier.
                    case MSG_TOKENIZE:
                        tokenizeQueued = false;
                        tokenizeAfter = tokenizeStart >= 0;
                        break;
                }
                if (tokenizeAfter && !abort && !isInterrupted()) {
                    int firstLine = tokenizeStart;
                    int line = firstLine;
                    S state = line == 0 ? getInitialState() : states.get(line - 1).state;
                    var modifier = spans.modify();
                    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(5);
                    do {
                        var result = tokenizeLine(shadowed.getLine(line), state, line);
                        modifier.setSpansOnLine(line, result.spans != null ? result.spans : generateSpansForLine(result));
                        var old = states.set(line, result.clearSpans());
                        // A line before the required frontier cannot prove the states converged.
                        boolean converged = line >= tokenizeEnd && old != null && stateEquals(old.state, result.state);
                        if (old != null) onAbandonState(old.state);
                        onAddState(result.state);
                        state = result.state;
                        line++;
                        if (converged || line == shadowed.getLineCount()) {
                            tokenizeStart = tokenizeEnd = -1;
                            break;
                        }
                        // Resume from here if the loop condition below decides to yield.
                        tokenizeStart = line;
                    } while (!abort && !isInterrupted() && messageQueue.isEmpty() && System.nanoTime() < deadline);

                    if (thread == this) {
                        AsyncIncrementalAnalyzeManager.this.onAnalysisUpdate(
                                styles, firstLine, line - 1,
                                msg.what == MSG_MOD ? (TextModification) msg.obj : null, true);
                        sendUpdate(styles, firstLine, line - 1);
                        if (tokenizeStart >= 0 && !tokenizeQueued) {
                            tokenizeQueued = true;
                            offerMessage(MSG_TOKENIZE, null);
                        }
                    }
                }
                // Folding is always a full pass. It waits until tokenization has caught up and
                // nothing else is queued, so it only ever sees converged states.
                if (msg.what != MSG_INIT && !abort && thread == this
                        && tokenizeStart < 0 && messageQueue.isEmpty()) {
                    var blocks = computeBlocks(shadowed, delegate);
                    if (delegate.isNotCancelled()) {
                        styles.blocks = blocks;
                        styles.finishBuilding();
                        styles.setSuppressSwitch(delegate.suppressSwitch);
                        sendUpdate(styles, 0, shadowed.getLineCount() - 1);
                    }
                }
                return true;
            } catch (Exception e) {
                Log.w("AsyncAnalysis", "Thread " + Thread.currentThread().getName() + " failed", e);
            }
            return false;
        }


        @Override
        public void run() {
            try {
                while (!abort && !isInterrupted()) {
                    var msg = messageQueue.take();
                    if (!handleMessage(msg)) {
                        break;
                    }
                    msg.recycle();
                }
            } catch (InterruptedException e) {
                // ignored
            } finally {
                if (useShallowCopy && shadowed != null) {
                    shadowed.release();
                }
            }
        }
    }


    public interface ReceiverConsumer {

        void accept(@NonNull StyleReceiver receiver);

    }


}
