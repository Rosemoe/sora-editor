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

import android.annotation.SuppressLint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.R;
import io.github.rosemoe.sora.event.EventReceiver;
import io.github.rosemoe.sora.event.HandleStateChangeEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.LongPressEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.Unsubscribe;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.EditorTouchEventHandler;
import io.github.rosemoe.sora.widget.base.EditorPopupWindow;

/**
 * This window will show when selecting text to present text actions.
 *
 * @author Rosemoe
 */
public class EditorTextActionWindow extends EditorPopupWindow implements View.OnClickListener, EventReceiver<SelectionChangeEvent>, EditorBuiltinComponent {
    private final static long DELAY = 200;
    private final CodeEditor editor;
    private final ImageButton pasteBtn;
    private final ImageButton copyBtn;
    private final ImageButton cutBtn;
    private final ImageButton longSelectBtn;
    private final View rootView;
    private final EditorTouchEventHandler handler;
    private long lastScroll;
    private int lastPosition;
    private int lastCause;
    private boolean enabled = true;

    /**
     * Create a panel for the given editor
     *
     * @param editor Target editor
     */
    public EditorTextActionWindow(CodeEditor editor) {
        super(editor, FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED);
        this.editor = editor;
        handler = editor.getEventHandler();
        // Since popup window does provide decor view, we have to pass null to this method
        @SuppressLint("InflateParams")
        View root = LayoutInflater.from(editor.getContext()).inflate(R.layout.text_compose_panel, null);
        ImageButton selectAll = root.findViewById(R.id.panel_btn_select_all);
        ImageButton cut = root.findViewById(R.id.panel_btn_cut);
        ImageButton copy = root.findViewById(R.id.panel_btn_copy);
        longSelectBtn = root.findViewById(R.id.panel_btn_long_select);
        pasteBtn = root.findViewById(R.id.panel_btn_paste);
        copyBtn = copy;
        cutBtn = cut;
        selectAll.setOnClickListener(this);
        cut.setOnClickListener(this);
        copy.setOnClickListener(this);
        pasteBtn.setOnClickListener(this);
        longSelectBtn.setOnClickListener(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(5 * editor.getDpUnit());
        gd.setColor(0xffffffff);
        root.setBackground(gd);
        setContentView(root);
        setSize(0, (int) (this.editor.getDpUnit() * 48));
        rootView = root;
        editor.subscribeEvent(SelectionChangeEvent.class, this);
        editor.subscribeEvent(ScrollEvent.class, ((event, unsubscribe) -> {
            var last = lastScroll;
            lastScroll = System.currentTimeMillis();
            if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
                postDisplay();
            }
        }));
        editor.subscribeEvent(HandleStateChangeEvent.class, ((event, unsubscribe) -> {
            if (event.isHeld()) {
                postDisplay();
            }
        }));
        editor.subscribeEvent(LongPressEvent.class, ((event, unsubscribe) -> {
            if (editor.getCursor().isSelected() && lastCause == SelectionChangeEvent.CAUSE_SEARCH) {
                var idx = event.getIndex();
                if (idx >= editor.getCursor().getLeft() && idx <= editor.getCursor().getRight()) {
                    lastCause = 0;
                    displayWindow();
                }
                event.intercept(InterceptTarget.TARGET_EDITOR);
            }
        }));
        editor.subscribeEvent(HandleStateChangeEvent.class, ((event, unsubscribe) -> {
            if (!event.getEditor().getCursor().isSelected()
                    && event.getHandleType() == HandleStateChangeEvent.HANDLE_TYPE_INSERT
                    && !event.isHeld()) {
                displayWindow();
                // Also, post to hide the window on handle disappearance
                editor.postDelayedInLifecycle(new Runnable() {
                    @Override
                    public void run() {
                        if (!editor.getEventHandler().shouldDrawInsertHandle()
                                && !editor.getCursor().isSelected()) {
                            dismiss();
                        } else if (!editor.getCursor().isSelected()) {
                            editor.postDelayedInLifecycle(this, 100);
                        }
                    }
                }, 100);
            }
        }));

        getPopup().setAnimationStyle(R.style.text_action_popup_animation);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            dismiss();
        }
    }

    /**
     * Get the view root of the panel.
     * <p>
     * Root view is {@link android.widget.LinearLayout}
     * Inside is a {@link android.widget.HorizontalScrollView}
     *
     * @see R.id#panel_root
     * @see R.id#panel_hv
     * @see R.id#panel_btn_select_all
     * @see R.id#panel_btn_copy
     * @see R.id#panel_btn_cut
     * @see R.id#panel_btn_paste
     */
    public ViewGroup getView() {
        return (ViewGroup) getPopup().getContentView();
    }

    private void postDisplay() {
        if (!isShowing()) {
            return;
        }
        dismiss();
        if (!editor.getCursor().isSelected()) {
            return;
        }
        editor.postDelayedInLifecycle(new Runnable() {
            @Override
            public void run() {
                if (!handler.hasAnyHeldHandle() && !editor.getSnippetController().isInSnippet() && System.currentTimeMillis() - lastScroll > DELAY
                        && editor.getScroller().isFinished()) {
                    displayWindow();
                } else {
                    editor.postDelayedInLifecycle(this, DELAY);
                }
            }
        }, DELAY);
    }

    @Override
    public void onReceive(@NonNull SelectionChangeEvent event, @NonNull Unsubscribe unsubscribe) {
        if (handler.hasAnyHeldHandle()) {
            return;
        }
        lastCause = event.getCause();
        if (event.isSelected()) {
            // Always post show. See #193
            if (event.getCause() != SelectionChangeEvent.CAUSE_SEARCH) {
                editor.postInLifecycle(this::displayWindow);
            } else {
                dismiss();
            }
            lastPosition = -1;
        } else {
            var show = false;
            if (event.getCause() == SelectionChangeEvent.CAUSE_TAP && event.getLeft().index == lastPosition && !isShowing() && !editor.getText().isInBatchEdit() && editor.isEditable()) {
                editor.postInLifecycle(this::displayWindow);
                show = true;
            } else {
                dismiss();
            }
            if (event.getCause() == SelectionChangeEvent.CAUSE_TAP && !show) {
                lastPosition = event.getLeft().index;
            } else {
                lastPosition = -1;
            }
        }
    }

    private int selectTop(@NonNull RectF rect) {
        var rowHeight = editor.getRowHeight();
        if (rect.top - rowHeight * 3 / 2F > getHeight()) {
            return (int) (rect.top - rowHeight * 3 / 2 - getHeight());
        } else {
            return (int) (rect.bottom + rowHeight / 2);
        }
    }

    public void displayWindow() {
        updateBtnState();
        int top;
        var cursor = editor.getCursor();
        if (cursor.isSelected()) {
            var leftRect = editor.getLeftHandleDescriptor().position;
            var rightRect = editor.getRightHandleDescriptor().position;
            var top1 = selectTop(leftRect);
            var top2 = selectTop(rightRect);
            top = Math.min(top1, top2);
        } else {
            top = selectTop(editor.getInsertHandleDescriptor().position);
        }
        top = Math.max(0, Math.min(top, editor.getHeight() - getHeight() - 5));
        float handleLeftX = editor.getOffset(editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn());
        float handleRightX = editor.getOffset(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
        int panelX = (int) ((handleLeftX + handleRightX) / 2f - rootView.getMeasuredWidth() / 2f);
        setLocationAbsolutely(panelX, top);
        show();
    }

    /**
     * Update the state of paste button
     */
    private void updateBtnState() {
        pasteBtn.setEnabled(editor.hasClip());
        copyBtn.setVisibility(editor.getCursor().isSelected() ? View.VISIBLE : View.GONE);
        pasteBtn.setVisibility(editor.isEditable() ? View.VISIBLE : View.GONE);
        cutBtn.setVisibility((editor.getCursor().isSelected() && editor.isEditable()) ? View.VISIBLE : View.GONE);
        longSelectBtn.setVisibility((!editor.getCursor().isSelected() && editor.isEditable()) ? View.VISIBLE : View.GONE);
        rootView.measure(View.MeasureSpec.makeMeasureSpec(1000000, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(100000, View.MeasureSpec.AT_MOST));
        setSize(Math.min(rootView.getMeasuredWidth(), (int) (editor.getDpUnit() * 230)), getHeight());
    }

    @Override
    public void show() {
        if (!enabled || editor.getSnippetController().isInSnippet()) {
            return;
        }
        super.show();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.panel_btn_select_all) {
            editor.selectAll();
            return;
        } else if (id == R.id.panel_btn_cut) {
            if (editor.getCursor().isSelected()) {
                editor.cutText();
            }
        } else if (id == R.id.panel_btn_paste) {
            editor.pasteText();
            editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
        } else if (id == R.id.panel_btn_copy) {
            editor.copyText();
            editor.setSelection(editor.getCursor().getRightLine(), editor.getCursor().getRightColumn());
        } else if (id == R.id.panel_btn_long_select) {
            editor.beginLongSelect();
        }
        dismiss();
    }

}

