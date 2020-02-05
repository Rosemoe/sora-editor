package com.rose.editor.simpleclass;


import com.rose.editor.common.Content;
import com.rose.editor.interfaces.ContentAction;

/**
 * Delete action model for UndoManager
 * @author Rose
 */
public final class DeleteAction implements ContentAction {

    public int startLine,endLine,startColumn,endColumn;

    public CharSequence text;

    @Override
    public void undo(Content content) {
        content.insert(startLine, startColumn, text);
    }

    @Override
    public void redo(Content content) {
        content.delete(startLine, startColumn, endLine, endColumn);
    }

    @Override
    public boolean canMerge(ContentAction action) {
        if(action instanceof DeleteAction) {
            DeleteAction ac = (DeleteAction)action;
            return (ac.endColumn == startColumn && ac.endLine == startLine);
        }
        return false;
    }

    @Override
    public void merge(ContentAction action) {
        if(!canMerge(action)) {
            throw new IllegalArgumentException();
        }
        DeleteAction ac = (DeleteAction)action;
        this.startColumn = ac.startColumn;
        this.startLine = ac.startLine;
        StringBuilder sb;
        if(text instanceof StringBuilder) {
            sb = (StringBuilder) text;
        }else {
            sb = new StringBuilder(text);
            text = sb;
        }
        sb.insert(0, ac.text);
    }

    @Override
    public String toString()
    {
        return "{DeleteAction : Start = " + startLine + "," + startColumn +
                " End = " + endLine + "," + endColumn + "\nContent = " + text +"}";
    }

}

