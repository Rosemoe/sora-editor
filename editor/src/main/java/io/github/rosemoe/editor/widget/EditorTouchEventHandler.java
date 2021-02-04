/*
 *   Copyright 2020-2021 Rosemoe
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
package io.github.rosemoe.editor.widget;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;
import android.graphics.RectF;
import android.util.TypedValue;
import android.content.res.Resources;

import io.github.rosemoe.editor.util.IntPair;

/**
 * Handles touch event of editor
 *
 * @author Rose
 */
@SuppressWarnings("CanBeFinal")
final class EditorTouchEventHandler implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {

    private final CodeEditor mEditor;
    private final OverScroller mScroller;
    private long mLastScroll = 0;
    private long mLastSetSelection = 0;
    private boolean mHolding = false;
    private boolean mHolding2 = false;
    private boolean mHolding3 = false;
    private float downY = 0;
    private float downX = 0;
    private float offsetX, offsetY;
    private SelectionHandle insert = null, left = null, right = null;
    private int type = -1;
    boolean isScaling = false;

    private final static int HIDE_DELAY = 3000;
    private final static int HIDE_DELAY_HANDLE = 5000;

    private float maxSize, minSize;

    /**
     * Create a event handler for the given editor
     *
     * @param editor Host editor
     */
    public EditorTouchEventHandler(CodeEditor editor) {
        mEditor = editor;
        mScroller = new OverScroller(editor.getContext());
        maxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32, Resources.getSystem().getDisplayMetrics());
        minSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 6, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * Whether we should draw scroll bars
     *
     * @return whether draw scroll bars
     */
    public boolean shouldDrawScrollBar() {
        return System.currentTimeMillis() - mLastScroll < HIDE_DELAY || mHolding || mHolding2;
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
        return mHolding;
    }

    /**
     * Whether the horizontal scroll bar is touched
     *
     * @return Whether touched
     */
    public boolean holdHorizontalScrollBar() {
        return mHolding2;
    }

    /**
     * Whether insert handle is touched
     *
     * @return Whether touched
     */
    public boolean holdInsertHandle() {
        return mHolding3;
    }

    /**
     * Whether the editor should draw insert handler
     *
     * @return Whether to draw
     */
    public boolean shouldDrawInsertHandle() {
        return System.currentTimeMillis() - mLastSetSelection < HIDE_DELAY || mHolding3;
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
    protected boolean handlingMotions() {
        return holdHorizontalScrollBar() || holdVerticalScrollBar() || holdInsertHandle() || type != -1;
    }

    /**
     * Get scroller for editor
     *
     * @return Scroller using
     */
    protected OverScroller getScroller() {
        return mScroller;
    }

    /**
     * Reset scroll state
     */
    protected void reset() {
        mScroller.startScroll(0, 0, 0, 0, 0);
    }

    /**
     * Handle events apart from detectors
     *
     * @param e The event editor received
     * @return Whether this touch event is handled by this class
     */
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mHolding = mHolding2 = false;
                RectF rect = mEditor.getVerticalScrollBarRect();
                if (rect.contains(e.getX(), e.getY())) {
                    mHolding = true;
                    downY = e.getY();
                    mEditor.hideAutoCompleteWindow();
                }
                rect = mEditor.getHorizontalScrollBarRect();
                if (rect.contains(e.getX(), e.getY())) {
                    mHolding2 = true;
                    downX = e.getX();
                    mEditor.hideAutoCompleteWindow();
                }
                if (mHolding && mHolding2) {
                    mHolding2 = false;
                }
                if (mHolding || mHolding2) {
                    mEditor.invalidate();
                }
                if (shouldDrawInsertHandle() && mEditor.getInsertHandleRect().contains(e.getX(), e.getY())) {
                    mHolding3 = true;
                    downY = e.getY();
                    downX = e.getX();
                    offsetX = mScroller.getCurrX() + downX;
                    offsetY = mScroller.getCurrY() + downY;
                    float startX = mScroller.getCurrX() + mEditor.getInsertHandleRect().centerX();
                    float startY = mScroller.getCurrY() + mEditor.getInsertHandleRect().top - mEditor.getRowHeight() / 5f;

                    insert = new SelectionHandle(SelectionHandle.BOTH, startX, startY);
                }
                boolean left = mEditor.getLeftHandleRect().contains(e.getX(), e.getY());
                boolean right = mEditor.getRightHandleRect().contains(e.getX(), e.getY());
                if (left || right) {
                    if (left) {
                        type = SelectionHandle.LEFT;
                    } else {
                        type = SelectionHandle.RIGHT;
                    }
                    downY = e.getY();
                    downX = e.getX();
                    offsetX = mScroller.getCurrX() + downX;
                    offsetY = mScroller.getCurrY() + downY;
                    float startX = mScroller.getCurrX() + mEditor.getLeftHandleRect().centerX();
                    float startY = mScroller.getCurrY() + mEditor.getLeftHandleRect().top - mEditor.getRowHeight() / 5f;
                    this.left = new SelectionHandle(SelectionHandle.LEFT, startX, startY);
                    startX = mScroller.getCurrX() + mEditor.getRightHandleRect().centerX();
                    startY = mScroller.getCurrY() + mEditor.getRightHandleRect().top - mEditor.getRowHeight() / 5f;
                    this.right = new SelectionHandle(SelectionHandle.RIGHT, startX, startY);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mHolding) {
                    float movedDis = e.getY() - downY;
                    downY = e.getY();
                    float all = mEditor.mLayout.getLayoutHeight() + mEditor.getHeight() / 2f;
                    float dy = movedDis / mEditor.getHeight() * all;
                    scrollBy(0, dy);
                    return true;
                }
                if (mHolding2) {
                    float movedDis = e.getX() - downX;
                    downX = e.getX();
                    float all = mEditor.getScrollMaxX() + mEditor.getWidth();
                    float dx = movedDis / mEditor.getWidth() * all;
                    scrollBy(dx, 0);
                    return true;
                }
                if (mHolding3) {
                    insert.applyPosition(e);
                    return true;
                }
                switch (type) {
                    case SelectionHandle.LEFT:
                        this.left.applyPosition(e);
                        return true;
                    case SelectionHandle.RIGHT:
                        this.right.applyPosition(e);
                        return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mHolding) {
                    mHolding = false;
                    mEditor.invalidate();
                    mLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if (mHolding2) {
                    mHolding2 = false;
                    mEditor.invalidate();
                    mLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if (mHolding3) {
                    mHolding3 = false;
                    mEditor.invalidate();
                    notifyLater();
                }
                type = -1;
                break;
        }
        return false;
    }
    
    protected void smoothScrollBy(float distanceX, float distanceY) {
        mEditor.getTextActionPresenter().onUpdate();
        mEditor.hideAutoCompleteWindow();
        int endX = mScroller.getCurrX() + (int) distanceX;
        int endY = mScroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, mEditor.getScrollMaxY());
        endX = Math.min(endX, mEditor.getScrollMaxX());
        mScroller.startScroll(mScroller.getCurrX(),
                              mScroller.getCurrY(),
                              endX - mScroller.getCurrX(),
                              endY - mScroller.getCurrY(), 0);
        mEditor.invalidate();
    }
    
    protected void scrollBy(float distanceX, float distanceY) {
        if (mEditor.getTextActionPresenter() != null) {
            mEditor.getTextActionPresenter().onUpdate();
        }
        mEditor.hideAutoCompleteWindow();
        int endX = mScroller.getCurrX() + (int) distanceX;
        int endY = mScroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, mEditor.getScrollMaxY());
        endX = Math.min(endX, mEditor.getScrollMaxX());
        mScroller.startScroll(mScroller.getCurrX(),
                mScroller.getCurrY(),
                endX - mScroller.getCurrX(),
                endY - mScroller.getCurrY(), 0);
        mEditor.invalidate();
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mEditor.showSoftInput();
        mEditor.getInputMethodManager().viewClicked(mEditor);
        mScroller.forceFinished(true);
        long res = mEditor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        if (mEditor.getCursor().isSelected() && mEditor.getCursor().isInSelectedRegion(line, column) && !mEditor.isOverMaxY(e.getY())) {
            mEditor.getTextActionPresenter().onSelectedTextClicked(e);
        } else {
            notifyLater();
            mEditor.setSelection(line, column);
            mEditor.hideAutoCompleteWindow();
        }
        mEditor.performClick();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mEditor.getCursor().isSelected() || e.getPointerCount() != 1) {
            return;
        }
        long res = mEditor.getPointPositionOnScreen(e.getX(), e.getY());
        int line = IntPair.getFirst(res);
        int column = IntPair.getSecond(res);
        //Find word edges
        int startLine = line, endLine = line;
        int startColumn = column;
        while (startColumn > 0 && isIdentifierPart(mEditor.getText().charAt(line, startColumn - 1))) {
            startColumn--;
        }
        int maxColumn = mEditor.getText().getColumnCount(line);
        int endColumn = column;
        while (endColumn < maxColumn && isIdentifierPart(mEditor.getText().charAt(line, endColumn))) {
            endColumn++;
        }
        if (startColumn == endColumn) {
            if (startColumn > 0) {
                startColumn--;
            } else if (endColumn < maxColumn) {
                endColumn++;
            } else {
                if (line > 0) {
                    int lastColumn = mEditor.getText().getColumnCount(line - 1);
                    startLine = line - 1;
                    startColumn = lastColumn;
                } else if (line < mEditor.getLineCount() - 1) {
                    endLine = line + 1;
                    endColumn = 0;
                }
            }
        }
        mEditor.setSelectionRegion(startLine, startColumn, endLine, endColumn);
    }

    /**
     * Whether this character is a part of word
     *
     * @param ch Character to check
     * @return Whether a part of word
     */
    private static boolean isIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }

    protected boolean topOrBottom; //true for bottom
    protected boolean leftOrRight; //true for right

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mEditor.getTextActionPresenter().onUpdate();
        int endX = mScroller.getCurrX() + (int) distanceX;
        int endY = mScroller.getCurrY() + (int) distanceY;
        endX = Math.max(endX, 0);
        endY = Math.max(endY, 0);
        endY = Math.min(endY, mEditor.getScrollMaxY());
        endX = Math.min(endX, mEditor.getScrollMaxX());
        boolean notifyY = true;
        boolean notifyX = true;
        if (!mEditor.getVerticalEdgeEffect().isFinished() && !mEditor.getVerticalEdgeEffect().isRecede()) {
            endY = mScroller.getCurrY();
            float displacement = Math.max(0, Math.min(1, e2.getX() / mEditor.getWidth()));
            mEditor.getVerticalEdgeEffect().onPull((topOrBottom ? distanceY : -distanceY) / mEditor.getMeasuredHeight(), !topOrBottom ? displacement : 1 - displacement);
            notifyY = false;
        }
        if (!mEditor.getHorizontalEdgeEffect().isFinished() && !mEditor.getHorizontalEdgeEffect().isRecede()) {
            endX = mScroller.getCurrX();
            float displacement = Math.max(0, Math.min(1, e2.getY() / mEditor.getHeight()));
            mEditor.getHorizontalEdgeEffect().onPull((leftOrRight ? distanceX : -distanceX) / mEditor.getMeasuredWidth(), !leftOrRight ? 1 - displacement : displacement);
            notifyX = false;
        }
        mScroller.startScroll(mScroller.getCurrX(),
                mScroller.getCurrY(),
                endX - mScroller.getCurrX(),
                endY - mScroller.getCurrY(), 0);
        final float minOverPull = 0;
        if (notifyY && mScroller.getCurrY() + distanceY <= -minOverPull) {
            mEditor.getVerticalEdgeEffect().onPull(-distanceY / mEditor.getMeasuredHeight(), Math.max(0, Math.min(1, e2.getX() / mEditor.getWidth())));
            topOrBottom = false;
        }
        if (notifyY && mScroller.getCurrY() + distanceY >= mEditor.getScrollMaxY() + minOverPull) {
            mEditor.getVerticalEdgeEffect().onPull(distanceY / mEditor.getMeasuredHeight(), Math.max(0, Math.min(1, e2.getX() / mEditor.getWidth())));
            topOrBottom = true;
        }
        if (notifyX && mScroller.getCurrX() + distanceX <= -minOverPull) {
            mEditor.getHorizontalEdgeEffect().onPull(-distanceX / mEditor.getMeasuredWidth(), Math.max(0, Math.min(1, e2.getY() / mEditor.getHeight())));
            leftOrRight = false;
        }
        if (notifyX && mScroller.getCurrX() + distanceX >= mEditor.getScrollMaxX() + minOverPull) {
            mEditor.getHorizontalEdgeEffect().onPull(distanceX / mEditor.getMeasuredWidth(), Math.max(0, Math.min(1, e2.getY() / mEditor.getHeight())));
            leftOrRight = true;
        }
        mEditor.invalidate();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mEditor.isDrag()) {
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
                mEditor.isOverScrollEnabled() && !mEditor.isWordwrap() ? (int) (20 * mEditor.getDpUnit()) : 0,
                mEditor.isOverScrollEnabled() ? (int) (20 * mEditor.getDpUnit()) : 0);
        mEditor.invalidate();
        float minVe = mEditor.getDpUnit() * 2000;
        if (Math.abs(velocityX) >= minVe || Math.abs(velocityY) >= minVe) {
            notifyScrolled();
            mEditor.hideAutoCompleteWindow();
        }
        if (Math.abs(velocityX) >= minVe / 2f) {
            mEditor.getHorizontalEdgeEffect().finish();
        }
        if (Math.abs(velocityY) >= minVe) {
            mEditor.getVerticalEdgeEffect().finish();
        }
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mEditor.canScale()) {
            float newSize = mEditor.getTextSizePx() * detector.getScaleFactor();
            if (newSize < minSize || newSize > maxSize) {
                return false;
            }
            int firstVisible = mEditor.getFirstVisibleRow();
            float top = mScroller.getCurrY() - firstVisible * mEditor.getRowHeight();
            int height = mEditor.getRowHeight();
            mEditor.setTextSizePxDirect(newSize);
            mEditor.invalidate();
            float newY = firstVisible * mEditor.getRowHeight() + top * mEditor.getRowHeight() / height;
            mScroller.startScroll(mScroller.getCurrX(), (int) newY, 0, 0, 0);
            isScaling = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mScroller.forceFinished(true);
        return mEditor.canScale();
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isScaling = false;
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
        onLongPress(e);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return true;
    }

    /**
     * This is a helper for EventHandler to control handles
     */
    @SuppressWarnings("CanBeFinal")
    private class SelectionHandle {

        public static final int LEFT = 0;
        public static final int RIGHT = 1;
        public static final int BOTH = 2;

        public int type;
        public float startX;
        public float startY;

        /**
         * Create a handle
         *
         * @param type Type :left,right,both
         * @param sx   Offset x
         * @param sy   Offset y
         */
        public SelectionHandle(int type, float sx, float sy) {
            this.type = type;
            startX = sx;
            startY = sy;
        }

        /**
         * Handle the event
         *
         * @param e Event sent by EventHandler
         */
        public void applyPosition(MotionEvent e) {
            float currX = mScroller.getCurrX() + e.getX();
            float currY = mScroller.getCurrY() + e.getY();
            float targetX = (currX - offsetX) + startX;
            float targetY = (currY - offsetY) + startY;
            int row = (int)(targetY / mEditor.getRowHeight());
            if (row >= mEditor.getLastVisibleRow()) {
                scrollBy(0, mEditor.getRowHeight());
            }
            if (row < mEditor.getFirstVisibleRow()) {
                scrollBy(0, -mEditor.getRowHeight());
            }
            int line = IntPair.getFirst(mEditor.getPointPosition(0, targetY));
            if (line >= 0 && line < mEditor.getLineCount()) {
                int column = IntPair.getSecond(mEditor.getPointPosition(targetX, targetY));
                int lastLine = type == RIGHT ? mEditor.getCursor().getRightLine() : mEditor.getCursor().getLeftLine();
                int lastColumn = type == RIGHT ? mEditor.getCursor().getLeftColumn() : mEditor.getCursor().getLeftColumn();
                int anotherLine = type != RIGHT ? mEditor.getCursor().getRightLine() : mEditor.getCursor().getLeftLine();
                int anotherColumn = type != RIGHT ? mEditor.getCursor().getRightColumn() : mEditor.getCursor().getLeftColumn();

                if (line != lastLine || column != lastColumn) {
                    switch (type) {
                        case BOTH:
                            mEditor.cancelAnimation();
                            mEditor.setSelection(line, column);
                            break;
                        case RIGHT:
                            if (anotherLine > line || (anotherLine == line && anotherColumn > column)) {
                                //Swap type
                                EditorTouchEventHandler.this.type = LEFT;
                                this.type = LEFT;
                                left.type = RIGHT;
                                SelectionHandle tmp = right;
                                right = left;
                                left = tmp;
                                mEditor.setSelectionRegion(line, column, anotherLine, anotherColumn, false);
                            } else {
                                mEditor.setSelectionRegion(anotherLine, anotherColumn, line, column, false);
                            }
                            break;
                        case LEFT:
                            if (anotherLine < line || (anotherLine == line && anotherColumn < column)) {
                                //Swap type
                                EditorTouchEventHandler.this.type = RIGHT;
                                this.type = RIGHT;
                                right.type = LEFT;
                                SelectionHandle tmp = right;
                                right = left;
                                left = tmp;
                                mEditor.setSelectionRegion(anotherLine, anotherColumn, line, column, false);
                            } else {
                                mEditor.setSelectionRegion(line, column, anotherLine, anotherColumn, false);
                            }
                            break;
                    }
                }
            }
            mEditor.getTextActionPresenter().onUpdate();
        }

    }
}

