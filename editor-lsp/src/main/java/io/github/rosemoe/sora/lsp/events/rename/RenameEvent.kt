/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.lsp.events.rename

import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createPosition
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.RenameParams
import java.util.concurrent.CompletableFuture

class RenameEvent : AsyncEventListener() {
    override val eventName = EventType.rename

    var future: CompletableFuture<*>? = null

    override suspend fun doHandleAsync(context: EventContext) {
        val editor = context.get<LspEditor>("lsp-editor")

        val newName = context.get<String>("newName")

        val position = context.getByClass<Position>()
            ?: context.getByClass<CharPosition>()?.asLspPosition()
            ?: editor.editor?.let { createPosition(it.cursor.leftLine, it.cursor.leftColumn) }
            ?: return

        val params = RenameParams(
            editor.uri.createTextDocumentIdentifier(),
            position,
            newName
        )

        val requestFuture = editor.requestManager.rename(params) ?: return

        future = requestFuture

        val result = withTimeout(Timeout[Timeouts.RENAME, editor].toLong()) {
            requestFuture.await()
        }

        context.put("result", result)
    }

    override fun dispose() {
        future?.cancel(true)
        future = null
    }
}

val EventType.rename: String
    get() = "textDocument/rename"
