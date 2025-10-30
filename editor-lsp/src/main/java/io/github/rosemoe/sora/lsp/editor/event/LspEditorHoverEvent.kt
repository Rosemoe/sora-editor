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

import android.view.MotionEvent
import io.github.rosemoe.sora.event.EventReceiver
import io.github.rosemoe.sora.event.HoverEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.hover.hover
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.ViewUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class LspEditorHoverEvent(private val editor: LspEditor) :
    EventReceiver<HoverEvent> {
    private var lastHoverPos = 0f to 0f

    private var hoverPosition: CharPosition? = null

    override fun onReceive(e: HoverEvent, unsubscribe: Unsubscribe) {
        if (!editor.isConnected) {
            return
        }

        val originEditor = editor.editor ?: return

        val callback = Runnable {
            val pos = hoverPosition
            pos?.let { updateHoverPosition(it) }
        }

        fun postUpdate(delay: Long = ViewUtils.HOVER_TOOLTIP_SHOW_TIMEOUT) {
            originEditor.removeCallbacks(callback)
            originEditor.postDelayedInLifecycle(callback, delay)
        }

        if (originEditor.isInMouseMode) {
            fun updateLastHover() {
                lastHoverPos = e.x to e.y
            }
            when (e.causingEvent.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    originEditor.removeCallbacks(callback)
                    updateHoverPosition(hoverPosition)
                    updateLastHover()
                }

                MotionEvent.ACTION_HOVER_EXIT -> {
                    hoverPosition = null
                }

                MotionEvent.ACTION_HOVER_MOVE -> {
                    if (originEditor.isScreenPointOnText(e.x, e.y)) {
                        if (abs(e.x - lastHoverPos.first) > ViewUtils.HOVER_TAP_SLOP || abs(
                                e.y - lastHoverPos.second
                            ) > ViewUtils.HOVER_TAP_SLOP
                        ) {
                            updateLastHover()
                            val pos = originEditor.getPointPositionOnScreen(e.x, e.y)
                            hoverPosition = originEditor.text.indexer.getCharPosition(
                                IntPair.getFirst(pos),
                                IntPair.getSecond(pos)
                            )
                            postUpdate()
                        }
                    } else {
                        hoverPosition = null
                        postUpdate()
                    }
                }
            }
        }


    }

    fun updateHoverPosition(pos: CharPosition?) {
        if (pos == null) return
        editor.coroutineScope.launch(Dispatchers.IO) {
            editor.eventManager.emitAsync(EventType.hover, pos)
        }
    }
}