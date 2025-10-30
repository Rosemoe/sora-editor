package io.github.rosemoe.sora.lsp.editor.codeaction

import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.jsonrpc.messages.Either

/**
 * Presentation model for an LSP code action entry that the popup can display.
 */
data class CodeActionItem(
    val title: String,
    val action: Either<Command, CodeAction>,
) {
    companion object {
        fun from(source: Either<Command, CodeAction>): CodeActionItem {
            val title = if (source.isLeft) {
                source.left?.title ?: ""
            } else {
                source.right?.title ?: ""
            }
            return CodeActionItem(title = title, action = source)
        }
    }
}
