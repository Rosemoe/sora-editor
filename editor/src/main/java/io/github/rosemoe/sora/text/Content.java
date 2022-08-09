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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.github.rosemoe.sora.text.bidi.ContentBidi;
import io.github.rosemoe.sora.text.bidi.Directions;

/**
 * This class saves the text content for editor and maintains line widths.
 * It is thread-safe by default. Use {@link #Content(CharSequence, boolean)} constructor to
 * create a non thread-safe one.
 *
 * @author Rose
 */
public class Content implements CharSequence {

    public final static int DEFAULT_MAX_UNDO_STACK_SIZE = 500;
    public final static int DEFAULT_LIST_CAPACITY = 1000;

    private static int sInitialListCapacity;

    static {
        setInitialLineCapacity(DEFAULT_LIST_CAPACITY);
    }

    private final List<ContentLine> lines;
    private final List<ContentListener> contentListeners;
    private final ReadWriteLock lock;
    private int textLength;
    private int nestedBatchEdit;
    private final Indexer indexer;
    private final ContentBidi bidi;
    private UndoManager undoManager;
    private Cursor cursor;
    private LineRemoveListener lineListener;

    /**
     * This constructor will create a Content object with no text
     */
    public Content() {
        this(null);
    }

    /**
     * This constructor will create a Content object with the given source.
     * If you give us null,it will just create an empty Content object
     *
     * @param src The source of Content
     */
    public Content(CharSequence src) {
        this(src, true);
    }

    /**
     * Create a Content object with the given content text. Specify whether thread-safe is enabled.
     */
    public Content(CharSequence src, boolean threadSafe) {
        if (src == null) {
            src = "";
        }
        if (threadSafe) {
            lock = new ReentrantReadWriteLock();
        } else {
            lock = null;
        }
        textLength = 0;
        nestedBatchEdit = 0;
        lines = new ArrayList<>(getInitialLineCapacity());
        lines.add(new ContentLine());
        contentListeners = new ArrayList<>();
        bidi = new ContentBidi(this);
        undoManager = new UndoManager();
        setMaxUndoStackSize(Content.DEFAULT_MAX_UNDO_STACK_SIZE);
        indexer = new CachedIndexer(this);
        if (src.length() == 0) {
            setUndoEnabled(true);
            return;
        }
        setUndoEnabled(false);
        insert(0, 0, src);
        setUndoEnabled(true);
    }

    /**
     * Returns the default capacity of text line list
     *
     * @return Default capacity
     */
    public static int getInitialLineCapacity() {
        return Content.sInitialListCapacity;
    }

    /**
     * Set the default capacity of text line list
     *
     * @param capacity Default capacity
     */
    public static void setInitialLineCapacity(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity can not be negative or zero");
        }
        sInitialListCapacity = capacity;
    }

    /**
     * Test whether the two ContentLine have the same content
     *
     * @param a ContentLine
     * @param b another ContentLine
     * @return Whether equals in content
     */
    private static boolean equals(ContentLine a, ContentLine b) {
        if (a.length() != b.length()) {
            return false;
        }
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean isThreadSafe() {
        return lock != null;
    }

    protected void lock(boolean write) {
        if (lock == null) {
            return;
        }
        (write ? lock.writeLock() : lock.readLock()).lock();
    }

    protected void unlock(boolean write) {
        if (lock == null) {
            return;
        }
        (write ? lock.writeLock() : lock.readLock()).unlock();
    }

    @Override
    public char charAt(int index) {
        checkIndex(index);
        lock(false);
        try {
            var p = getIndexer().getCharPosition(index);
            return lines.get(p.line).charAt(p.column);
        } finally {
            unlock(false);
        }
    }

    /**
     * Get the character at the given position
     *
     * @param line   The line position of character
     * @param column The column position of character
     * @return The character at the given position
     */
    public char charAt(int line, int column) {
        lock(false);
        try {
            checkLineAndColumn(line, column, true);
            return lines.get(line).charAt(column);
        } finally {
            unlock(false);
        }
    }

    @Override
    public int length() {
        return textLength;
    }

    @NonNull
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start > end) {
            throw new StringIndexOutOfBoundsException("start > end");
        }
        lock(false);
        try {
            CharPosition s = getIndexer().getCharPosition(start);
            CharPosition e = getIndexer().getCharPosition(end);
            return subContentInternal(s.getLine(), s.getColumn(), e.getLine(), e.getColumn());
        } finally {
            unlock(false);
        }
    }

    /**
     * Set a line listener
     *
     * @param lis the listener,maybe null
     * @see LineRemoveListener
     */
    public void setLineListener(LineRemoveListener lis) {
        this.lineListener = lis;
    }

    /**
     * Get raw data of line
     * The result is not expected to be modified
     *
     * @param line Line
     * @return Raw ContentLine used by Content
     */
    public ContentLine getLine(int line) {
        lock(false);
        try {
            return lines.get(line);
        } finally {
            unlock(false);
        }
    }

    /**
     * Get how many lines there are
     *
     * @return Line count
     */
    public int getLineCount() {
        return lines.size();
    }

    /**
     * Get how many characters is on the given line
     * If (line < 0 or line >= getLineCount()),it will throw a IndexOutOfBoundsException
     *
     * @param line The line to get
     * @return Character count on line
     */
    public int getColumnCount(int line) {
        return getLine(line).length();
    }

    /**
     * Get the given line text without '\n' character
     *
     * @param line The line to get
     * @return New String object of this line
     */
    public String getLineString(int line) {
        lock(false);
        try {
            checkLine(line);
            return lines.get(line).toString();
        } finally {
            unlock(false);
        }
    }

    /**
     * Get region of the given line
     *
     * @param dest   Destination of characters
     * @param offset Offset in dest to store the chars
     */
    public void getRegionOnLine(int line, int start, int end, char[] dest, int offset) {
        lock(false);
        try {
            lines.get(line).getChars(start, end, dest, offset);
        } finally {
            unlock(false);
        }
    }

    /**
     * Get characters of line
     */
    public void getLineChars(int line, char[] dest) {
        getRegionOnLine(line, 0, getColumnCount(line), dest, 0);
    }

    /**
     * Transform the (line,column) position to index
     * This task will usually be completed by {@link Indexer}
     *
     * @param line   Line of index
     * @param column Column on line of index
     * @return Transformed index for the given arguments
     */
    public int getCharIndex(int line, int column) {
        lock(false);
        try {
            return getIndexer().getCharIndex(line, column);
        } finally {
            unlock(false);
        }
    }

    /**
     * Insert content to this object
     *
     * @param line   The insertion's line position
     * @param column The insertion's column position
     * @param text   The text you want to insert at the position
     */
    public void insert(int line, int column, CharSequence text) {
        lock(true);
        try {
            insertInternal(line, column, text);
        } finally {
            unlock(true);
        }
    }

    private void insertInternal(int line, int column, CharSequence text) {
        checkLineAndColumn(line, column, true);
        if (text == null) {
            throw new IllegalArgumentException("text can not be null");
        }

        //-----Notify------
        if (cursor != null)
            cursor.beforeInsert(line, column);
        for (var lis : contentListeners) {
            lis.beforeModification(this);
        }

        int workLine = line;
        int workIndex = column;
        if (workIndex == -1) {
            workIndex = 0;
        }
        var currLine = lines.get(workLine);
        var helper = InsertTextHelper.forInsertion(text);
        int type, peekType = InsertTextHelper.TYPE_EOF;
        boolean fromPeek = false;
        var newLines = new LinkedList<ContentLine>();
        var startSeparator = currLine.getLineSeparator();
        while (true) {
            type = fromPeek ? peekType : helper.forward();
            fromPeek = false;
            if (type == InsertTextHelper.TYPE_EOF) {
                break;
            }
            if (type == InsertTextHelper.TYPE_LINE_CONTENT) {
                currLine.insert(workIndex, text, helper.getIndex(), helper.getIndexNext());
                workIndex += helper.getIndexNext() - helper.getIndex();
            } else {
                var separator = LineSeparator.fromSeparatorString(text, helper.getIndex(), helper.getIndexNext());
                currLine.setLineSeparator(separator);

                // Peek!
                peekType = helper.forward();
                fromPeek = true;

                var newLine = new ContentLine(currLine.length() - workIndex + helper.getIndexNext() - helper.getIndex() + 10);
                newLine.insert(0, currLine, workIndex, currLine.length());
                currLine.delete(workIndex, currLine.length());
                workIndex = 0;
                currLine = newLine;
                newLines.add(newLine);
                workLine++;
            }
        }
        currLine.setLineSeparator(startSeparator);
        lines.addAll(line + 1, newLines);
        helper.recycle();
        textLength += text.length();
        this.dispatchAfterInsert(line, column, workLine, workIndex, text);
    }

    /**
     * Delete character in [start,end)
     *
     * @param start Start position in content
     * @param end   End position in content
     */
    public void delete(int start, int end) {
        checkIndex(start);
        checkIndex(end);
        lock(true);
        try {
            CharPosition startPos = getIndexer().getCharPosition(start);
            CharPosition endPos = getIndexer().getCharPosition(end);
            if (start != end) {
                deleteInternal(startPos.line, startPos.column, endPos.line, endPos.column);
            }
        } finally {
            unlock(true);
        }
    }

    /**
     * Delete text in the given region
     *
     * @param startLine         The start line position
     * @param columnOnStartLine The start column position
     * @param endLine           The end line position
     * @param columnOnEndLine   The end column position
     */
    public void delete(int startLine, int columnOnStartLine, int endLine, int columnOnEndLine) {
        lock(true);
        try {
            deleteInternal(startLine, columnOnStartLine, endLine, columnOnEndLine);
        } finally {
            unlock(true);
        }
    }

    private void deleteInternal(int startLine, int columnOnStartLine, int endLine, int columnOnEndLine) {
        var changedContent = new StringBuilder();
        if (startLine == endLine) {
            checkLineAndColumn(endLine, columnOnEndLine, true);
            checkLineAndColumn(startLine, columnOnStartLine, true);
            var curr = lines.get(startLine);
            int len = curr.length();
            if (columnOnStartLine < 0 || columnOnEndLine > len || columnOnStartLine > columnOnEndLine) {
                throw new StringIndexOutOfBoundsException("invalid bounds");
            }

            //-----Notify------
            if (cursor != null) {
                cursor.beforeDelete(startLine, columnOnStartLine, endLine, columnOnEndLine);
            }
            for (var lis : contentListeners) {
                lis.beforeModification(this);
            }

            changedContent.append(curr, columnOnStartLine, columnOnEndLine);
            curr.delete(columnOnStartLine, columnOnEndLine);
            textLength -= columnOnEndLine - columnOnStartLine;
        } else if (startLine < endLine) {
            checkLineAndColumn(startLine, columnOnStartLine, true);
            checkLineAndColumn(endLine, columnOnEndLine, true);

            //-----Notify------
            if (cursor != null)
                cursor.beforeDelete(startLine, columnOnStartLine, endLine, columnOnEndLine);
            for (var lis : contentListeners) {
                lis.beforeModification(this);
            }

            for (int i = startLine + 1; i <= endLine - 1; i++) {
                var line = lines.get(i);
                if (lineListener != null) {
                    lineListener.onRemove(this, line);
                }
                var separator = lines.get(i).getLineSeparator();
                textLength -= line.length() + separator.getLength();
                changedContent.append(line).append(separator.getContent());
            }
            if (lineListener != null) {
                lineListener.onRemove(this, lines.get(endLine));
            }
            if (endLine > startLine + 1) {
                lines.subList(startLine + 1, endLine).clear();
            }

            int currEnd = startLine + 1;
            var start = lines.get(startLine);
            var end = lines.get(currEnd);
            textLength -= start.length() - columnOnStartLine;
            changedContent.insert(0, start, columnOnStartLine, start.length())
                    .insert(start.length() - columnOnStartLine, start.getLineSeparator().getContent());
            start.delete(columnOnStartLine, start.length());
            textLength -= columnOnEndLine;
            changedContent.append(end, 0, columnOnEndLine);
            end.delete(0, columnOnEndLine);
            textLength -= start.getLineSeparator().getLength();
            lines.remove(currEnd);
            start.append(end);
            start.setLineSeparator(end.getLineSeparator());
        } else {
            throw new IllegalArgumentException("start line > end line");
        }
        this.dispatchAfterDelete(startLine, columnOnStartLine, endLine, columnOnEndLine, changedContent);
    }

    /**
     * Replace the text in the given region
     * This action will be completed by calling {@link Content#delete(int, int, int, int)} and {@link Content#insert(int, int, CharSequence)}
     *
     * @param startLine         The start line position
     * @param columnOnStartLine The start column position
     * @param endLine           The end line position
     * @param columnOnEndLine   The end column position
     * @param text              The text to replace old text
     */
    public void replace(int startLine, int columnOnStartLine, int endLine, int columnOnEndLine, CharSequence text) {
        if (text == null) {
            throw new IllegalArgumentException("text can not be null");
        }
        lock(true);
        try {
            this.dispatchBeforeReplace();
            deleteInternal(startLine, columnOnStartLine, endLine, columnOnEndLine);
            insertInternal(startLine, columnOnStartLine, text);
        } finally {
            unlock(true);
        }
    }

    /**
     * Replace text in the given region with the text
     */
    public void replace(int startIndex, int endIndex, @NonNull CharSequence text) {
        var start = getIndexer().getCharPosition(startIndex);
        var end = getIndexer().getCharPosition(endIndex);
        replace(start.line, start.column, end.line, end.column, text);
    }

    /**
     * Undo the last modification
     * NOTE:When there are too much modification,old modification will be deleted from UndoManager
     */
    public void undo() {
        undoManager.undo(this);
    }

    /**
     * Redo the last modification
     */
    public void redo() {
        undoManager.redo(this);
    }

    /**
     * Check whether the {@link UndoManager} is working to undo/redo
     */
    public boolean isUndoManagerWorking() {
        return undoManager.isModifyingContent();
    }

    /**
     * Whether we can undo
     *
     * @return Whether we can undo
     */
    public boolean canUndo() {
        return undoManager.canUndo();
    }

    /**
     * Whether we can redo
     *
     * @return Whether we can redo
     */
    public boolean canRedo() {
        return undoManager.canRedo();
    }

    /**
     * Get whether UndoManager is enabled
     *
     * @return Whether UndoManager is enabled
     */
    public boolean isUndoEnabled() {
        return undoManager.isUndoEnabled();
    }

    /**
     * Set whether enable the UndoManager.
     * If false,any modification will not be taken down and previous modification that
     * is already in UndoManager will be removed.Does not make changes to content.
     *
     * @param enabled New state for UndoManager
     */
    public void setUndoEnabled(boolean enabled) {
        undoManager.setUndoEnabled(enabled);
    }

    /**
     * Get current max stack size of UndoManager
     *
     * @return current max stack size
     */
    public int getMaxUndoStackSize() {
        return undoManager.getMaxUndoStackSize();
    }

    /**
     * Set the max size of stack in UndoManager
     *
     * @param maxSize New max size
     */
    public void setMaxUndoStackSize(int maxSize) {
        undoManager.setMaxUndoStackSize(maxSize);
    }

    /**
     * A delegate method.
     * Notify the UndoManager to begin batch edit(enter a new layer).
     * NOTE: batch edit in Android can be nested.
     *
     * @return Whether in batch edit
     */
    public boolean beginBatchEdit() {
        nestedBatchEdit++;
        return isInBatchEdit();
    }

    /**
     * A delegate method.
     * Notify the UndoManager to end batch edit(exit current layer).
     *
     * @return Whether in batch edit
     */
    public boolean endBatchEdit() {
        nestedBatchEdit--;
        if (nestedBatchEdit < 0) {
            nestedBatchEdit = 0;
        }
        return isInBatchEdit();
    }

    public void resetBatchEdit() {
        nestedBatchEdit = 0;
    }

    /**
     * Returns whether we are in batch edit
     *
     * @return Whether in batch edit
     */
    public boolean isInBatchEdit() {
        return nestedBatchEdit > 0;
    }

    /**
     * Add a new {@link ContentListener} to the Content
     *
     * @param listener The listener to add
     */
    public void addContentListener(ContentListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        if (listener instanceof Indexer) {
            throw new IllegalArgumentException("Permission denied");
        }
        if (!contentListeners.contains(listener)) {
            contentListeners.add(listener);
        }
    }

    /**
     * Remove the given listener of this Content
     *
     * @param listener The listener to remove
     */
    public void removeContentListener(ContentListener listener) {
        if (listener instanceof Indexer) {
            throw new IllegalArgumentException("Permission denied");
        }
        contentListeners.remove(listener);
    }

    /**
     * Get the using {@link Indexer} object
     *
     * @return Indexer for this object
     */
    public Indexer getIndexer() {
        if (cursor != null) {
            return cursor.getIndexer();
        }
        return indexer;
    }

    /**
     * Quick method to get sub string of this object
     *
     * @param startLine   The start line position
     * @param startColumn The start column position
     * @param endLine     The end line position
     * @param endColumn   The end column position
     * @return sub-sequence of this Content
     */
    public Content subContent(int startLine, int startColumn, int endLine, int endColumn) {
        lock(false);
        try {
            return subContentInternal(startLine, startColumn, endLine, endColumn);
        } finally {
            unlock(false);
        }
    }

    private Content subContentInternal(int startLine, int startColumn, int endLine, int endColumn) {
        Content c = new Content();
        c.setUndoEnabled(false);
        if (startLine == endLine) {
            c.insert(0, 0, lines.get(startLine).subSequence(startColumn, endColumn));
        } else if (startLine < endLine) {
            c.insert(0, 0, lines.get(startLine).subSequence(startColumn, lines.get(startLine).length()));
            for (int i = startLine + 1; i < endLine; i++) {
                c.lines.add(new ContentLine(lines.get(i)));
                c.textLength += lines.get(i).length() + 1;
            }
            ContentLine end = lines.get(endLine);
            c.lines.add(new ContentLine().insert(0, end, 0, endColumn));
            c.textLength += endColumn + 1;
        } else {
            throw new StringIndexOutOfBoundsException("start > end");
        }
        c.setUndoEnabled(true);
        return c;
    }

    @NonNull
    public Directions getLineDirections(int line) {
        lock(false);
        try {
            return bidi.getLineDirections(line);
        } finally {
            unlock(false);
        }
    }

    public void setBidiEnabled(boolean enabled) {
        bidi.setEnabled(enabled);
    }

    public boolean isBidiEnabled() {
        return bidi.isEnabled();
    }

    public boolean isRtlAt(int line, int column) {
        var dirs = getLineDirections(line);
        for (int i = 0; i < dirs.getRunCount(); i++) {
            if (column >= dirs.getRunStart(i) && column < dirs.getRunEnd(i)) {
                return dirs.isRunRtl(i);
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (anotherObject instanceof Content) {
            Content content = (Content) anotherObject;
            if (content.length() != this.length()) {
                return false;
            }
            for (int i = 0; i < this.getLineCount(); i++) {
                if (!equals(lines.get(i), content.lines.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(lines, textLength);
    }

    @NonNull
    @Override
    public String toString() {
        return toStringBuilder().toString();
    }

    /**
     * Get the text in StringBuilder form
     * <p>
     * This can improve the speed in char getting for tokenizing
     *
     * @return StringBuilder form of Content
     */
    public StringBuilder toStringBuilder() {
        var sb = new StringBuilder();
        appendToStringBuilder(sb);
        return sb;
    }

    /**
     * Get UndoManager instance in use
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Set undo manager. You may use this to recover to a previously saved state of undo stack.
     */
    public void setUndoManager(UndoManager manager) {
        this.undoManager = manager;
    }

    /**
     * Append the content to the given {@link StringBuilder}
     */
    public void appendToStringBuilder(StringBuilder sb) {
        sb.ensureCapacity(sb.length() + length());
        final int lines = getLineCount();
        for (int i = 0; i < lines; i++) {
            var line = this.lines.get(i);
            line.appendTo(sb);
            sb.append(line.getLineSeparator().getContent());
        }
    }

    /**
     * Get Cursor for editor (Create if there is not)
     *
     * @return Cursor
     */
    public Cursor getCursor() {
        if (cursor == null) {
            cursor = new Cursor(this);
        }
        return cursor;
    }

    /**
     * Dispatch events to listener before replacement
     */
    private void dispatchBeforeReplace() {
        undoManager.beforeReplace(this);
        if (cursor != null)
            cursor.beforeReplace();
        if (indexer instanceof ContentListener) {
            ((ContentListener) indexer).beforeReplace(this);
        }
        for (ContentListener lis : contentListeners) {
            lis.beforeReplace(this);
        }
    }

    /**
     * Dispatch events to listener after deletion
     *
     * @param a Start line
     * @param b Start Column
     * @param c End line
     * @param d End column
     * @param e Text deleted
     */
    private void dispatchAfterDelete(int a, int b, int c, int d, CharSequence e) {
        undoManager.afterDelete(this, a, b, c, d, e);
        if (cursor != null)
            cursor.afterDelete(a, b, c, d, e);
        if (indexer instanceof ContentListener) {
            ((ContentListener) indexer).afterDelete(this, a, b, c, d, e);
        }
        for (ContentListener lis : contentListeners) {
            lis.afterDelete(this, a, b, c, d, e);
        }
    }

    /**
     * Dispatch events to listener after insertion
     *
     * @param a Start line
     * @param b Start Column
     * @param c End line
     * @param d End column
     * @param e Text deleted
     */
    private void dispatchAfterInsert(int a, int b, int c, int d, CharSequence e) {
        undoManager.afterInsert(this, a, b, c, d, e);
        if (cursor != null)
            cursor.afterInsert(a, b, c, d, e);
        if (indexer instanceof ContentListener) {
            ((ContentListener) indexer).afterInsert(this, a, b, c, d, e);
        }
        for (ContentListener lis : contentListeners) {
            lis.afterInsert(this, a, b, c, d, e);
        }
    }

    /**
     * Check whether the index is valid
     *
     * @param index Index to check
     */
    protected void checkIndex(int index) {
        if (index > length() || index < 0) {
            throw new StringIndexOutOfBoundsException("Index " + index + " out of bounds. length:" + length());
        }
    }

    /**
     * Check whether the line is valid
     *
     * @param line Line to check
     */
    protected void checkLine(int line) {
        if (line >= getLineCount() || line < 0) {
            throw new StringIndexOutOfBoundsException("Line " + line + " out of bounds. line count:" + getLineCount());
        }
    }

    /**
     * Check whether the line and column is valid
     *
     * @param line       The line to check
     * @param column     The column to check
     * @param allowEqual Whether allow (column == getColumnCount(line))
     */
    protected void checkLineAndColumn(int line, int column, boolean allowEqual) {
        checkLine(line);
        int len = lines.get(line).length();
        if (column > len || (!allowEqual && column == len) || column < 0) {
            throw new StringIndexOutOfBoundsException(
                    "Column " + column + " out of bounds.line: " + line + " ,column count:" + len);
        }
    }

    /**
     * Copy text in this Content object.
     * Returns a new thread-safe Content object with the same text as this object.
     */
    public Content copyText() {
        return copyText(true);
    }

    /**
     * Copy text in this Content object.
     * Returns a new Content object with the same text as this object.
     */
    public Content copyText(boolean newContentThreadSafe) {
        lock(false);
        try {
            var n = new Content(null, newContentThreadSafe);
            n.lines.remove(0);
            for (int i = 0; i < getLineCount(); i++) {
                var line = lines.get(i);
                n.lines.add(line.subSequence(0, line.length()));
            }
            return n;
        } finally {
            unlock(false);
        }
    }

    protected int getColumnCountUnsafe(int line) {
        return lines.get(line).length();
    }

    @NonNull
    protected LineSeparator getLineSeparatorUnsafe(int line) {
        return lines.get(line).getLineSeparator();
    }

    /**
     * Read the lines (ordered).
     * This is for optimizing frequent lock acquiring.
     *
     * @param startLine inclusive
     * @param endLine   inclusive
     */
    public void runReadActionsOnLines(int startLine, int endLine, @NonNull ContentLineConsumer consumer) {
        lock(false);
        try {
            for (int i = startLine; i <= endLine; i++) {
                consumer.accept(i, lines.get(i), bidi.getLineDirections(i));
            }
        } finally {
            unlock(false);
        }
    }

    /**
     * Read the lines (ordered).
     * This is for optimizing frequent lock acquiring.
     *
     * @param startLine inclusive
     * @param endLine   inclusive
     */
    public void runReadActionsOnLines(int startLine, int endLine, @NonNull ContentLineConsumer2 consumer) {
        lock(false);
        try {
            var flag = new ContentLineConsumer2.AbortFlag();
            for (int i = startLine; i <= endLine && !flag.set; i++) {
                consumer.accept(i, lines.get(i), flag);
            }
        } finally {
            unlock(false);
        }
    }


    public interface ContentLineConsumer {

        void accept(int lineIndex, @NonNull ContentLine line, @NonNull Directions dirs);

    }

    public interface ContentLineConsumer2 {

        void accept(int lineIndex, @NonNull ContentLine line, @NonNull AbortFlag flag);

        class AbortFlag {
            public boolean set = false;
        }

    }
}
