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

package io.github.rosemoe.sora.lsp.editor.event

import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.requestDocumentColor
import io.github.rosemoe.sora.lsp.editor.requestInlayHint
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.diagnostics.queryDocumentDiagnostics
import io.github.rosemoe.sora.lsp.events.document.documentChange
import io.github.rosemoe.sora.lsp.events.highlight.DocumentHighlightEvent
import io.github.rosemoe.sora.lsp.events.highlight.documentHighlight
import io.github.rosemoe.sora.lsp.events.hover.hover
import io.github.rosemoe.sora.lsp.events.inlayhint.inlayHint
import io.github.rosemoe.sora.lsp.events.signature.signatureHelp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.DocumentDiagnosticReport


class LspEditorContentChangeEvent(private val editor: LspEditor) :
    EventReceiver<ContentChangeEvent> {
    override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
        if (!editor.isConnected) {
            return
        }


        editor.coroutineScope.launch(Dispatchers.IO) {
            // send to server
            editor.eventManager.emitAsync(EventType.documentChange, event)

            if (editor.hitReTrigger(event.changedText)) {
                editor.showSignatureHelp(null)
            } else {
                editor.eventManager.emitAsync(EventType.signatureHelp, event.changeStart)
            }

            editor.eventManager.emitAsync(EventType.hover, event.changeStart)

            editor.eventManager.emitAsync(EventType.documentHighlight) {
                put(
                    DocumentHighlightEvent.DocumentHighlightRequest(event.changeStart)
                )
            }

            // request inlay hint
            editor.requestInlayHint(event.changeStart)
            editor.requestDocumentColor()

            val diagnostics =
                editor.eventManager.emitAsync(EventType.queryDocumentDiagnostics)
                    .getOrNull<DocumentDiagnosticReport>("diagnostics") ?: return@launch

            if (diagnostics.isRelatedUnchangedDocumentDiagnosticReport) {
                // no-op
                return@launch
            }

            if (diagnostics.isRelatedFullDocumentDiagnosticReport) {
                val diagnosticsContainer = editor.project.diagnosticsContainer
                val fileUri = editor.uri

                diagnosticsContainer.clearDiagnostics(fileUri)
                diagnosticsContainer.addDiagnostics(
                    fileUri,
                    diagnostics.left.items
                )

                editor.onDiagnosticsUpdate()
            }
        }


    }
}

