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
package com.rose.editor.interfaces;

import com.rose.editor.simpleclass.SingleLineSpan;

/**
 * The interface for Content
 * @author Rose
 */
public interface ITextContent extends CharSequence {

    /**
     * Make a copy of this TextContent object including the content
     * @return The copy of this TextContent
     */
    ITextContent makeCopy();

    /**
     * Get the character at the given position
     * If (column == getColumnCount(line)),it returns '\n'
     * IndexOutOfBoundsException is thrown
     * @param line The line position of character
     * @param column The column position of character
     * @return The character at the given position
     */
    char charAt(int line,int column);

    /**
     * Get how many lines there are
     * @return Line count
     */
    int getLineCount();

    /**
     * Get how many characters is on the given line
     * If (line < 0 or line >= getLineCount()),it will throw a IndexOutOfBoundsException
     * @param line The line to get
     * @return Character count on line
     */
    int getColumnCount(int line);

    /**
     * Get the given line text
     * @param line The line to get
     * @return New character array of this line
     */
    char[] getLineChars(int line);

    /**
     * Get the given line text
     * @param line The line to get
     * @return New String object of this line
     */
    String getLineString(int line);

    /**
     * Transform the (line,column) position to index
     * This task will usually completed by {@link com.rose.editor.interfaces.Indexer}
     * @param line Line of index
     * @param column Column on line of index
     * @return Transformed index for the given arguments
     */
    int getCharIndex(int line,int column);

    /**
     * Insert content to this object
     * @param line The insertion's line position
     * @param column The insertion's column position
     * @param text The text you want to insert at the position
     */
    void insert(int line,int column,CharSequence text);

    /**
     * Delete text in the given region
     * @param startLine The start line position
     * @param columnOnStartLine The start column position
     * @param endLine The end line position
     * @param columnOnEndLine The end column position
     */
    void delete(int startLine,int columnOnStartLine,int endLine,int columnOnEndLine);

    /**
     * Replace the text in the given region
     * This action will completed by calling {@link ITextContent#delete(int, int, int, int)} and {@link ITextContent#insert(int, int, CharSequence)}
     * @param startLine The start line position
     * @param columnOnStartLine The start column position
     * @param endLine The end line position
     * @param columnOnEndLine The end column position
     * @param text The text to replace old text
     */
    void replace(int startLine,int columnOnStartLine,int endLine,int columnOnEndLine,CharSequence text);

    /**
     * Add a new {@link ContentListener} to the ITextContent
     * @param listener The listener to add
     */
    void addContentListener(ContentListener listener);

    /**
     * Remove the given listener of this ITextContent
     * @param listener The listener to remove
     */
    void removeContentListener(ContentListener listener);

    /**
     * Get the using {@link Indexer} object
     * @return
     */
    Indexer getIndexer();

    /**
     * Quick method to get sub string of this object
     * @param startLine The start line position
     * @param startColumn The start column position
     * @param endLine The end line position
     * @param endColumn The end column position
     * @return
     */
    ITextContent subContent(int startLine,int startColumn,int endLine,int endColumn);

    /**
     * When you are going to use {@link CharSequence#charAt(int)} frequently,you are required to call
     * this method.Because the way ITextContent save text,it is usually slow to transform index to
     * (line,column) from the start of text when the text is big.
     * By calling this method,you will be able to get faster because calling this will
     * cause the ITextContent object use a Indexer with cache.
     * The performance is highly improved while linearly getting characters.
     * @param initialIndex The Indexer with cache will take it into this index to its cache
     */
    void beginStreamCharGetting(int initialIndex);

    /**
     * When you finished calling {@link CharSequence#charAt(int)} frequently,you can call this method
     * to free the Indexer with cache.
     * This is not forced.
     */
    void endStreamCharGetting();

    /**
     * Undo the last modification
     * NOTE:When there are too much modification,old modification will be deleted from UndoManager
     */
    void undo();

    /**
     * Redo the last modification
     */
    void redo();

    /**
     * Whether we can undo
     * @return Whether we can undo
     */
    boolean canUndo();

    /**
     * Whether we can redo
     * @return Whether we can redo
     */
    boolean canRedo();

    /**
     * Set whether enable the UndoManager.
     * If false,any modification will not be taken down and previous modification that
     * is already in UndoManager will be removed.Does not make changes to content.
     * @param enabled New state for UndoManager
     */
    void setUndoEnabled(boolean enabled);

    /**
     * Get whether UndoManager is enabled
     * @return Whether UndoManager is enabled
     */
    boolean isUndoEnabled();

    /**
     * Set the max size of stack in UndoManager
     * @param maxSize New max size
     */
    void setMaxUndoStackSize(int maxSize);

    /**
     * Get current max stack size of UndoManager
     * @return current max stack size
     */
    int getMaxUndoStackSize();

    /**
     * A delegate method.
     * Notify the UndoManager to begin batch edit(enter a new layer).
     * NOTE: batch edit in Android can be nested.
     * @return Whether in batch edit
     */
    boolean beginBatchEdit();

    /**
     * A delegate method.
     * Notify the UndoManager to end batch edit(exit current layer).
     * @return Whether in batch edit
     */
    boolean endBatchEdit();

    /**
     * Returns whether we are in batch edit
     * @return Whether in batch edit
     */
    boolean isInBatchEdit();

    /**
     * Set a single line span for this text.
     * Editor will draw a underline for text in this region
     * The begin and end column position are on the given line.
     * @param line The line for the span
     * @param begin The start column on line
     * @param end The end column on line
     */
    void setComposingSpan(int line,int begin,int end);

    /**
     * Get the span set
     * @return The span you set
     */
    SingleLineSpan getComposingSpan();

    /**
     * Remove the span
     */
    void removeComposingSpan();

}
