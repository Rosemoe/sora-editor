/*
 *   Copyright 2020 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.struct;


import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.text.ContentAction;

/**
 * Delete action model for UndoManager
 * @author Rose
 */
public final class DeleteAction implements ContentAction {

    public int startLine,endLine,startColumn,endColumn;

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
        if(action instanceof DeleteAction) {
            DeleteAction ac = (DeleteAction)action;
            return (ac.endColumn == startColumn && ac.endLine == startLine && ac.text.length() + text.length() < 10000);
        }
        return false;
    }

    @Override
    public void merge(ContentAction action) {
        if(!canMerge(action)) {
            throw new IllegalArgumentException();
        }
        DeleteAction ac = (DeleteAction)action;
        this.startColumn = ac.startColumn;
        this.startLine = ac.startLine;
        StringBuilder sb;
        if(text instanceof StringBuilder) {
            sb = (StringBuilder) text;
        }else {
            sb = new StringBuilder(text);
            text = sb;
        }
        sb.insert(0, ac.text);
    }

    @Override
    public String toString()
    {
        return "{DeleteAction : Start = " + startLine + "," + startColumn +
                " End = " + endLine + "," + endColumn + "\nContent = " + text +"}";
    }

}

