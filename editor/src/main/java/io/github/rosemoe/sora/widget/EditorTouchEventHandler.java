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
package io.github.rosemoe.sora.widget;

import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Build;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.annotations.UnsupportedUserUsage;
import io.github.rosemoe.sora.event.ClickEvent;
import io.github.rosemoe.sora.event.DoubleClickEvent;
import io.github.rosemoe.sora.event.HandleStateChangeEvent;
import io.github.rosemoe.sora.event.InterceptTarget;
import io.github.rosemoe.sora.event.LongPressEvent;
import io.github.rosemoe.sora.event.ScrollEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.SideIconClickEvent;
import io.github.rosemoe.sora.graphics.RectUtils;
import io.github.rosemoe.sora.lang.styling.line.LineSideIcon;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.component.Magnifier;
import io.github.rosemoe.sora.widget.style.SelectionHandleStyle;

/**
 * Handles touch events of editor
 *
 * @author Rosemoe
 */
@SuppressWarnings("CanBeFinal")
public final class EditorTouchEventHandler implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

    private final static int HIDE_DELAY = 3000;
    private final static int HIDE_DELAY_HANDLE = 3500;

    private final static int SCROLLBAR_FADE_ANIMATION_TIME = 200;
    private final static int MAGNIFIER_TOUCH_SLOP = 4;

    private final static int LEFT_EDGE = 1;
    private final static int RIGHT_EDGE = 1 << 1;
    private final static int TOP_EDGE = 1 << 2;
    private final static int BOTTOM_EDGE = 1 << 3;

    private final CodeEditor editor;
    private final EditorScroller scroller;
    private final SelectionHandle insertHandle;
    Magnifier magnifier;
    int selHandleType = -1;
    float motionX;
    float motionY;
    boolean glowTopOrBottom; //true for bottom
    boolean glowLeftOrRight; //true for right
    public boolean isScaling = false;
    float scaleMaxSize;
    float scaleMinSize;
    private float textSizeStart;
    private long timeLastScroll = 0;
    private long timeLastSetSelection = 0;
    private boolean holdingScrollbarVertical = false;
    private boolean holdingScrollbarHorizontal = false;
    private boolean holdingInsertHandle = false;
    private float thumbDownY = 0;
    private float thumbDownX = 0;
    private SelectionHandle leftHandle;
    private SelectionHandle rightHandle;
    private int touchedHandleType = -1;
    private float edgeFieldSize;
    private int edgeFlags;
    private MotionEvent thumbMotionRecord;

    /**
     * Create an event handler for the given editor
     *
     * @param editor Host editor
     */
    public EditorTouchEventHandler(@NonNull CodeEditor editor) {
        this.editor = editor;
        scroller = new EditorScroller(editor);
        scaleMaxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26, Resources.getSystem().getDisplayMetrics());
        scaleMinSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, Resources.getSystem().getDisplayMetrics());
        magnifier = new Magnifier(editor);
        this.leftHandle = new SelectionHandle(SelectionHandle.LEFT);
        this.rightHandle = new SelectionHandle(SelectionHandle.RIGHT);
        insertHandle = new SelectionHandle(SelectionHandle.BOTH);
    }

    public boolean hasAnyHeldHandle() {
        return holdInsertHandle() || selHandleType != -1;
    }

    /**
     * Whether we should draw scroll bars
     *
     * @return whether draw scroll bars
     */
    public boolean shouldDrawScrollBar() {
        return System.currentTimeMillis() - timeLastScroll < HIDE_DELAY + SCROLLBAR_FADE_ANIMATION_TIME || holdingScrollbarVertical || holdingScrollbarHorizontal;
    }

    @UnsupportedUserUsage
    public float getScrollBarMovementPercentage() {
        if (System.currentTimeMillis() - timeLastScroll < HIDE_DELAY || holdingScrollbarVertical || holdingScrollbarHorizontal) {
            return 0f;
        } else if (System.currentTimeMillis() - timeLastScroll >= HIDE_DELAY && System.currentTimeMillis() - timeLastScroll < HIDE_DELAY + SCROLLBAR_FADE_ANIMATION_TIME) {
            editor.postInvalidateOnAnimation();
            return (System.currentTimeMillis() - timeLastScroll - HIDE_DELAY) * 1f / SCROLLBAR_FADE_ANIMATION_TIME;
        }
        return 1f;
    }

    /**
     * Hide the insert handle at once
     */
    public void hideInsertHandle() {
        if (!shouldDrawInsertHandle()) {
            return;
        }
        timeLastSetSelection = 0;
        editor.invalidate();
    }

    /**
     * Whether the vertical scroll bar is touched
     *
     * @return Whether touched
     */
    public boolean holdVerticalScrollBar() {
        return holdingScrollbarVertical;
    }

    /**
     * Whether the horizontal scroll bar is touched
     *
     * @return Whether touched
     */
    public boolean holdHorizontalScrollBar() {
        return holdingScrollbarHorizontal;
    }

    /**
     * Whether insert handle is touched
     *
     * @return Whether touched
     */
    public boolean holdInsertHandle() {
        return holdingInsertHandle;
    }

    /**
     * Whether the editor should draw insert handler
     *
     * @return Whether to draw
     */
    public boolean shouldDrawInsertHandle() {
        return (System.currentTimeMillis() - timeLastSetSelection < HIDE_DELAY_HANDLE || holdingInsertHandle);
    }

    /**
     * Notify the editor later to hide scroll bars
     */
    public void notifyScrolled() {
        timeLastScroll = System.currentTimeMillis();
        class ScrollNotifier implements Runnable {

            @Override
            public void run() {
                if (System.currentTimeMillis() - timeLastScroll >= HIDE_DELAY) {
                    editor.invalidate();
                }
            }

        }
        editor.postDelayedInLifecycle(new ScrollNotifier(), HIDE_DELAY);
    }

    /**
     * Notify the editor later to hide insert handle
     */
    public void notifyLater() {
        timeLastSetSelection = System.currentTimeMillis();
        class InvalidateNotifier implements Runnable {

            @Override
            public void run() {
                if (System.currentTimeMillis() - timeLastSetSelection >= HIDE_DELAY_HANDLE) {
                    editor.invalidate();
                }
            }

        }
        editor.postDelayedInLifecycle(new InvalidateNotifier(), HIDE_DELAY_HANDLE);
    }

    /**
     * Called by editor
     * Whether this class is handling motions by user
     *
     * @return Whether handling
     */
    public boolean handlingMotions() {
        return holdHorizontalScrollBar() || holdVerticalScrollBar() || holdInsertHandle() || selHandleType != -1;
    }

    /**
     * Get scroller for editor
     *
     * @return Scroller using
     */
    public EditorScroller getScroller() {
        return scroller;
    }

    /**
     * Reset scroll state
     */
    public void reset() {
        scroller.startScroll(0, 0, 0, 0, 0);
        reset2();
    }

    public void reset2() {
        holdingInsertHandle = holdingScrollbarHorizontal = holdingScrollbarVertical = false;
        dismissMagnifier();
    }

    public void updateMagnifier(MotionEvent e) {
        if (edgeFlags != 0) {
            dismissMagnifier();
            return;
        }
        if (magnifier.isEnabled()) {
            var insertHandlePos = editor.getInsertHandleDescriptor().position;
            var leftHandlePos = editor.getLeftHandleDescriptor().position;
            var rightHandlePos = editor.getRightHandleDescriptor().position;
            if (editor.isStickyTextSelection()) {
                boolean isLeftHandle = selHandleType == SelectionHandle.LEFT;
                boolean isRightHandle = selHandleType == SelectionHandle.RIGHT;

                float x = 0, y = 0;
                var height = Math.max(Math.max(insertHandlePos.height(), leftHandlePos.height()), rightHandlePos.height());
                if (holdInsertHandle()) {
                    x = Math.abs(insertHandlePos.left - e.getX()) > editor.getRowHeight() ? insertHandlePos.left : e.getX();
                    y = insertHandlePos.top;
                } else if (isLeftHandle) {
                    x = Math.abs(leftHandlePos.left - e.getX()) > editor.getRowHeight() ? leftHandlePos.left : e.getX();
                    y = leftHandlePos.top;
                } else if (isRightHandle) {
                    x = Math.abs(rightHandlePos.left - e.getX()) > editor.getRowHeight() ? rightHandlePos.left : e.getX();
                    y = rightHandlePos.top;
                }
                magnifier.show((int) x, (int) (y - height / 2));
            } else {
                var height = Math.max(Math.max(insertHandlePos.height(), leftHandlePos.height()), rightHandlePos.height());
                magnifier.show((int) e.getX(), (int) (e.getY() - height / 2 - editor.getRowHeight()));
            }
        }
    }

    public void dismissMagnifier() {
        magnifier.dismiss();
    }

    /**
     * Handle events apart from detectors
     *
     * @param e The event editor received
     * @return Whether this touch event is handled by this class
     */
    public boolean onTouchEvent(MotionEvent e) {
        if (edgeFieldSize == 0) {
            edgeFieldSize = editor.getDpUnit() * 18;
        }
        motionY = e.getY();
        motionX = e.getX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                holdingScrollbarVertical = holdingScrollbarHorizontal = false;
                RectF rect = editor.getRenderer().getVerticalScrollBarRect();
                if (RectUtils.contains(rect, e.getX(), e.getY(), editor.getDpUnit() * 10)) {
                    holdingScrollbarVertical = true;
                    thumbDownY = e.getY();
                    editor.hideAutoCompleteWindow();
                }
                rect = editor.getRenderer().getHorizontalScrollBarRect();
                if (rect.contains(e.getX(), e.getY())) {
                    holdingScrollbarHorizontal = true;
                    thumbDownX = e.getX();
                    editor.hideAutoCompleteWindow();
                }
                if (holdingScrollbarVertical && holdingScrollbarHorizontal) {
                    holdingScrollbarHorizontal = false;
                }
                if (holdingScrollbarVertical || holdingScrollbarHorizontal) {
                    editor.invalidate();
                }
                final var allowedDistance = editor.getDpUnit() * 7;
                if (shouldDrawInsertHandle() && RectUtils.almostContains(editor.getInsertHandleDescriptor().position, e.getX(), e.getY(), allowedDistance)) {
                    holdingInsertHandle = true;
                    dispatchHandle(HandleStateChangeEvent.HANDLE_TYPE_INSERT, true);
                    thumbDownY = e.getY();
                    thumbDownX = e.getX();
                }
                boolean left = RectUtils.almostContains(editor.getLeftHandleDescriptor().position, e.getX(), e.getY(), allowedDistance);
                boolean right = RectUtils.almostContains(editor.getRightHandleDescriptor().position, e.getX(), e.getY(), allowedDistance);
                if (left || right) {
                    if (left) {
                        selHandleType = SelectionHandle.LEFT;
                        touchedHandleType = SelectionHandle.LEFT;
                    } else {
                        selHandleType = SelectionHandle.RIGHT;
                        touchedHandleType = SelectionHandle.RIGHT;
                    }
                    dispatchHandle(selHandleType, true);
                    thumbDownY = e.getY();
                    thumbDownX = e.getX();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (holdingScrollbarVertical) {
                    float movedDis = e.getY() - thumbDownY;
                    thumbDownY = e.getY();
                    float all = editor.getScrollMaxY();
                    float dy = movedDis / (editor.getHeight() - editor.getRenderer().getVerticalScrollBarRect().height()) * all;
                    scrollBy(0, dy);
                    return true;
                }
                if (holdingScrollbarHorizontal) {
                    float movedDis = e.getX() - thumbDownX;
                    thumbDownX = e.getX();
                    float all = editor.getScrollMaxX() + editor.getWidth();
                    float dx;
                    if (editor.getRenderer().getHorizontalScrollBarRect().width() <= 60 * editor.getDpUnit()) {
                        dx = movedDis / (editor.getWidth() - editor.getRenderer().getHorizontalScrollBarRect().width()) * all;
                    } else {
                        dx = movedDis / editor.getWidth() * all;
                    }
                    scrollBy(dx, 0);
                    return true;
                }
                if (handleSelectionChange(e)) {
                    if (magnifier.isShowing() || Math.sqrt((e.getX() - thumbDownX) * (e.getX() - thumbDownX) +
                            (e.getY() - thumbDownY) * (e.getY() - thumbDownY)) >= MAGNIFIER_TOUCH_SLOP) {
                        updateMagnifier(e);
                    }
                    if (touchedHandleType != -1 || holdInsertHandle()) {
                        editor.invalidate();
                    }
                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (holdingScrollbarVertical) {
                    holdingScrollbarVertical = false;
                    editor.invalidate();
                    timeLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if (holdingScrollbarHorizontal) {
                    holdingScrollbarHorizontal = false;
                    editor.invalidate();
                    timeLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if (holdingInsertHandle) {
                    holdingInsertHandle = false;
                    editor.invalidate();
                    notifyLater();
                    dispatchHandle(HandleStateChangeEvent.HANDLE_TYPE_INSERT, false);
                }
                if (selHandleType != -1) {
                    dispatchHandle(selHandleType, false);
                    selHandleType = -1;
                }
                editor.invalidate();
                // check touch event is related to text selection or not
                if (touchedHandleType > -1) {
                    touchedHandleType = -1;
                }
                stopEdgeScroll();
                dismissMagnifier();
                break;
        }
        return false;
    }

    private void dispatchHandle(int type, boolean held) {
        editor.dispatchEvent(new HandleStateChangeEvent(editor, type, held));
    }

    private boolean handleSelectionChange(MotionEvent e) {
        if (holdingInsertHandle) {
            insertHandle.applyPosition(e);
            scrollIfThumbReachesEdge(e);
            return true;
        }
        switch (selHandleType) {
            case SelectionHandle.LEFT:
                editor.selectionAnchor = editor.getCursor().right();
                this.leftHandle.applyPosition(e);
                scrollIfThumbReachesEdge(e);
                return true;
            case SelectionHandle.RIGHT:
                editor.selectionAnchor = editor.getCursor().left();
                this.rightHandle.applyPosition(e);
                scrollIfThumbReachesEdge(e);
                return true;
        }
        return false;
    }

    private void handleSelectionChange2(MotionEvent e) {
        if (holdingInsertHandle) {
            insertHandle.applyPosition(e);
        } else {
            switch (selHandleType) {
                case SelectionHandle.LEFT:
                    this.leftHandle.applyPosition(e);
                    break;
                case SelectionHandle.RIGHT:
                    this.rightHandle.applyPosition(e);
                    break;
            }
        }
    }

    private int computeEdgeFlags(float x, float y) {
        int flags = 0;
        if (x < edgeFieldSize) {
            flags |= LEFT_EDGE;
        }
        if (y < edgeFieldSize) {
            flags |= TOP_EDGE;
        }
        if (x > editor.getWidth() - edgeFieldSize) {
            flags |= RIGHT_EDGE;
        }
        if (y > editor.getHeight() - edgeFieldSize) {
            flags |= BOTTOM_EDGE;
        }
        return flags;
    }

    public void scrollIfThumbReachesEdge(MotionEvent e) {
        int flag = computeEdgeFlags(e.getX(), e.getY());
        int initialDelta = (int) (8 * editor.getDpUnit());
        if (flag != 0 && edgeFlags == 0) {
            edgeFlags = flag;
            thumbMotionRecord = MotionEvent.obtain(e);
            editor.postInLifecycle(new EdgeScrollRunnable(initialDelta));
        } else if (flag == 0) {
            stopEdgeScroll();
        } else {
            edgeFlags = flag;
            thumbMotionRecord = MotionEvent.obtain(e);
        }
    }

    private boolean isSameSign(int a, int b) {
        return (a < 0 && b < 0) || (a > 0 && b > 0);
    }

    public void stopEdgeScroll() {
        edgeFlags = 0;
    }

    public void scrollBy(float distanceX, float distanceY) {
        scrollBy(distanceX, distanceY, false);
    }

    public void scrollBy(float distanceX, float distanceY, boolean smooth) {
        editor.hideAutoCompleteWindow();
        int endX = scroller.getCurrX() + (int) distanceX;
        int endY = scroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, editor.getScrollMaxY());
        endX = Math.min(endX, editor.getScrollMaxX());
        editor.dispatchEvent(new ScrollEvent(editor, scroller.getCurrX(),
                scroller.getCurrY(), endX, endY, ScrollEvent.CAUSE_USER_DRAG));
        if (smooth) {
            scroller.startScroll(scroller.getCurrX(),
                    scroller.getCurrY(),
                    endX - scroller.getCurrX(),
                    endY - scroller.getCurrY());
        } else {
            scroller.startScroll(scroller.getCurrX(),
                    scroller.getCurrY(),
                    endX - scroller.getCurrX(),
                    endY - scroller.getCurrY(), 0);
            scroller.abortAnimation();
        }
        editor.invalidate();
    }

    public int getTouchedHandleType() {
        return touchedHandleType;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        scroller.forceFinished(true);
        if (editor.isFormatting()) {
            return true;
        }
        var resolved = RegionResolverKt.resolveTouchRegion(editor, e);
        var region = IntPair.getFirst(resolved);
        long res = editor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        editor.performClick();
        if (region == RegionResolverKt.REGION_SIDE_ICON) {
            int row = (int) (e.getY() + editor.getOffsetX()) / editor.getRowHeight();
            row = Math.max(0, Math.min(row, editor.getLayout().getRowCount() - 1));
            var inf = editor.getLayout().getRowAt(row);
            if (inf.isLeadingRow) {
                var style = editor.getRenderer().getLineStyle(inf.lineIndex, LineSideIcon.class);
                if (style != null) {
                    if ((editor.dispatchEvent(new SideIconClickEvent(editor, style)) & InterceptTarget.TARGET_EDITOR) != 0) {
                        editor.hideAutoCompleteWindow();
                        return true;
                    }
                }
            }
        }
        if ((editor.dispatchEvent(new ClickEvent(editor, editor.getText().getIndexer().getCharPosition(line, column), e)) & InterceptTarget.TARGET_EDITOR) != 0) {
            return true;
        }
        editor.showSoftInput();
        notifyLater();
        var lnAction = editor.getProps().actionWhenLineNumberClicked;
        if (region == RegionResolverKt.REGION_TEXT) {
            editor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP);
        } else if (region == RegionResolverKt.REGION_LINE_NUMBER) {
            switch (lnAction) {
                case DirectAccessProps.LN_ACTION_SELECT_LINE:
                    editor.setSelectionRegion(line, 0, line, editor.getText().getColumnCount(line), false, SelectionChangeEvent.CAUSE_TAP);
                    break;
                case DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME:
                    editor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP);
                    break;
                case DirectAccessProps.LN_ACTION_NOTHING:
                default:
                    // do nothing
            }
        }
        editor.hideAutoCompleteWindow();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (editor.isFormatting()) {
            return;
        }
        long res = editor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        if ((editor.dispatchEvent(new LongPressEvent(editor, editor.getText().getIndexer().getCharPosition(line, column), e)) & InterceptTarget.TARGET_EDITOR) != 0) {
            return;
        }
        if ((!editor.getProps().reselectOnLongPress && editor.getCursor().isSelected()) || e.getPointerCount() != 1) {
            return;
        }
        editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        editor.selectWord(line, column);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        int endX = scroller.getCurrX() + (int) distanceX;
        int endY = scroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, editor.getScrollMaxY());
        endX = Math.min(endX, editor.getScrollMaxX());
        boolean notifyY = true;
        boolean notifyX = true;
        if (!editor.getVerticalEdgeEffect().isFinished()) {
            float displacement = Math.max(0, Math.min(1, e2.getX() / editor.getWidth()));
            float distance = (glowTopOrBottom ? distanceY : -distanceY) / editor.getMeasuredHeight();
            if (distance > 0) {
                endY = scroller.getCurrY();
                editor.getVerticalEdgeEffect().onPull(distance, !glowTopOrBottom ? displacement : 1 - displacement);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var edgeEffect = editor.getVerticalEdgeEffect();
                edgeEffect.onPullDistance(distance, !glowTopOrBottom ? displacement : 1 - displacement);
                if (edgeEffect.getDistance() != 0) {
                    endY = scroller.getCurrY();
                }
            } else {
                editor.getVerticalEdgeEffect().finish();
            }
            notifyY = false;
        }
        if (!editor.getHorizontalEdgeEffect().isFinished()) {
            float displacement = Math.max(0, Math.min(1, e2.getY() / editor.getHeight()));
            float distance = (glowLeftOrRight ? distanceX : -distanceX) / editor.getMeasuredWidth();
            if (distance > 0) {
                endX = scroller.getCurrX();
                editor.getHorizontalEdgeEffect().onPull(distance, !glowLeftOrRight ? 1 - displacement : displacement);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var edgeEffect = editor.getHorizontalEdgeEffect();
                edgeEffect.onPullDistance(distance, !glowLeftOrRight ? 1 - displacement : displacement);
                if (edgeEffect.getDistance() != 0) {
                    endX = scroller.getCurrX();
                }
            } else {
                editor.getHorizontalEdgeEffect().finish();
            }
            notifyX = false;
        }
        scroller.startScroll(scroller.getCurrX(),
                scroller.getCurrY(),
                endX - scroller.getCurrX(),
                endY - scroller.getCurrY(), 0);
        editor.updateCompletionWindowPosition(false);
        final float minOverPull = 2f;
        if (notifyY && scroller.getCurrY() + distanceY < -minOverPull) {
            editor.getVerticalEdgeEffect().onPull(-distanceY / editor.getMeasuredHeight(), Math.max(0, Math.min(1, e2.getX() / editor.getWidth())));
            glowTopOrBottom = false;
        }
        if (notifyY && scroller.getCurrY() + distanceY > editor.getScrollMaxY() + minOverPull) {
            editor.getVerticalEdgeEffect().onPull(distanceY / editor.getMeasuredHeight(), Math.max(0, Math.min(1, e2.getX() / editor.getWidth())));
            glowTopOrBottom = true;
        }
        if (notifyX && scroller.getCurrX() + distanceX < -minOverPull) {
            editor.getHorizontalEdgeEffect().onPull(-distanceX / editor.getMeasuredWidth(), Math.max(0, Math.min(1, e2.getY() / editor.getHeight())));
            glowLeftOrRight = false;
        }
        if (notifyX && scroller.getCurrX() + distanceX > editor.getScrollMaxX() + minOverPull) {
            editor.getHorizontalEdgeEffect().onPull(distanceX / editor.getMeasuredWidth(), Math.max(0, Math.min(1, e2.getY() / editor.getHeight())));
            glowLeftOrRight = true;
        }
        editor.invalidate();
        editor.dispatchEvent(new ScrollEvent(editor, scroller.getCurrX(),
                scroller.getCurrY(), endX, endY, ScrollEvent.CAUSE_USER_DRAG));
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!editor.getProps().scrollFling) {
            return false;
        }
        // If we do not finish it here, it can produce a high speed and cause the final scroll range to be broken, even a NaN for velocity
        scroller.forceFinished(true);
        scroller.fling(scroller.getCurrX(),
                scroller.getCurrY(),
                (int) -velocityX,
                (int) -velocityY,
                0,
                editor.getScrollMaxX(),
                0,
                editor.getScrollMaxY(),
                editor.getProps().overScrollEnabled && !editor.isWordwrap() ? (int) (20 * editor.getDpUnit()) : 0,
                editor.getProps().overScrollEnabled ? (int) (20 * editor.getDpUnit()) : 0);
        float minVe = editor.getDpUnit() * 2000;
        if (Math.abs(velocityX) >= minVe || Math.abs(velocityY) >= minVe) {
            notifyScrolled();
            editor.hideAutoCompleteWindow();
        }
        editor.releaseEdgeEffects();
        editor.dispatchEvent(new ScrollEvent(editor, scroller.getCurrX(),
                scroller.getCurrY(), scroller.getFinalX(), scroller.getFinalY(), ScrollEvent.CAUSE_USER_FLING));
        editor.postInvalidateOnAnimation();
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (editor.isFormatting()) {
            return true;
        }
        if (editor.isScalable()) {
            float newSize = editor.getTextSizePx() * detector.getScaleFactor();
            if (newSize < scaleMinSize || newSize > scaleMaxSize) {
                return true;
            }
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            int originHeight = editor.getRowHeight();
            editor.setTextSizePxDirect(newSize);
            float heightFactor = editor.getRowHeight() * 1f / originHeight;
            float afterScrollY = (scroller.getCurrY() + focusY) * heightFactor - focusY;
            float afterScrollX = (scroller.getCurrX() + focusX) * detector.getScaleFactor() - focusX;
            afterScrollX = Math.max(0, Math.min(afterScrollX, editor.getScrollMaxX()));
            afterScrollY = Math.max(0, Math.min(afterScrollY, editor.getScrollMaxY()));
            editor.dispatchEvent(new ScrollEvent(editor, scroller.getCurrX(),
                    scroller.getCurrY(), (int) afterScrollX, (int) afterScrollY, ScrollEvent.CAUSE_SCALE_TEXT));
            scroller.startScroll((int) afterScrollX, (int) afterScrollY, 0, 0, 0);
            scroller.abortAnimation();
            isScaling = true;
            editor.invalidate();
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        scroller.forceFinished(true);
        textSizeStart = editor.getTextSizePx();
        return editor.isScalable() && !editor.isFormatting() && !holdingInsertHandle && touchedHandleType == -1;
    }

    long memoryPosition;
    boolean positionNotApplied;
    float focusY;

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isScaling = false;
        if (textSizeStart == editor.getTextSizePx()) {
            return;
        }
        editor.getRenderer().forcedRecreateLayout = true;
        if (editor.isWordwrap()) {
            focusY = detector.getFocusY();
            memoryPosition = editor.getPointPositionOnScreen(detector.getFocusX(), detector.getFocusY());
            positionNotApplied = true;
        } else {
            positionNotApplied = false;
        }
        editor.getRenderer().invalidateRenderNodes();
        editor.getRenderer().updateTimestamp();
        editor.invalidate();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return editor.isEnabled();
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (editor.isFormatting()) {
            return true;
        }
        long res = editor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        if ((editor.dispatchEvent(new DoubleClickEvent(editor, editor.getText().getIndexer().getCharPosition(line, column), e)) & InterceptTarget.TARGET_EDITOR) != 0) {
            return true;
        }
        if (editor.getCursor().isSelected() || e.getPointerCount() != 1) {
            return true;
        }
        editor.selectWord(line, column);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return true;
    }


    /**
     * This is a helper for EventHandler to control handles
     */
    final class SelectionHandle {

        public static final int LEFT = HandleStateChangeEvent.HANDLE_TYPE_LEFT;
        public static final int RIGHT = HandleStateChangeEvent.HANDLE_TYPE_RIGHT;
        public static final int BOTH = HandleStateChangeEvent.HANDLE_TYPE_INSERT;

        public int type;

        /**
         * Create a handle
         *
         * @param type Type :left,right,both
         */
        public SelectionHandle(int type) {
            this.type = type;
        }

        private boolean checkNoIntersection(SelectionHandleStyle.HandleDescriptor one, SelectionHandleStyle.HandleDescriptor another) {
            return !RectF.intersects(one.position, another.position);
        }

        /**
         * Handle the event
         *
         * @param e Event sent by EventHandler
         */
        public void applyPosition(MotionEvent e) {
            SelectionHandleStyle.HandleDescriptor descriptor;
            switch (type) {
                case LEFT:
                    descriptor = editor.getLeftHandleDescriptor();
                    break;
                case RIGHT:
                    descriptor = editor.getRightHandleDescriptor();
                    break;
                default:
                    descriptor = editor.getInsertHandleDescriptor();
            }
            var anotherDesc = type == LEFT ? editor.getRightHandleDescriptor() : editor.getLeftHandleDescriptor();
            float targetX = scroller.getCurrX() + e.getX() + (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER ? descriptor.position.width() : 0) * (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT ? 1 : -1);
            float targetY = scroller.getCurrY() + e.getY() - descriptor.position.height();
            int line = IntPair.getFirst(editor.getPointPosition(0, targetY));
            if (line >= 0 && line < editor.getLineCount()) {
                int column = IntPair.getSecond(editor.getPointPosition(targetX, targetY));
                int lastLine = type == RIGHT ? editor.getCursor().getRightLine() : editor.getCursor().getLeftLine();
                int lastColumn = type == RIGHT ? editor.getCursor().getRightColumn() : editor.getCursor().getLeftColumn();
                int anotherLine = type != RIGHT ? editor.getCursor().getRightLine() : editor.getCursor().getLeftLine();
                int anotherColumn = type != RIGHT ? editor.getCursor().getRightColumn() : editor.getCursor().getLeftColumn();

                if ((line != lastLine || column != lastColumn) && (type == BOTH || (line != anotherLine || column != anotherColumn))) {
                    switch (type) {
                        case BOTH:
                            editor.cancelAnimation();
                            editor.setSelection(line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                            break;
                        case RIGHT:
                            if (anotherLine > line || (anotherLine == line && anotherColumn > column)) {
                                //Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandle(selHandleType, false);
                                    EditorTouchEventHandler.this.selHandleType = LEFT;
                                    dispatchHandle(selHandleType, true);
                                    this.type = LEFT;
                                    leftHandle.type = RIGHT;
                                    SelectionHandle tmp = rightHandle;
                                    rightHandle = leftHandle;
                                    leftHandle = tmp;
                                    editor.setSelectionRegion(line, column, anotherLine, anotherColumn, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                                }
                            } else {
                                editor.setSelectionRegion(anotherLine, anotherColumn, line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                            }
                            break;
                        case LEFT:
                            if (anotherLine < line || (anotherLine == line && anotherColumn < column)) {
                                //Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandle(selHandleType, false);
                                    EditorTouchEventHandler.this.selHandleType = RIGHT;
                                    dispatchHandle(selHandleType, true);
                                    this.type = RIGHT;
                                    rightHandle.type = LEFT;
                                    SelectionHandle tmp = rightHandle;
                                    rightHandle = leftHandle;
                                    leftHandle = tmp;
                                    editor.setSelectionRegion(anotherLine, anotherColumn, line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                                }
                            } else {
                                editor.setSelectionRegion(line, column, anotherLine, anotherColumn, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                            }
                            break;
                    }
                }
            }
        }

    }

    /**
     * Runnable for controlling auto-scrolling when thumb reaches the edges of editor
     */
    private class EdgeScrollRunnable implements Runnable {
        private final static int MAX_FACTOR = 32;
        private final static float INCREASE_FACTOR = 1.06f;

        private final int initialDelta;
        private int deltaHorizontal;
        private int deltaVertical;
        private int lastDx, lastDy;
        private int factorX, factorY;
        private long postTimes;

        public EdgeScrollRunnable(int initDelta) {
            initialDelta = deltaHorizontal = deltaVertical = initDelta;
            postTimes = 0;
        }

        @Override
        public void run() {
            int dx = (((edgeFlags & LEFT_EDGE) != 0) ? -deltaHorizontal : 0) + (((edgeFlags & RIGHT_EDGE) != 0) ? deltaHorizontal : 0);
            int dy = (((edgeFlags & TOP_EDGE) != 0) ? -deltaVertical : 0) + (((edgeFlags & BOTTOM_EDGE) != 0) ? deltaVertical : 0);
            if (dx > 0) {
                // Check whether there is content at right
                int line;
                if (holdingInsertHandle || selHandleType == SelectionHandle.LEFT) {
                    line = editor.getCursor().getLeftLine();
                } else {
                    line = editor.getCursor().getRightLine();
                }
                int column = editor.getText().getColumnCount(line);
                // Do not scroll too far from text region of this line
                float maxOffset = editor.measureTextRegionOffset() + editor.layout.getCharLayoutOffset(line, column)[1] - editor.getWidth() * 0.85f;
                if (scroller.getCurrX() > maxOffset) {
                    dx = 0;
                }
            }
            scrollBy(dx, dy);
            if (magnifier.isShowing()) {
                magnifier.dismiss();
            }

            // Speed up if we are scrolling in the direction
            if (isSameSign(dx, lastDx)) {
                if (factorX < MAX_FACTOR && (postTimes & 1) == 0) {
                    factorX++;
                    deltaHorizontal *= INCREASE_FACTOR;
                }
            } else {
                // Recover initial speed because direction changed
                deltaHorizontal = initialDelta;
                factorX = 0;
            }
            if (isSameSign(dy, lastDy)) {
                if (factorY < MAX_FACTOR && (postTimes & 1) == 0) {
                    factorY++;
                    deltaVertical *= INCREASE_FACTOR;
                }
            } else {
                deltaVertical = initialDelta;
                factorY = 0;
            }
            lastDx = dx;
            lastDy = dy;

            // Update selection
            handleSelectionChange2(thumbMotionRecord);

            postTimes++;
            // Post for animation
            if (edgeFlags != 0) {
                editor.postDelayedInLifecycle(this, 10);
            }
        }
    }
}

