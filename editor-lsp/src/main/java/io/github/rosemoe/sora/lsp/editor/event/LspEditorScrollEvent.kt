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
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.requestInlayHint
import io.github.rosemoe.sora.text.CharPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class LspEditorScrollEvent(private val editor: LspEditor) :
    EventReceiver<ScrollEvent> {

    private val scrollEventFlow = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    init {
        editor.coroutineScope.launch(Dispatchers.Main) {
            scrollEventFlow
                .debounce(50) // Debounce for 50ms to drop rapid events
                .collect { firstVisibleLine ->
                    // request inlay hint
                    editor.requestInlayHint(
                        CharPosition(firstVisibleLine, 0)
                    )
                }
        }
    }

    override fun onReceive(event: ScrollEvent, unsubscribe: Unsubscribe) {
        if (!editor.isConnected) {
            return
        }

        val firstVisibleLine = event.editor.firstVisibleLine
        // Older events will be dropped if buffer is full
        scrollEventFlow.tryEmit(firstVisibleLine)
    }
}