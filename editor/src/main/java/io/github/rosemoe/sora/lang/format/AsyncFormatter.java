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
package io.github.rosemoe.sora.lang.format;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.StyleReceiver;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.IntPair;

public abstract class AsyncFormatter implements Formatter {

    private FormatResultReceiver receiver;

    private final static String LOG_TAG = "AsyncFormatter";
    private static int sThreadId = 0;


    private final ReentrantLock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private volatile Content text;
    private volatile Pair<CharPosition, CharPosition> range;

    private FormattingThread thread;

    @Override
    public void setReceiver(FormatResultReceiver receiver) {
        this.receiver = receiver;
    }

    private void run() {

        if (thread == null || !thread.isAlive()) {
            // Create new thread
            Log.v(LOG_TAG, "Starting a new thread for formatting");
            thread = new FormattingThread();
            thread.setDaemon(true);
            thread.setName("AsyncFormatter-" + nextThreadId());
            thread.start();
        } else {
            // Wake up thread
            Log.v(LOG_TAG, "Waking up thread for formatting");
            lock.lock();
            condition.signal();
            lock.unlock();
        }

    }

    private synchronized static int nextThreadId() {
        sThreadId++;
        return sThreadId;
    }

    @Override
    public void format(Content text) {
        this.text = text;
        range = null;
        run();
    }

    @Override
    public  boolean isRunning() {
        return thread != null && thread.isAlive() && lock.isLocked();
    }

    @Override
    public void formatRegion(Content text, CharPosition start, CharPosition end) {
        this.text = text;
        range = new Pair<>(start, end);
        run();
    }


    /**
     * like {@link Formatter#format(Content)}, but run in background thread
     */
    @WorkerThread
    public abstract void formatAsync(Content text);

    /**
     * like {@link Formatter#formatRegion(Content, CharPosition, CharPosition)}, but run in background thread
     */
    @WorkerThread
    public abstract void formatRegionAsync(Content text, CharPosition start, CharPosition end);

    private void sendUpdate(Content text) {
        if (receiver != null) {
            receiver.onFormatSucceed(text);
        }
    }

    private void sendFailure(Throwable throwable) {
        if (receiver != null) {
            receiver.onFormatFail(throwable);
        }
    }

    private class FormattingThread extends Thread {

        @Override
        public void run() {
            Log.v(LOG_TAG, "AsyncFormatter thread started");
            try {
                while (!isInterrupted()) {
                    lock.lock();
                    if (text == null) {
                        continue;
                    }
                    if (range == null) {
                        formatAsync(text);
                    } else {
                        formatRegionAsync(text, range.first, range.second);
                    }
                    sendUpdate(text);
                    // Wait for next time
                    condition.await();
                }
            } catch (InterruptedException e) {
                Log.v(LOG_TAG, "Thread is interrupted.");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unexpected exception is thrown in the thread.", e);
                sendFailure(e);
            }
        }
    }

    @Override
    public void destroy() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        thread = null;
        receiver = null;
        text = null;
        range = null;
    }
}
