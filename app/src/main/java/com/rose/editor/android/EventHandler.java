/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.android;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;
import android.graphics.RectF;
import android.util.TypedValue;
import android.content.res.Resources;

/**
 * Handles touch event of editor
 * @author Rose
 */
final class EventHandler implements GestureDetector.OnGestureListener,GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {
    private CodeEditor mEditor;
    private OverScroller mScroller;
    private long mLastScroll = 0;
    private long mLastSetSelection = 0;
    private boolean mHolding = false;
    private boolean mHolding2 = false;
    private boolean mHolding3 = false;
    private float downY = 0;
    private float downX = 0;
    private float offsetX,offsetY;
    private SelectionHandle insert = null,left = null,right = null;
    private int type = -1;

    private final static int HIDE_DELAY = 3000;
    private final static int HIDE_DELAY_HANDLE = 5000;

    private float maxSize,minSize;

    /**
     * Create a event handler for the given editor
     * @param editor Host editor
     */
    public EventHandler(CodeEditor editor){
        mEditor = editor;
        mScroller = new OverScroller(editor.getContext());
        maxSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,32,Resources.getSystem().getDisplayMetrics());
        minSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,5,Resources.getSystem().getDisplayMetrics());
    }

    /**
     * Whether we should draw scroll bars
     * @return whether draw scroll bars
     */
    public boolean shouldDrawScrollBar() {
        return System.currentTimeMillis() - mLastScroll < HIDE_DELAY || mHolding || mHolding2;
    }

    /**
     * Hide the insert handle at once
     */
    public void hideInsertHandle() {
        if(!shouldDrawInsertHandle()){
            return;
        }
        mLastSetSelection = 0;
        mEditor.invalidate();
    }

    /**
     * Whether the vertical scroll bar is touched
     * @return Whether touched
     */
    public boolean holdVerticalScrollBar() {
        return mHolding;
    }

    /**
     * Whether the horizontal scroll bar is touched
     * @return Whether touched
     */
    public boolean holdHorizontalScrollBar() {
        return mHolding2;
    }

    /**
     * Whether insert handle is touched
     * @return Whether touched
     */
    public boolean holdInsertHandle() {
        return mHolding3;
    }

    /**
     * Whether the editor should draw insert handler
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
        mEditor.postDelayed(new Runnable(){
            @Override
            public void run()
            {
                if(System.currentTimeMillis() - mLastScroll >= HIDE_DELAY_HANDLE) {
                    mEditor.invalidate();
                }
            }
        },HIDE_DELAY_HANDLE + 10);
    }

    /**
     * Notify the editor later to hide insert handle
     */
    public void notifyLater() {
        mLastSetSelection = System.currentTimeMillis();
        mEditor.postDelayed(new Runnable(){
            @Override
            public void run()
            {
                if(System.currentTimeMillis() - mLastSetSelection >= HIDE_DELAY) {
                    mEditor.invalidate();
                }
            }
        },HIDE_DELAY + 10);
    }

    /**
     * Called by editor
     * Whether this class is handling motions by user
     * @return Whether handling
     */
    protected boolean handlingMotions() {
        return holdHorizontalScrollBar() || holdVerticalScrollBar() || holdInsertHandle() || type != -1;
    }

    /**
     * Get scroller for editor
     * @return Scroller using
     */
    protected OverScroller getScroller(){
        return mScroller;
    }

    /**
     * Reset scroll state
     */
    protected void reset(){
        mScroller.startScroll(0,0,0,0,0);
    }

    /**
     * Handle events apart from detectors
     * @param e The event editor received
     * @return Whether this touch event is handled by this class
     */
    public boolean onTouchEvent(MotionEvent e){
        switch(e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mHolding = mHolding2 = false;
                RectF rect = mEditor.getVerticalScrollBarRect();
                if(rect.contains(e.getX(),e.getY())){
                    mHolding = true;
                    downY = e.getY();
                    mEditor.hideAutoCompletePanel();
                }
                rect = mEditor.getHorizontalScrollBarRect();
                if(rect.contains(e.getX(),e.getY())) {
                    mHolding2 = true;
                    downX = e.getX();
                    mEditor.hideAutoCompletePanel();
                }
                if(mHolding && mHolding2) {
                    mHolding2 = false;
                }
                if(mHolding || mHolding2){
                    mEditor.invalidate();
                }
                if(shouldDrawInsertHandle() && mEditor.getInsertHandleRect().contains(e.getX(),e.getY())){
                    mHolding3 = true;
                    downY = e.getY();
                    downX = e.getX();
                    offsetX = mScroller.getCurrX() + downX;
                    offsetY = mScroller.getCurrY() + downY;
                    float startX = mScroller.getCurrX() + mEditor.getInsertHandleRect().centerX() - mEditor.measurePrefix();
                    float startY = mScroller.getCurrY() + mEditor.getInsertHandleRect().top - mEditor.getLineHeight() / 5f;

                    insert = new SelectionHandle(SelectionHandle.BOTH,startX,startY);
                }
                boolean left = mEditor.getLeftHandleRect().contains(e.getX(),e.getY());
                boolean right = mEditor.getRightHandleRect().contains(e.getX(),e.getY());
                if(left || right) {
                    if(left) {
                        type = SelectionHandle.LEFT;
                    }else{
                        type = SelectionHandle.RIGHT;
                    }
                    downY = e.getY();
                    downX = e.getX();
                    offsetX = mScroller.getCurrX() + downX;
                    offsetY = mScroller.getCurrY() + downY;
                    float startX = mScroller.getCurrX() + mEditor.getLeftHandleRect().centerX() - mEditor.measurePrefix();
                    float startY = mScroller.getCurrY() + mEditor.getLeftHandleRect().top - mEditor.getLineHeight() / 5f;
                    this.left = new SelectionHandle(SelectionHandle.LEFT,startX,startY);
                    startX = mScroller.getCurrX() + mEditor.getRightHandleRect().centerX() - mEditor.measurePrefix();
                    startY = mScroller.getCurrY() + mEditor.getRightHandleRect().top - mEditor.getLineHeight() / 5f;
                    this.right = new SelectionHandle(SelectionHandle.RIGHT,startX,startY);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if(mHolding) {
                    float movedDis = e.getY() - downY;
                    downY = e.getY();
                    float all = mEditor.getLineHeight() * mEditor.getLineCount() + mEditor.getHeight() / 2f;
                    float dy = movedDis / mEditor.getHeight() * all;
                    onScroll(null,null,0,dy);
                    return true;
                }
                if(mHolding2) {
                    float movedDis = e.getX() - downX;
                    downX = e.getX();
                    float all = mEditor.getScrollMaxX() + mEditor.getWidth();
                    float dx = movedDis / mEditor.getWidth() * all;
                    onScroll(null,null,dx,0);
                    return true;
                }
                if(mHolding3) {
                    insert.applyPosition(e);
                    return true;
                }
                switch(type) {
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
                if(mHolding) {
                    mHolding = false;
                    mEditor.invalidate();
                    mLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if(mHolding2) {
                    mHolding2 = false;
                    mEditor.invalidate();
                    mLastScroll = System.currentTimeMillis();
                    notifyScrolled();
                }
                if(mHolding3) {
                    mHolding3 = false;
                    mEditor.invalidate();
                    notifyLater();
                }
                type = -1;
                break;
        }
        return false;
    }

    /**
     * Scroll the view smoothly
     * @param deltaY The delta y
     */
    private void smoothScrollBy(float deltaY) {
        float finalY = mScroller.getCurrY() + deltaY;
        if(finalY < 0) {
            finalY = 0;
        }else if(finalY > mEditor.getScrollMaxY()) {
            finalY = mEditor.getScrollMaxY();
        }
        mScroller.startScroll(mScroller.getCurrX(),mScroller.getCurrY(),0,(int)(finalY - mScroller.getCurrY()));
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mEditor.showSoftInput();
        mEditor.getInputMethodManager().viewClicked(mEditor);
        mScroller.forceFinished(true);
        int line = mEditor.getPointLineOnScreen(e.getY());
        int column = mEditor.getPointColumnOnScreen(line,e.getX());
        if(mEditor.getCursor().isSelected() && mEditor.getCursor().isInSelectedRegion(line,column) && !(mEditor.isOverMaxY(e.getY()) || mEditor.isOverMaxX(line,e.getX()))) {
            TextComposePanel panel = mEditor.getTextActionPanel();
            if(panel.isShowing()) {
                panel.hide();
            }else{
                int first = mEditor.getFirstVisibleLine();
                int last = mEditor.getLastVisibleLine();
                int left = mEditor.getCursor().getLeftLine();
                int right = mEditor.getCursor().getRightLine();
                int toLineBottom;
                if(right <= first) {
                    toLineBottom = first;
                }else if(right > last){
                    if(left <= first) {
                        toLineBottom = (first + last) / 2;
                    }else if(left >= last) {
                        toLineBottom = last - 2;
                    }else{
                        if(left + 3 >= last) {
                            toLineBottom = left - 2;
                        }else{
                            toLineBottom = left + 1;
                        }
                    }
                }else{
                    if(left <= first) {
                        if(right+ 3 >= last) {
                            toLineBottom = right - 2;
                        }else{
                            toLineBottom = right + 1;
                        }
                    }else{
                        if(left + 5 >= right) {
                            toLineBottom = right + 1;
                        }else{
                            toLineBottom = (left + right) / 2;
                        }
                    }
                }
                toLineBottom = Math.max(0,toLineBottom);
                int panelY = mEditor.getLineBottom(toLineBottom) - mEditor.getOffsetY();
                float handleLeftX = mEditor.getOffset(left,mEditor.getCursor().getLeftColumn());
                float handleRightX = mEditor.getOffset(right,mEditor.getCursor().getRightColumn());
                int panelX  = (int) ((handleLeftX + handleRightX) / 2f) ;
                panel.setExtendedX(panelX);
                panel.setExtendedY(panelY);
                panel.show();
            }
        }else{
            notifyLater();
            mEditor.setSelection(line,column);
            mEditor.contentChanged();
            mEditor.hideAutoCompletePanel();
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if(mEditor.getCursor().isSelected() || e.getPointerCount() != 1) {
            return;
        }
        int line = mEditor.getPointLineOnScreen(e.getY());
        int column = mEditor.getPointColumnOnScreen(line,e.getX());
        //Find word edges
        int startLine = line,endLine = line;
        int startColumn = column;
        while(startColumn > 0 && isIdentifierPart(mEditor.getText().charAt(line,startColumn - 1))) {
            startColumn--;
        }
        int maxColumn = mEditor.getText().getColumnCount(line);
        int endColumn = column;
        while(endColumn < maxColumn && isIdentifierPart(mEditor.getText().charAt(line,endColumn))) {
            endColumn++;
        }
        if(startColumn == endColumn) {
            if(startColumn > 0) {
                startColumn--;
            }else if(endColumn < maxColumn) {
                endColumn++;
            }else{
                if(line > 0) {
                    int lastColumn = mEditor.getText().getColumnCount(line - 1);
                    startLine = line - 1;
                    startColumn = lastColumn;
                }else if(line < mEditor.getLineCount() - 1) {
                    endLine = line + 1;
                    endColumn = 0;
                }
            }
        }
        mEditor.setSelectionRegion(startLine,startColumn,endLine,endColumn);
        mEditor.contentChanged();
    }

    /**
     * Whether this character is a part of word
     * @param ch Character to check
     * @return Whether a part of word
     */
    private static boolean isIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mEditor.getTextActionPanel().hide();
        int endX = mScroller.getCurrX() + (int)distanceX;
        int endY = mScroller.getCurrY() + (int)distanceY;
        endX = Math.max(endX,0);
        endY = Math.max(endY,0);
        endY = Math.min(endY,mEditor.getScrollMaxY());
        endX = Math.min(endX,mEditor.getScrollMaxX());
        mScroller.startScroll(mScroller.getCurrX(),
                mScroller.getCurrY(),
                endX - mScroller.getCurrX(),
                endY - mScroller.getCurrY(),0);
        mEditor.invalidate();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mScroller.fling(mScroller.getCurrX(),
                mScroller.getCurrY(),
                (int)-velocityX,
                (int)-velocityY,
                0,
                mEditor.getScrollMaxX(),
                0,
                mEditor.getScrollMaxY(),
                20,
                20);
        mEditor.invalidate();
        float minVe = mEditor.getDpUnit() * 2000;
        if(Math.abs(velocityX) >= minVe || Math.abs(velocityY) >= minVe) {
            notifyScrolled();
            mEditor.hideAutoCompletePanel();
        }
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if(mEditor.canScale()) {
            float newSize = mEditor.getTextSizePx() * detector.getScaleFactor();
            if(newSize < minSize || newSize > maxSize) {
                return false;
            }
            int firstVisible = mEditor.getFirstVisibleLine();
			float top = mScroller.getCurrY() - firstVisible * mEditor.getLineHeight();
			int height = mEditor.getLineHeight();
            mEditor.setTextSizePx(newSize);
			float newY = firstVisible * mEditor.getLineHeight() + top * mEditor.getLineHeight() / height;
			mScroller.startScroll(mScroller.getCurrX(),(int)newY,0,0,0);
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
    public void onScaleEnd(ScaleGestureDetector detector) { }

    @Override
    public boolean onDown(MotionEvent e) { return mEditor.isEnabled(); }

    @Override
    public void onShowPress(MotionEvent e) { }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) { return true; }

    @Override
    public boolean onDoubleTap(MotionEvent e) { 
        onLongPress(e);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) { return true; }

    /**
     * This is a helper for EventHandler to control handles
     */
    private class SelectionHandle {

        public static final int LEFT = 0;
        public static final int RIGHT = 1;
        public static final int BOTH = 2;

        public int type;
        public float startX;
        public float startY;

        /**
         * Create a handle
         * @param type Type :left,right,both
         * @param sx Offset x
         * @param sy Offset y
         */
        public SelectionHandle(int type,float sx,float sy) {
            this.type = type;
            startX = sx;
            startY = sy;
        }

        /**
         * Handle the event
         * @param e Event sent by EventHandler
         */
        public void applyPosition(MotionEvent e) {
            float currX = mScroller.getCurrX() + e.getX();
            float currY = mScroller.getCurrY() + e.getY();
            float targetX = (currX - offsetX) + startX;
            float targetY = (currY - offsetY) + startY;
            int line = mEditor.getPointLine(targetY);
            if(line >= mEditor.getLastVisibleLine()) {
                smoothScrollBy(mEditor.getLineHeight() * 8);
            }
            if(line <= mEditor.getFirstVisibleLine()) {
                smoothScrollBy(-mEditor.getLineHeight() * 8);
            }
            line = mEditor.getPointLine(targetY);
            if(line >= 0 && line < mEditor.getLineCount()) {
                int column = mEditor.getPointColumn(line,targetX);
                int lastLine = type == RIGHT ? mEditor.getCursor().getRightLine() : mEditor.getCursor().getLeftLine();
                int lastColumn = type == RIGHT ? mEditor.getCursor().getLeftColumn() : mEditor.getCursor().getLeftColumn();
                int anotherLine = type != RIGHT ? mEditor.getCursor().getRightLine() : mEditor.getCursor().getLeftLine();
                int anotherColumn = type != RIGHT ? mEditor.getCursor().getRightColumn() : mEditor.getCursor().getLeftColumn();

                if(line != lastLine || column != lastColumn) {
                    switch(type) {
                        case BOTH:
                            mEditor.cancelAnimation();
                            mEditor.setSelection(line,column);
                            break;
                        case RIGHT:
                            if(anotherLine > line || (anotherLine == line && anotherColumn > column)) {
                                //Swap type
                                EventHandler.this.type = LEFT;
                                this.type = LEFT;
                                left.type = RIGHT;
                                SelectionHandle tmp = right;
                                right = left;
                                left = tmp;
                                mEditor.setSelectionRegion(line,column,anotherLine,anotherColumn,false);
                            }else{
                                mEditor.setSelectionRegion(anotherLine,anotherColumn,line,column,false);
                            }
                            break;
                        case LEFT:
                            if(anotherLine < line || (anotherLine == line && anotherColumn < column)) {
                                //Swap type
                                EventHandler.this.type = RIGHT;
                                this.type = RIGHT;
                                right.type = LEFT;
                                SelectionHandle tmp = right;
                                right = left;
                                left = tmp;
                                mEditor.setSelectionRegion(anotherLine,anotherColumn,line,column,false);
                            }else{
                                mEditor.setSelectionRegion(line,column,anotherLine,anotherColumn,false);
                            }
                            break;
                    }
                }
            }
            mEditor.getTextActionPanel().hide();
        }

    }
}

