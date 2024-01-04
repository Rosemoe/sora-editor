/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * Copyright (c) 2022-2023 Vegard IT GmbH and others.
 * <p>
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - major refactoring, performance improvement, solving out-of-sync issues
 */
package org.eclipse.tm4e.core.model;

import static org.eclipse.tm4e.core.internal.utils.NullSafetyHelper.castNullable;
import static org.eclipse.tm4e.core.internal.utils.NullSafetyHelper.lazyNonNull;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IStateStack;
import org.eclipse.tm4e.core.internal.grammar.StateStack;
import org.eclipse.tm4e.core.internal.utils.MoreCollections;
import org.eclipse.tm4e.core.internal.utils.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.github.rosemoe.sora.util.Logger;

/**
 * The {@link TMModel} runs a background thread tokenizing out-of-date lines of the text model.
 * <p>
 * Concrete implementations of this class are supposed to announce editor's content changes using the
 * {@link #onLinesReplaced(int, int, int)}
 * method. This results in (re)tokenization of the changed lines resulting in {@link ModelTokensChangedEvent}s being emitted.
 * <p>
 * UI elements are supposed to subscribe and react to the events with
 * {@link TMModel#addModelTokensChangedListener(ModelTokensChangedEvent.Listener)}.
 *
 * @see <a href="https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/model/tokenizationTextModelPart.ts">
 * github.com/microsoft/vscode/src/vs/editor/common/model/tokenizationTextModelPart.ts <code>#TokenizationTextModelPart</code></a>
 */
public abstract class TMModel implements ITMModel {

    private static final class Edit {
        final int lineIndex;
        final int replacedCount;
        final int replacementCount;

        public Edit(final int lineIndex, final int replacedCount, final int replacementCount) {
            this.lineIndex = lineIndex;
            this.replacedCount = replacedCount;
            this.replacementCount = replacementCount;
        }

        @Override
        public String toString() {
            return "{lineNumber=" + (lineIndex + 1) + ", replacedCount=" + replacedCount + ", replacementCount=" + replacementCount + '}';
        }
    }

    /**
     * package visibility for tests
     **/
    static final class LineTokens {
        volatile IStateStack startState = StateStack.NULL;
        @Nullable
        IStateStack endState;
        @Nullable
        volatile List<TMToken> tokens;

        void reset() {
            startState = StateStack.NULL;
            endState = null;
            tokens = null;
        }

        @Override
        public String toString() {
            return "{startState=" + startState + ", tokens=" + tokens + '}';
        }
    }

    private static final Logger LOGGER = Logger.instance(TMModel.class.getName());

    /**
     * The TextMate grammar to use to tokenize lines of the attached document
     **/
    private @Nullable IGrammar grammar;

    /**
     * Listeners that are notified when (re)tokenization of changed lines was performed
     **/
    private final ModelTokensChangedEvent.Listeners listeners = new ModelTokensChangedEvent.Listeners();

    /**
     * The background thread performing async line tokenizations
     */
    private @Nullable
    volatile TokenizerThread tokenizerThread;
    private volatile boolean tokenizerThreadHasWork;
    private TMTokenizationSupport tokenizer = lazyNonNull();

    /**
     * package visibility for tests
     **/
    final ArrayList<LineTokens> lines;
    final Object linesWriteLock;

    private final BlockingQueue<Edit> edits = new LinkedBlockingQueue<>();

    protected TMModel(final int initialNumberOfLines) {
        lines = new ArrayList<>(Math.max(10, initialNumberOfLines));
        linesWriteLock = lines;
        onLinesReplaced(0, 0, initialNumberOfLines);
    }

    // if your want to debug the tokenization, set this to true
    private static final boolean DEBUG_LOGGING = false;

    private void logDebug(final String msg, final Object... args) {
        if (!DEBUG_LOGGING)
            return;
        final var t = Thread.currentThread();
        final var caller = t.getStackTrace()[2];
        final var threadName = t.getName().endsWith(TokenizerThread.class.getSimpleName()) ? "tknz" : t.getName();
        LOGGER.i("[" + threadName + "] " + caller.getMethodName() + String.format(msg, args));
    }

    /**
     * The {@link TokenizerThread} continuously runs tokenizing in background on the lines found in {@link TMModel#lines}.
     */
    private final class TokenizerThread extends Thread {

        /**
         * max time allowed to tokenize a single line
         */
        private static final Duration MAX_TIME_PER_LINE_TOKENIZATION = Duration.ofSeconds(1);

        /**
         * max time in milliseconds for multi-line validations before a consolidated {@link ModelTokensChangedEvent} is emitted
         */
        private static final int MAX_TIME_PER_MULTI_LINE_VALIDATIONS = 200;

        /**
         * Creates a new background thread. The thread runs with minimal priority.
         */
        TokenizerThread() {
            super("tm4e." + TokenizerThread.class.getSimpleName());
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                // loop as long as the thread is active
                while (tokenizerThread == this) {
                    tokenizerThreadHasWork = !(isAllTokensAreValid() && edits.isEmpty());

                    if (!tokenizerThreadHasWork || !edits.isEmpty()) {
                        // wait for the first edit
                        applyEdit(edits.take());

                        // poll all subsequent edits
                        for (; ; ) {
                            // wait up to 50ms for the next edit, so that edits made in fast succession (e.g. by a formater) are applied
                            // in one go before the token revalidation loop happens
                            final var edit = castNullable(edits.poll(50, TimeUnit.MILLISECONDS));
                            if (edit == null)
                                break;
                            applyEdit(edit);
                        }
                    }

                    revalidateTokens();
                }
            } catch (final InterruptedException ex) {
                interrupt();
            } finally {
                tokenizerThreadHasWork = false;
            }
        }

        private int firstLineToRevalidate = -1;

        private boolean isAllTokensAreValid() {
            return firstLineToRevalidate == -1;
        }

        private void setAllTokensAreValid() {
            firstLineToRevalidate = -1;
        }

        /**
         * revalidates tokens of lines starting at {@link #firstLineToRevalidate} until all lines are processed or new {@link Edit} arrive.
         *
         * @throws InterruptedException
         */
        private void revalidateTokens() throws InterruptedException {
            final int startLineIndex = firstLineToRevalidate;
            final int startLineNumber = startLineIndex + 1;
            if (DEBUG_LOGGING)
                logDebug("(%d)", startLineNumber);

            long startTime = System.currentTimeMillis();
            var changedRanges = new ArrayList<Range>();
            Range prevRange = null;
            var prevLineTokens = getLineTokensOrNull(startLineIndex - 1);

            final int linesCount = lines.size();
            int currLineIndex = -1;

            // iterate over all lines from startLineIndex to end of file to check if (re)tokenization is required
            for (currLineIndex = startLineIndex; currLineIndex < linesCount; currLineIndex++) {

                // check if TokenizerThread is still running
                if (isInterrupted()) {
                    break;
                }

                // check if new edits are queued -> if so, abort current tokenization loop
                if (!edits.isEmpty()) {
                    break;
                }

                final var currLineTokens = lines.get(currLineIndex);

                if (currLineIndex == 0) {
                    currLineTokens.startState = tokenizer.getInitialState();
                }

                final int currLineNumber = currLineIndex + 1;

                // check if (re)tokenization is required
                if (prevLineTokens != null) {
                    if (currLineTokens.tokens != null && currLineTokens.startState.equals(prevLineTokens.endState)) {
                        // has matching start and has tokens ==> is up to date
                        if (DEBUG_LOGGING)
                            logDebug("(%d) >> DONE - tokens of line %d are up-to-date", startLineNumber, currLineNumber);
                        firstLineToRevalidate = currLineIndex + 1;
                        prevLineTokens = currLineTokens;
                        continue;
                    }
                    if (prevLineTokens.endState != null)
                        currLineTokens.startState = prevLineTokens.endState;
                }

                // (re)tokenize the line
                if (DEBUG_LOGGING)
                    logDebug("(%d) >> tokenizing line %d...", startLineNumber, currLineNumber);
                TokenizationResult r;
                try {
                    final String lineText = getLineText(currLineIndex);
                    r = tokenizer.tokenize(lineText, currLineTokens.startState, 0, MAX_TIME_PER_LINE_TOKENIZATION);
                } catch (final Exception ex) {
                    LOGGER.e(ex.toString());
                    r = new TokenizationResult(new ArrayList<>(1), 0, currLineTokens.startState, true);
                }

                // check if complete line was tokenized
                if (r.stoppedEarly) {
                    // treat the rest of the line as one default token
                    r.tokens.add(new TMToken(r.actualStopOffset, ""));
                    // Use the line's starting state as end state in case of incomplete tokenization
                    r.endState = currLineTokens.startState;
                }

                currLineTokens.endState = r.endState;
                currLineTokens.tokens = r.tokens;
                prevLineTokens = currLineTokens;
                firstLineToRevalidate = currLineIndex + 1;

                // add the line number to the changed ranges
                if (prevRange != null && prevRange.toLineNumber == currLineNumber - 1) {
                    prevRange.toLineNumber = currLineNumber; // extend range from previous line change
                } else {
                    prevRange = new Range(currLineNumber);
                    changedRanges.add(prevRange); // insert new range
                }

                // if MAX_TIME_PER_MULTI_LINE_VALIDATIONS reached, notify listeners about line changes
                if (System.currentTimeMillis() - startTime >= MAX_TIME_PER_MULTI_LINE_VALIDATIONS) {
                    if (DEBUG_LOGGING)
                        logDebug("(%d) >> changedRanges: %s", startLineNumber, changedRanges);
                    listeners.dispatchEvent(changedRanges, TMModel.this);
                    changedRanges = new ArrayList<>();
                    prevRange = null;
                    startTime = System.currentTimeMillis();
                }
            }

            // notify listeners about remaining line changes
            if (DEBUG_LOGGING)
                logDebug("(%d) >> changedRanges: %s", startLineNumber, changedRanges);
            listeners.dispatchEvent(changedRanges, TMModel.this);

            setAllTokensAreValid();
        }

        private void applyEdit(final Edit edit) {
            if (DEBUG_LOGGING)
                logDebug("(%s)", edit);

            final var lineIndex = edit.lineIndex;
            if (isAllTokensAreValid() || lineIndex < firstLineToRevalidate)
                firstLineToRevalidate = lineIndex;

            // check if single line update
            if (edit.replacedCount == 1 && edit.replacementCount == 1) {
                final var firstLineOfEdit = getLineTokensOrNull(lineIndex);
                if (firstLineOfEdit == null)
                    return;
                // reuse the LineToken instance by resetting it's state
                firstLineOfEdit.reset();
                return;
            }

            final int replacedCount = Math.min(edit.replacedCount, lines.size() - lineIndex);
            final var lineDiff = edit.replacementCount - edit.replacedCount;
            final var editRange = lines.subList(lineIndex, lineIndex + replacedCount);

            // (1) number of lines not changed by edit
            if (lineDiff == 0) {
                // reset tokenization state of affected lines
                editRange.forEach(LineTokens::reset);
                return;
            }

            // (2) new lines added by edit
            if (lineDiff > 0) {
                // reset tokenization state of affected lines
                editRange.forEach(LineTokens::reset);

                // add extra lines
                final var additionalLines = new ArrayList<LineTokens>(lineDiff);
                for (int i = 0; i < lineDiff; i++) {
                    additionalLines.add(new LineTokens());
                }
                synchronized (linesWriteLock) {
                    editRange.addAll(additionalLines);
                }
                return;
            }

            // (3) lines removed by edit
            /* if (lineDiff < 0) */
            {
                synchronized (linesWriteLock) {
                    editRange.subList(0, -lineDiff).clear();
                }
                // reset tokenization state of the other affected lines
                editRange.forEach(LineTokens::reset);
            }
        }
    }

    private @Nullable LineTokens getLineTokensOrNull(final int index) {
        return index > -1 && index < lines.size()
                ? lines.get(index)
                : null;
    }

    @Override
    public BackgroundTokenizationState getBackgroundTokenizationState() {
        return tokenizerThreadHasWork ? BackgroundTokenizationState.IN_PROGRESS : BackgroundTokenizationState.COMPLETED;
    }

    @Override
    public @Nullable IGrammar getGrammar() {
        return grammar;
    }

    @Override
    public synchronized void setGrammar(final IGrammar grammar) {
        if (!Objects.equals(grammar, this.grammar)) {
            this.grammar = grammar;
            final var tokenizer = this.tokenizer = new TMTokenizationSupport(grammar);
            synchronized (linesWriteLock) {
                if (!lines.isEmpty()) {
                    lines.get(0).startState = tokenizer.getInitialState();
                }
                onLinesReplaced(0, 1, 1);
            }
            startTokenizerThread();
        }
    }

    /**
     * Informs the model about lines being replaced at the given index.
     * <p>
     * Examples, e.g. for replacements at line index 10:
     * <li>for a single line update use <code>onLinesReplaced(10, 1, 1);</code>
     * <li>for deletion of 5 lines use <code>onLinesReplaced(10, 5, 0);</code>
     * <li>for inserting of 5 lines use <code>onLinesReplaced(10, 0, 5);</code>
     * <li>for replacing 5 lines with 2 new lines use <code>onLinesReplaced(10, 5, 2);</code>
     *
     * @param lineIndex             (0-based)
     * @param replacedLinesCount    number of lines that are replaced
     * @param replacementLinesCount number of lines of the replacement text
     */
    public void onLinesReplaced(final int lineIndex, final int replacedLinesCount, final int replacementLinesCount) {
        if (replacedLinesCount == 0 && replacementLinesCount == 0)
            return;

        if (DEBUG_LOGGING)
            logDebug("(%d, -%d, +%d)", lineIndex + 1, replacedLinesCount, replacementLinesCount);

        edits.add(new Edit(lineIndex, replacedLinesCount, replacementLinesCount));
    }

    @Override
    public synchronized boolean addModelTokensChangedListener(final ModelTokensChangedEvent.Listener listener) {
        if (listeners.add(listener)) {
            startTokenizerThread();
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean removeModelTokensChangedListener(final ModelTokensChangedEvent.Listener listener) {
        if (listeners.remove(listener)) {
            if (listeners.isEmpty()) {
                stopTokenizerThread(); // no need to keep tokenizing if no-one cares
            }
            return true;
        }
        return false;
    }

    @Override
    public void dispose() {
        stopTokenizerThread();
    }

    private synchronized void startTokenizerThread() {
        if (grammar != null && listeners.isNotEmpty()) {
            var thread = this.tokenizerThread;
            if (thread == null || !thread.isAlive() || thread.isInterrupted()) {
                thread = this.tokenizerThread = new TokenizerThread();
                thread.start();
            }
        }
    }

    /**
     * Interrupt the thread if running.
     */
    private synchronized void stopTokenizerThread() {
        final var thread = this.tokenizerThread;
        if (thread == null)
            return;

        thread.interrupt();
        this.tokenizerThread = null;
    }

    @Override
    public int getNumberOfLines() {
        synchronized (linesWriteLock) {
            return lines.size();
        }
    }

    @Override
    public @Nullable List<TMToken> getLineTokens(final int lineIndex) {
        synchronized (linesWriteLock) {
            final var lineTokens = getLineTokensOrNull(lineIndex);
            return lineTokens == null ? null : lineTokens.tokens;
        }
    }

    @Override
    public String toString() {
        return StringUtils.toString(this, sb -> {
            sb.append("grammar=").append(grammar);
            synchronized (linesWriteLock) {
                sb.append(", lines=").append(MoreCollections.toStringWithIndex(lines));
            }
        });
    }
}
