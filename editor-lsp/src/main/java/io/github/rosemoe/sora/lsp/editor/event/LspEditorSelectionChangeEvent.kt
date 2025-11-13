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

package io.github.rosemoe.sora.lsp.editor.event

import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.highlight.DocumentHighlightEvent
import io.github.rosemoe.sora.lsp.events.highlight.documentHighlight
import io.github.rosemoe.sora.lsp.events.hover.hover
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LspEditorSelectionChangeEvent(private val editor: LspEditor) :
    EventReceiver<SelectionChangeEvent> {
    override fun onReceive(event: SelectionChangeEvent, unsubscribe: Unsubscribe) {
        if (!editor.isConnected) {
            return
        }

        editor.showSignatureHelp(null)
        editor.showHover(null)

        editor.coroutineScope.launch(Dispatchers.IO) {
            editor.eventManager.emitAsync(EventType.documentHighlight) {
                put(
                    DocumentHighlightEvent.DocumentHighlightRequest(
                        event.left.fromThis()
                    )
                )
            }
        }

        val originEditor = editor.editor ?: return

        val hoverWindow = editor.hoverWindow ?: return

        val isInCompletion = originEditor.getComponent<EditorAutoCompletion>().isShowing

        if ((!originEditor.hasMouseHovering() && (!hoverWindow.alwaysShowOnTouchHover || event.isSelected)) || isInCompletion) {
            return
        }

        editor.coroutineScope.launch(Dispatchers.IO) {
            editor.eventManager.emitAsync(EventType.hover, event.left)
        }
    }
}
