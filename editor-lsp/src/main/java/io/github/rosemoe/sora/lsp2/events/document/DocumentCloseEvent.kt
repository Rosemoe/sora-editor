/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.lsp2.events.document

import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lsp2.editor.LspEditor
import io.github.rosemoe.sora.lsp2.events.EventContext
import io.github.rosemoe.sora.lsp2.events.EventListener
import io.github.rosemoe.sora.lsp2.events.EventType
import io.github.rosemoe.sora.lsp2.events.getByClass
import io.github.rosemoe.sora.lsp2.utils.createDidChangeTextDocumentParams
import io.github.rosemoe.sora.lsp2.utils.createDidCloseTextDocumentParams
import io.github.rosemoe.sora.lsp2.utils.createDidSaveTextDocumentParams
import io.github.rosemoe.sora.lsp2.utils.createRange
import io.github.rosemoe.sora.lsp2.utils.createTextDocumentContentChangeEvent
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentSyncKind
import java.util.concurrent.CompletableFuture


class DocumentCloseEvent : EventListener {
    override val eventName = "textDocument/didClose"

    var future: CompletableFuture<Void>? = null

    override suspend fun handle(context: EventContext): EventContext {
        val editor = context.get<LspEditor>("lsp-editor")

        val params = editor.uri.createDidCloseTextDocumentParams()

        editor.requestManager?.let { requestManager ->
            future = CompletableFuture.runAsync {
                requestManager.didClose(
                    params
                )
            }?.apply {
                await()
            }
        }

        return context
    }

    override fun dispose() {
        future?.cancel(true);
        future = null;
    }


}

val EventType.documentCloseEvent: String
    get() = "textDocument/didClose"