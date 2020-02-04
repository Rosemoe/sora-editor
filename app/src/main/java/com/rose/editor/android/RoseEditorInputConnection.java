package com.rose.editor.android;

import android.view.inputmethod.BaseInputConnection;

import com.rose.editor.common.Cursor;

/**
 * Connection between input method and editor
 * @author Rose
 */
class RoseEditorInputConnection extends BaseInputConnection {

    private RoseEditor mEditor;

    private int actionCode = -1;

    private int length = -1;

    private StringBuilder commit = new StringBuilder();
    protected int composingLine = -1;
    protected int composingStart = -1;
    protected int composingEnd = -1;

    /**
     * Create a connection for the given editor
     * @param targetView Host editor
     */
    public RoseEditorInputConnection(RoseEditor targetView) {
        super(targetView, true);
        mEditor = targetView;
        finishComposingText();
    }

    /**
     * Reset the state of this connection
     */
    protected void reset(){
        commit = new StringBuilder();
        composingEnd = composingStart = composingLine = -1;
        actionCode = -1;
        length = -1;
    }

    /**
     * Private use.
     * Get the Cursor of Content displaying by Editor
     * @return Cursor
     */
    private Cursor getCursor(){
        return mEditor.getCursor();
    }

    @Override
    public void closeConnection() {
        super.closeConnection();
        mEditor.onCloseConnection();
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        if(text == null){
            return false;
        }
        if(mEditor.isEditable()){
            if(composingLine != -1){
                commit.append(text);
                return true;
            }
            getCursor().onCommitText(text);
            actionCode = 0;
            length = text.length();
        }
        return mEditor.isEditable();
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        if(mEditor.isEditable()){
            getCursor().onDeleteKeyPressed();
            actionCode = 1;
        }
        return mEditor.isEditable();
    }

    @Override
    public boolean beginBatchEdit() {
        return mEditor.getText().beginBatchEdit();
    }

    @Override
    public boolean endBatchEdit() {
        return mEditor.getText().endBatchEdit();
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition)
    {
        if(!mEditor.isEditable()){
            return false;
        }
        if(text == null){
            finishComposingText();
            return true;
        }
        if(composingLine == -1){
            commit = new StringBuilder();
            if(getCursor().isSelected())
                getCursor().onDeleteKeyPressed();
            composingLine = mEditor.getCursor().getRightLine();
            composingStart = mEditor.getCursor().getRightColumn();
            composingEnd = composingStart + text.length();
            getCursor().onCommitText(text);
        }else{
            getCursor().setLeft(composingLine,composingStart);
            getCursor().setRight(composingLine,composingEnd);
            composingEnd = composingStart + text.length();
            getCursor().onCommitText(text);
        }
        return true;
    }

    @Override
    public boolean finishComposingText(){
        composingStart = -1;
        composingEnd = -1;
        composingLine = -1;
        if(commit.length() != 0){
            getCursor().onCommitText(commit);
            commit = new StringBuilder();
        }
        return true;
    }

    @Override
    public boolean setSelection(int start, int end) {
        if(actionCode == 0 && length == 2){
            Cursor cur = getCursor();
            cur.setLeft(cur.getLeftLine(),cur.getLeftColumn() - 1);
            return true;
        }
        return false;
    }
}
