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
package com.rose.editor.simpleclass;


import com.rose.editor.common.Content;
import com.rose.editor.interfaces.ContentListener;

/**
 * @author Rose
 *
 */
public class SingleLineSpan implements ContentListener{

    private int line,begin,end;
    private boolean set = false;

    public void set(int l,int b,int e) {
        line = l;
        begin = b;
        end = e;
        set = b != e;
    }

    /**
     * Set this single line span invalid
     */
    public void unset() {
        set = false;
    }

    /**
     * Whether this single line span is valid
     * @return
     */
    public boolean isSet() {
        return set;
    }

    /* Getters */

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public int getLine() {
        return line;
    }

    @Override
    public void beforeReplace(Content content) {
        //Do nothing
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence insertedContent) {
        //If we are unset or the modification is not on single line,we should cancel and make self unset
        if(isSet() && startLine == line && startLine == endLine && startColumn == end) {
            end += endColumn - startColumn;
        }else {
            unset();
        }
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn,
                            CharSequence deletedContent) {
        if(isSet() && startLine == line && endColumn == end && startLine == endLine) {
            end -= endColumn - startColumn;
            if(begin >= end) {
                unset();
            }
        }else {
            unset();
        }
    }

}
