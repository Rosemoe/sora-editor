package io.github.rosemoe.editor.widget;

import io.github.rosemoe.editor.text.Content;

class LineBreakLayout /*implements Layout*/ {
    
    private final CodeEditor editor;
    
    private final Content text;
    
    LineBreakLayout(CodeEditor editor, Content text) {
        this.editor = editor;
        this.text = text;
    }
    
}
