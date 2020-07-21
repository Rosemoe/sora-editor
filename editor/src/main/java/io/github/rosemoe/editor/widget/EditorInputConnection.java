/*
 *   Copyright 2020 Rosemoe
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

import android.text.NoCopySpan;
import android.text.Spanned;
import android.util.Log;
import android.view.inputmethod.BaseInputConnection;

import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.text.Cursor;

import android.text.Editable;
import android.text.TextUtils;
import io.github.rosemoe.editor.struct.CharPosition;

import android.text.SpannableStringBuilder;
import android.os.Bundle;

/**
 * Connection between input method and editor
 * @author Rose
 */
class EditorInputConnection extends BaseInputConnection {

    private final static String LOG_TAG = "EditorInputConnection";

    private final CodeEditor mEditor;
    protected int mComposingLine = -1;
    protected int mComposingStart = -1;
    protected int mComposingEnd = -1;
    private boolean mInvalid;

    /**
     * Create a connection for the given editor
     * @param targetView Host editor
     */
    public EditorInputConnection(CodeEditor targetView) {
        super(targetView, true);
        mEditor = targetView;
        mInvalid = false;
    }

    protected void invalid() {
        mInvalid = true;
        mComposingEnd = mComposingStart = mComposingLine = -1;
        mEditor.invalidate();
    }

    /**
     * Reset the state of this connection
     */
    protected void reset() {
        mComposingEnd = mComposingStart = mComposingLine = -1;
        mInvalid = false;
    }

    /**
     * Private use.
     * Get the Cursor of Content displaying by Editor
     * @return Cursor
     */
    private Cursor getCursor() {
        return mEditor.getCursor();
    }

    @Override
    public Editable getEditable() {
        // This action is not supported by editor
        // We handle all the requests by ourselves
        return null;
    }

    @Override
    public synchronized void closeConnection() {
        super.closeConnection();
        Content content = mEditor.getText();
        while(content.isInBatchEdit()) {
            content.endBatchEdit();
        }
        mComposingLine = mComposingEnd = mComposingStart = -1;
        mEditor.onCloseConnection();
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        return TextUtils.getCapsMode(mEditor.getText(), getCursor().getLeft(), reqModes);
    }

    private CharSequence getTextRegionInternal(int start, int end, int flags) {
        Content origin = mEditor.getText();
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        if (start < 0) {
            start = 0;
        }
        if (end >= origin.length()) {
            end = origin.length() - 1;
        }
        if (end < start) {
            start = end = 0;
        }
        Content sub = (Content)origin.subSequence(start, end);
        if (flags == GET_TEXT_WITH_STYLES) {
            sub.beginStreamCharGetting(0);
            SpannableStringBuilder text = new SpannableStringBuilder(sub);
            if(mComposingLine != -1) {
                text.setSpan(new ComposingText(), getCursor().getIndexer().getCharIndex(mComposingLine, mComposingStart), getCursor().getIndexer().getCharIndex(mComposingLine, mComposingEnd), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return text;
        }
        return sub.toStringBuilder();
    }

    private CharSequence getTextRegion(int start, int end, int flags) {
        try {
            return getTextRegionInternal(start, end, flags);
        } catch (IndexOutOfBoundsException e) {
            Log.w(LOG_TAG, "Failed to get text region for IME", e);
            return "";
        }
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        //This text should be limited because when the user try to select all text
        //it can be quite large text and costs time, which will finally cause ANR
        int left = getCursor().getLeft();
        int right = getCursor().getRight();
        if(right - left > 1000) {
            right = left + 1000;
        }
        return left == right ? null : getTextRegion(left, right, flags);
    }

    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        int start = getCursor().getLeft();
        return getTextRegion(start - length, start, flags);
    }

    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        int end = getCursor().getRight();
        return getTextRegion(end, end + length, flags);
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if (!mEditor.isEditable() || mInvalid) {
            return false;
        }
        // NOTE: Text styles are ignored by editor
        //Remove composing text first if there is
        deleteComposingText();
        // newCursorPosition ignored
        // Call this can make auto indent and delete text selected automatically
        getCursor().onCommitText(text);
        return true;
    }

    private void deleteComposingText() {
        if (mComposingLine == -1) {
            return;
        }
        try {
            mEditor.getText().delete(mComposingLine, mComposingStart, mComposingLine, mComposingEnd);
        } catch(IndexOutOfBoundsException e){
            e.printStackTrace();
        }
        mComposingLine = mComposingStart = mComposingEnd = -1;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        if (!mEditor.isEditable() || mInvalid) {
            return false;
        }
        if(beforeLength == afterLength && beforeLength == 0) {
            getCursor().onDeleteKeyPressed();
            return true;
        }
        if(beforeLength < 0 || afterLength < 0) {
            return false;
        }
        beginBatchEdit();
        int rangeEnd = getCursor().getLeft();
        int rangeStart = rangeEnd - beforeLength;
        if (rangeStart < 0) {
            rangeStart = 0;
        }
        mEditor.getText().delete(rangeStart, rangeEnd);
        rangeStart = getCursor().getRight();
        rangeEnd = rangeStart + afterLength;
        if (rangeEnd > mEditor.getText().length()) {
            rangeEnd = mEditor.getText().length();
        }
        mEditor.getText().delete(rangeStart, rangeEnd);
        endBatchEdit();
        return true;
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        // Unsupported operation
        // According to document, we should return false
        return false;
    }

    @Override
    public synchronized boolean beginBatchEdit() {
        return mEditor.getText().beginBatchEdit();
    }

    @Override
    public synchronized boolean endBatchEdit() {
        boolean inBatch = mEditor.getText().endBatchEdit();
        if (!inBatch) {
            mEditor.updateSelection();
        }
        return inBatch;
    }

    private void deleteSelected() {
        if (getCursor().isSelected()) {
            // Delete selected text
            getCursor().onDeleteKeyPressed();
        }
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        if (!mEditor.isEditable() || mInvalid) {
            return false;
        }
        if (mComposingLine == -1) {
            // Create composing info
            deleteSelected();
            mComposingLine = getCursor().getLeftLine();
            mComposingStart = getCursor().getLeftColumn();
            mComposingEnd = mComposingStart + text.length();
            getCursor().onCommitText(text);
        } else {
            // Already have composing text
            // Delete first
            if (mComposingStart != mComposingEnd) {
                mEditor.getText().delete(mComposingLine, mComposingStart, mComposingLine, mComposingEnd);
            }
            // Reset range
            mComposingEnd = mComposingStart + text.length();
            mEditor.getText().insert(mComposingLine, mComposingStart, text);
        }
        return true;
    }

    @Override
    public boolean finishComposingText() {
        if (!mEditor.isEditable() || mInvalid) {
            return false;
        }
        mComposingLine = mComposingStart = mComposingEnd = -1;
        mEditor.invalidate();
        return true;
    }

    @Override
    public boolean setSelection(int start, int end) {
        if (!mEditor.isEditable() || mInvalid) {
            return false;
        }
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        if (start < 0) {
            start = 0;
        }
        Content content = mEditor.getText();
        if (end > content.length()) {
            end = content.length();
        }
        CharPosition startPos = content.getIndexer().getCharPosition(start);
        CharPosition endPos = content.getIndexer().getCharPosition(end);
        getCursor().setLeft(startPos.line, startPos.column);
        getCursor().setRight(endPos.line, endPos.column);
        mEditor.invalidate();
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        if (!mEditor.isEditable() || mInvalid) {
            return false;
        }
        try {
            if (start > end) {
                int tmp = start;
                start = end;
                end = tmp;
            }
            if (start < 0) {
                start = 0;
            }
            Content content = mEditor.getText();
            if (end > content.length()) {
                end = content.length();
            }
            CharPosition startPos = content.getIndexer().getCharPosition(start);
            CharPosition endPos = content.getIndexer().getCharPosition(end);
            if(startPos.line != endPos.line) {
                return false;
            }
            mComposingLine = startPos.line;
            mComposingStart = startPos.column;
            mComposingEnd = endPos.column;
            mEditor.invalidate();
        } catch (IndexOutOfBoundsException e) {
            Log.w(LOG_TAG, "set composing region for IME failed", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        return false;
    }

    @Override
    public boolean performEditorAction(int actionCode) {
        return super.performEditorAction(actionCode);
    }

    @Override
    public boolean performContextMenuAction(int id) {
        switch(id) {
            case android.R.id.selectAll:
                mEditor.selectAll();
                return true;
            case android.R.id.cut:
                mEditor.copyText();
                if(getCursor().isSelected()){
                    getCursor().onDeleteKeyPressed();
                }
                return true;
            case android.R.id.paste:
            case android.R.id.pasteAsPlainText:
                mEditor.pasteText();
                return true;
            case android.R.id.copy:
                mEditor.copyText();
                return true;
            case android.R.id.undo:
                mEditor.undo();
                return true;
            case android.R.id.redo:
                mEditor.redo();
                return true;
        }
        return false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        mEditor.updateCursorAnchor();
        return true;
    }

}

class ComposingText implements NoCopySpan {

}