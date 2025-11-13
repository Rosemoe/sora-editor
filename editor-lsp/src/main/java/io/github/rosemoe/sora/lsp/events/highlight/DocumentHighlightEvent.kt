/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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

package io.github.rosemoe.sora.lsp.events.highlight

import android.util.Log
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.createRange
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightParams
import java.util.concurrent.CompletableFuture

class DocumentHighlightEvent : AsyncEventListener() {
    override val eventName: String = EventType.documentHighlight

    var future: CompletableFuture<Void>? = null

    override val isAsync = true

    data class DocumentHighlightRequest(
        val selectionStart: CharPosition,
    )

    override suspend fun handleAsync(context: EventContext) = withContext(Dispatchers.IO) {
        val editor = context.get<LspEditor>("lsp-editor")
        val request = context.getByClass<DocumentHighlightRequest>() ?: return@withContext

        val requestManager = editor.requestManager ?: return@withContext

        val params = DocumentHighlightParams(
            editor.uri.createTextDocumentIdentifier(),
            request.selectionStart.asLspPosition()
        )

        val future = requestManager.documentHighlight(params) ?: return@withContext

        this@DocumentHighlightEvent.future = future.thenAccept { }

        try {
            val documentHighlights: List<DocumentHighlight>?

            withTimeout(Timeout[Timeouts.DOC_HIGHLIGHT].toLong()) {
                documentHighlights = future.await()
            }

            editor.showDocumentHighlight(documentHighlights)
        } catch (exception: Exception) {
            exception.printStackTrace()
            Log.e("LSP client", "show document highlight timeout", exception)
        }
    }

    override fun dispose() {
        future?.cancel(true)
        future = null
    }
}

val EventType.documentHighlight: String
    get() = "textDocument/documentHighlight"
