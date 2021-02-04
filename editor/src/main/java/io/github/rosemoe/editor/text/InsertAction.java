/*
 *   Copyright 2020-2021 Rosemoe
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
package io.github.rosemoe.editor.text;


/**
 * Insert action model for UndoManager
 *
 * @author Rose
 */
public final class InsertAction implements ContentAction {

    public int startLine, endLine, startColumn, endColumn;

    public CharSequence text;

    @Override
    public void undo(Content content) {
        content.delete(startLine, startColumn, endLine, endColumn);
    }

    @Override
    public void redo(Content content) {
        content.insert(startLine, startColumn, text);
    }

    @Override
    public boolean canMerge(ContentAction action) {
        if (action instanceof InsertAction) {
            InsertAction ac = (InsertAction) action;
            return (ac.startColumn == endColumn && ac.startLine == endLine && ac.text.length() + text.length() < 10000);
        }
        return false;
    }

    @Override
    public void merge(ContentAction action) {
        if (!canMerge(action)) {
            throw new IllegalArgumentException();
        }
        InsertAction ac = (InsertAction) action;
        this.endColumn = ac.endColumn;
        this.endLine = ac.endLine;
        StringBuilder sb;
        if (text instanceof StringBuilder) {
            sb = (StringBuilder) text;
        } else {
            sb = new StringBuilder(text);
            text = sb;
        }
        sb.append(ac.text);
    }

    @Override
    public String toString() {
        return "{InsertAction : Start = " + startLine + "," + startColumn +
                " End = " + endLine + "," + endColumn + "\nContent = " + text + "}";
    }

}

