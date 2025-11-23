package io.github.rosemoe.sora.lsp.editor

import io.github.rosemoe.sora.widget.CodeEditor

internal class LspEditorUIDelegate(private val editor: LspEditor) {

    // TODO: extract hover/signature windows, cached hints, and decorations from LspEditor.
    fun attachEditor(codeEditor: CodeEditor) {
        // Placeholder until UI responsibilities are migrated out of LspEditor.
    }

    fun detachEditor() {
        // Placeholder to release UI state.
    }
}
