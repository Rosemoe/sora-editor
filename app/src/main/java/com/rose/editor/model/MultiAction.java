package com.rose.editor.model;


import java.util.ArrayList;
import java.util.List;

import com.rose.editor.common.Content;
import com.rose.editor.interfaces.ContentAction;

/**
 * MultiAction saves several actions for UndoManager
 * @author Rose
 */
public final class MultiAction implements ContentAction{

    private List<ContentAction> _actions = new ArrayList<>();

    public void addAction(ContentAction action) {
        if(_actions.isEmpty()) {
            _actions.add(action);
        }else {
            ContentAction last = _actions.get(_actions.size() - 1);
            if(last.canMerge(action)) {
                last.merge(action);
            }else {
                _actions.add(action);
            }
        }
    }

    @Override
    public void undo(Content content) {
        for(int i = _actions.size() - 1;i >= 0;i--) {
            _actions.get(i).undo(content);
        }
    }

    @Override
    public void redo(Content content) {
        for(int i = 0;i < _actions.size();i++) {
            _actions.get(i).redo(content);
        }
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
