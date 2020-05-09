/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.rose.editor.common;

import android.annotation.TargetApi;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import com.rose.editor.interfaces.ContentListener;
import com.rose.editor.interfaces.ITextContent;
import com.rose.editor.interfaces.Indexer;
import com.rose.editor.simpleclass.CharPosition;
import com.rose.editor.simpleclass.SingleLineSpan;

/**
 * Implementation of ITextContent
 * This class saves the text content of editor
 * @author Rose
 */
public class Content implements ITextContent {

    public final static int DEFAULT_MAX_UNDO_STACK_SIZE = 50;
    public final static int DEFAULT_LIST_CAPACITY = 100;

    private static int LIST_CAPACITY = Content.DEFAULT_LIST_CAPACITY;

    private List<StringBuilder> _lines;
    private int _textLength;
    private int _nestedBatchEdit;

    private List<ContentListener> _listeners;
    private Indexer _indexer;
    private SingleLineSpan _composingSpan = new SingleLineSpan();
    private UndoManager _undoMgr;
    private Cursor _cursor = null;
    private boolean _lex = false;

    /**
     * Set the default capacity of text line list
     * @param capacity Default capacity
     */
    public static void setInitialLineCapacity(int capacity) {
        if(capacity <= 0) {
            throw new IllegalArgumentException("capcity can not be under or equal zero");
        }
        LIST_CAPACITY = capacity;
    }

    /**
     * Returns the default capacity of text line list
     * @return Default capacity
     */
    public static int getInitialLineCapacity() {
        return Content.LIST_CAPACITY;
    }


    /**
     * This constructor will create a Content object with no text
     */
    public Content() {
        this(null);
    }

    /**
     * This constructor will create a Content object with the given source
     * If you give us null,it will just create a empty Content object
     * @param src The source of Content
     */
    public Content(CharSequence src) {
        if (src == null) {
            src = "";
        }
        _textLength = 0;
        _nestedBatchEdit = 0;
        _lines = new ArrayList<>(getInitialLineCapacity());
        _lines.add(new StringBuilder());
        _listeners = new ArrayList<>();
        _undoMgr = new UndoManager(this);
        setMaxUndoStackSize(Content.DEFAULT_MAX_UNDO_STACK_SIZE);
        _indexer = new NoCacheIndexer(this);
        if (src.length() == 0) {
            setUndoEnabled(true);
            return;
        }
        setUndoEnabled(false);
        insert(0, 0, src);
        setUndoEnabled(true);
    }

    @Override
    public char charAt(int index) {
        checkIndex(index);
        CharPosition p = _indexer.getCharPosition(index);
        return charAt(p.line, p.column);
    }

    @Override
    public int length() {
        return _textLength;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if(start > end) {
            throw new StringIndexOutOfBoundsException("start > end");
        }
        CharPosition s = _indexer.getCharPosition(start);
        CharPosition e = _indexer.getCharPosition(end);
        return subContent(s.getLine(), s.getColumn(), e.getColumn(), e.getColumn());
    }

    @Override
    public Content makeCopy() {
        Content c = new Content();
        c._lines.remove(0);
        ((ArrayList<StringBuilder>)c._lines).ensureCapacity(getLineCount());
        for (int i = 0; i < getLineCount(); i++) {
            StringBuilder line = _lines.get(i);
            c._lines.add(new StringBuilder(line.length()).append(line));
        }
        c._textLength = this._textLength;
        return c;
    }

    @Override
    public char charAt(int line, int column) {
        checkLineAndColumn(line, column, true);
        if(column == getColumnCount(line)) {
            return '\n';
        }
        return _lines.get(line).charAt(column);
    }

    @Override
    public int getLineCount() {
        return _lines.size();
    }

    @Override
    public int getColumnCount(int line) {
        checkLine(line);
        return _lines.get(line).length();
    }

    @Override
    public char[] getLineChars(int line) {
        checkLine(line);
        StringBuilder sb = _lines.get(line);
        char[] dest = new char[sb.length()];
        for (int i = 0; i < sb.length(); i++) {
            dest[i] = sb.charAt(i);
        }
        return dest;
    }

    @Override
    public String getLineString(int line) {
        checkLine(line);
        return _lines.get(line).toString();
    }

    public StringBuilder directGet(int line) {
        return _lines.get(line);
    }

    @Override
    public int getCharIndex(int line, int column) {
        return _indexer.getCharIndex(line, column);
    }

    @Override
    public void insert(int line, int column, CharSequence text) {
        checkLineAndColumn(line, column, true);
        if (text == null) {
            throw new IllegalArgumentException("text can not be null");
        }

        //-----Notify------
        if(_cursor != null)
            _cursor.beforeInsert(line,column,text.length());

        int workLine = line;
        int workIndex = column;
        if(workIndex == -1){
            workIndex = 0;
        }
        StringBuilder currLine = _lines.get(workLine);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                StringBuilder newLine = new StringBuilder();
                newLine.append(currLine, workIndex, currLine.length());
                currLine.delete(workIndex, currLine.length());
                _lines.add(workLine + 1, newLine);
                currLine = newLine;
                workIndex = 0;
                workLine++;
            } else {
                currLine.insert(workIndex, c);
                workIndex++;
            }
        }
        _textLength += text.length();
        this.dispatchAfterInsert(line, column, workLine, workIndex, text);
    }
    
    public void delete(int start, int end) {
        CharPosition startPos = getIndexer().getCharPosition(start);
        CharPosition endPos = getIndexer().getCharPosition(end);
        if(start != end) {
            delete(startPos.line, startPos.column, endPos.line, endPos.column);
        }
    }

    @Override
    public void delete(int startLine, int columnOnStartLine, int endLine, int columnOnEndLine) {
        StringBuilder changedContent = new StringBuilder();
        if (startLine == endLine) {
            checkLineAndColumn(endLine, columnOnEndLine, true);
            checkLineAndColumn(startLine, columnOnStartLine == -1 ? 0 : columnOnStartLine, true);
            int beginIdx = columnOnStartLine;
            int endIdx = columnOnEndLine;
            if (columnOnStartLine == -1) {
                beginIdx = 0;
            }
            if (beginIdx > endIdx) {
                throw new IllegalArgumentException("start > end");
            }
            StringBuilder curr = _lines.get(startLine);
            int len = curr.length();
            if (beginIdx < 0 || endIdx < 0 || beginIdx > len || endIdx > len) {
                throw new StringIndexOutOfBoundsException("column start or column end is out of bounds");
            }

            //-----Notify------
            if(_cursor != null)
                if(columnOnStartLine != -1)
                    _cursor.beforeDelete(startLine,columnOnStartLine,endLine,columnOnEndLine);
                else
                    _cursor.beforeDelete(startLine == 0 ? 0 : startLine - 1,startLine == 0 ? 0 : getColumnCount(startLine - 1),endLine,columnOnEndLine);

            changedContent.append(curr, beginIdx, endIdx);
            curr.delete(beginIdx, endIdx);
            _textLength -= columnOnEndLine - columnOnStartLine;
            if (columnOnStartLine == -1) {
                if (startLine == 0) {
                    _textLength++;
                } else {
                    StringBuilder previous = _lines.get(startLine - 1);
                    previous.append(curr);
                    _lines.remove(startLine);
                    changedContent.insert(0, '\n');
                    startLine--;
                    columnOnStartLine = getColumnCount(startLine);
                }
            }
        } else if (startLine < endLine) {
            checkLineAndColumn(startLine, columnOnStartLine, true);
            checkLineAndColumn(endLine, columnOnEndLine, true);

            //-----Notify------
            if(_cursor != null)
                _cursor.beforeDelete(startLine,columnOnStartLine,endLine,columnOnEndLine);

            int currEnd = endLine;
            while (currEnd - 1 != startLine) {
                StringBuilder line = _lines.remove(currEnd - 1);
                _textLength -= line.length() + 1;
                changedContent.append('\n').append(line);
                currEnd--;
            }
            StringBuilder start = _lines.get(startLine);
            StringBuilder end = _lines.get(currEnd);
            _textLength -= start.length() - columnOnStartLine;
            changedContent.insert(0, start, columnOnStartLine, start.length());
            start.delete(columnOnStartLine, start.length());
            _textLength -= columnOnEndLine;
            changedContent.append('\n').append(end, 0, columnOnEndLine);
            end.delete(0, columnOnEndLine);
            _textLength--;
            _lines.remove(currEnd);
            start.append(end);
        } else {
            throw new IllegalArgumentException("start line > end line");
        }
        this.dispatchAfterDelete(startLine, columnOnStartLine, endLine, columnOnEndLine, changedContent);
    }

    @Override
    public void replace(int startLine, int columnOnStartLine, int endLine, int columnOnEndLine, CharSequence text) {
        if (text == null) {
            throw new IllegalArgumentException("text can not be null");
        }
        this.dispatchBeforeReplace();
        delete(startLine, columnOnStartLine, endLine, columnOnEndLine);
        insert(startLine, columnOnStartLine, text);
    }

    @Override
    public void beginStreamCharGetting(int initialIndex) {
        _indexer = new CachedIndexer(this);
        _indexer.getCharPosition(initialIndex);
    }

    @Override
    public void endStreamCharGetting() {
        _indexer = new NoCacheIndexer(this);
    }

    @Override
    public void undo() {
        _undoMgr.undo(this);
    }

    @Override
    public void redo() {
        _undoMgr.redo(this);
    }

    @Override
    public boolean canUndo() {
        return _undoMgr.canUndo();
    }

    @Override
    public boolean canRedo() {
        return _undoMgr.canRedo();
    }

    @Deprecated
    public void enableLexMode(){
        _indexer = new LexIndexer(this);
        _lex = true;
    }

    @Override
    public void setUndoEnabled(boolean enabled) {
        _undoMgr.setUndoEnabled(enabled);
    }

    @Override
    public boolean isUndoEnabled() {
        return _undoMgr.isUndoEnabled();
    }

    @Override
    public void setMaxUndoStackSize(int maxSize) {
        _undoMgr.setMaxUndoStackSize(maxSize);
    }

    @Override
    public int getMaxUndoStackSize() {
        return _undoMgr.getMaxUndoStackSize();
    }

    @Override
    public boolean beginBatchEdit() {
        _nestedBatchEdit++;
        return isInBatchEdit();
    }

    @Override
    public boolean endBatchEdit() {
        _nestedBatchEdit--;
        if (_nestedBatchEdit < 0) {
            _nestedBatchEdit = 0;
        }
        return isInBatchEdit();
    }

    @Override
    public boolean isInBatchEdit() {
        return _nestedBatchEdit > 0;
    }

    @Override
    public void setComposingSpan(int line,int begin, int end) {
        _composingSpan.set(line, begin, end);
    }

    @Override
    public SingleLineSpan getComposingSpan() {
        return _composingSpan;
    }

    @Override
    public void removeComposingSpan() {
        _composingSpan.unset();
    }

    @Override
    public void addContentListener(ContentListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        if(listener instanceof Indexer) {
            throw new IllegalArgumentException("Permission denied");
        }
        if (!_listeners.contains(listener)) {
            _listeners.add(listener);
        }
    }

    @Override
    public void removeContentListener(ContentListener listener) {
        if(listener instanceof Indexer) {
            throw new IllegalArgumentException("Permission denied");
        }
        _listeners.remove(listener);
    }

    @Override
    public Indexer getIndexer() {
        return _indexer;
    }

    @Override
    public Content subContent(int startLine,int startColumn,int endLine,int endColumn) {
        Content c = new Content();
        c.setUndoEnabled(false);
        if(startLine == endLine) {
            c.insert(0, 0, _lines.get(startLine).substring(startColumn,endColumn));
        }else if(startLine < endLine){
            c.insert(0, 0, _lines.get(startLine).substring(startColumn));
            for(int i = startLine + 1;i < endLine;i++) {
                c._lines.add(new StringBuilder(_lines.get(i)));
                c._textLength += _lines.get(i).length() + 1;
            }
            StringBuilder end = _lines.get(endLine);
            c._lines.add(new StringBuilder().append(end,0,endColumn));
            c._textLength += endColumn + 1;
        }else {
            throw new IllegalArgumentException("start > end");
        }
        c.setUndoEnabled(true);
        return c;
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (anotherObject instanceof Content) {
            Content content = (Content) anotherObject;
            if (content.getLineCount() != this.getLineCount()) {
                return false;
            }
            for (int i = 0; i < this.getLineCount(); i++) {
                if (!equals(_lines.get(i), content._lines.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (StringBuilder line : _lines) {
            if (!first) {
                sb.append('\n');
            } else {
                first = false;
            }
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * Get the text in StringBuilder form
     * Used by TextColorProvider
     * This can improve the speed in char getting for tokenizing
     * @return StringBuilder form of Content
     */
    public StringBuilder toStringBuilder() {
        StringBuilder sb = new StringBuilder();
        sb.ensureCapacity(_textLength + 10);
        boolean first = true;
        for (StringBuilder line : _lines) {
            if (!first) {
                sb.append('\n');
            } else {
                first = false;
            }
            sb.append(line);
        }
        return sb;
    }

    /**
     * Get Cursor for editor (Create if there is not)
     * @return Cursor
     */
    public Cursor getCursor() {
        if(_cursor == null) {
            _cursor = new Cursor(this);
        }
        return _cursor;
    }

    /**
     * Test whether the two StringBuilder have the same content
     * @param a StringBuilder
     * @param b another StringBuilder
     * @return Whether equals in content
     */
    private static boolean equals(StringBuilder a, StringBuilder b) {
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

    /**
     * Dispatch events to listener before replacement
     */
    private void dispatchBeforeReplace() {
        _undoMgr.beforeReplace(this);
        if(_cursor != null)
            _cursor.beforeReplace();
        if(_indexer instanceof ContentListener) {
            ((ContentListener)_indexer).beforeReplace(this);
        }
        _composingSpan.beforeReplace(this);
        for (ContentListener lis : _listeners) {
            lis.beforeReplace(this);
        }
    }

    /**
     * Dispatch events to listener after deletion
     * @param a Start line
     * @param b Start Column
     * @param c End line
     * @param d End column
     * @param e Text deleted
     */
    private void dispatchAfterDelete(int a, int b, int c, int d, CharSequence e) {
        _undoMgr.afterDelete(this, a, b, c, d, e);
        if(_cursor != null)
            _cursor.afterDelete(a, b, c, d, e);
        if(_indexer instanceof ContentListener) {
            ((ContentListener)_indexer).afterDelete(this, a, b, c, d, e);
        }
        _composingSpan.afterDelete(this, a, b, c, d, e);
        for (ContentListener lis : _listeners) {
            lis.afterDelete(this, a, b, c, d, e);
        }
    }

    /**
     * Dispatch events to listener after insertion
     * @param a Start line
     * @param b Start Column
     * @param c End line
     * @param d End column
     * @param e Text deleted
     */
    private void dispatchAfterInsert(int a, int b, int c, int d, CharSequence e) {
        _undoMgr.afterInsert(this, a, b, c, d, e);
        if(_cursor != null)
            _cursor.afterInsert(a, b, c, d, e);
        if(_indexer instanceof ContentListener) {
            ((ContentListener)_indexer).afterInsert(this, a, b, c, d, e);
        }
        _composingSpan.afterInsert(this, a, b, c, d, e);
        for (ContentListener lis : _listeners) {
            lis.afterInsert(this, a, b, c, d, e);
        }
    }

    /**
     * Check whether the index is valid
     * @param index Index to check
     */
    protected void checkIndex(int index) {
        if (index > length()) {
            throw new StringIndexOutOfBoundsException("Index " + index + " out of bounds. length:" + length());
        }
    }

    /**
     * Check whether the line is valid
     * @param line Line to check
     */
    protected void checkLine(int line) {
        if (line >= getLineCount()) {
            throw new StringIndexOutOfBoundsException("Line " + line + " out of bounds. line count:" + getLineCount());
        }
    }

    /**
     * Check whether the line and column is valid
     * @param line The line to check
     * @param column The column to check
     * @param allowEqual Whether allow (column == getColumnCount(line))
     */
    protected void checkLineAndColumn(int line, int column, boolean allowEqual) {
        checkLine(line);
        int len = _lines.get(line).length();
        if (column > len || (!allowEqual && column == len)) {
            throw new StringIndexOutOfBoundsException(
                    "Column " + column + " out of bounds.line: " + line + " ,column count:" + len);
        }
    }

    //The following methods works on higher Android API with language level 8
    //AIDE does not support this and if we copy default implementation code with some modification, it does not works as well.
    //So we had to add a empty implementation

    @Override
    @TargetApi(24)
    public IntStream chars() {
        return null;
    }

    @Override
    @TargetApi(24)
    public IntStream codePoints() {
        return null;
    }


}
