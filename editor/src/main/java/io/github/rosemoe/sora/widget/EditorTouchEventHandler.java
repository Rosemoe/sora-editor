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

import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Build;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;

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
    private final static int HIDE_DELAY_HANDLE = 5000;

    private final static int LEFT_EDGE = 1;
    private final static int RIGHT_EDGE = 1 << 1;
    private final static int TOP_EDGE = 1 << 2;
    private final static int BOTTOM_EDGE = 1 << 3;

    private final CodeEditor mEditor;
    private final OverScroller mScroller;
    private final SelectionHandle mInsertHandle;
    Magnifier mMagnifier;
    int selHandleType = -1;
    float motionX;
    float motionY;
    boolean glowTopOrBottom; //true for bottom
    boolean glowLeftOrRight; //true for right
    boolean isScaling = false;
    float scaleMaxSize;
    float scaleMinSize;
    private float textSizeStart;
    private long mLastScroll = 0;
    private long mLastSetSelection = 0;
    private boolean mHoldingScrollbarVertical = false;
    private boolean mHoldingScrollbarHorizontal = false;
    private boolean mHoldingInsertHandle = false;
    private float mThumbDownY = 0;
    private float mThumbDownX = 0;
    private SelectionHandle mLeftHandle;
    private SelectionHandle mRightHandle;
    private int mTouchedHandleType = -1;
    private float mEdgeFieldSize;
    private int mEdgeFlags;
    private MotionEvent mThumbRecord;

    /**
     * Create an event handler for the given editor
     *
     * @param editor Host editor
     */
    public EditorTouchEventHandler(CodeEditor editor) {
        mEditor = editor;
        mScroller = new OverScroller(editor.getContext());
        scaleMaxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 26, Resources.getSystem().getDisplayMetrics());
        scaleMinSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, Resources.getSystem().getDisplayMetrics());
        mMagnifier = new Magnifier(editor);
        this.mLeftHandle = new SelectionHandle(SelectionHandle.LEFT);
        this.mRightHandle = new SelectionHandle(SelectionHandle.RIGHT);
        mInsertHandle = new SelectionHandle(SelectionHandle.BOTH);
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
        return System.currentTimeMillis() - mLastScroll < HIDE_DELAY || mHoldingScrollbarVertical || mHoldingScrollbarHorizontal;
    }

    /**
     * Hide the insert handle at once
     */
    public void hideInsertHandle() {
        if (!shouldDrawInsertHandle()) {
            return;
        }
        mLastSetSelection = 0;
        mEditor.invalidate();
    }

    /**
     * Whether the vertical scroll bar is touched
     *
     * @return Whether touched
     */
    public boolean holdVerticalScrollBar() {
        return mHoldingScrollbarVertical;
    }

    /**
     * Whether the horizontal scroll bar is touched
     *
     * @return Whether touched
     */
    public boolean holdHorizontalScrollBar() {
        return mHoldingScrollbarHorizontal;
    }

    /**
     * Whether insert handle is touched
     *
     * @return Whether touched
     */
    public boolean holdInsertHandle() {
        return mHoldingInsertHandle;
    }

    /**
     * Whether the editor should draw insert handler
     *
     * @return Whether to draw
     */
    public boolean shouldDrawInsertHandle() {
        return (System.currentTimeMillis() - mLastSetSelection < HIDE_DELAY || mHoldingInsertHandle);
    }

    /**
     * Notify the editor later to hide scroll bars
     */
    public void notifyScrolled() {
        mLastScroll = System.currentTimeMillis();
        class ScrollNotifier implements Runnable {

            @Override
            public void run() {
                if (System.currentTimeMillis() - mLastScroll >= HIDE_DELAY_HANDLE) {
                    mEditor.invalidate();
                }
            }

        }
        mEditor.postDelayed(new ScrollNotifier(), HIDE_DELAY_HANDLE);
    }

    /**
     * Notify the editor later to hide insert handle
     */
    public void notifyLater() {
        mLastSetSelection = System.currentTimeMillis();
        class InvalidateNotifier implements Runnable {

            @Override
            public void run() {
                if (System.currentTimeMillis() - mLastSetSelection >= HIDE_DELAY) {
                    mEditor.invalidate();
                }
            }

        }
        mEditor.postDelayed(new InvalidateNotifier(), HIDE_DELAY);
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
    public OverScroller getScroller() {
        return mScroller;
    }

    /**
     * Reset scroll state
     */
    public void reset() {
        mScroller.startScroll(0, 0, 0, 0, 0);
        reset2();
    }

    public void reset2() {
        mHoldingInsertHandle = mHoldingScrollbarHorizontal = mHoldingScrollbarVertical = false;
        dismissMagnifier();
    }

    public void updateMagnifier(MotionEvent e) {
        if (mEdgeFlags != 0) {
            dismissMagnifier();
            return;
        }
        if (mMagnifier.isEnabled()) {
            var insertHandlePos = mEditor.getInsertHandleDescriptor().position;
            var leftHandlePos = mEditor.getLeftHandleDescriptor().position;
            var rightHandlePos = mEditor.getRightHandleDescriptor().position;
            if (mEditor.isStickyTextSelection()) {
                boolean isLeftHandle = selHandleType == SelectionHandle.LEFT;
                boolean isRightHandle = selHandleType == SelectionHandle.RIGHT;

                float x = 0, y = 0;
                var height = Math.max(Math.max(insertHandlePos.height(), leftHandlePos.height()), rightHandlePos.height());
                if (holdInsertHandle()) {
                    x = Math.abs(insertHandlePos.left - e.getX()) > mEditor.getRowHeight() ? insertHandlePos.left : e.getX();
                    y = insertHandlePos.top;
                } else if (isLeftHandle) {
                    x = Math.abs(leftHandlePos.left - e.getX()) > mEditor.getRowHeight() ? leftHandlePos.left : e.getX();
                    y = leftHandlePos.top;
                } else if (isRightHandle) {
                    x = Math.abs(rightHandlePos.left - e.getX()) > mEditor.getRowHeight() ? rightHandlePos.left : e.getX();
                    y = rightHandlePos.top;
                }
                mMagnifier.show((int) x, (int) (y - height / 2));
            } else {
                var height = Math.max(Math.max(insertHandlePos.height(), leftHandlePos.height()), rightHandlePos.height());
                mMagnifier.show((int) e.getX(), (int) (e.getY() - height / 2 - mEditor.getRowHeight()));
            }
        }
    }

    public void dismissMagnifier() {
        mMagnifier.dismiss();
    }

    /**
     * Handle events apart from detectors
     *
     * @param e The event editor received
     * @return Whether this touch event is handled by this class
     */
    public boolean onTouchEvent(MotionEvent e) {
        if (mEdgeFieldSize == 0) {
            mEdgeFieldSize = mEditor.getDpUnit() * 18;
        }
        motionY = e.getY();
        motionX = e.getX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mHoldingScrollbarVertical = mHoldingScrollbarHorizontal = false;
                RectF rect = mEditor.getRenderer().getVerticalScrollBarRect();
                if (RectUtils.contains(rect, e.getX(), e.getY(), mEditor.getDpUnit() * 10)) {
                    mHoldingScrollbarVertical = true;
                    mThumbDownY = e.getY();
                    mEditor.hideAutoCompleteWindow();
                }
                rect = mEditor.getRenderer().getHorizontalScrollBarRect();
                if (rect.contains(e.getX(), e.getY())) {
                    mHoldingScrollbarHorizontal = true;
                    mThumbDownX = e.getX();
                    mEditor.hideAutoCompleteWindow();
                }
                if (mHoldingScrollbarVertical && mHoldingScrollbarHorizontal) {
                    mHoldingScrollbarHorizontal = false;
                }
                if (mHoldingScrollbarVertical || mHoldingScrollbarHorizontal) {
                    mEditor.invalidate();
                }
                if (shouldDrawInsertHandle() && mEditor.getInsertHandleDescriptor().position.contains(e.getX(), e.getY())) {
                    mHoldingInsertHandle = true;
                    dispatchHandle(HandleStateChangeEvent.HANDLE_TYPE_INSERT, true);
                    updateMagnifier(e);
                    mThumbDownY = e.getY();
                    mThumbDownX = e.getX();
                }
                boolean left = mEditor.getLeftHandleDescriptor().position.contains(e.getX(), e.getY());
                boolean right = mEditor.getRightHandleDescriptor().position.contains(e.getX(), e.getY());
                if (left || right) {
                    if (left) {
                        selHandleType = SelectionHandle.LEFT;
                        mTouchedHandleType = SelectionHandle.LEFT;
                    } else {
                        selHandleType = SelectionHandle.RIGHT;
                        mTouchedHandleType = SelectionHandle.RIGHT;
                    }
                    dispatchHandle(selHandleType, true);
                    updateMagnifier(e);
                    mThumbDownY = e.getY();
                    mThumbDownX = e.getX();
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mHoldingScrollbarVertical) {
                    float movedDis = e.getY() - mThumbDownY;
                    mThumbDownY = e.getY();
                    float all = mEditor.mLayout.getLayoutHeight() - mEditor.getHeight() / 2f;
                    float dy = movedDis / (mEditor.getHeight() - mEditor.getRenderer().getVerticalScrollBarRect().height()) * all;
                    scrollBy(0, dy);
                    return true;
                }
                if (mHoldingScrollbarHorizontal) {
                    float movedDis = e.getX() - mThumbDownX;
                    mThumbDownX = e.getX();
                    float all = mEditor.getScrollMaxX() + mEditor.getWidth();
                    float dx = movedDis / mEditor.getWidth() * all;
                    scrollBy(dx, 0);
                    return true;
                }
                if (handleSelectionChange(e)) {
                    updateMagnifier(e);
                    if (mTouchedHandleType != -1 || holdInsertHandle()) {
                        mEditor.invalidate();
                    }
                    return true;
                } else {
                    return false;
                }
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mHoldingScrollbarVertical) {
                    mHoldingScrollbarVertical = false;
                    mEditor.invalidate();
                    mLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if (mHoldingScrollbarHorizontal) {
                    mHoldingScrollbarHorizontal = false;
                    mEditor.invalidate();
                    mLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if (mHoldingInsertHandle) {
                    mHoldingInsertHandle = false;
                    mEditor.invalidate();
                    notifyLater();
                    dispatchHandle(HandleStateChangeEvent.HANDLE_TYPE_INSERT, false);
                }
                if (selHandleType != -1) {
                    dispatchHandle(selHandleType, false);
                    selHandleType = -1;
                }
                mEditor.invalidate();
                // check touch event is related to text selection or not
                if (mTouchedHandleType > -1) {
                    mTouchedHandleType = -1;
                }
                stopEdgeScroll();
                dismissMagnifier();
                break;
        }
        return false;
    }

    private void dispatchHandle(int type, boolean held) {
        mEditor.dispatchEvent(new HandleStateChangeEvent(mEditor, type, held));
    }

    private boolean handleSelectionChange(MotionEvent e) {
        if (mHoldingInsertHandle) {
            mInsertHandle.applyPosition(e);
            scrollIfThumbReachesEdge(e);
            return true;
        }
        switch (selHandleType) {
            case SelectionHandle.LEFT:
                this.mLeftHandle.applyPosition(e);
                scrollIfThumbReachesEdge(e);
                return true;
            case SelectionHandle.RIGHT:
                this.mRightHandle.applyPosition(e);
                scrollIfThumbReachesEdge(e);
                return true;
        }
        return false;
    }

    private void handleSelectionChange2(MotionEvent e) {
        if (mHoldingInsertHandle) {
            mInsertHandle.applyPosition(e);
        } else {
            switch (selHandleType) {
                case SelectionHandle.LEFT:
                    this.mLeftHandle.applyPosition(e);
                    break;
                case SelectionHandle.RIGHT:
                    this.mRightHandle.applyPosition(e);
                    break;
            }
        }
    }

    private int computeEdgeFlags(float x, float y) {
        int flags = 0;
        if (x < mEdgeFieldSize) {
            flags |= LEFT_EDGE;
        }
        if (y < mEdgeFieldSize) {
            flags |= TOP_EDGE;
        }
        if (x > mEditor.getWidth() - mEdgeFieldSize) {
            flags |= RIGHT_EDGE;
        }
        if (y > mEditor.getHeight() - mEdgeFieldSize) {
            flags |= BOTTOM_EDGE;
        }
        return flags;
    }

    public void scrollIfThumbReachesEdge(MotionEvent e) {
        int flag = computeEdgeFlags(e.getX(), e.getY());
        int initialDelta = (int) (8 * mEditor.getDpUnit());
        if (flag != 0 && mEdgeFlags == 0) {
            mEdgeFlags = flag;
            mThumbRecord = MotionEvent.obtain(e);
            mEditor.post(new EdgeScrollRunnable(initialDelta));
        } else if (flag == 0) {
            stopEdgeScroll();
        } else {
            mEdgeFlags = flag;
            mThumbRecord = MotionEvent.obtain(e);
        }
    }

    private boolean isSameSign(int a, int b) {
        return (a < 0 && b < 0) || (a > 0 && b > 0);
    }

    public void stopEdgeScroll() {
        mEdgeFlags = 0;
    }

    public void scrollBy(float distanceX, float distanceY) {
        scrollBy(distanceX, distanceY, false);
    }

    public void scrollBy(float distanceX, float distanceY, boolean smooth) {
        mEditor.hideAutoCompleteWindow();
        int endX = mScroller.getCurrX() + (int) distanceX;
        int endY = mScroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, mEditor.getScrollMaxY());
        endX = Math.min(endX, mEditor.getScrollMaxX());
        mEditor.dispatchEvent(new ScrollEvent(mEditor, mScroller.getCurrX(),
                mScroller.getCurrY(), endX, endY, ScrollEvent.CAUSE_USER_DRAG));
        if (smooth) {
            mScroller.startScroll(mScroller.getCurrX(),
                    mScroller.getCurrY(),
                    endX - mScroller.getCurrX(),
                    endY - mScroller.getCurrY());
        } else {
            mScroller.startScroll(mScroller.getCurrX(),
                    mScroller.getCurrY(),
                    endX - mScroller.getCurrX(),
                    endY - mScroller.getCurrY(), 0);
            mScroller.abortAnimation();
        }
        mEditor.invalidate();
    }

    public int getTouchedHandleType() {
        return mTouchedHandleType;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mScroller.forceFinished(true);
        if (mEditor.isFormatting()) {
            return true;
        }
        var resolved = RegionResolverKt.resolveTouchRegion(mEditor, e);
        var region = IntPair.getFirst(resolved);
        long res = mEditor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        mEditor.performClick();
        if (region == RegionResolverKt.REGION_SIDE_ICON) {
            int row = (int) (e.getY() + mEditor.getOffsetX()) / mEditor.getRowHeight();
            row = Math.max(0, Math.min(row, mEditor.getLayout().getRowCount() - 1));
            var inf = mEditor.getLayout().getRowAt(row);
            if (inf.isLeadingRow) {
                var style = mEditor.getRenderer().getLineStyle(inf.lineIndex, LineSideIcon.class);
                if (style != null) {
                    if ((mEditor.dispatchEvent(new SideIconClickEvent(mEditor, style)) & InterceptTarget.TARGET_EDITOR) != 0) {
                        mEditor.hideAutoCompleteWindow();
                        return true;
                    }
                }
            }
        }
        if ((mEditor.dispatchEvent(new ClickEvent(mEditor, mEditor.getText().getIndexer().getCharPosition(line, column), e)) & InterceptTarget.TARGET_EDITOR) != 0) {
            return true;
        }
        mEditor.showSoftInput();
        notifyLater();
        var lnAction = mEditor.getProps().actionWhenLineNumberClicked;
        System.out.println(region);
        if (region == RegionResolverKt.REGION_TEXT) {
            mEditor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP);
        } else if (region == RegionResolverKt.REGION_LINE_NUMBER) {
            switch (lnAction) {
                case DirectAccessProps.LN_ACTION_SELECT_LINE:
                    mEditor.setSelectionRegion(line, 0, line, mEditor.getText().getColumnCount(line), false, SelectionChangeEvent.CAUSE_TAP);
                    break;
                case DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME:
                    mEditor.setSelection(line, column, SelectionChangeEvent.CAUSE_TAP);
                    break;
                case DirectAccessProps.LN_ACTION_NOTHING:
                default:
                    // do nothing
            }
        }
        mEditor.hideAutoCompleteWindow();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mEditor.isFormatting()) {
            return;
        }
        long res = mEditor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        if ((mEditor.dispatchEvent(new LongPressEvent(mEditor, mEditor.getText().getIndexer().getCharPosition(line, column), e)) & InterceptTarget.TARGET_EDITOR) != 0) {
            return;
        }
        if (mEditor.getCursor().isSelected() || e.getPointerCount() != 1) {
            return;
        }
        mEditor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        mEditor.selectWord(line, column);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        int endX = mScroller.getCurrX() + (int) distanceX;
        int endY = mScroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, mEditor.getScrollMaxY());
        endX = Math.min(endX, mEditor.getScrollMaxX());
        boolean notifyY = true;
        boolean notifyX = true;
        if (!mEditor.getVerticalEdgeEffect().isFinished()) {
            float displacement = Math.max(0, Math.min(1, e2.getX() / mEditor.getWidth()));
            float distance = (glowTopOrBottom ? distanceY : -distanceY) / mEditor.getMeasuredHeight();
            if (distance > 0) {
                endY = mScroller.getCurrY();
                mEditor.getVerticalEdgeEffect().onPull(distance, !glowTopOrBottom ? displacement : 1 - displacement);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var edgeEffect = mEditor.getVerticalEdgeEffect();
                edgeEffect.onPullDistance(distance, !glowTopOrBottom ? displacement : 1 - displacement);
                if (edgeEffect.getDistance() != 0) {
                    endY = mScroller.getCurrY();
                }
            }
            notifyY = false;
        }
        if (!mEditor.getHorizontalEdgeEffect().isFinished()) {
            float displacement = Math.max(0, Math.min(1, e2.getY() / mEditor.getHeight()));
            float distance = (glowLeftOrRight ? distanceX : -distanceX) / mEditor.getMeasuredWidth();
            if (distance > 0) {
                endX = mScroller.getCurrX();
                mEditor.getHorizontalEdgeEffect().onPull(distance, !glowLeftOrRight ? 1 - displacement : displacement);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var edgeEffect = mEditor.getHorizontalEdgeEffect();
                edgeEffect.onPullDistance(distance, !glowLeftOrRight ? 1 - displacement : displacement);
                if (edgeEffect.getDistance() != 0) {
                    endX = mScroller.getCurrX();
                }
            }
            notifyX = false;
        }
        mScroller.startScroll(mScroller.getCurrX(),
                mScroller.getCurrY(),
                endX - mScroller.getCurrX(),
                endY - mScroller.getCurrY(), 0);
        mEditor.updateCompletionWindowPosition(false);
        final float minOverPull = 2f;
        if (notifyY && mScroller.getCurrY() + distanceY < -minOverPull) {
            mEditor.getVerticalEdgeEffect().onPull(-distanceY / mEditor.getMeasuredHeight(), Math.max(0, Math.min(1, e2.getX() / mEditor.getWidth())));
            glowTopOrBottom = false;
        }
        if (notifyY && mScroller.getCurrY() + distanceY > mEditor.getScrollMaxY() + minOverPull) {
            mEditor.getVerticalEdgeEffect().onPull(distanceY / mEditor.getMeasuredHeight(), Math.max(0, Math.min(1, e2.getX() / mEditor.getWidth())));
            glowTopOrBottom = true;
        }
        if (notifyX && mScroller.getCurrX() + distanceX < -minOverPull) {
            mEditor.getHorizontalEdgeEffect().onPull(-distanceX / mEditor.getMeasuredWidth(), Math.max(0, Math.min(1, e2.getY() / mEditor.getHeight())));
            glowLeftOrRight = false;
        }
        if (notifyX && mScroller.getCurrX() + distanceX > mEditor.getScrollMaxX() + minOverPull) {
            mEditor.getHorizontalEdgeEffect().onPull(distanceX / mEditor.getMeasuredWidth(), Math.max(0, Math.min(1, e2.getY() / mEditor.getHeight())));
            glowLeftOrRight = true;
        }
        mEditor.invalidate();
        mEditor.dispatchEvent(new ScrollEvent(mEditor, mScroller.getCurrX(),
                mScroller.getCurrY(), endX, endY, ScrollEvent.CAUSE_USER_DRAG));
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (!mEditor.getProps().scrollFling) {
            return false;
        }
        // If we do not finish it here, it can produce a high speed and cause the final scroll range to be broken, even a NaN for velocity
        mScroller.forceFinished(true);
        mScroller.fling(mScroller.getCurrX(),
                mScroller.getCurrY(),
                (int) -velocityX,
                (int) -velocityY,
                0,
                mEditor.getScrollMaxX(),
                0,
                mEditor.getScrollMaxY(),
                mEditor.getProps().overScrollEnabled && !mEditor.isWordwrap() ? (int) (20 * mEditor.getDpUnit()) : 0,
                mEditor.getProps().overScrollEnabled ? (int) (20 * mEditor.getDpUnit()) : 0);
        float minVe = mEditor.getDpUnit() * 2000;
        if (Math.abs(velocityX) >= minVe || Math.abs(velocityY) >= minVe) {
            notifyScrolled();
            mEditor.hideAutoCompleteWindow();
        }
        mEditor.releaseEdgeEffects();
        mEditor.dispatchEvent(new ScrollEvent(mEditor, mScroller.getCurrX(),
                mScroller.getCurrY(), mScroller.getFinalX(), mScroller.getFinalY(), ScrollEvent.CAUSE_USER_FLING));
        mEditor.postInvalidateOnAnimation();
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mEditor.isFormatting()) {
            return true;
        }
        if (mEditor.isScalable()) {
            float newSize = mEditor.getTextSizePx() * detector.getScaleFactor();
            if (newSize < scaleMinSize || newSize > scaleMaxSize) {
                return true;
            }
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            int originHeight = mEditor.getRowHeight();
            mEditor.setTextSizePxDirect(newSize);
            float heightFactor = mEditor.getRowHeight() * 1f / originHeight;
            float afterScrollY = (mScroller.getCurrY() + focusY) * heightFactor - focusY;
            float afterScrollX = (mScroller.getCurrX() + focusX) * detector.getScaleFactor() - focusX;
            afterScrollX = Math.max(0, Math.min(afterScrollX, mEditor.getScrollMaxX()));
            afterScrollY = Math.max(0, Math.min(afterScrollY, mEditor.getScrollMaxY()));
            mEditor.dispatchEvent(new ScrollEvent(mEditor, mScroller.getCurrX(),
                    mScroller.getCurrY(), (int) afterScrollX, (int) afterScrollY, ScrollEvent.CAUSE_SCALE_TEXT));
            mScroller.startScroll((int) afterScrollX, (int) afterScrollY, 0, 0, 0);
            isScaling = true;
            mEditor.invalidate();
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mScroller.forceFinished(true);
        textSizeStart = mEditor.getTextSizePx();
        return mEditor.isScalable() && !mEditor.isFormatting();
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isScaling = false;
        if (textSizeStart == mEditor.getTextSizePx()) {
            return;
        }
        mEditor.getRenderer().updateTimestamp();
        mEditor.createLayout();
        mEditor.invalidate();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return mEditor.isEnabled();
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
        if (mEditor.isFormatting()) {
            return true;
        }
        long res = mEditor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        if ((mEditor.dispatchEvent(new DoubleClickEvent(mEditor, mEditor.getText().getIndexer().getCharPosition(line, column), e)) & InterceptTarget.TARGET_EDITOR) != 0) {
            return true;
        }
        if (mEditor.getCursor().isSelected() || e.getPointerCount() != 1) {
            return true;
        }
        mEditor.selectWord(line, column);
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
                    descriptor = mEditor.getLeftHandleDescriptor();
                    break;
                case RIGHT:
                    descriptor = mEditor.getRightHandleDescriptor();
                    break;
                default:
                    descriptor = mEditor.getInsertHandleDescriptor();
            }
            var anotherDesc = type == LEFT ? mEditor.getRightHandleDescriptor() : mEditor.getLeftHandleDescriptor();
            float targetX = mScroller.getCurrX() + e.getX() + (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER ? descriptor.position.width() : 0) * (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT ? 1 : -1);
            float targetY = mScroller.getCurrY() + e.getY() - descriptor.position.height();
            int line = IntPair.getFirst(mEditor.getPointPosition(0, targetY));
            if (line >= 0 && line < mEditor.getLineCount()) {
                int column = IntPair.getSecond(mEditor.getPointPosition(targetX, targetY));
                int lastLine = type == RIGHT ? mEditor.getCursor().getRightLine() : mEditor.getCursor().getLeftLine();
                int lastColumn = type == RIGHT ? mEditor.getCursor().getRightColumn() : mEditor.getCursor().getLeftColumn();
                int anotherLine = type != RIGHT ? mEditor.getCursor().getRightLine() : mEditor.getCursor().getLeftLine();
                int anotherColumn = type != RIGHT ? mEditor.getCursor().getRightColumn() : mEditor.getCursor().getLeftColumn();

                if ((line != lastLine || column != lastColumn) && (type == BOTH || (line != anotherLine || column != anotherColumn))) {
                    switch (type) {
                        case BOTH:
                            mEditor.cancelAnimation();
                            mEditor.setSelection(line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                            break;
                        case RIGHT:
                            if (anotherLine > line || (anotherLine == line && anotherColumn > column)) {
                                //Swap type
                                if (checkNoIntersection(descriptor, anotherDesc)) {
                                    dispatchHandle(selHandleType, false);
                                    EditorTouchEventHandler.this.selHandleType = LEFT;
                                    dispatchHandle(selHandleType, true);
                                    this.type = LEFT;
                                    mLeftHandle.type = RIGHT;
                                    SelectionHandle tmp = mRightHandle;
                                    mRightHandle = mLeftHandle;
                                    mLeftHandle = tmp;
                                    mEditor.setSelectionRegion(line, column, anotherLine, anotherColumn, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                                }
                            } else {
                                mEditor.setSelectionRegion(anotherLine, anotherColumn, line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
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
                                    mRightHandle.type = LEFT;
                                    SelectionHandle tmp = mRightHandle;
                                    mRightHandle = mLeftHandle;
                                    mLeftHandle = tmp;
                                    mEditor.setSelectionRegion(anotherLine, anotherColumn, line, column, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
                                }
                            } else {
                                mEditor.setSelectionRegion(line, column, anotherLine, anotherColumn, false, SelectionChangeEvent.CAUSE_SELECTION_HANDLE);
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
            int dx = (((mEdgeFlags & LEFT_EDGE) != 0) ? -deltaHorizontal : 0) + (((mEdgeFlags & RIGHT_EDGE) != 0) ? deltaHorizontal : 0);
            int dy = (((mEdgeFlags & TOP_EDGE) != 0) ? -deltaVertical : 0) + (((mEdgeFlags & BOTTOM_EDGE) != 0) ? deltaVertical : 0);
            if (dx > 0) {
                // Check whether there is content at right
                int line;
                if (mHoldingInsertHandle || selHandleType == SelectionHandle.LEFT) {
                    line = mEditor.getCursor().getLeftLine();
                } else {
                    line = mEditor.getCursor().getRightLine();
                }
                int column = mEditor.getText().getColumnCount(line);
                // Do not scroll too far from text region of this line
                float maxOffset = mEditor.measureTextRegionOffset() + mEditor.mLayout.getCharLayoutOffset(line, column)[1] - mEditor.getWidth() * 0.85f;
                if (mScroller.getCurrX() > maxOffset) {
                    dx = 0;
                }
            }
            scrollBy(dx, dy);
            if (mMagnifier.isShowing()) {
                mMagnifier.dismiss();
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
            handleSelectionChange2(mThumbRecord);

            postTimes++;
            // Post for animation
            if (mEdgeFlags != 0) {
                mEditor.postDelayed(this, 10);
            }
        }
    }
}

