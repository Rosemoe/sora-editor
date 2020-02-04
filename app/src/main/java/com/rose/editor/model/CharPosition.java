package com.rose.editor.model;


/**
 * This a model of a character position in Content
 * @author Rose
 */
public final class CharPosition{

    //Packaged due to make changes

    public int index;

    public int line;

    public int column;

    /**
     * Get the index
     * @return index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get column
     * @return column
     */
    public int getColumn() {
        return column;
    }

    /**
     * Get line
     * @return line
     */
    public int getLine() {
        return line;
    }

    /**
     * Make this CharPosition zero and return self
     * @return self
     */
    public CharPosition zero() {
        index = line = column = 0;
        return this;
    }

    /**
     * Make a copy of this CharPosition and return the copy
     * @return New CharPosition including info of this CharPosition
     */
    public CharPosition fromThis() {
        CharPosition pos = new CharPosition();
        pos.index = index;
        pos.line = line;
        pos.column = column;
        return pos;
    }

    @Override
    public String toString() {
        return "CharPosition(line = " + line + ",column = " + column + ",index = " + index +")";
    }

}