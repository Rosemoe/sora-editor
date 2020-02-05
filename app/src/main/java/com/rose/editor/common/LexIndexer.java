package com.rose.editor.common;


import com.rose.editor.interfaces.Indexer;
import com.rose.editor.simpleclass.CharPosition;

/**
 * Quick linear indexer for tokenizing
 * @author Rose
 */
public class LexIndexer implements Indexer
{
    private CharPosition pos;
    private Content content;
    private int line,column,index;
    private int linecount;

    /**
     * Create a new Indexer for forward linear char getting
     * @param content
     */
    public LexIndexer(Content content){
        this.content = content;
        index = line = column = 0;
        pos = new CharPosition();
        linecount = content.getColumnCount(0);
    }

    @Override
    public int getCharIndex(int line, int column) {
        //Not required
        return 0;
    }

    @Override
    public int getCharLine(int index) {
        //Not required
        return 0;
    }

    @Override
    public int getCharColumn(int index) {
        //Not required
        return 0;
    }

    /**
     * Make info to next position
     */
    private void forward() {
        index++;
        column++;
        if(column == linecount + 1) {
            line++;
            column = 0;
            linecount = content.getColumnCount(line);
        }
    }

    /**
     * Make info to last position
     */
    private void backward() {
        index--;
        if(column == 0) {
            line--;
            linecount = column = content.getColumnCount(line);
        }else{
            column--;
        }
    }

    @Override
    public CharPosition getCharPosition(int index0)
    {
        while(index < index0) forward();
        while(index > index0) backward();
        pos.column = column;
        pos.line = line;
        pos.index = index0;
        return pos;
    }

    @Override
    public CharPosition getCharPosition(int line, int column) {
        //Not required
        return null;
    }

}