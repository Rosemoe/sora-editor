package com.rose.editor.common;


import java.util.ArrayList;
import java.util.List;

import com.rose.editor.interfaces.ContentAction;
import com.rose.editor.interfaces.ContentListener;
import com.rose.editor.simpleclass.DeleteAction;
import com.rose.editor.simpleclass.InsertAction;
import com.rose.editor.simpleclass.MultiAction;
import com.rose.editor.simpleclass.ReplaceAction;

/**
 * Helper class for Content to take down modification
 * As well as provide Undo/Redo actions
 * @author Rose
 */
final class UndoManager implements ContentListener {

    private Content _c;
    private boolean _undoEnabled;
    private int _maxStackSize;
    private List<ContentAction> _undoStack;
    private InsertAction _insertAction;
    private DeleteAction _deleteAction;
    private boolean _replace;
    private int _undoPos;
    private boolean _ignore;

    /**
     * Create UndoManager with the target content
     * @param content The Content going to attach
     */
    protected UndoManager(Content content) {
        _c = content;
        _undoStack = new ArrayList<>();
        _replace = false;
        _insertAction = null;
        _deleteAction = null;
        _undoPos = 0;
        _ignore = false;
    }

    /**
     * Undo on the given Content
     * @param content Undo Target
     */
    public void undo(Content content) {
        if(canUndo()) {
            _ignore = true;
            _undoStack.get(_undoPos - 1).undo(content);
            _undoPos--;
            _ignore = false;
        }
    }

    /**
     * Redo on the given Content
     * @param content Redo Target
     */
    public void redo(Content content) {
        if(canRedo()) {
            _ignore = true;
            _undoStack.get(_undoPos).redo(content);
            _undoPos++;
            _ignore = false;
        }
    }

    /**
     * Whether can undo
     * @return Whether can undo
     */
    public boolean canUndo() {
        return isUndoEnabled() && (_undoPos > 0);
    }

    /**
     * Whether can redo
     * @return Whether can redo
     */
    public boolean canRedo() {
        return isUndoEnabled() && (_undoPos < _undoStack.size());
    }

    /**
     * Set whether enable this module
     * @param enabled Enable or disable
     */
    public void setUndoEnabled(boolean enabled) {
        _undoEnabled = enabled;
        if (!enabled) {
            cleanStack();
        }
    }

    /**
     * Whether this UndoManager is enabled
     * @return Whether enabled
     */
    public boolean isUndoEnabled() {
        return _undoEnabled;
    }

    /**
     * Set a max stack size for this UndoManager
     * @param maxSize max stack size
     */
    public void setMaxUndoStackSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException(
                    "max size can not be zero or smaller.Did you want to disable undo module by calling set_undoEnabled(false)?");
        }
        _maxStackSize = maxSize;
        cleanStack();
    }

    /**
     * Get current max stack size
     * @return max stack size
     */
    public int getMaxUndoStackSize() {
        return _maxStackSize;
    }

    /**
     * Clean stack after add or state change
     * This is to limit stack size
     */
    private void cleanStack() {
        if(!_undoEnabled) {
            _undoStack.clear();
            _undoPos = 0;
        }else {
            while(_undoPos > 1 && _undoStack.size() > _maxStackSize) {
                _undoStack.remove(0);
                _undoPos--;
            }
        }
    }

    /**
     * Clean the stack before pushing
     * If we are not at the end(Undo action executed),remove those actions
     */
    private void cleanBeforePush() {
        while(_undoPos < _undoStack.size()) {
            _undoStack.remove(_undoStack.size() - 1);
        }
    }

    /**
     * Push a new ContentAction to stack
     * It will merge actions if possible
     * @param action New ContentAction
     */
    private void _push(ContentAction action) {
        if(!isUndoEnabled()) {
            return;
        }
        cleanBeforePush();
        if(_c.isInBatchEdit()) {
            if(_undoStack.isEmpty()) {
                MultiAction a = new MultiAction();
                a.addAction(action);
                _undoStack.add(a);
                _undoPos++;
            }else {
                ContentAction a = _undoStack.get(_undoStack.size() - 1);
                if(a instanceof MultiAction) {
                    MultiAction ac = (MultiAction)a;
                    ac.addAction(action);
                }else {
                    MultiAction ac = new MultiAction();
                    ac.addAction(action);
                    _undoStack.add(ac);
                    _undoPos++;
                }
            }
        }else {
            if(_undoStack.isEmpty()) {
                _undoStack.add(action);
                _undoPos++;
            }else {
                ContentAction last = _undoStack.get(_undoStack.size() - 1);
                if(last.canMerge(action)) {
                    last.merge(action);
                }else {
                    _undoStack.add(action);
                    _undoPos++;
                }
            }
        }
        cleanStack();
    }

    @Override
    public void beforeReplace(Content content) {
        if(_ignore) {
            return;
        }
        _replace = true;
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence insertedContent) {
        if(_ignore) {
            return;
        }
        _insertAction = new InsertAction();
        _insertAction.startLine = startLine;
        _insertAction.startColumn = startColumn;
        _insertAction.endLine = endLine;
        _insertAction.endColumn = endColumn;
        _insertAction.text = insertedContent;
        if(_replace) {
            ReplaceAction rep = new ReplaceAction();
            rep._delete = _deleteAction;
            rep._insert = _insertAction;
            _push(rep);
        }else {
            _push(_insertAction);
        }
        _replace = false;
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence deletedContent) {
        if(_ignore) {
            return;
        }
        _deleteAction = new DeleteAction();
        _deleteAction.endColumn = endColumn;
        _deleteAction.startColumn = startColumn;
        _deleteAction.endLine = endLine;
        _deleteAction.startLine = startLine;
        _deleteAction.text = deletedContent;
        if(!_replace) {
            _push(_deleteAction);
        }
    }

}
