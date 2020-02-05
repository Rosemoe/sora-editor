package com.rose.editor.interfaces;

import com.rose.editor.simpleclass.CharPosition;

/**
 * A helper class for ITextContent to transform (line,column) and index
 * @author Rose
 */
public interface Indexer {

    /**
     * Get the index of (line,column)
     * @param line The line position of index
     * @param column The column position of index
     * @return Calculated index
     */
    int getCharIndex(int line,int column);

    /**
     * Get the line position of index
     * @param index The index you want to know its line
     * @return Line position of index
     */
    int getCharLine(int index);

    /**
     * Get the column position of index
     * @param index The index you want to know its column
     * @return Column position of index
     */
    int getCharColumn(int index);

    /**
     * Get the CharPosition for the given index
     * You are not expected to make changes with this CharPosition
     * @param index The index you want to get
     * @return The CharPosition object.
     */
    CharPosition getCharPosition(int index);

    /**
     * Get the CharPosition for the given (line,column)
     * You are not expected to make changes with this CharPosition
     * @param line The line position you want to get
     * @param column The column position you want to get
     * @return The CharPosition object.
     */
    CharPosition getCharPosition(int line,int column);

}
