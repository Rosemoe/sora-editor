/*
 *   Copyright 2020 Rose2073
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.rose.editor.text;

import com.rose.editor.android.ColorScheme;
import com.rose.editor.interfaces.CodeAnalyzer;
import com.rose.editor.struct.BlockLine;
import com.rose.editor.struct.NavigationLabel;
import com.rose.editor.struct.Span;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a manager of analyzing text
 *
 * @author Rose
 */
public class TextAnalyzer {

    /**
     * Debug:Start time
     */
    public long mThreadStartTime;
    private TextColors mColors;
    private Callback mCallback;
    private AnalyzeThread mThread;
    private CodeAnalyzer mCodeAnalyzer;
    private final Allocator mAllocator = new Allocator();

    /**
     * Create a new manager for the given codeAnalyzer
     *
     * @param codeAnalyzer0 Target codeAnalyzer
     */
    public TextAnalyzer(CodeAnalyzer codeAnalyzer0) {
        if (codeAnalyzer0 == null) {
            throw new IllegalArgumentException();
        }
        mColors = new TextColors(mAllocator);
        mColors.addNormalIfNull();
        mCodeAnalyzer = codeAnalyzer0;
    }

    /**
     * Set callback of analysis
     *
     * @param cb New callback
     */
    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    /**
     * Analyze the given text
     *
     * @param origin The source text
     */
    public void analyze(Content origin) {
        final AnalyzeThread mT = this.mThread;
        if (mT == null) {
            this.mThread = new AnalyzeThread(mCodeAnalyzer, origin);
            this.mThread.start();
        } else {
            if (mT.isAlive()) {
                mT.restartWith(origin);
            } else {
                this.mThread = new AnalyzeThread(mCodeAnalyzer, origin);
                this.mThread.start();
            }
        }
    }

    /**
     * Get analysis result
     *
     * @return Result of analysis
     */
    public TextColors getColors() {
        return mColors;
    }

    public interface Callback {

        void onAnalyzeDone(TextAnalyzer provider, TextColors colors);

    }

    /**
     * The result of analysis
     */
    public static class TextColors {

        public Object mExtra;
        @SuppressWarnings("CanBeFinal")
        private List<Span> mSpans;
        @SuppressWarnings("CanBeFinal")
        private List<BlockLine> mLines;
        private List<NavigationLabel> mLabels;
        private final Allocator mAllocator;
        private Span mLast;
        private int mSuppressSwitch = Integer.MAX_VALUE;

        /**
         * Create a new result with the given Allocator
         *
         * @param al Allocator to use
         */
        public TextColors(Allocator al) {
            mAllocator = al;
            mLast = null;
            mSpans = new ArrayList<>(8192);
            mLines = new ArrayList<>(1024);
        }

        /**
         * Add a new span if required (colorId is different from last)
         *
         * @param start   Start index
         * @param colorId Type
         * @param c       Target content
         */
        public void addIfNeeded(int start, int colorId, Content c) {
            if (mLast == null) {
                mSpans.add(mLast = mAllocator.obtain(start, colorId).setupLineColumn(c));
                return;
            }
            if (mLast.colorId != colorId) {
                mSpans.add(mLast = mAllocator.obtain(start, colorId).setupLineColumn(c));
            }
        }

        /**
         * Add a new span if required (colorId is different from last)
         *
         * @param start   Start index
         * @param line    Line
         * @param column  Column
         * @param colorId Type
         */
        public void addIfNeeded(int start, int line, int column, int colorId) {
            if (mLast == null) {
                mSpans.add(mLast = mAllocator.obtain(start, line, column, colorId));
                return;
            }
            if (mLast.colorId != colorId) {
                mSpans.add(mLast = mAllocator.obtain(start, line, column, colorId));
            }
        }

        /**
         * Add a span directly
         * @param span The span
         */
        public void add(Span span) {
            mSpans.add(mLast = span);
        }

        /**
         * Add a new span directly
         *
         * @param start   Start index
         * @param line    Line
         * @param column  Column
         * @param colorId Type
         */
        public void add(int start, int line, int column, int colorId) {
            mSpans.add(mLast = mAllocator.obtain(start, line, column, colorId));
        }

        /**
         * Get a new BlockLine object
         * <strong>It fields maybe not initialized with zero</strong>
         *
         * @return An idle BlockLine
         */
        public BlockLine obtainNewBlock() {
            return mAllocator.next2();
        }

        /**
         * Add a new code block info
         *
         * @param block Info of code block
         */
        public void addBlockLine(BlockLine block) {
            mLines.add(block);
        }

        /**
         * Get list of code blocks
         *
         * @return code blocks
         */
        public List<BlockLine> getBlocks() {
            return mLines;
        }

        /**
         * Get list of span
         *
         * @return spans
         */
        public List<Span> getSpans() {
            return mSpans;
        }

        /**
         * Ensure the list not empty
         */
        public void addNormalIfNull() {
            if (mLast == null) {
                mSpans.add(mLast = mAllocator.obtain(0, ColorScheme.TEXT_NORMAL).applyZero());
            }
        }

        /**
         * Get code navigation list
         *
         * @return Current navigation list
         */
        public List<NavigationLabel> getNavigation() {
            return mLabels;
        }

        /**
         * Set code navigation list
         *
         * @param navigation New navigation list
         */
        public void setNavigation(List<NavigationLabel> navigation) {
            mLabels = navigation;
        }

        /**
         * Get a NavigationLabel with given arguments
         *
         * @param line  The line of NavigationLabel
         * @param label The description of NavagationLabel
         * @return Created NavigationLabel
         */
        public NavigationLabel obtainLabel(int line, String label) {
            return mAllocator.next3(line, label);
        }

        /**
         * Returns suppress switch
         *
         * @return suppress switch
         * @see TextColors#setSuppressSwitch(int)
         */
        public int getSuppressSwitch() {
            return mSuppressSwitch;
        }

        /**
         * Set suppress switch for editor
         * What is 'suppress switch' ?:
         * Suppress switch is a switch size for code block line drawing
         * and for the process to find out which code block the cursor is in.
         * Because the code blocks are not saved by the order of both start line and
         * end line,we are unable to know exactly when we should stop the process.
         * So without a suppress switch,it will cost a large of time to search code
         * blocks.So I added this switch.
         * A suppress switch is the code block count in the first layer code block
         * (as well as its sub code blocks).
         * If you are unsure,do not set it.
         * The default value if Integer.MAX_VALUE
         *
         * @param suppressSwitch Suppress switch
         */
        public void setSuppressSwitch(int suppressSwitch) {
            mSuppressSwitch = suppressSwitch;
        }

    }

    /**
     * A object provider for speed improvement
     * Now meaningless because it is not as well as it expected
     *
     * @author Rose
     */
    public static class Allocator {

        private List<Span> cache;

        private final int max = 1024 * 128;
        private List<BlockLine> cache2;
        private final int max2 = 1024 * 8;
        private List<NavigationLabel> cache3;
        private final int max3 = 1024 * 8;

        public void addSource(List<Span> src) {
            if (src == null) {
                return;
            }
            if (cache == null) {
                cache = src;
                return;
            }
            int size = cache.size();
            int sizeAnother = src.size();
            while (sizeAnother > 0 && size < max) {
                size++;
                sizeAnother--;
                cache.add(src.get(sizeAnother));
            }
        }

        public Span next() {
            int size_t;
            if (cache == null || (size_t = cache.size()) == 0) {
                return new Span(0, 0);
            }
            return cache.remove(size_t - 1);
        }

        public Span obtain(int p1, int p2) {
            Span span = next();
            span.startIndex = p1;
            span.colorId = p2;
            return span;
        }

        public Span obtain(int p1, int p2, int p3, int p4) {
            Span span = next();
            span.startIndex = p1;
            span.line = p2;
            span.column = p3;
            span.colorId = p4;
            return span;
        }

        public void addSource2(List<BlockLine> src) {
            if (src == null) {
                return;
            }
            if (cache2 == null) {
                cache2 = src;
                return;
            }
            int size = cache2.size();
            int sizeAnother = src.size();
            while (sizeAnother > 0 && size < max2) {
                size++;
                sizeAnother--;
                cache2.add(src.get(sizeAnother));
            }
        }

        public BlockLine next2() {
            return (cache2 == null || cache2.isEmpty()) ? new BlockLine() : cache2.remove(cache2.size() - 1);
        }

        public void addSource3(List<NavigationLabel> src) {
            if (src == null) {
                return;
            }
            if (cache3 == null) {
                cache3 = src;
                return;
            }
            int size = cache3.size();
            int sizeAnother = src.size();
            while (sizeAnother > 0 && size < max3) {
                size++;
                sizeAnother--;
                cache3.add(src.get(sizeAnother));
            }
        }

        public NavigationLabel next3(int line, String label) {
            return (cache3 == null || cache3.isEmpty()) ? new NavigationLabel(line, label) : cache3.remove(cache3.size() - 1);
        }
    }

    /**
     * AnalyzeThread to control
     */
    public class AnalyzeThread extends Thread {

        private boolean waiting = false;
        private Content content;
        private final CodeAnalyzer codeAnalyzer;

        /**
         * Create a new thread
         *
         * @param a       The CodeAnalyzer to call
         * @param content The Content to analyze
         */
        public AnalyzeThread(CodeAnalyzer a, Content content) {
            codeAnalyzer = a;
            this.content = content;
        }

        @Override
        public void run() {
            TextColors colors = new TextColors(mAllocator);
            Delegate d = new Delegate();
            mThreadStartTime = System.currentTimeMillis();
            do {
                waiting = false;
                StringBuilder c = content.toStringBuilder();
                codeAnalyzer.analyze(c, colors, d);
                if (waiting) {
                    colors.mSpans.clear();
                    colors.mLast = null;
                    colors.mLines.clear();
                    colors.mSuppressSwitch = Integer.MAX_VALUE;
                    colors.mLabels = null;
                    colors.mExtra = null;
                }
            } while (waiting);

            List<Span> garbages = mColors.getSpans();
            mColors = colors;
            colors.addNormalIfNull();
            try {
                if (mCallback != null)
                    mCallback.onAnalyzeDone(TextAnalyzer.this, colors);
                mAllocator.addSource(garbages);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        /**
         * New content has been sent
         * Notify us to restart
         *
         * @param content New source
         */
        public synchronized void restartWith(Content content) {
            waiting = true;
            this.content = content;
        }

        /**
         * A delegate for token stream loop
         * To make it stop in time
         */
        public class Delegate {

            /**
             * Whether new input is set
             * If it returns true,you should stop your tokenizing at once
             *
             * @return Whether re-analyze required
             */
            public boolean shouldAnalyze() {
                return !waiting;
            }

        }

    }
}

