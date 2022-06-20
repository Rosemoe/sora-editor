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
package org.eclipse.tm4e.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.eclipse.tm4e.core.grammar.IGrammar;

/**
 * TextMate model class.
 *
 */
public class TMModel implements ITMModel {

    private static final Logger LOGGER = Logger.getLogger(TMModel.class.getName());
    /** Listener when TextMate model tokens changed **/
    private final List<IModelTokensChangedListener> listeners;
    private final IModelLines lines;
    Tokenizer tokenizer;
    /**
     * The TextMate grammar to use to parse for each lines of the document the
     * TextMate tokens.
     **/
    private IGrammar grammar;
    /** The background thread. */
    private TokenizerThread fThread;
    private PriorityBlockingQueue<Integer> invalidLines = new PriorityBlockingQueue<>();

    public TMModel(IModelLines lines) {
        this.listeners = new ArrayList<>();
        this.lines = lines;
        ((AbstractLineList) lines).setModel(this);
        lines.forEach(ModelLine::resetTokenizationState);
        invalidateLine(0);
    }

    @Override
    public IGrammar getGrammar() {
        return grammar;
    }

    @Override
    public void setGrammar(IGrammar grammar) {
        if (!Objects.equals(grammar, this.grammar)) {
            this.grammar = grammar;
            this.tokenizer = new Tokenizer(grammar);
            lines.get(0).setState(tokenizer.getInitialState());
        }
    }

    @Override
    public void addModelTokensChangedListener(IModelTokensChangedListener listener) {
        if (this.fThread == null || this.fThread.isInterrupted()) {
            this.fThread = new TokenizerThread(getClass().getName(), this);
        }
        if (!this.fThread.isAlive()) {
            this.fThread.start();
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeModelTokensChangedListener(IModelTokensChangedListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            // no need to keep tokenizing if no-one cares
            stop();
        }
    }

    @Override
    public void dispose() {
        stop();
        getLines().dispose();
    }

    /**
     * Interrupt the thread.
     */
    private void stop() {
        if (fThread == null) {
            return;
        }
        this.fThread.interrupt();
        this.fThread = null;
    }

    private void buildEventWithCallback(Consumer<ModelTokensChangedEventBuilder> callback) {
        ModelTokensChangedEventBuilder eventBuilder = new ModelTokensChangedEventBuilder(this);

        callback.accept(eventBuilder);

        ModelTokensChangedEvent e = eventBuilder.build();
        if (e != null) {
            this.emit(e);
        }
    }

    private void emit(ModelTokensChangedEvent e) {
        for (IModelTokensChangedListener listener : listeners) {
            listener.modelTokensChanged(e);
        }
    }

    @Override
    public void forceTokenization(int lineNumber) {
        this.buildEventWithCallback(eventBuilder ->
                this.fThread.updateTokensInRange(eventBuilder, lineNumber, lineNumber)
        );
    }

    @Override
    public List<TMToken> getLineTokens(int lineNumber) {
        return lines.get(lineNumber).tokens;
    }

    public boolean isLineInvalid(int lineNumber) {
        return lines.get(lineNumber).isInvalid;
    }

    void invalidateLine(int lineIndex) {
        this.lines.get(lineIndex).isInvalid = true;
        this.invalidLines.add(lineIndex);
    }

    public IModelLines getLines() {
        return this.lines;
    }

    /**
     * The {@link TokenizerThread} takes as input an {@link TMModel} and continuously
     * runs tokenizing in background on the lines found in {@link TMModel#lines}.
     * The {@link TMModel#lines} are expected to be accessed through {@link TMModel#getLines()}
     * and manipulated by the UI part to inform of needs to (re)tokenize area, then the {@link TokenizerThread}
     * processes them and emits events through the model. UI elements are supposed to subscribe and react to the events with
     * {@link TMModel#addModelTokensChangedListener(IModelTokensChangedListener)}.
     *
     */
    static class TokenizerThread extends Thread {
        private TMModel model;
        private TMState lastState;

        /**
         * Creates a new background thread. The thread runs with minimal
         * priority.
         *
         * @param name
         *            the thread's name
         */
        public TokenizerThread(String name, TMModel model) {
            super(name);
            this.model = model;
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }

        @Override
        public void run() {
            if (isInterrupted()) {
                return;
            }

            do {
                try {
                    Integer toProcess = model.invalidLines.take();
                    if (model.lines.get(toProcess).isInvalid) {
                        try {
                            this.revalidateTokensNow(toProcess.intValue(), null);
                        } catch (Exception t) {
                            LOGGER.severe(t.getMessage());
                            if (toProcess < model.lines.getNumberOfLines()) {
                                model.invalidateLine(toProcess);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    interrupt();
                }
            } while (!isInterrupted() && model.fThread != null);
        }

        /**
         *
         * @param startLine 0-based
         * @param toLineIndexOrNull 0-based
         */
        private void revalidateTokensNow(int startLine, Integer toLineIndexOrNull) {
            model.buildEventWithCallback(eventBuilder -> {
                Integer toLineIndex = toLineIndexOrNull;
                if (toLineIndex == null || toLineIndex >= model.lines.getNumberOfLines()) {
                    toLineIndex = model.lines.getNumberOfLines() - 1;
                }

                long tokenizedChars = 0;
                long currentCharsToTokenize = 0;
                final long MAX_ALLOWED_TIME = 20;
                long currentEstimatedTimeToTokenize = 0;
                long elapsedTime;
                long startTime = System.currentTimeMillis();
                // Tokenize at most 1000 lines. Estimate the tokenization speed per
                // character and stop when:
                // - MAX_ALLOWED_TIME is reached
                // - tokenizing the next line would go above MAX_ALLOWED_TIME

                int lineIndex = startLine;
                while (lineIndex <= toLineIndex && lineIndex < model.getLines().getNumberOfLines()) {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime > MAX_ALLOWED_TIME) {
                        // Stop if MAX_ALLOWED_TIME is reached
                        model.invalidateLine(lineIndex);
                        return;
                    }

                    // Compute how many characters will be tokenized for this line
                    try {
                        currentCharsToTokenize = model.lines.getLineLength(lineIndex);
                    } catch (Exception e) {
                        LOGGER.severe(e.getMessage());
                    }

                    if (tokenizedChars > 0) {
                        // If we have enough history, estimate how long tokenizing this line would take
                        currentEstimatedTimeToTokenize = (long) ((double) elapsedTime / tokenizedChars) * currentCharsToTokenize;
                        if (elapsedTime + currentEstimatedTimeToTokenize > MAX_ALLOWED_TIME) {
                            // Tokenizing this line will go above MAX_ALLOWED_TIME
                            model.invalidateLine(lineIndex);
                            return;
                        }
                    }

                    lineIndex = this.updateTokensInRange(eventBuilder, lineIndex, lineIndex) + 1;
                    tokenizedChars += currentCharsToTokenize;
                }
            });

        }

        /**
         *
         * @param eventBuilder
         * @param startIndex 0-based
         * @param endLineIndex 0-based
         * @param emitEvents
         * @return the first line index (0-based) that was NOT processed by this operation
         */
        private int updateTokensInRange(ModelTokensChangedEventBuilder eventBuilder, int startIndex, int endLineIndex) {
            int stopLineTokenizationAfter = 1000000000; // 1 billion, if a line is
            // so long, you have other
            // trouble :).
            // Validate all states up to and including endLineIndex
            int nextInvalidLineIndex = startIndex;
            int lineIndex = startIndex;
            while (lineIndex <= endLineIndex && lineIndex < model.lines.getNumberOfLines()) {
                int endStateIndex = lineIndex + 1;
                LineTokens r = null;
                String text = null;
                ModelLine modeLine = model.lines.get(lineIndex);
                try {
                    text = model.lines.getLineText(lineIndex);
                    // Tokenize only the first X characters
                    r = model.tokenizer.tokenize(text, modeLine.getState(), 0, stopLineTokenizationAfter);
                } catch (Exception e) {
                    LOGGER.severe(e.getMessage());
                }

                if (r != null && r.tokens != null && !r.tokens.isEmpty()) {
                    // Cannot have a stop offset before the last token
                    r.actualStopOffset = Math.max(r.actualStopOffset, r.tokens.get(r.tokens.size() - 1).startIndex + 1);
                }

                if (r != null && r.actualStopOffset < text.length()) {
                    // Treat the rest of the line (if above limit) as one default token
                    r.tokens.add(new TMToken(r.actualStopOffset, ""));
                    // Use as end state the starting state
                    r.endState = modeLine.getState();
                }

                if (r == null) {
                    r = new LineTokens(Collections.singletonList(new TMToken(0, "")), text.length(), modeLine.getState());
                }
                modeLine.setTokens(r.tokens);
                eventBuilder.registerChangedTokens(lineIndex + 1);
                modeLine.isInvalid = false;

                if (endStateIndex < model.lines.getNumberOfLines()) {
                    ModelLine endStateLine = model.lines.get(endStateIndex);
                    if (endStateLine.getState() != null && r.endState.equals(endStateLine.getState())) {
                        // The end state of this line remains the same
                        nextInvalidLineIndex = lineIndex + 1;
                        while (nextInvalidLineIndex < model.lines.getNumberOfLines()) {
                            boolean isLastLine = nextInvalidLineIndex + 1 >= model.lines.getNumberOfLines();
                            if (model.lines.get(nextInvalidLineIndex).isInvalid
                                    || (!isLastLine && model.lines.get(nextInvalidLineIndex + 1).getState() == null)
                                    || (isLastLine && this.lastState == null)) {
                                break;
                            }
                            nextInvalidLineIndex++;
                        }
                        lineIndex = nextInvalidLineIndex;
                    } else {
                        endStateLine.setState(r.endState);
                        lineIndex++;
                    }
                } else {
                    this.lastState = r.endState;
                    lineIndex++;
                }
            }
            return nextInvalidLineIndex;
        }

    }

}
