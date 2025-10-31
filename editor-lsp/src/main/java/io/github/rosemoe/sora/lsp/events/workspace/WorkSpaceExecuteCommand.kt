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

package io.github.rosemoe.sora.lsp.events.workspace

import android.util.Log
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventListener
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.document.applyEdits
import io.github.rosemoe.sora.lsp.events.get
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.FileUri
import io.github.rosemoe.sora.lsp.utils.LSPException
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.lsp.utils.toFileUri
import io.github.rosemoe.sora.lsp.utils.toURI
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.ResourceOperation
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.TextDocumentEdit
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.util.concurrent.CompletableFuture


class WorkSpaceExecuteCommand : AsyncEventListener() {
    override val eventName: String = EventType.workSpaceExecuteCommand

    override val isAsync = true

    var future: CompletableFuture<Void>? = null

    override suspend fun handleAsync(context: EventContext) {
        val command = context.get<String>("command")
        val args = context.get<List<Any>>("args")

        val editor = context.get<LspEditor>("lsp-editor")
        val requestManager = editor.requestManager ?: return
        val executeCommandParams = ExecuteCommandParams(command, args)
        val future = requestManager.executeCommand(executeCommandParams)

        this@WorkSpaceExecuteCommand.future = future?.thenAccept { }

        try {
            val result: Any?

            withTimeout(Timeout[Timeouts.EXECUTE_COMMAND].toLong()) {
                result =
                    future?.await()
            }

            context.put("result", result)

        } catch (exception: Exception) {
            // throw?
            exception.printStackTrace()
            Log.e("LSP client", "workspace execute command timeout", exception)
        }
    }

    override fun dispose() {
        future?.cancel(true);
        future = null;
    }
}

val EventType.workSpaceExecuteCommand: String
    get() = "workspace/executeCommand"