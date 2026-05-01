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

import android.view.GestureDetector
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.debugInspectorInfo
import io.github.rosemoe.sora.compose.internal.CodeEditorHostImpl
import io.github.rosemoe.sora.widget.CodeEditorDelegate
import io.github.rosemoe.sora.widget.DirectAccessProps

internal fun Modifier.editorTouchEvents(
    host: CodeEditorHostImpl,
    delegate: CodeEditorDelegate,
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "editorTouchEvents"
        properties["host"] = host
        properties["delegate"] = delegate
    }
) {
    val context = LocalContext.current

    val detectors = remember(context, host, delegate) {
        val gestureDetector = GestureDetector(context, delegate.touchHandler)
        gestureDetector.setOnDoubleTapListener(delegate.touchHandler)
        val scaleDetector = ScaleGestureDetector(context, delegate.touchHandler)
        scaleDetector.isQuickScaleEnabled = false
        EditorDetectors(gestureDetector, scaleDetector)
    }

    var downX by remember { mutableFloatStateOf(0f) }

    pointerInteropFilter(requestDisallowInterceptTouchEvent = host.disallowInterceptTouchEvent) { motionEvent ->
        if (!host.isEnabled) return@pointerInteropFilter false

        val x = motionEvent.x
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                host.requestFocus()

                if (delegate.isInterceptParentHorizontalScrollEnabled) {
                    host.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - downX
                if (delegate.isInterceptParentHorizontalScrollEnabled && !delegate.touchHandler.hasAnyHeldHandle()) {
                    val currX = delegate.scroller.currX

                    if ((deltaX > 0 && currX == 0) || (deltaX < 0 && currX == delegate.scrollMaxX)) {
                        host.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
        }

        if (motionEvent.isFromSource(InputDevice.SOURCE_MOUSE) && delegate.props.mouseMode != DirectAccessProps.MOUSE_MODE_NEVER) {
            return@pointerInteropFilter delegate.touchHandler.onMouseEvent(motionEvent)
        }

        if (delegate.isFormatting) {
            delegate.touchHandler.reset2()
            detectors.scaleDetector.onTouchEvent(motionEvent)
            return@pointerInteropFilter detectors.gestureDetector.onTouchEvent(motionEvent)
        }

        val handlingBefore = delegate.touchHandler.handlingMotions()
        val res = delegate.touchHandler.onTouchEvent(motionEvent)
        val handling = delegate.touchHandler.handlingMotions()
        var res2 = false
        val res3 = detectors.scaleDetector.onTouchEvent(motionEvent)

        if (!handling && !handlingBefore) {
            res2 = detectors.gestureDetector.onTouchEvent(motionEvent)
        }

        val action = motionEvent.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            delegate.horizontalEdgeEffect?.onRelease()
            delegate.verticalEdgeEffect?.onRelease()
        }

        res3 || res2 || res
    }
}

private data class EditorDetectors(
    val gestureDetector: GestureDetector,
    val scaleDetector: ScaleGestureDetector
)
