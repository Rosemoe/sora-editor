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

package io.github.rosemoe.sora.lsp.events.hover

import android.util.Log
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import java.util.concurrent.CompletableFuture

class HoverEvent : AsyncEventListener() {
    override val eventName: String = EventType.hover

    var future: CompletableFuture<Void>? = null

    override val isAsync = true

    override suspend fun handleAsync(context: EventContext) = withContext(Dispatchers.IO) {
        val editor = context.get<LspEditor>("lsp-editor")
        val position = context.getByClass<CharPosition>() ?: return@withContext

        val requestManager = editor.requestManager ?: return@withContext

        val hoverParams = HoverParams(
            editor.uri.createTextDocumentIdentifier(),
            position.asLspPosition()
        )

        val future = requestManager.hover(hoverParams) ?: return@withContext

        this@HoverEvent.future = future.thenAccept { }


        try {
            val hover: Hover?

            withTimeout(Timeout[Timeouts.HOVER].toLong()) {
                hover =
                    future.await()
            }

            editor.showHover(hover)
        } catch (exception: Exception) {
            // throw?
            exception.printStackTrace()
            Log.e("LSP client", "show hover timeout", exception)
        }
    }

    override fun dispose() {
        future?.cancel(true);
        future = null;
    }

}

val EventType.hover: String
    get() = "textDocument/hover"