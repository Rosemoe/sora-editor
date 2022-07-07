/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.text;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Content to take down modification
 * As well as provide Undo/Redo actions
 *
 * @author Rosemoe
 */
public final class UndoManager implements ContentListener, Parcelable {

    public final static Creator<UndoManager> CREATOR = new Creator<>() {
        @Override
        public UndoManager createFromParcel(Parcel parcel) {
            var o = new UndoManager();
            o.mMaxStackSize = parcel.readInt();
            o.mStackPointer = parcel.readInt();
            o.mUndoEnabled = parcel.readInt() > 0;
            var count = parcel.readInt();
            while (count > 0) {
                o.mActionStack.add(parcel.readParcelable(UndoManager.class.getClassLoader()));
                count--;
            }
            return o;
        }

        @Override
        public UndoManager[] newArray(int flags) {
            return new UndoManager[flags];
        }
    };
    /**
     * The max time span limit for merging actions
     */
    private static long sMergeTimeLimit = 8000L;
    private final List<ContentAction> mActionStack;
    private boolean mUndoEnabled;
    private int mMaxStackSize;
    private InsertAction mInsertAction;
    private DeleteAction mDeleteAction;
    private boolean mReplaceMark;
    private int mStackPointer;
    private boolean mIgnoreModification;
    /**
     * Create an UndoManager
     */
    UndoManager() {
        mActionStack = new ArrayList<>();
        mReplaceMark = false;
        mInsertAction = null;
        mDeleteAction = null;
        mStackPointer = 0;
        mIgnoreModification = false;
    }

    /**
     * @see #setMergeTimeLimit(long)
     */
    public static long getMergeTimeLimit() {
        return sMergeTimeLimit;
    }

    /**
     * Set max time span limit for merging actions
     *
     * @param mergeTimeLimit Time in millisecond
     */
    public static void setMergeTimeLimit(long mergeTimeLimit) {
        UndoManager.sMergeTimeLimit = mergeTimeLimit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mMaxStackSize);
        parcel.writeInt(mStackPointer);
        parcel.writeInt(mUndoEnabled ? 1 : 0);
        parcel.writeInt(mActionStack.size());
        for (ContentAction contentAction : mActionStack) {
            parcel.writeParcelable(contentAction, flags);
        }
    }

    /**
     * Check whether we are currently in undo/redo operations
     */
    public boolean isModifyingContent() {
        return mIgnoreModification;
    }

    /**
     * Undo on the given Content
     *
     * @param content Undo Target
     */
    public void undo(Content content) {
        if (canUndo()) {
            mIgnoreModification = true;
            mActionStack.get(mStackPointer - 1).undo(content);
            mStackPointer--;
            mIgnoreModification = false;
        }
    }

    /**
     * Redo on the given Content
     *
     * @param content Redo Target
     */
    public void redo(Content content) {
        if (canRedo()) {
            mIgnoreModification = true;
            mActionStack.get(mStackPointer).redo(content);
            mStackPointer++;
            mIgnoreModification = false;
        }
    }

    /**
     * Whether it can undo
     */
    public boolean canUndo() {
        return isUndoEnabled() && (mStackPointer > 0);
    }

    /**
     * Whether it can redo
     */
    public boolean canRedo() {
        return isUndoEnabled() && (mStackPointer < mActionStack.size());
    }

    /**
     * Whether this UndoManager is enabled
     *
     * @return Whether enabled
     */
    public boolean isUndoEnabled() {
        return mUndoEnabled;
    }

    /**
     * Set whether enable this module
     *
     * @param enabled Enable or disable
     */
    public void setUndoEnabled(boolean enabled) {
        mUndoEnabled = enabled;
        if (!enabled) {
            cleanStack();
        }
    }

    /**
     * Get current max stack size
     *
     * @return max stack size
     */
    public int getMaxUndoStackSize() {
        return mMaxStackSize;
    }

    /**
     * Set a max stack size for this UndoManager
     *
     * @param maxSize max stack size
     */
    public void setMaxUndoStackSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException(
                    "max size can not be zero or smaller.Did you want to disable undo module by calling setUndoEnabled()?");
        }
        mMaxStackSize = maxSize;
        cleanStack();
    }

    /**
     * Clean stack after add or state change
     * This is to limit stack size
     */
    private void cleanStack() {
        if (!mUndoEnabled) {
            mActionStack.clear();
            mStackPointer = 0;
        } else {
            while (mStackPointer > 1 && mActionStack.size() > mMaxStackSize) {
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
        while (mStackPointer < mActionStack.size()) {
            mActionStack.remove(mActionStack.size() - 1);
        }
    }

    /**
     * Push a new {@link ContentAction} to stack
     * It will merge actions if possible
     *
     * @param action New {@link ContentAction}
     */
    private void pushAction(Content content, ContentAction action) {
        if (!isUndoEnabled()) {
            return;
        }
        cleanBeforePush();
        if (content.isInBatchEdit()) {
            if (mActionStack.isEmpty()) {
                MultiAction a = new MultiAction();
                a.addAction(action);
                mActionStack.add(a);
                mStackPointer++;
            } else {
                ContentAction a = mActionStack.get(mActionStack.size() - 1);
                if (a instanceof MultiAction) {
                    MultiAction ac = (MultiAction) a;
                    ac.addAction(action);
                } else {
                    MultiAction ac = new MultiAction();
                    ac.addAction(action);
                    mActionStack.add(ac);
                    mStackPointer++;
                }
            }
        } else {
            if (mActionStack.isEmpty()) {
                mActionStack.add(action);
                mStackPointer++;
            } else {
                ContentAction last = mActionStack.get(mActionStack.size() - 1);
                if (last.canMerge(action)) {
                    last.merge(action);
                } else {
                    mActionStack.add(action);
                    mStackPointer++;
                }
            }
        }
        cleanStack();
    }

    @Override
    public void beforeReplace(Content content) {
        if (mIgnoreModification) {
            return;
        }
        mReplaceMark = true;
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence insertedContent) {
        if (mIgnoreModification) {
            return;
        }
        mInsertAction = new InsertAction();
        mInsertAction.startLine = startLine;
        mInsertAction.startColumn = startColumn;
        mInsertAction.endLine = endLine;
        mInsertAction.endColumn = endColumn;
        mInsertAction.text = insertedContent;
        if (mReplaceMark) {
            ReplaceAction rep = new ReplaceAction();
            rep._delete = mDeleteAction;
            rep._insert = mInsertAction;
            pushAction(content, rep);
        } else {
            pushAction(content, mInsertAction);
        }
        mReplaceMark = false;
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence deletedContent) {
        if (mIgnoreModification) {
            return;
        }
        mDeleteAction = new DeleteAction();
        mDeleteAction.endColumn = endColumn;
        mDeleteAction.startColumn = startColumn;
        mDeleteAction.endLine = endLine;
        mDeleteAction.startLine = startLine;
        mDeleteAction.text = deletedContent;
        if (!mReplaceMark) {
            pushAction(content, mDeleteAction);
        }
    }

    /**
     * For saving modification better
     *
     * @author Rose
     */
    public interface ContentAction extends Parcelable {

        /**
         * Undo this action
         *
         * @param content On the given object
         */
        void undo(Content content);

        /**
         * Redo this action
         *
         * @param content On the given object
         */
        void redo(Content content);

        /**
         * Get whether the target action can be merged with this action
         *
         * @param action Target action to merge
         * @return Whether they can merge
         */
        boolean canMerge(ContentAction action);

        /**
         * Merge with target action
         *
         * @param action Target action to merge
         */
        void merge(ContentAction action);

    }

    /**
     * Insert action model for UndoManager
     *
     * @author Rose
     */
    public static final class InsertAction implements ContentAction {

        public static final Creator<InsertAction> CREATOR = new Creator<>() {
            @Override
            public InsertAction createFromParcel(Parcel parcel) {
                var o = new InsertAction();
                o.startLine = parcel.readInt();
                o.startColumn = parcel.readInt();
                o.endLine = parcel.readInt();
                o.endColumn = parcel.readInt();
                o.text = parcel.readString();
                return o;
            }

            @Override
            public InsertAction[] newArray(int size) {
                return new InsertAction[size];
            }
        };
        public int startLine, endLine, startColumn, endColumn;
        public transient long createTime = System.currentTimeMillis();
        public CharSequence text;

        @Override
        public void undo(Content content) {
            content.delete(startLine, startColumn, endLine, endColumn);
        }

        @Override
        public void redo(Content content) {
            content.insert(startLine, startColumn, text);
        }

        @Override
        public boolean canMerge(ContentAction action) {
            if (action instanceof InsertAction) {
                InsertAction ac = (InsertAction) action;
                return (ac.startColumn == endColumn && ac.startLine == endLine
                        && ac.text.length() + text.length() < 10000
                        && Math.abs(ac.createTime - createTime) < sMergeTimeLimit);
            }
            return false;
        }

        @Override
        public void merge(ContentAction action) {
            if (!canMerge(action)) {
                throw new IllegalArgumentException();
            }
            InsertAction ac = (InsertAction) action;
            this.endColumn = ac.endColumn;
            this.endLine = ac.endLine;
            StringBuilder sb;
            if (text instanceof StringBuilder) {
                sb = (StringBuilder) text;
            } else {
                sb = new StringBuilder(text);
                text = sb;
            }
            sb.append(ac.text);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(startLine);
            parcel.writeInt(startColumn);
            parcel.writeInt(endLine);
            parcel.writeInt(endColumn);
            parcel.writeString(text.toString());
        }
    }

    /**
     * MultiAction saves several actions for UndoManager
     *
     * @author Rose
     */
    public static final class MultiAction implements ContentAction {

        public final static Creator<MultiAction> CREATOR = new Creator<>() {
            @Override
            public MultiAction createFromParcel(Parcel parcel) {
                var o = new MultiAction();
                var count = parcel.readInt();
                while (count > 0) {
                    o._actions.add(parcel.readParcelable(MultiAction.class.getClassLoader()));
                    count--;
                }
                return o;
            }

            @Override
            public MultiAction[] newArray(int size) {
                return new MultiAction[size];
            }
        };
        private final List<ContentAction> _actions = new ArrayList<>();

        public void addAction(ContentAction action) {
            if (_actions.isEmpty()) {
                _actions.add(action);
            } else {
                ContentAction last = _actions.get(_actions.size() - 1);
                if (last.canMerge(action)) {
                    last.merge(action);
                } else {
                    _actions.add(action);
                }
            }
        }

        @Override
        public void undo(Content content) {
            for (int i = _actions.size() - 1; i >= 0; i--) {
                _actions.get(i).undo(content);
            }
        }

        @Override
        public void redo(Content content) {
            for (int i = 0; i < _actions.size(); i++) {
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(_actions.size());
            for (ContentAction action : _actions) {
                parcel.writeParcelable(action, flags);
            }
        }
    }

    /**
     * Delete action model for UndoManager
     *
     * @author Rose
     */
    public static final class DeleteAction implements ContentAction {

        public final static Creator<DeleteAction> CREATOR = new Creator<>() {
            @Override
            public DeleteAction createFromParcel(Parcel parcel) {
                var o = new DeleteAction();
                o.startLine = parcel.readInt();
                o.startColumn = parcel.readInt();
                o.endLine = parcel.readInt();
                o.endColumn = parcel.readInt();
                o.text = parcel.readString();
                return o;
            }

            @Override
            public DeleteAction[] newArray(int size) {
                return new DeleteAction[size];
            }
        };
        public int startLine, endLine, startColumn, endColumn;
        public transient long createTime = System.currentTimeMillis();
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
            if (action instanceof DeleteAction) {
                DeleteAction ac = (DeleteAction) action;
                return (ac.endColumn == startColumn && ac.endLine == startLine
                        && ac.text.length() + text.length() < 10000
                        && Math.abs(ac.createTime - createTime) < sMergeTimeLimit);
            }
            return false;
        }

        @Override
        public void merge(ContentAction action) {
            if (!canMerge(action)) {
                throw new IllegalArgumentException();
            }
            DeleteAction ac = (DeleteAction) action;
            this.startColumn = ac.startColumn;
            this.startLine = ac.startLine;
            StringBuilder sb;
            if (text instanceof StringBuilder) {
                sb = (StringBuilder) text;
            } else {
                sb = new StringBuilder(text);
                text = sb;
            }
            sb.insert(0, ac.text);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(startLine);
            parcel.writeInt(startColumn);
            parcel.writeInt(endLine);
            parcel.writeInt(endColumn);
            parcel.writeString(text.toString());
        }
    }

    /**
     * Replace action model for UndoManager
     *
     * @author Rose
     */
    public static final class ReplaceAction implements ContentAction {

        public final static Creator<ReplaceAction> CREATOR = new Creator<>() {
            @Override
            public ReplaceAction createFromParcel(Parcel parcel) {
                var o = new ReplaceAction();
                o._insert = parcel.readParcelable(ReplaceAction.class.getClassLoader());
                o._delete = parcel.readParcelable(ReplaceAction.class.getClassLoader());
                return o;
            }

            @Override
            public ReplaceAction[] newArray(int size) {
                return new ReplaceAction[size];
            }
        };
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeParcelable(_insert, flags);
            parcel.writeParcelable(_delete, flags);
        }
    }
}
