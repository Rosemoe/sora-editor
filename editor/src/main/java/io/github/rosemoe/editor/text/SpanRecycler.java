/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.text;

import android.util.Log;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.github.rosemoe.editor.struct.Span;

public class SpanRecycler {

    private static SpanRecycler INSTANCE;
    private final BlockingQueue<List<List<Span>>> taskQueue;
    private Thread recycleThread;
    private SpanRecycler() {
        taskQueue = new ArrayBlockingQueue<>(8);
    }

    public static synchronized SpanRecycler getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpanRecycler();
        }
        return INSTANCE;
    }

    public void recycle(List<List<Span>> spans) {
        if (spans == null) {
            return;
        }
        if (recycleThread == null || !recycleThread.isAlive()) {
            recycleThread = new RecycleThread();
            recycleThread.start();
        }
        taskQueue.offer(spans);
    }

    private class RecycleThread extends Thread {

        private final static String LOG_TAG = "Span Recycle Thread";

        RecycleThread() {
            setDaemon(true);
            setName("SpanRecycleDaemon");
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    try {
                        List<List<Span>> spanMap = taskQueue.take();
                        int count = 0;
                        for (List<Span> spans : spanMap) {
                            int size = spans.size();
                            for (int i = 0; i < size; i++) {
                                spans.remove(size - 1 - i).recycle();
                                count++;
                            }
                        }
                        //Log.i(LOG_TAG, "Recycled " + count + " spans");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, e);
            }
            Log.i(LOG_TAG, "Recycler exited");
        }

    }

}
