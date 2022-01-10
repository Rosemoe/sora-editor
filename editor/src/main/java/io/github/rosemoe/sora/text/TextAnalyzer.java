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
package io.github.rosemoe.sora.text;

import android.util.Log;

import java.util.List;

import io.github.rosemoe.sora.data.BlockLine;
import io.github.rosemoe.sora.data.ObjectAllocator;
import io.github.rosemoe.sora.data.Span;
import io.github.rosemoe.sora.interfaces.CodeAnalyzer;

/**
 * This is a manager of analyzing text
 *
 * @author Rose
 */
public class TextAnalyzer {

    private static int sThreadId = 0;
    public final RecycleObjContainer mObjContainer = new RecycleObjContainer();
    private final Object mLock = new Object();
    /**
     * Debug:Start time
     */
    public long mOpStartTime;
    private TextAnalyzeResult mResult;
    private Callback mCallback;
    private AnalyzeThread mThread;
    private final CodeAnalyzer mCodeAnalyzer;
    /**
     * Create a new manager for the given codeAnalyzer
     *
     * @param codeAnalyzer0 Target codeAnalyzer
     */
    public TextAnalyzer(CodeAnalyzer codeAnalyzer0) {
        if (codeAnalyzer0 == null) {
            throw new IllegalArgumentException();
        }
        mResult = new TextAnalyzeResult();
        mResult.addNormalIfNull();
        mCodeAnalyzer = codeAnalyzer0;
    }

    private synchronized static int nextThreadId() {
        sThreadId++;
        return sThreadId;
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
     * Stop the text analyzer
     */
    public void shutdown() {
        final AnalyzeThread thread = mThread;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            mThread = null;
        }
    }

    /**
     * Called from painting process to recycle outdated objects for reusing
     */
    public void notifyRecycle() {
        mObjContainer.recycle();
    }

    /**
     * Analyze the given text
     *
     * @param origin The source text
     */
    public synchronized void analyze(Content origin) {
        AnalyzeThread thread = this.mThread;
        if (thread == null || !thread.isAlive()) {
            Log.d("TextAnalyzer", "Starting a new thread for analyzing");
            thread = this.mThread = new AnalyzeThread(mLock, mCodeAnalyzer, origin);
            thread.setName("TextAnalyzeDaemon-" + nextThreadId());
            thread.setDaemon(true);
            thread.start();
        } else {
            thread.restartWith(origin);
            synchronized (mLock) {
                mLock.notify();
            }
        }
    }

    /**
     * Get analysis result
     *
     * @return Result of analysis
     */
    public TextAnalyzeResult getResult() {
        return mResult;
    }

    /**
     * Callback for text analyzing
     *
     * @author Rose
     */
    public interface Callback {

        /**
         * Called when analyze result is available
         * Count of calling this method is not always equal to the count you call {@link TextAnalyzer#analyze(Content)}
         *
         * @param analyzer Host TextAnalyzer
         */
        void onAnalyzeDone(TextAnalyzer analyzer);

    }

    /**
     * Container for objects that is going to be recycled
     *
     * @author Rose
     */
    public static class RecycleObjContainer {

        public List<List<Span>> spanMap;

        public List<BlockLine> blockLines;

        void recycle() {
            ObjectAllocator.recycleBlockLines(blockLines);
            SpanRecycler.getInstance().recycle(spanMap);
            clear();
        }

        void clear() {
            spanMap = null;
            blockLines = null;
        }

    }

    /**
     * AnalyzeThread to control
     */
    public class AnalyzeThread extends Thread {

        private final CodeAnalyzer codeAnalyzer;
        private final Object lock;
        private volatile boolean waiting = false;
        private Content content;

        /**
         * Create a new thread
         *
         * @param a       The CodeAnalyzer to call
         * @param content The Content to analyze
         */
        public AnalyzeThread(Object lock, CodeAnalyzer a, Content content) {
            this.lock = lock;
            codeAnalyzer = a;
            this.content = content;
        }

        @Override
        public void run() {
            try {
                // Use a cached object to cut down StringBuilder and char array allocations
                StringBuilder buffer = new StringBuilder();
                while (true) {
                    TextAnalyzeResult result = new TextAnalyzeResult();
                    Delegate d = new Delegate();
                    mOpStartTime = System.currentTimeMillis();
                    do {
                        waiting = false;
                        buffer.setLength(0);
                        content.appendToStringBuilder(buffer);
                        codeAnalyzer.analyze(buffer, result, d);
                        if (waiting) {
                            result.reset();
                        }
                    } while (waiting);

                    mObjContainer.blockLines = mResult.mBlocks;
                    mObjContainer.spanMap = mResult.mSpanMap;
                    mResult = result;
                    result.addNormalIfNull();
                    result.runBeforePublish();
                    try {
                        final var callback = mCallback;
                        if (callback != null) {
                            callback.onAnalyzeDone(TextAnalyzer.this);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                    try {
                        synchronized (lock) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        Log.d("AnalyzeThread", "Analyze daemon is being interrupted. Exiting...");
                        break;
                    }
                }
            } catch (Exception ex) {
                Log.i("AnalyzeThread", "Analyze daemon got an exception. Exiting...", ex);
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

