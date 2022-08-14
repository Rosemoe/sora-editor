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
public class EditorSearcher {

    private final CodeEditor editor;
    protected String currentPattern;
    protected SearchOptions searchOptions;
    protected Thread currentThread;
    protected LongArrayList lastResults;

    EditorSearcher(@NonNull CodeEditor editor) {
        this.editor = editor;
        this.editor.subscribeEvent(ContentChangeEvent.class, ((event, unsubscribe) -> {
            if (hasQuery() && searchOptions.useRegex) {
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
        currentPattern = pattern;
        searchOptions = options;
        if (options.useRegex) {
            runRegexMatch();
        } else if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
        }
        editor.postInvalidate();
    }

    private void runRegexMatch() {
        if (currentThread != null) {
            currentThread.interrupt();
        }
        var options = searchOptions;
        var regex = options.ignoreCase ? Pattern.compile(currentPattern, Pattern.CASE_INSENSITIVE) : Pattern.compile(currentPattern);
        var runnable = new SearchRunnable(editor.getText(), regex);
        currentThread = new Thread(runnable);
        currentThread.start();
    }

    public void stopSearch() {
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
        }
        currentThread = null;
        lastResults = null;
        currentPattern = null;
        searchOptions = null;
    }

    public boolean hasQuery() {
        return currentPattern != null;
    }

    private void checkState() {
        if (!hasQuery()) {
            throw new IllegalStateException("pattern not set");
        }
    }

    public boolean gotoNext() {
        checkState();
        if (searchOptions.useRegex) {
            if (isResultValid()) {
                var res = lastResults;
                var right = editor.getCursor().getRight();
                for (int i = 0; i < res.size(); i++) {
                    var data = res.get(i);
                    var start = IntPair.getFirst(data);
                    if (start >= right) {
                        var pos1 = editor.getText().getIndexer().getCharPosition(start);
                        var pos2 = editor.getText().getIndexer().getCharPosition(IntPair.getSecond(data));
                        editor.setSelectionRegion(pos1.line, pos1.column, pos2.line, pos2.column, SelectionChangeEvent.CAUSE_SEARCH);
                        return true;
                    }
                }
            }
        } else {
            Content text = editor.getText();
            Cursor cursor = text.getCursor();
            int line = cursor.getRightLine();
            int column = cursor.getRightColumn();
            for (int i = line; i < text.getLineCount(); i++) {
                int idx = column >= text.getColumnCount(i) ? -1 : TextUtils.indexOf(text.getLine(i), currentPattern, searchOptions.ignoreCase, column);
                if (idx != -1) {
                    editor.setSelectionRegion(i, idx, i, idx + currentPattern.length(), SelectionChangeEvent.CAUSE_SEARCH);
                    return true;
                }
                column = 0;
            }
        }
        return false;
    }

    public boolean gotoPrevious() {
        checkState();
        if (searchOptions.useRegex) {
            if (isResultValid()) {
                var res = lastResults;
                var left = editor.getCursor().getLeft();
                for (int i = 0; i < res.size(); i++) {
                    var data = res.get(i);
                    var end = IntPair.getSecond(data);
                    if (end <= left) {
                        var pos1 = editor.getText().getIndexer().getCharPosition(IntPair.getFirst(data));
                        var pos2 = editor.getText().getIndexer().getCharPosition(end);
                        editor.setSelectionRegion(pos1.line, pos1.column, pos2.line, pos2.column, SelectionChangeEvent.CAUSE_SEARCH);
                        return true;
                    }
                }
            }
        } else {
            Content text = editor.getText();
            Cursor cursor = text.getCursor();
            int line = cursor.getLeftLine();
            int column = cursor.getLeftColumn();
            for (int i = line; i >= 0; i--) {
                int idx = column - 1 < 0 ? -1 : TextUtils.lastIndexOf(text.getLine(i), currentPattern, searchOptions.ignoreCase, column - 1);
                if (idx != -1) {
                    editor.setSelectionRegion(i, idx, i, idx + currentPattern.length(), SelectionChangeEvent.CAUSE_SEARCH);
                    return true;
                }
                column = i - 1 >= 0 ? text.getColumnCount(i - 1) : 0;
            }
        }
        return false;
    }

    public boolean isMatchedPositionSelected() {
        checkState();
        var cur = editor.getCursor();
        if (!cur.isSelected()) {
            return false;
        }
        var left = cur.getLeft();
        var right = cur.getRight();
        if (searchOptions.useRegex) {
            if (isResultValid()) {
                var res = lastResults;
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
            var selected = editor.getText().subSequence(left, right).toString();
            return searchOptions.ignoreCase ? selected.equalsIgnoreCase(currentPattern) : selected.equals(currentPattern);
        }
        return false;
    }

    public void replaceThis(@NonNull String replacement) {
        if (!editor.isEditable()) {
            return;
        }
        if (isMatchedPositionSelected()) {
            editor.commitText(replacement);
        } else {
            gotoNext();
        }
    }

    public void replaceAll(@NonNull String replacement) {
        replaceAll(replacement, null);
    }

    public void replaceAll(@NonNull String replacement, @Nullable final Runnable whenFinished) {
        if (!editor.isEditable()) {
            return;
        }
        checkState();
        if (!isResultValid()) {
            Toast.makeText(editor.getContext(), R.string.editor_search_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        var context = editor.getContext();
        final var dialog = ProgressDialog.show(context, context.getString(R.string.replaceAll), context.getString(R.string.editor_search_replacing), true, false);
        final var res = lastResults;
        new Thread(() -> {
            try {
                var sb = editor.getText().toStringBuilder();
                int newLength = replacement.length();
                if (searchOptions.useRegex) {
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
                    while ((foundIndex = TextUtils.indexOf(sb, currentPattern, searchOptions.ignoreCase, fromIndex)) != -1) {
                        sb.replace(foundIndex, foundIndex + currentPattern.length(), replacement);
                        fromIndex = foundIndex + newLength;
                    }
                }
                editor.post(() -> {
                    var pos = editor.getCursor().left();
                    //stopSearch();
                    editor.getText().replace(0, 0, editor.getLineCount() - 1, editor.getText().getColumnCount(editor.getLineCount() - 1), sb);
                    editor.setSelectionAround(pos.line, pos.column);
                    dialog.dismiss();

                    if (whenFinished != null) {
                        whenFinished.run();
                    }
                });
            } catch (Exception e) {
                editor.post(() -> {
                    Toast.makeText(editor.getContext(), "Replace failed:" + e, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        }).start();
    }

    protected boolean isResultValid() {
        return currentThread == null || !currentThread.isAlive();
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
            return currentThread == mLocalThread && !Thread.interrupted();
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
                lastResults = results;
                editor.postInvalidate();
            }
        }
    }

}
