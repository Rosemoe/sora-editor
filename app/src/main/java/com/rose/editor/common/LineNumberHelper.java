package com.rose.editor.common;

/**
 * A line number calculator for spanner
 * @author Rose
 */
public class LineNumberHelper
{

    private CharSequence mTarget;
    private int mOffset;
    private int mLine;
    private int mColumn;
    private int mLength;

    /**
     * Create a new helper for the given text and set offset to start
     * @param target Target text
     */
    public LineNumberHelper(CharSequence target) {
        mTarget = target;
        mOffset = mLine = mColumn = 0;
        mLength = mTarget.length();
    }

    /**
     * Update line and column for the given advance
     * @param length Advance
     */
    public void update(int length) {
        for(int i = 0;i < length;i++) {
            if(mOffset + i == mLength) {
                break;
            }
            if(mTarget.charAt(mOffset + i) == '\n') {
                mLine++;
                mColumn = 0;
            }else{
                mColumn++;
            }
        }
        mOffset = mOffset + length;
    }

    /**
     * Get line start index
     * @return line start index
     */
    public int findLineStart() {
        return mOffset - mColumn;
    }

    /**
     * Get line end index
     * @return line end index
     */
    public int findLineEnd() {
        int i = 0;
        for(;i + mOffset < mLength;i++) {
            if(mTarget.charAt(mOffset + i) == '\n') {
                break;
            }
        }
        return mOffset + i;
    }

    /**
     * Get current line position
     * @return line
     */
    public int getLine() {
        return mLine;
    }

    /**
     * Get current column position
     * @return column
     */
    public int getColumn() {
        return mColumn;
    }

}

