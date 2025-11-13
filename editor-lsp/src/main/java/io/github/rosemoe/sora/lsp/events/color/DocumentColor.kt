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

package io.github.rosemoe.sora.lsp.events.color

import android.util.Log
import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.FileUri
import io.github.rosemoe.sora.lsp.utils.createRange
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.ColorInformation
import org.eclipse.lsp4j.DocumentColorParams
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

@OptIn(FlowPreview::class)
@Experimental
class DocumentColorEvent : AsyncEventListener() {
    override val eventName: String = EventType.documentColor

    var future: CompletableFuture<Void>? = null

    override val isAsync = true

    private val requestFlows = ConcurrentHashMap<FileUri, MutableSharedFlow<DocumentColorRequest>>()

    data class DocumentColorRequest(
        val editor: LspEditor,
        val uri: FileUri
    )

    private fun getOrCreateFlow(
        coroutineScope: CoroutineScope,
        uri: FileUri
    ): MutableSharedFlow<DocumentColorRequest> {
        return requestFlows.getOrPut(uri) {
            val flow = MutableSharedFlow<DocumentColorRequest>(
                replay = 0,
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )


            coroutineScope.launch(Dispatchers.Main) {
                flow
                    .debounce(50)
                    .collect { request ->
                        processInlayHintRequest(request)
                    }
            }

            flow
        }
    }

    override suspend fun handleAsync(context: EventContext) {
        val editor = context.get<LspEditor>("lsp-editor")
        val uri = editor.uri

        val flow = getOrCreateFlow(editor.coroutineScope, uri)
        flow.tryEmit(DocumentColorRequest(editor, uri))
    }

    private suspend fun processInlayHintRequest(request: DocumentColorRequest) =
        withContext(Dispatchers.IO) {
            val editor = request.editor

            val requestManager = editor.requestManager ?: return@withContext

            val future = requestManager.documentColor(DocumentColorParams(request.uri.createTextDocumentIdentifier())) ?: return@withContext

            this@DocumentColorEvent.future = future.thenAccept { }

            try {
                val documentColors: List<ColorInformation>?

                withTimeout(Timeout[Timeouts.DOC_HIGHLIGHT].toLong()) {
                    documentColors =
                        future.await()
                }

                if (documentColors == null || documentColors.isEmpty()) {
                    editor.showDocumentColors(null)
                    return@withContext
                }

                editor.showDocumentColors(documentColors)
            } catch (exception: Exception) {
                // throw?
                exception.printStackTrace()
                Log.e("LSP client", "show document color timeout", exception)
            }
        }

    override fun dispose() {
        future?.cancel(true)
        future = null
        requestFlows.clear()
    }

}

@get:Experimental
val EventType.documentColor: String
    get() = "textDocument/documentColor"