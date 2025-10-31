/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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

package io.github.rosemoe.sora.widget.component

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.HoverEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.event.subscribeAlways
import io.github.rosemoe.sora.event.subscribeEvent
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.util.ViewUtils
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.getComponent
import kotlin.math.PI
import kotlin.math.abs

open class EditorDiagnosticTooltipWindow(editor: CodeEditor) : EditorPopupWindow(editor, FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED), EditorBuiltinComponent {

    protected val eventManager = editor.createSubEventManager()
    private lateinit var rootView: View
    private var layoutImpl: DiagnosticTooltipLayout = DefaultDiagnosticTooltipLayout()
    protected var memorizedPosition: CharPosition? = null
    protected var currentDiagnostic: DiagnosticDetail? = null
        private set
    protected var currentRegion: DiagnosticRegion? = null
        private set
    protected var maxHeight = (editor.dpUnit * 175).toInt()
    private val diagnosticList = mutableListOf<DiagnosticRegion>()
    private val buffer = FloatArray(2)
    private val locationBuffer = IntArray(2)
    private var hoverPosition: CharPosition? = null
    private var lastHoverPos = 0f to 0f

    var layout: DiagnosticTooltipLayout
        get() = layoutImpl
        set(value) {
            if (::rootView.isInitialized && layoutImpl === value) {
                return
            }
            layoutImpl = value
            layoutImpl.attach(this)
            rootView = layoutImpl.createView(LayoutInflater.from(editor.context))
            super.setContentView(rootView)
            applyColorScheme()
            currentDiagnostic?.let {
                layoutImpl.renderDiagnostic(it, currentRegion)
                if (isShowing) {
                    updateWindowSize()
                    updateWindowPosition()
                }
            }
        }

    override fun isEnabled() = eventManager.isEnabled

    override fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    init {
        layout = layoutImpl
        popup.animationStyle = R.style.diagnostic_popup_animation
        registerEditorEvents()
        popup.setOnDismissListener {
            currentDiagnostic = null
            currentRegion = null
            layoutImpl.onWindowDismissed()
        }
        applyColorScheme()
    }

    private fun registerEditorEvents() {
        eventManager.subscribeEvent<SelectionChangeEvent> { event, _ ->
            if (!isEnabled || editor.isInMouseMode) {
                return@subscribeEvent
            }
            if (event.isSelected || (event.cause != SelectionChangeEvent.CAUSE_TAP && event.cause != SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)) {
                updateDiagnostic(null, null, null)
                return@subscribeEvent
            }
            updateDiagnostic(event.left)
        }
        eventManager.subscribeEvent<ScrollEvent> { _, _ ->
            if (editor.isInMouseMode) {
                return@subscribeEvent
            }
            if (currentDiagnostic != null && isShowing) {
                if (!isSelectionVisible()) {
                    dismiss()
                } else {
                    updateWindowPosition()
                }
            }
        }
        val callback = Runnable {
            val pos = hoverPosition
            if (popup.isShowing) {
                if (!(layoutImpl.isPointerOverPopup() || layoutImpl.isMenuShowing())) {
                    pos?.let { updateDiagnostic(it) }
                }
            } else {
                pos?.let { updateDiagnostic(it) }
            }
        }

        fun postUpdate(delay: Long = ViewUtils.HOVER_TOOLTIP_SHOW_TIMEOUT) {
            editor.removeCallbacks(callback)
            editor.postDelayedInLifecycle(callback, delay)
        }
        eventManager.subscribeAlways<HoverEvent> { e ->
            if (editor.isInMouseMode) {
                fun updateLastHover() {
                    lastHoverPos = e.x to e.y
                }
                when (e.causingEvent.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        editor.removeCallbacks(callback)
                        updateDiagnostic(null, null, null)
                        updateLastHover()
                    }

                    MotionEvent.ACTION_HOVER_EXIT -> {
                        hoverPosition = null
                        if (!(layoutImpl.isPointerOverPopup() || layoutImpl.isMenuShowing())) {
                            postUpdate()
                            updateLastHover()
                        }
                    }

                    MotionEvent.ACTION_HOVER_MOVE -> {
                        if (!(layoutImpl.isPointerOverPopup() || layoutImpl.isMenuShowing())) {
                            if (editor.isScreenPointOnText(e.x, e.y)) {
                                if (abs(e.x - lastHoverPos.first) > ViewUtils.HOVER_TAP_SLOP || abs(
                                        e.y - lastHoverPos.second
                                    ) > ViewUtils.HOVER_TAP_SLOP
                                ) {
                                    updateLastHover()
                                    val pos = editor.getPointPositionOnScreen(e.x, e.y)
                                    hoverPosition = editor.text.indexer.getCharPosition(
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
        }
        eventManager.subscribeEvent<ColorSchemeUpdateEvent> { _, _ ->
            applyColorScheme()
        }
        eventManager.subscribeEvent<TextSizeChangeEvent> { event, _ ->
            layoutImpl.onTextSizeChanged(event.oldTextSize, event.newTextSize)
            val diagnostic = currentDiagnostic
            val region = currentRegion
            if (diagnostic != null && region != null) {
                layoutImpl.renderDiagnostic(diagnostic, region)
            }
            if (isShowing) {
                updateWindowSize()
                updateWindowPosition()
            }
        }
        eventManager.subscribeEvent<EditorFocusChangeEvent> { event, _ ->
            if (!event.isGainFocus) {
                dismiss()
            }
        }
        eventManager.subscribeEvent<EditorReleaseEvent> { _, _ ->
            isEnabled = false
        }
    }

    override fun dismiss() {
        if (isShowing) {
            super.dismiss()
        }
    }

    protected open fun updateDiagnostic(pos: CharPosition) {
        val diagnostics = editor.diagnostics
        if (diagnostics != null) {
            diagnostics.queryInRegion(
                diagnosticList,
                pos.index - 1,
                pos.index + 1
            )
            if (diagnosticList.isNotEmpty()) {
                var minLength = diagnosticList[0].endIndex - diagnosticList[0].startIndex
                var minIndex = 0
                for (i in 1 until diagnosticList.size) {
                    val length = diagnosticList[i].endIndex - diagnosticList[i].startIndex
                    if (length < minLength) {
                        minLength = length
                        minIndex = i
                    }
                }
                updateDiagnostic(diagnosticList[minIndex].detail, diagnosticList[minIndex], pos)
                if (!editor.getComponent<EditorAutoCompletion>().isCompletionInProgress)
                    show()
            } else {
                updateDiagnostic(null, null, null)
            }
            diagnosticList.clear()
        } else {
            updateDiagnostic(null, null, null)
        }
    }

    protected open fun applyColorScheme() {
        layoutImpl.applyColorScheme(editor.colorScheme)
    }

    protected open fun isSelectionVisible(): Boolean {
        val selection = editor.cursor.left()
        editor.layout.getCharLayoutOffset(selection.line, selection.column, buffer)
        return buffer[0] >= editor.offsetY && buffer[0] - editor.rowHeight <= editor.offsetY + editor.height && buffer[1] >= editor.offsetX && buffer[1] - 100f /* larger than a single character */ <= editor.offsetX + editor.width
    }

    protected open fun updateDiagnostic(diagnostic: DiagnosticDetail?, region: DiagnosticRegion?, position: CharPosition?) {
        if (!isEnabled) {
            return
        }

        // dismiss if completion windows is showing
        if (editor.getComponent<EditorAutoCompletion>().isShowing) {
            dismiss()
            return
        }

        // update the cursor position first
        memorizedPosition = position

        val previousRegion = currentRegion
        val sameRegion = when {
            region === previousRegion -> true
            region == null || previousRegion == null -> false
            else -> region.id == previousRegion.id &&
                region.startIndex == previousRegion.startIndex &&
                region.endIndex == previousRegion.endIndex &&
                region.severity == previousRegion.severity
        }
        if (diagnostic == currentDiagnostic && sameRegion) {
            if (diagnostic != null && !editor.isInMouseMode) {
                updateWindowPosition()
            }
            return
        }
        currentDiagnostic = diagnostic
        currentRegion = region
        if (diagnostic == null) {
            layoutImpl.renderDiagnostic(null, null)
            dismiss()
            return
        }
        layoutImpl.renderDiagnostic(diagnostic, region)
        updateWindowSize()
        updateWindowPosition()
    }

    protected open fun updateWindowSize() {
        val width = (editor.width * 0.9).toInt()
        val (dialogWidth, dialogHeight) = layoutImpl.measureContent(width, maxHeight)
        setSize(dialogWidth, dialogHeight)
    }

    protected open fun updateWindowPosition() {
        val selection = memorizedPosition ?: return
        val charX = editor.getCharOffsetX(selection.line, selection.column)
        val charY = editor.getCharOffsetY(selection.line, selection.column) - editor.rowHeight
        editor.getLocationInWindow(locationBuffer)
        val restAbove = charY + locationBuffer[1]
        val restBottom = editor.height - charY - editor.rowHeight
        val completionShowing = editor.getComponent<EditorAutoCompletion>().isShowing
        val windowY = if (restAbove > restBottom || completionShowing) {
            charY - height
        } else {
            charY + editor.rowHeight * 1.5f
        }
        if (completionShowing && windowY < 0) {
            dismiss()
            return
        }
        val windowX = (charX - width / 2).coerceAtLeast(0f)
        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }

}
