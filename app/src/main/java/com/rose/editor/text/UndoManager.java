/*
 *   Copyright 2020 Rose2073
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
package com.rose.editor.text;

import java.util.ArrayList;
import java.util.List;

import com.rose.editor.struct.DeleteAction;
import com.rose.editor.struct.InsertAction;
import com.rose.editor.struct.MultiAction;
import com.rose.editor.struct.ReplaceAction;

/**
 * Helper class for Content to take down modification
 * As well as provide Undo/Redo actions
 * @author Rose
 */
final class UndoManager implements ContentListener {

    private final Content mContent;
    private boolean mUndoEnabled;
    private int mMaxStackSize;
    private final List<ContentAction> mActionStack;
    private InsertAction mInsertAction;
    private DeleteAction mDeleteAction;
    private boolean mReplaceMark;
    private int mStackPointer;
    private boolean mIgnoreModification;

    /**
     * Create UndoManager with the target content
     * @param content The Content going to attach
     */
    protected UndoManager(Content content) {
        mContent = content;
        mActionStack = new ArrayList<>();
        mReplaceMark = false;
        mInsertAction = null;
        mDeleteAction = null;
        mStackPointer = 0;
        mIgnoreModification = false;
    }

    /**
     * Undo on the given Content
     * @param content Undo Target
     */
    public void undo(Content content) {
        if(canUndo()) {
            mIgnoreModification = true;
            mActionStack.get(mStackPointer - 1).undo(content);
            mStackPointer--;
            mIgnoreModification = false;
        }
    }

    /**
     * Redo on the given Content
     * @param content Redo Target
     */
    public void redo(Content content) {
        if(canRedo()) {
            mIgnoreModification = true;
            mActionStack.get(mStackPointer).redo(content);
            mStackPointer++;
            mIgnoreModification = false;
        }
    }

    /**
     * Whether can undo
     * @return Whether can undo
     */
    public boolean canUndo() {
        return isUndoEnabled() && (mStackPointer > 0);
    }

    /**
     * Whether can redo
     * @return Whether can redo
     */
    public boolean canRedo() {
        return isUndoEnabled() && (mStackPointer < mActionStack.size());
    }

    /**
     * Set whether enable this module
     * @param enabled Enable or disable
     */
    public void setUndoEnabled(boolean enabled) {
        mUndoEnabled = enabled;
        if (!enabled) {
            cleanStack();
        }
    }

    /**
     * Whether this UndoManager is enabled
     * @return Whether enabled
     */
    public boolean isUndoEnabled() {
        return mUndoEnabled;
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
        mMaxStackSize = maxSize;
        cleanStack();
    }

    /**
     * Get current max stack size
     * @return max stack size
     */
    public int getMaxUndoStackSize() {
        return mMaxStackSize;
    }

    /**
     * Clean stack after add or state change
     * This is to limit stack size
     */
    private void cleanStack() {
        if(!mUndoEnabled) {
            mActionStack.clear();
            mStackPointer = 0;
        }else {
            while(mStackPointer > 1 && mActionStack.size() > mMaxStackSize) {
                mActionStack.remove(0);
                mStackPointer--;
            }
        }
    }

    /**
     * Clean the stack before pushing
     * If we are not at the end(Undo action executed),remove those actions
     */
    private void cleanBeforePush() {
        while(mStackPointer < mActionStack.size()) {
            mActionStack.remove(mActionStack.size() - 1);
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
        if(mContent.isInBatchEdit()) {
            if(mActionStack.isEmpty()) {
                MultiAction a = new MultiAction();
                a.addAction(action);
                mActionStack.add(a);
                mStackPointer++;
            }else {
                ContentAction a = mActionStack.get(mActionStack.size() - 1);
                if(a instanceof MultiAction) {
                    MultiAction ac = (MultiAction)a;
                    ac.addAction(action);
                }else {
                    MultiAction ac = new MultiAction();
                    ac.addAction(action);
                    mActionStack.add(ac);
                    mStackPointer++;
                }
            }
        }else {
            if(mActionStack.isEmpty()) {
                mActionStack.add(action);
                mStackPointer++;
            }else {
                ContentAction last = mActionStack.get(mActionStack.size() - 1);
                if(last.canMerge(action)) {
                    last.merge(action);
                }else {
                    mActionStack.add(action);
                    mStackPointer++;
                }
            }
        }
        cleanStack();
    }

    @Override
    public void beforeReplace(Content content) {
        if(mIgnoreModification) {
            return;
        }
        mReplaceMark = true;
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence insertedContent) {
        if(mIgnoreModification) {
            return;
        }
        mInsertAction = new InsertAction();
        mInsertAction.startLine = startLine;
        mInsertAction.startColumn = startColumn;
        mInsertAction.endLine = endLine;
        mInsertAction.endColumn = endColumn;
        mInsertAction.text = insertedContent;
        if(mReplaceMark) {
            ReplaceAction rep = new ReplaceAction();
            rep._delete = mDeleteAction;
            rep._insert = mInsertAction;
            _push(rep);
        }else {
            _push(mInsertAction);
        }
        mReplaceMark = false;
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence deletedContent) {
        if(mIgnoreModification) {
            return;
        }
        mDeleteAction = new DeleteAction();
        mDeleteAction.endColumn = endColumn;
        mDeleteAction.startColumn = startColumn;
        mDeleteAction.endLine = endLine;
        mDeleteAction.startLine = startLine;
        mDeleteAction.text = deletedContent;
        if(!mReplaceMark) {
            _push(mDeleteAction);
        }
    }

}
