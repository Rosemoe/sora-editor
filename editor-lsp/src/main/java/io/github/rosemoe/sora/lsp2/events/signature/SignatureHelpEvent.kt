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

package io.github.rosemoe.sora.lsp2.events.signature

import android.util.Log
import com.google.gson.Gson
import io.github.rosemoe.sora.lsp2.requests.Timeout
import io.github.rosemoe.sora.lsp2.requests.Timeouts
import io.github.rosemoe.sora.lsp2.editor.LspEditor
import io.github.rosemoe.sora.lsp2.events.EventContext
import io.github.rosemoe.sora.lsp2.events.EventListener
import io.github.rosemoe.sora.lsp2.events.EventType
import io.github.rosemoe.sora.lsp2.events.getByClass
import io.github.rosemoe.sora.lsp2.utils.createTextDocumentIdentifier
import io.github.rosemoe.sora.lsp2.utils.toPosition
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureHelpParams
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class SignatureHelpEvent : EventListener {
    override val eventName: String = "textDocument/signatureHelp"

    var future: CompletableFuture<Void>? = null

    override suspend fun handle(context: EventContext): EventContext {
        val editor = context.get<LspEditor>("lsp-editor")
        val position = context.getByClass<CharPosition>() ?: return context

        val requestManager = editor.requestManager ?: return context

        val signatureHelpParams = SignatureHelpParams(
            editor.uri.createTextDocumentIdentifier(),
            position.toPosition()
        )

        val future = requestManager.signatureHelp(signatureHelpParams) ?: return context

        this.future = future.thenAccept { }

        try {
            withTimeout(Timeout[Timeouts.SIGNATURE].toLong()) {
                val signatureHelp =
                    future.await()
                editor.showSignatureHelp(signatureHelp)
            }
        } catch (exception: Exception) {
            // throw?
            exception.printStackTrace()
            Log.e("LSP client", "show signatureHelp timeout", exception)
        }

        return context
    }

    override fun dispose() {
        future?.cancel(true);
        future = null;
    }

}

val EventType.signatureHelp: String
    get() = "textDocument/signatureHelp"