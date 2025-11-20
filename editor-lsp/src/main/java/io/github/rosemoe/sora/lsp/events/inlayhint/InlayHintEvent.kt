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

package io.github.rosemoe.sora.lsp.events.inlayhint

import android.util.Log
import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
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
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.InlayHintParams
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

@OptIn(FlowPreview::class)
@Experimental
class InlayHintEvent : AsyncEventListener() {
    override val eventName: String = EventType.inlayHint

    var future: CompletableFuture<Void>? = null

    override val isAsync = true

    private val requestFlows = ConcurrentHashMap<String, MutableSharedFlow<InlayHintRequest>>()

    data class InlayHintRequest(
        val editor: LspEditor,
        val position: CharPosition
    )

    private fun getOrCreateFlow(
        coroutineScope: CoroutineScope,
        uri: String
    ): MutableSharedFlow<InlayHintRequest> {
        return requestFlows.getOrPut(uri) {
            val flow = MutableSharedFlow<InlayHintRequest>(
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
        val position = context.getByClass<CharPosition>() ?: return

        val uri = editor.uri.toString()

        val flow = getOrCreateFlow(editor.coroutineScope, uri)
        flow.tryEmit(InlayHintRequest(editor, position))
    }

    private suspend fun processInlayHintRequest(request: InlayHintRequest) =
        withContext(Dispatchers.IO) {
            val editor = request.editor
            val position = request.position
            val content = editor.editor?.text ?: return@withContext

            val requestManager = editor.requestManager ?: return@withContext

            // Request over 500 lines for current window

            val upperLine = max(0, position.line - 500)
            val lowerLine = min(content.lineCount - 1, position.line + 500)

            val inlayHintParams = InlayHintParams(
                editor.uri.createTextDocumentIdentifier(),
                createRange(
                    CharPosition(upperLine, 0),
                    CharPosition(
                        lowerLine,
                        content.getColumnCount(
                            lowerLine
                        )
                    )
                )
            )

            val future = requestManager.inlayHint(inlayHintParams) ?: return@withContext

            this@InlayHintEvent.future = future.thenAccept { }


            try {
                val inlayHints: List<InlayHint>?

                withTimeout(Timeout[Timeouts.INLAY_HINT].toLong()) {
                    inlayHints = future.await()
                }

                if (inlayHints == null || inlayHints.isEmpty()) {
                    editor.showInlayHints(null)
                    return@withContext
                }

                editor.showInlayHints(inlayHints)
            } catch (exception: Exception) {
                // throw?
                exception.printStackTrace()
                Log.e("LSP client", "show inlay hint timeout", exception)
            }
        }

    override fun dispose() {
        future?.cancel(true)
        future = null
        requestFlows.clear()
    }

}

@get:Experimental
val EventType.inlayHint: String
    get() = "textDocument/inlayHint"