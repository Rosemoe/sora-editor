package io.github.rosemoe.sora.lsp2.events.document

import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lsp2.editor.LspEditor
import io.github.rosemoe.sora.lsp2.events.AsyncEventListener
import io.github.rosemoe.sora.lsp2.events.EventContext
import io.github.rosemoe.sora.lsp2.events.EventListener
import io.github.rosemoe.sora.lsp2.events.EventType
import io.github.rosemoe.sora.lsp2.events.getByClass
import io.github.rosemoe.sora.lsp2.utils.createDidChangeTextDocumentParams
import io.github.rosemoe.sora.lsp2.utils.createDidSaveTextDocumentParams
import io.github.rosemoe.sora.lsp2.utils.createRange
import io.github.rosemoe.sora.lsp2.utils.createTextDocumentContentChangeEvent
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentSyncKind
import java.util.concurrent.CompletableFuture


class DocumentSaveEvent : AsyncEventListener() {
    override val eventName = "textDocument/didSave"

    var future: CompletableFuture<Void>? = null

    override suspend fun handleAsync(context: EventContext) {
        val editor = context.get<LspEditor>("lsp-editor")

        val params = editor.createDidSaveTextDocumentParams()

        editor.requestManager?.let { requestManager ->
            future = CompletableFuture.runAsync {
                requestManager.didSave(
                    params
                )
            }?.apply {
                await()
            }
        }
    }

    override fun dispose() {
        future?.cancel(true);
        future = null;
    }


}