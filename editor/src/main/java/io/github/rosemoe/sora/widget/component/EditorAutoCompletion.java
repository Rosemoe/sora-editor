/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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
package io.github.rosemoe.sora.widget.component;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.styling.StylesUtils;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.TextReference;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * Auto complete window for editing code quicker
 *
 * @author Rosemoe
 */
public class EditorAutoCompletion extends EditorPopupWindow implements EditorBuiltinComponent {

    private final static long SHOW_PROGRESS_BAR_DELAY = 50;
    private final CodeEditor editor;
    protected boolean cancelShowUp = false;
    protected long requestTime;
    protected int maxHeight;
    protected CompletionThread completionThread;
    protected CompletionPublisher publisher;
    protected WeakReference<List<CompletionItem>> lastAttachedItems;
    protected int currentSelection = -1;
    protected EditorCompletionAdapter adapter;
    private CompletionLayout layout;
    private long requestShow = 0;
    private long requestHide = -1;
    private boolean enabled = true;
    private boolean loading = false;

    /**
     * Create a panel instance for the given editor
     *
     * @param editor Target editor
     */
    public EditorAutoCompletion(@NonNull CodeEditor editor) {
        super(editor, FEATURE_HIDE_WHEN_FAST_SCROLL);
        this.editor = editor;
        adapter = new DefaultCompletionItemAdapter();
        setLayout(new DefaultCompletionLayout());
        editor.subscribeEvent(ColorSchemeUpdateEvent.class, ((event, unsubscribe) -> applyColorScheme()));
    }

    @SuppressWarnings("unchecked")
    public void setLayout(@NonNull CompletionLayout layout) {
        this.layout = layout;
        layout.setEditorCompletion(this);
        setContentView(layout.inflate(editor.getContext()));
        applyColorScheme();
        if (adapter != null) {
            this.layout.getCompletionList().setAdapter(adapter);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hide();
        }
    }

    public boolean isCompletionInProgress() {
        final var thread = completionThread;
        return super.isShowing() || requestShow > requestHide || (thread != null && thread.isAlive());
    }

    /**
     * Some layout may support to display more animations,
     * this method provides control over the animation of the layout。
     * @see CompletionLayout#setEnabledAnimation(boolean)
     */
    public void setEnabledAnimation(boolean enabledAnimation) {
        layout.setEnabledAnimation(enabledAnimation);
    }

    @SuppressWarnings("unchecked")
    public void setAdapter(@Nullable EditorCompletionAdapter adapter) {
        this.adapter = adapter;
        if (adapter == null) {
            this.adapter = new DefaultCompletionItemAdapter();
        }

        layout.getCompletionList().setAdapter(adapter);
    }

    @Override
    public void show() {
        if (cancelShowUp || !isEnabled()) {
            return;
        }
        requestShow = System.currentTimeMillis();
        final var requireRequest = requestTime;
        editor.postDelayedInLifecycle(() -> {
            if (requestHide < requestShow && requestTime == requireRequest) {
                super.show();
            }
        }, 70);
    }

    public void hide() {
        super.dismiss();
        cancelCompletion();
        requestHide = System.currentTimeMillis();
    }

    public Context getContext() {
        return editor.getContext();
    }

    public int getCurrentPosition() {
        return currentSelection;
    }

    /**
     * Apply colors for self
     */
    public void applyColorScheme() {
        EditorColorScheme colors = editor.getColorScheme();
        layout.onApplyColorScheme(colors);
    }

    /**
     * Change layout to loading/idle
     *
     * @param state Whether loading
     */
    public void setLoading(boolean state) {
        loading = state;
        if (state) {
            editor.postDelayedInLifecycle(() -> {
                if (loading) {
                    layout.setLoading(true);
                }
            }, SHOW_PROGRESS_BAR_DELAY);
        } else {
            layout.setLoading(false);
        }

    }

    /**
     * Move selection down
     */
    public void moveDown() {
        var adpView = layout.getCompletionList();
        if (currentSelection + 1 >= adpView.getAdapter().getCount()) {
            return;
        }
        currentSelection++;
        ((EditorCompletionAdapter) adpView.getAdapter()).notifyDataSetChanged();
        ensurePosition();
    }

    /**
     * Move selection up
     */
    public void moveUp() {
        var adpView = layout.getCompletionList();
        if (currentSelection - 1 < 0) {
            return;
        }
        currentSelection--;
        ((EditorCompletionAdapter) adpView.getAdapter()).notifyDataSetChanged();
        ensurePosition();
    }

    /**
     * Make current selection visible
     */
    private void ensurePosition() {
        if (currentSelection != -1)
            layout.ensureListPositionVisible(currentSelection, adapter.getItemHeight());
    }

    /**
     * Reject the requests from IME to set composing region/text
     */
    public boolean shouldRejectComposing() {
        return cancelShowUp;
    }

    /**
     * Select current position
     *
     * @return if the action is performed
     */
    public boolean select() {
        return select(currentSelection);
    }

    /**
     * Select the given position
     *
     * @param pos Index of auto complete item
     * @return if the action is performed
     */
    public boolean select(int pos) {
        if (pos == -1) {
            return false;
        }
        var adpView = layout.getCompletionList();
        var item = ((EditorCompletionAdapter) adpView.getAdapter()).getItem(pos);
        Cursor cursor = editor.getCursor();
        final var completionThread = this.completionThread;
        if (!cursor.isSelected() && completionThread != null) {
            cancelShowUp = true;
            editor.restartInput();
            editor.getText().beginBatchEdit();
            item.performCompletion(editor, editor.getText(), completionThread.requestPosition);
            editor.getText().endBatchEdit();
            editor.updateCursor();
            cancelShowUp = false;
            editor.restartInput();
        }
        hide();
        return true;
    }

    /**
     * Stop previous completion thread
     */
    public void cancelCompletion() {
        var previous = completionThread;
        if (previous != null && previous.isAlive()) {
            previous.cancel();
            previous.requestTimestamp = -1;
        }
        completionThread = null;
    }

    /**
     * Check cursor position's span.
     * If {@link io.github.rosemoe.sora.lang.styling.TextStyle#NO_COMPLETION_BIT} is set, true is returned.
     */
    public boolean checkNoCompletion() {
        var pos = editor.getCursor().left();
        var styles = editor.getStyles();
        return StylesUtils.checkNoCompletion(styles, pos);
    }

    /**
     * Start completion at current selection position
     */
    public void requireCompletion() {
        if (cancelShowUp || !isEnabled()) {
            return;
        }
        var text = editor.getText();
        if (text.getCursor().isSelected() || checkNoCompletion()) {
            hide();
            return;
        }
        if (System.nanoTime() - requestTime < editor.getProps().cancelCompletionNs) {
            hide();
            requestTime = System.nanoTime();
            return;
        }
        cancelCompletion();
        requestTime = System.nanoTime();
        currentSelection = -1;
        publisher = new CompletionPublisher(editor.getHandler(), () -> {
            var items = publisher.getItems();
            if (lastAttachedItems == null || lastAttachedItems.get() != items) {
                adapter.attachValues(this, items);
                adapter.notifyDataSetInvalidated();
                lastAttachedItems = new WeakReference<>(items);
            } else {
                adapter.notifyDataSetChanged();
            }
            float newHeight = adapter.getItemHeight() * adapter.getCount();
            if (newHeight == 0) {
                hide();
            }
            editor.updateCompletionWindowPosition();
            setSize(getWidth(), (int) Math.min(newHeight, maxHeight));
            if (!isShowing()) {
                show();
            }
        }, editor.getEditorLanguage().getInterruptionLevel());
        completionThread = new CompletionThread(requestTime, publisher);
        setLoading(true);
        completionThread.start();
    }

    public void setMaxHeight(int height) {
        maxHeight = height;
    }

    /**
     * Auto-completion Analyzing thread
     *
     * @author Rosemoe
     */
    public final class CompletionThread extends Thread implements TextReference.Validator {

        private final Bundle extraData;
        private final CharPosition requestPosition;
        private final Language targetLanguage;
        private final ContentReference contentRef;
        private final CompletionPublisher localPublisher;
        private long requestTimestamp;
        private boolean isAborted;

        public CompletionThread(long requestTime, @NonNull CompletionPublisher publisher) {
            requestTimestamp = requestTime;
            requestPosition = editor.getCursor().left();
            targetLanguage = editor.getEditorLanguage();
            contentRef = new ContentReference(editor.getText());
            contentRef.setValidator(this);
            localPublisher = publisher;
            extraData = editor.getExtraArguments();
            isAborted = false;
        }

        /**
         * Abort the completion thread
         */
        public void cancel() {
            isAborted = true;
            var level = targetLanguage.getInterruptionLevel();
            if (level == Language.INTERRUPTION_LEVEL_STRONG) {
                interrupt();
            }
            localPublisher.cancel();
        }

        public boolean isCancelled() {
            return isAborted;
        }

        @Override
        public void validate() {
            if (requestTime != requestTimestamp || isAborted) {
                throw new CompletionCancelledException();
            }
        }

        @Override
        public void run() {
            try {
                targetLanguage.requireAutoComplete(contentRef, requestPosition, localPublisher, extraData);
                if (localPublisher.hasData()) {
                    if (completionThread == Thread.currentThread()) {
                        localPublisher.updateList(true);
                    }
                } else {
                    editor.postInLifecycle(EditorAutoCompletion.this::hide);
                }
                editor.postInLifecycle(() -> setLoading(false));
            } catch (Exception e) {
                if (e instanceof CompletionCancelledException) {
                    Log.v("CompletionThread", "Completion is cancelled");
                } else {
                    e.printStackTrace();
                }
            }
        }


    }

}

