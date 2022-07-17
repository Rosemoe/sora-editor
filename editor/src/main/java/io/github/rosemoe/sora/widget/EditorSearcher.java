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
package io.github.rosemoe.sora.widget;

import android.app.ProgressDialog;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.TextUtils;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.util.LongArrayList;

/**
 * Search text in editor
 *
 * @author Rosemoe
 */
@SuppressWarnings("deprecated")
public class EditorSearcher {

    private final CodeEditor mEditor;
    protected String mPattern;
    protected SearchOptions mOptions;
    protected Thread mThread;
    protected LongArrayList mLastResults;

    EditorSearcher(@NonNull CodeEditor editor) {
        mEditor = editor;
        mEditor.subscribeEvent(ContentChangeEvent.class, ((event, unsubscribe) -> {
            if (hasQuery() && mOptions.useRegex) {
                runRegexMatch();
            }
        }));
    }

    public void search(@NonNull String pattern, @NonNull SearchOptions options) {
        if (pattern.length() == 0) {
            throw new IllegalArgumentException("pattern length must be > 0");
        }
        if (options.useRegex) {
            // Pre-check
            //noinspection ResultOfMethodCallIgnored
            Pattern.compile(pattern);
        }
        mPattern = pattern;
        mOptions = options;
        if (options.useRegex) {
            runRegexMatch();
        } else if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
        mEditor.postInvalidate();
    }

    private void runRegexMatch() {
        if (mThread != null) {
            mThread.interrupt();
        }
        var options = mOptions;
        var regex = options.ignoreCase ? Pattern.compile(mPattern, Pattern.CASE_INSENSITIVE) : Pattern.compile(mPattern);
        var runnable = new SearchRunnable(mEditor.getText(), regex);
        mThread = new Thread(runnable);
        mThread.start();
    }

    public void stopSearch() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
        mThread = null;
        mLastResults = null;
        mPattern = null;
        mOptions = null;
    }

    public boolean hasQuery() {
        return mPattern != null;
    }

    private void checkState() {
        if (!hasQuery()) {
            throw new IllegalStateException("pattern not set");
        }
    }

    public boolean gotoNext() {
        checkState();
        if (mOptions.useRegex) {
            if (isResultValid()) {
                var res = mLastResults;
                var right = mEditor.getCursor().getRight();
                for (int i = 0; i < res.size(); i++) {
                    var data = res.get(i);
                    var start = IntPair.getFirst(data);
                    if (start >= right) {
                        var pos1 = mEditor.getText().getIndexer().getCharPosition(start);
                        var pos2 = mEditor.getText().getIndexer().getCharPosition(IntPair.getSecond(data));
                        mEditor.setSelectionRegion(pos1.line, pos1.column, pos2.line, pos2.column, SelectionChangeEvent.CAUSE_SEARCH);
                        return true;
                    }
                }
            }
        } else {
            Content text = mEditor.getText();
            Cursor cursor = text.getCursor();
            int line = cursor.getRightLine();
            int column = cursor.getRightColumn();
            for (int i = line; i < text.getLineCount(); i++) {
                int idx = column >= text.getColumnCount(i) ? -1 : TextUtils.indexOf(text.getLine(i), mPattern, mOptions.ignoreCase, column);
                if (idx != -1) {
                    mEditor.setSelectionRegion(i, idx, i, idx + mPattern.length(), SelectionChangeEvent.CAUSE_SEARCH);
                    return true;
                }
                column = 0;
            }
        }
        return false;
    }

    public boolean gotoPrevious() {
        checkState();
        if (mOptions.useRegex) {
            if (isResultValid()) {
                var res = mLastResults;
                var left = mEditor.getCursor().getLeft();
                for (int i = 0; i < res.size(); i++) {
                    var data = res.get(i);
                    var end = IntPair.getSecond(data);
                    if (end <= left) {
                        var pos1 = mEditor.getText().getIndexer().getCharPosition(IntPair.getFirst(data));
                        var pos2 = mEditor.getText().getIndexer().getCharPosition(end);
                        mEditor.setSelectionRegion(pos1.line, pos1.column, pos2.line, pos2.column, SelectionChangeEvent.CAUSE_SEARCH);
                        return true;
                    }
                }
            }
        } else {
            Content text = mEditor.getText();
            Cursor cursor = text.getCursor();
            int line = cursor.getLeftLine();
            int column = cursor.getLeftColumn();
            for (int i = line; i >= 0; i--) {
                int idx = column - 1 < 0 ? -1 : TextUtils.lastIndexOf(text.getLine(i), mPattern, mOptions.ignoreCase, column - 1);
                if (idx != -1) {
                    mEditor.setSelectionRegion(i, idx, i, idx + mPattern.length(), SelectionChangeEvent.CAUSE_SEARCH);
                    return true;
                }
                column = i - 1 >= 0 ? text.getColumnCount(i - 1) : 0;
            }
        }
        return false;
    }

    public boolean isMatchedPositionSelected() {
        checkState();
        var cur = mEditor.getCursor();
        if (!cur.isSelected()) {
            return false;
        }
        var left = cur.getLeft();
        var right = cur.getRight();
        if (mOptions.useRegex) {
            if (isResultValid()) {
                var res = mLastResults;
                var packed = IntPair.pack(left, right);
                for (int i = 0; i < res.size(); i++) {
                    var value = res.get(i);
                    if (value == packed) {
                        return true;
                    } else if (value > packed) {
                        // Values behind can not be valid
                        break;
                    }
                }
            }
        } else {
            var selected = mEditor.getText().subSequence(left, right).toString();
            return mOptions.ignoreCase ? selected.equalsIgnoreCase(mPattern) : selected.equals(mPattern);
        }
        return false;
    }

    public void replaceThis(@NonNull String replacement) {
        if (!mEditor.isEditable()) {
            return;
        }
        if (isMatchedPositionSelected()) {
            mEditor.commitText(replacement);
        } else {
            gotoNext();
        }
    }

    public void replaceAll(@NonNull String replacement) {
        replaceAll(replacement, null);
    }

    public void replaceAll(@NonNull String replacement, @Nullable final Runnable whenFinished) {
        if (!mEditor.isEditable()) {
            return;
        }
        checkState();
        if (!isResultValid()) {
            Toast.makeText(mEditor.getContext(), R.string.editor_search_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        var context = mEditor.getContext();
        final var dialog = ProgressDialog.show(context, context.getString(R.string.replaceAll), context.getString(R.string.editor_search_replacing), true, false);
        final var res = mLastResults;
        new Thread(() -> {
            try {
                var sb = mEditor.getText().toStringBuilder();
                int newLength = replacement.length();
                if (mOptions.useRegex) {
                    int delta = 0;
                    for (int i = 0; i < res.size(); i++) {
                        var region = res.get(i);
                        var start = IntPair.getFirst(region);
                        var end = IntPair.getSecond(region);
                        var oldLength = end - start;
                        sb.replace(start + delta, end + delta, replacement);
                        delta += newLength - oldLength;
                    }
                } else {
                    int fromIndex = 0;
                    int foundIndex;
                    while ((foundIndex = TextUtils.indexOf(sb, mPattern, mOptions.ignoreCase, fromIndex)) != -1) {
                        sb.replace(foundIndex, foundIndex + mPattern.length(), replacement);
                        fromIndex = foundIndex + newLength;
                    }
                }
                mEditor.post(() -> {
                    var pos = mEditor.getCursor().left();
                    //stopSearch();
                    mEditor.getText().replace(0, 0, mEditor.getLineCount() - 1, mEditor.getText().getColumnCount(mEditor.getLineCount() - 1), sb);
                    mEditor.setSelectionAround(pos.line, pos.column);
                    dialog.dismiss();

                    if (whenFinished != null) {
                        whenFinished.run();
                    }
                });
            } catch (Exception e) {
                mEditor.post(() -> {
                    Toast.makeText(mEditor.getContext(), "Replace failed:" + e, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        }).start();
    }

    protected boolean isResultValid() {
        return mThread == null || !mThread.isAlive();
    }

    public static class SearchOptions {

        public final boolean ignoreCase;
        public final boolean useRegex;

        public SearchOptions(boolean ignoreCase, boolean useRegex) {
            this.ignoreCase = ignoreCase;
            this.useRegex = useRegex;
        }

    }

    /**
     * Run for regex matching
     */
    private final class SearchRunnable implements Runnable {

        private final StringBuilder mText;
        private final Pattern mPattern;
        private Thread mLocalThread;

        public SearchRunnable(@NonNull Content content, @NonNull Pattern pattern) {
            this.mText = content.toStringBuilder();
            this.mPattern = pattern;
        }

        private boolean checkNotCancelled() {
            return mThread == mLocalThread && !Thread.interrupted();
        }

        @Override
        public void run() {
            mLocalThread = Thread.currentThread();
            var results = new LongArrayList();
            var matcher = mPattern.matcher(mText);
            var start = 0;
            while (start < mText.length() && matcher.find(start) && checkNotCancelled()) {
                // Search next one from end
                start = matcher.end();
                results.add(IntPair.pack(matcher.start(), start));
            }
            if (checkNotCancelled()) {
                mLastResults = results;
                mEditor.postInvalidate();
            }
        }
    }

}
