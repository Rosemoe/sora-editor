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

package io.github.rosemoe.sora.compose.component

import android.graphics.RectF
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.LocalEditorColorScheme
import io.github.rosemoe.sora.compose.LocalEditorFontFamily
import io.github.rosemoe.sora.compose.internal.createPopupLayout
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.DragSelectStopEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.HandleStateChangeEvent
import io.github.rosemoe.sora.event.InterceptTarget
import io.github.rosemoe.sora.event.LongPressEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.subscribeAlways
import io.github.rosemoe.sora.widget.EditorTouchEventHandler
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlin.math.max
import kotlin.math.min

internal fun CodeEditorState.createTextActionWindow(
    content: @Composable (CodeEditorState) -> Unit
): TextActionWindow {
    val window = TextActionWindow(this)
    window.content = content
    return window
}

internal class TextActionWindow(
    val state: CodeEditorState,
) : EditorPopupWindow(state.delegate, state.host, FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED), EditorBuiltinComponent {

    companion object {
        private const val DELAY = 200L
        private const val CHECK_FOR_DISMISS_INTERVAL = 100L
    }

    private val density = Density(host.context)
    private val touchHandler: EditorTouchEventHandler = editor.eventHandler
    private val eventManager = editor.createSubEventManager()

    private var lastScroll = 0L
    private var lastPosition = 0
    private var lastCause = 0
    private var enabled = true

    private var size by mutableStateOf(
        IntSize(
            (editor.dpUnit * 230).toInt(),
            (editor.dpUnit * 48).toInt()
        )
    )
    private var colorScheme by mutableStateOf(editor.colorScheme)

    var content: @Composable (CodeEditorState) -> Unit by mutableStateOf({})

    init {
        applyColorScheme(editor.colorScheme)
        setContentView(host.attachedView.createPopupLayout(::Content))
        setSize(0, size.height)
        popup.animationStyle = io.github.rosemoe.sora.R.style.text_action_popup_animation
        subscribeEvents()
    }

    @Composable
    private fun Content() {
        CompositionLocalProvider(
            LocalEditorColorScheme provides colorScheme,
            LocalEditorFontFamily provides FontFamily(editor.typefaceText),
        ) {
            SubcomposeLayout {
                val placeables = subcompose("content", { content(state) }).map {
                    it.measure(
                        Constraints(
                            minWidth = 0,
                            minHeight = 0,
                            maxWidth = Int.MAX_VALUE,
                            maxHeight = Int.MAX_VALUE
                        )
                    )
                }

                val width = placeables.maxOfOrNull { it.width } ?: 0
                val height = placeables.maxOfOrNull { it.height } ?: 0

                if (size.width != width || size.height != height) {
                    size = IntSize(width, height)
                    setSize(width, height)
                    editor.postInLifecycle(::displayWindow)
                }

                layout(width, height) {
                    placeables.fastForEach {
                        it.place(0, 0)
                    }
                }
            }
        }
    }

    private fun subscribeEvents() {
        eventManager.subscribeAlways<SelectionChangeEvent>(::onSelectionChange)
        eventManager.subscribeAlways<ScrollEvent>(::onScroll)
        eventManager.subscribeAlways<HandleStateChangeEvent>(::onHandleStateChange)
        eventManager.subscribeAlways<LongPressEvent>(::onLongPress)
        eventManager.subscribeAlways<EditorFocusChangeEvent>(::onFocusChange)
        eventManager.subscribeAlways<EditorReleaseEvent>(::onRelease)
        eventManager.subscribeAlways<ColorSchemeUpdateEvent>(::onColorChange)
        eventManager.subscribeAlways<DragSelectStopEvent>(::onDragSelectingStop)
    }

    private fun onSelectionChange(event: SelectionChangeEvent) {
        if (touchHandler.hasAnyHeldHandle() || event.cause == SelectionChangeEvent.CAUSE_DEAD_KEYS) {
            return
        }

        if (touchHandler.isDragSelecting) {
            dismiss()
            return
        }
        lastCause = event.cause

        if (event.isSelected || event.cause == SelectionChangeEvent.CAUSE_LONG_PRESS && editor.text.isEmpty()) {
            // Always post show. See #193
            if (event.cause != SelectionChangeEvent.CAUSE_SEARCH) {
                editor.postInLifecycle(::displayWindow)
            } else {
                dismiss()
            }
            lastPosition = -1
        } else {
            var show = false
            if (event.cause == SelectionChangeEvent.CAUSE_TAP && event.left.index == lastPosition && !isShowing && !editor.text.isInBatchEdit && editor.isEditable) {
                editor.postInLifecycle(::displayWindow)
                show = true
            } else {
                dismiss()
            }

            lastPosition = if (event.cause == SelectionChangeEvent.CAUSE_TAP && !show) {
                event.left.index
            } else {
                -1
            }
        }
    }

    private fun onScroll(event: ScrollEvent) {
        val last = lastScroll
        lastScroll = System.currentTimeMillis()
        if (lastScroll - last < DELAY && lastCause != SelectionChangeEvent.CAUSE_SEARCH) {
            postDisplay()
        }
    }

    private fun onHandleStateChange(event: HandleStateChangeEvent) {
        if (event.isHeld) {
            postDisplay()
        }

        if (!event.editor.cursor.isSelected
            && event.handleType == HandleStateChangeEvent.HANDLE_TYPE_INSERT
            && !event.isHeld
        ) {
            displayWindow()
            // Also, post to hide the window on handle disappearance
            host.postDelayedInLifecycle(object : Runnable {
                override fun run() {
                    if (!editor.eventHandler.shouldDrawInsertHandle() && !editor.cursor.isSelected) {
                        dismiss()
                    } else if (!editor.cursor.isSelected) {
                        host.postDelayedInLifecycle(this, CHECK_FOR_DISMISS_INTERVAL)
                    }
                }
            }, CHECK_FOR_DISMISS_INTERVAL)
        }
    }

    private fun onLongPress(event: LongPressEvent) {
        if (editor.cursor.isSelected && lastCause == SelectionChangeEvent.CAUSE_SEARCH) {
            val idx = event.index
            if (idx >= editor.cursor.left && idx <= editor.cursor.right) {
                lastCause = 0
                displayWindow()
            }
            event.intercept(InterceptTarget.TARGET_EDITOR)
        }
    }

    private fun onFocusChange(event: EditorFocusChangeEvent) {
        if (!event.isGainFocus) dismiss()
    }

    private fun onRelease(event: EditorReleaseEvent) {
        isEnabled = false
    }

    private fun onColorChange(event: ColorSchemeUpdateEvent) {
        applyColorScheme(event.colorScheme)
    }

    private fun onDragSelectingStop(event: DragSelectStopEvent) {
        displayWindow()
    }

    private fun applyColorScheme(colorScheme: EditorColorScheme) {
        this.colorScheme = colorScheme
    }

    private fun selectTop(rect: RectF): Int {
        val rowHeight = editor.getRowHeight()
        return if (rect.top - rowHeight * 3 / 2f > height) {
            (rect.top - rowHeight * 3 / 2 - height).toInt()
        } else {
            (rect.bottom + rowHeight / 2).toInt()
        }
    }

    private fun displayWindow() {
        updateStates()
        var top: Int
        val cursor = editor.cursor
        if (cursor.isSelected) {
            val leftRect = editor.leftHandleDescriptor.position
            val rightRect = editor.rightHandleDescriptor.position
            val top1 = selectTop(leftRect)
            val top2 = selectTop(rightRect)
            top = min(top1, top2)
        } else {
            top = selectTop(editor.insertHandleDescriptor.position)
        }

        top = max(0, min(top, editor.height - height - 5))
        val handleLeftX = editor.getOffset(editor.cursor.leftLine, editor.cursor.leftColumn)
        val handleRightX = editor.getOffset(editor.cursor.rightLine, editor.cursor.rightColumn)
        val panelX = ((handleLeftX + handleRightX) / 2f - size.width / 2f).toInt()
        setLocationAbsolutely(panelX, top)
        show()
    }

    private fun updateStates() {

    }

    override fun show() {
        if (!enabled || editor.snippetController.isInSnippet() || !host.isFocused || editor.isInMouseMode) {
            return
        }
        super.show()
    }

    private fun postDisplay() {
        if (!isShowing) return
        dismiss()

        if (!editor.cursor.isSelected) return
        host.postDelayedInLifecycle(object : Runnable {
            override fun run() {
                if (!touchHandler.hasAnyHeldHandle()
                    && !editor.snippetController.isInSnippet()
                    && System.currentTimeMillis() - lastScroll > DELAY
                    && editor.scroller.isFinished
                ) {
                    displayWindow()
                } else {
                    host.postDelayedInLifecycle(this, DELAY)
                }
            }
        }, DELAY)
    }

    override fun isEnabled() = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        eventManager.isEnabled = enabled
        if (!enabled) dismiss()
    }
}
