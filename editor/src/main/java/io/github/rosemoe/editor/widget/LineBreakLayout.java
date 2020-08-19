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
package io.github.rosemoe.editor.widget;

import io.github.rosemoe.editor.text.Content;
import io.github.rosemoe.editor.struct.CharPosition;
import io.github.rosemoe.editor.text.ContentLine;

//Work in progress
class LineBreakLayout implements Layout {

    @Override
    public RowIterator obtainRowIterator(int initialRow) {
        // TODO: Implement this method
        return null;
    }

    @Override
    public void beforeReplace(Content content) {
        
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent) {
        // TODO: Implement this method
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {
        // TODO: Implement this method
    }

    @Override
    public void onRemove(Content content, ContentLine line) {
        // TODO: Implement this method
    }

    @Override
    public void destroyLayout() {
        // TODO: Implement this method
    }

    @Override
    public int getLineNumberForRow(int row) {
        // TODO: Implement this method
        return 0;
    }

    @Override
    public int getLayoutWidth() {
        // TODO: Implement this method
        return 0;
    }

    @Override
    public int getLayoutHeight() {
        return text.getLineCount() * editor.getRowHeight();
    }

    @Override
    public CharPosition getCharPositionForLayoutOffset(float xOffset, float yOffset) {
        int lineCount = text.getLineCount();
        int line = Math.min(lineCount - 1, Math.max((int)(yOffset / editor.getRowHeight()), 0));
        
        return null;
    }

    @Override
    public float[] getCharLayoutOffset(int line, int column) {
        // TODO: Implement this method
        return null;
    }
    
    private final CodeEditor editor;
    private final Content text;
    
    LineBreakLayout(CodeEditor editor, Content text) {
        this.editor = editor;
        this.text = text;
        
    }
    
    
    
}
