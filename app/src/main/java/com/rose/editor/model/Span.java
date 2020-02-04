package com.rose.editor.model;

import com.rose.editor.common.Content;

/**
 * The span model
 * @author Rose
 */
public class Span {

    public int startIndex,line,column;

    public int colorId;

    /**
     * Create a span with brief position
     * @param i Index
     * @param l Line
     * @param c Column
     * @param cid Color ID
     */
    public Span(int i, int l, int c, int cid){
        colorId = cid;
        startIndex = i;
        line = l;
        column = c;
    }

    /**
     * Make self zero
     * @return self
     */
    public Span wrap(){
        line = column = 0;
        return this;
    }

    /**
     * Calculate line and column
     * @param c Target content
     * @return self
     */
    public Span wrap(Content c){
        CharPosition pos = c.getIndexer().getCharPosition(startIndex);
        line = pos.line;
        column = pos.column;
        return this;
    }

    /**
     * Create a new span from the given start index and color ID
     * @param start The start index
     * @param colorId Type of span
     */
    public Span(int start, int colorId){
        startIndex = start;
        this.colorId = colorId;
    }

    /**
     * Get span start line
     * @return Start line
     */
    public int getLine(){
        return line;
    }

    /**
     * Get span start column
     * @return Start column
     */
    public int getColumn(){
        return column;
    }

}
