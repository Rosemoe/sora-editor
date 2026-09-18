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

package io.github.rosemoe.sora.compose.internal.modifier

import android.view.MotionEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import io.github.rosemoe.sora.event.HoverEvent
import io.github.rosemoe.sora.widget.CodeEditorDelegate

internal fun Modifier.editorMouseEvents(
    delegate: CodeEditorDelegate
) = pointerInput(delegate) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val type = event.type
            val modifiers = event.keyboardModifiers
            val change = event.changes.first()

            if (change.type != PointerType.Mouse) continue

            when (type) {
                PointerEventType.Enter -> delegate.mouseHover = true
                PointerEventType.Exit -> delegate.mouseHover = false

                PointerEventType.Press -> delegate.mouseButtonPressed = true
                PointerEventType.Release -> delegate.mouseButtonPressed = false
            }

            if (type == PointerEventType.Enter || type == PointerEventType.Move || type == PointerEventType.Exit) {
                val action = when (type) {
                    PointerEventType.Enter -> MotionEvent.ACTION_HOVER_ENTER
                    PointerEventType.Exit -> MotionEvent.ACTION_HOVER_EXIT
                    else -> MotionEvent.ACTION_HOVER_MOVE
                }

                val nativeEvent = MotionEvent.obtain(
                    change.uptimeMillis,
                    change.uptimeMillis,
                    action,
                    change.position.x,
                    change.position.y,
                    0
                )

                delegate.touchHandler.dispatchEditorMotionEvent(
                    ::HoverEvent,
                    null,
                    nativeEvent
                )

                nativeEvent.recycle()
            }

            if (type == PointerEventType.Scroll && !modifiers.isCtrlPressed) {
                val delta = change.scrollDelta

                var distanceX = delta.x * delegate.verticalScrollFactor * delegate.props.mouseWheelScrollFactor
                var distanceY = delta.y * delegate.verticalScrollFactor * delegate.props.mouseWheelScrollFactor

                if (modifiers.isAltPressed) {
                    val multiplier = delegate.props.fastScrollSensitivity
                    distanceX *= multiplier
                    distanceY *= multiplier
                }

                if (modifiers.isShiftPressed) {
                    val tmp = distanceX
                    distanceX = distanceY
                    distanceY = tmp
                }

                val scrollEvent = MotionEvent.obtain(
                    change.uptimeMillis,
                    change.uptimeMillis,
                    MotionEvent.ACTION_SCROLL,
                    change.position.x,
                    change.position.y,
                    0
                )

                delegate.touchHandler.onScroll(scrollEvent, scrollEvent, distanceX, distanceY)

                scrollEvent.recycle()
                change.consume()
            }
        }
    }
}
