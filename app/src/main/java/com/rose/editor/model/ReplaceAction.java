package com.rose.editor.model;

import com.rose.editor.common.Content;
import com.rose.editor.interfaces.ContentAction;
import com.rose.editor.model.DeleteAction;
import com.rose.editor.model.InsertAction;

/**
 * Replace action model for UndoManager
 * @author Rose
 */
public final class ReplaceAction implements ContentAction {

    public InsertAction _insert;
    public DeleteAction _delete;

    @Override
    public void undo(Content content) {
        _insert.undo(content);
        _delete.undo(content);
    }

    @Override
    public void redo(Content content) {
        _delete.redo(content);
        _insert.redo(content);
    }

    @Override
    public boolean canMerge(ContentAction action) {
        return false;
    }

    @Override
    public void merge(ContentAction action) {
        throw new UnsupportedOperationException();
    }

}

