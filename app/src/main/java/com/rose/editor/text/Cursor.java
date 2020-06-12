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
package com.rose.editor.text;

import com.rose.editor.interfaces.EditorLanguage;
import com.rose.editor.struct.CharPosition;

/**
 * @author Rose
 * Warning:The cursor position will update automatically when the content has been changed by other way
 */
public final class Cursor {

    private Content _content;
    private CachedIndexer _indexer;
    private CharPosition _left,_right;
    private CharPosition _cache0,_cache1,_cache2;
    private boolean autoIndent;
    private EditorLanguage mLang;
    private int tabWidth;

    /**
     * Create a new Cursor for Content
     * @param content Target content
     */
    public Cursor(Content content) {
        _content = content;
        _indexer = new CachedIndexer(content);
        _left = new CharPosition().zero();
        _right = new CharPosition().zero();
        tabWidth = 4;
    }

    /**
     * Make left and right cursor on the given position
     * @param line The line position
     * @param column The column position
     */
    public void set(int line,int column){
        setLeft(line,column);
        setRight(line,column);
    }

    /**
     * Make left cursor on the given position
     * @param line The line position
     * @param column The column position
     */
    public void setLeft(int line,int column) {
        _left = _indexer.getCharPosition(line, column).fromThis();
    }

    /**
     * Make right cursor on the given position
     * @param line The line position
     * @param column The column position
     */
    public void setRight(int line,int column) {
        _right = _indexer.getCharPosition(line, column).fromThis();
    }

    /**
     * Get the left cursor line
     * @return line of left cursor
     */
    public int getLeftLine() {
        return _left.getLine();
    }

    /**
     * Get the left cursor column
     * @return column of left cursor
     */
    public int getLeftColumn() {
        return _left.getColumn();
    }

    /**
     * Get the right cursor line
     * @return line of right cursor
     */
    public int getRightLine() {
        return _right.getLine();
    }

    /**
     * Get the right cursor column
     * @return column of right cursor
     */
    public int getRightColumn() {
        return _right.getColumn();
    }

    /**
     * Whether the given position is in selected region
     * @param line The line to query
     * @param column The column to query
     * @return Whether is in selected region
     */
    public boolean isInSelectedRegion(int line,int column) {
        if(line >= getLeftLine() && line <= getRightLine()) {
            boolean yes = true;
            if(line == getLeftLine()) {
                yes = yes && column >= getLeftColumn();
            }
            if(line == getRightLine()) {
                yes = yes && column < getRightColumn();
            }
            return yes;
        }
        return false;
    }

    /**
     * Get the left cursor index
     * @return index of left cursor
     */
    public int getLeft() {
        return _left.index;
    }

    /**
     * Get the right cursor index
     * @return index of right cursor
     */
    public int getRight() {
        return _right.index;
    }

    /**
     * Notify the Indexer to update its cache for current display position
     * This will make querying actions quicker
     * Especially when the editor user want to set a new cursor position after scrolling long time
     * @param line First visible line
     */
    public void updateCache(int line) {
        _indexer.getCharIndex(line,0);
    }

    /**
     * Get the using Indexer object
     * @return Using Indexer
     */
    public CachedIndexer getIndexer() {
        return _indexer;
    }

    /**
     * Get whether text is selected
     * @return Whether selected
     */
    public boolean isSelected() {
        return (getLeftColumn() != getRightColumn() || getLeftLine() != getRightLine());
    }

    /**
     * Enable or disable auto indent when insert text through Cursor
     * @param enabled Auto Indent state
     */
    public void setAutoIndent(boolean enabled) {
        autoIndent = enabled;
    }

    /**
     * Returns whether auto indent is enabled
     * @return Enabled or disabled
     */
    public boolean isAutoIndent() {
        return autoIndent;
    }

    /**
     * Set language for auto indent
     * @param lang The target language
     */
    public void setLanguage(EditorLanguage lang) {
        mLang = lang;
    }

    /**
     * Set tab width for auto indent
     * @param width tab width
     */
    public void setTabWidth(int width){
        tabWidth = width;
    }

    /**
     * Commit text at current state
     * @param text Text commit by InputConnection
     */
    public void onCommitText(CharSequence text) {
        if(isSelected()) {
            _content.replace(getLeftLine(), getLeftColumn(), getRightLine(), getRightColumn(), text);
        }else {
            if(autoIndent && text.length() != 0) {
                char first = text.charAt(0);
                if(first == '\n') {
                    String line = _content.getLineString(getLeftLine());
                    int p = 0,count = 0;
                    while(p < getLeftColumn()) {
                        if(isWhitespace(line.charAt(p))){
                            if(line.charAt(p) == '\t') {
                                count += tabWidth;
                            }else{
                                count++;
                            }
                            p++;
                        }else{
                            break;
                        }
                    }
                    String sub = line.substring(0,getLeftColumn());
                    count += mLang.getIndentAdvance(sub);
                    StringBuilder sb = new StringBuilder(text);
                    sb.insert(1,create(count));
                    text = sb;
                }
            }
            _content.insert(getLeftLine(), getLeftColumn(), text);
        }
    }

    /**
     * Create indent space
     * @param p Target width effect
     * @return Generated space string
     */
    private String create(int p) {
        int tab = 0;
        int space;
        if(mLang.useTab()) {
            tab = p / tabWidth;
            space = p % tabWidth;
        }else{
            space = p;
        }
        StringBuilder s = new StringBuilder();
        for(int i = 0;i < tab;i++) {
            s.append('\t');
        }
        for(int i = 0;i < space;i++) {
            s.append(' ');
        }
        return s.toString();
    }

    /**
     * Whether the given character is a white space character
     * @param c Character to check
     * @return Result whether a space char
     */
    protected static boolean isWhitespace(char c) {
        return (c == '\t' || c == ' ');
    }

    /**
     * Handle delete submit by InputConnection
     */
    public void onDeleteKeyPressed() {
        if(isSelected()) {
            _content.delete(getLeftLine(), getLeftColumn(), getRightLine(), getRightColumn());
        }else {
            int col = getLeftColumn(),len = 1;
            //Do not put cursor inside a emotion character
            if(col > 1) {
                char before = _content.charAt(getLeftLine(),col - 2);
                if(isEmoji(before)) {
                    len = 2;
                }
            }
            _content.delete(getLeftLine(), getLeftColumn() - len, getLeftLine(), getLeftColumn());
        }
    }

    /**
     * Whether the char is a emoji
     * @param ch Character to check
     * @return Whether the char is a emoji
     */
    private static boolean isEmoji(char ch){
        return ch == 0xd83c || ch == 0xd83d;
    }

    /**
     * Internal call back before insertion
     * @param startLine Start line
     * @param startColumn Start column
     * @param length Text to insert length
     */
    void beforeInsert(int startLine, int startColumn, int length) {
        _cache0 = _indexer.getCharPosition(startLine,startColumn).fromThis();
    }

    /**
     * Internal call back before deletion
     * @param startLine Start line
     * @param startColumn Start column
     * @param endLine End line
     * @param endColumn End column
     */
    void beforeDelete(int startLine, int startColumn, int endLine, int endColumn) {
        _cache1 = _indexer.getCharPosition(startLine, startColumn).fromThis();
        _cache2 = _indexer.getCharPosition(endLine, endColumn).fromThis();
    }

    /**
     * Internal call back before replace
     */
    void beforeReplace() {
        _indexer.beforeReplace(_content);
    }

    /**
     * Internal call back after insertion
     * @param startLine Start line
     * @param startColumn Start column
     * @param endLine End line
     * @param endColumn End column
     * @param insertedContent Inserted content
     */
    void afterInsert(int startLine, int startColumn, int endLine, int endColumn,
                     CharSequence insertedContent) {
        _indexer.afterInsert(_content, startLine, startColumn, endLine, endColumn, insertedContent);
        int beginIdx = _cache0.getIndex();
        if(getLeft() >= beginIdx) {
            _left = _indexer.getCharPosition(getLeft() + insertedContent.length()).fromThis();
        }
        if(getRight() >= beginIdx) {
            _right = _indexer.getCharPosition(getRight() + insertedContent.length()).fromThis();
        }
    }

    /**
     * Internal call back
     * @param startLine Start line
     * @param startColumn Start column
     * @param endLine End line
     * @param endColumn End column
     * @param deletedContent Deleted content
     */
    void afterDelete(int startLine, int startColumn, int endLine, int endColumn,
                     CharSequence deletedContent) {
        _indexer.afterDelete(_content, startLine, startColumn, endLine, endColumn, deletedContent);
        int beginIdx = _cache1.getIndex();
        int endIdx = _cache2.getIndex();
        int left = getLeft();
        int right = getRight();
        if(beginIdx > right) {
            return;
        }
        if(endIdx <= left) {
            _left = _indexer.getCharPosition(left - (endIdx - beginIdx)).fromThis();
            _right = _indexer.getCharPosition(right - (endIdx - beginIdx)).fromThis();
        }else if(endIdx > left && endIdx < right) {
            if(beginIdx <= left) {
                _left = _indexer.getCharPosition(beginIdx).fromThis();
                _right = _indexer.getCharPosition(right - (endIdx - Math.max(beginIdx,left))).fromThis();
            }else{
                _right = _indexer.getCharPosition(right - (endIdx - beginIdx)).fromThis();
            }
        }else{
            if(beginIdx <= left) {
                _left = _indexer.getCharPosition(beginIdx).fromThis();
                _right = _left.fromThis();
            }else{
                _right = _indexer.getCharPosition(left + (right - beginIdx)).fromThis();
            }
        }
		/*
        if(beginIdx <= left) {
            _left = _indexer.getCharPosition(left - (Math.min(endIdx, left) - beginIdx)).fromThis();
            int region = endIdx - Math.max(beginIdx, left);
            int region2 = right - left;
            int len = region2 - region;
            if(len > 0) {
                _right = _indexer.getCharPosition(getLeft() + len).fromThis();
            }else {
                _right = _left.fromThis();
            }
        }else {
            int len = Math.max(0,Math.min(endIdx, right) - beginIdx);
            if(len > 0) {
                _right = _indexer.getCharPosition(right - len).fromThis();
            }
        }*/
    }

}

