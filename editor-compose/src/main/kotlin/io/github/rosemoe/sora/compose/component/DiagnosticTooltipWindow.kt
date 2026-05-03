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

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.LocalEditorColorScheme
import io.github.rosemoe.sora.compose.LocalEditorFontFamily
import io.github.rosemoe.sora.compose.internal.createPopupLayout
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
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
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun CodeEditorState.createDiagnosticTooltipWindow(
    content: DiagnosticTooltipContent,
): DiagnosticTooltipWindow {
    val window = DiagnosticTooltipWindow(this)
    window.content = content
    return window
}

internal class DiagnosticTooltipWindow(
    val state: CodeEditorState,
) : EditorPopupWindow(
    state.delegate,
    state.host,
    FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED
), EditorBuiltinComponent {

    private val eventManager = editor.createSubEventManager()
    private val diagnosticList = mutableListOf<DiagnosticRegion>()
    private val buffer = FloatArray(2)
    private val locationBuffer = IntArray(2)

    private var memorizedPosition: CharPosition? = null
    private var enabled = true

    /** Currently rendered diagnostic, or null when the window is empty/dismissed. */
    private var currentDiagnostic: DiagnosticDetail? = null
    private var currentRegion: DiagnosticRegion? = null

    var maxHeight: Int = (editor.dpUnit * 175).toInt()

    private var size by mutableStateOf(IntSize.Zero)
    private var colorSchemeState by mutableStateOf(editor.colorScheme)
    private val tooltipStateFlow = MutableStateFlow<DiagnosticTooltipState?>(null)
    private var tooltipState by mutableStateOf<DiagnosticTooltipState?>(null)
    private var menuShowing by mutableStateOf(false)
    private var pointerOverPopup = false

    var content: DiagnosticTooltipContent by mutableStateOf({})

    private val scope: CoroutineScope = MainScope()
    private val hoverFlow = MutableSharedFlow<HoverRequest>(
        replay = 0,
        extraBufferCapacity = 16,
    )
    private var hoverJob: Job? = null
    private var lastHoverX = 0f
    private var lastHoverY = 0f

    private data class HoverRequest(val pos: CharPosition?)

    init {
        setContentView(host.attachedView.createPopupLayout(::Content))
        popup.animationStyle = io.github.rosemoe.sora.R.style.diagnostic_popup_animation
        popup.setOnDismissListener {
            currentDiagnostic = null
            currentRegion = null
            tooltipState = null
            menuShowing = false
            pointerOverPopup = false
        }
        subscribeEvents()
        startHoverPipeline()
    }

    @Composable
    private fun Content() {
        CompositionLocalProvider(
            LocalEditorColorScheme provides colorSchemeState,
            LocalEditorFontFamily provides FontFamily(editor.typefaceText),
        ) {
            val current = tooltipState
            SubcomposeLayout {
                val maxW = (editor.width * 0.9f).toInt().coerceAtLeast(1)
                val maxH = maxHeight.coerceAtLeast(1)
                val childConstraints = Constraints(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = maxW,
                    maxHeight = maxH,
                )
                val placeables = if (current != null) {
                    subcompose("diagnostic") { content(current) }
                        .map { it.measure(childConstraints) }
                } else {
                    emptyList()
                }

                val width = placeables.maxOfOrNull { it.width } ?: 0
                val height = placeables.maxOfOrNull { it.height } ?: 0

                if (size.width != width || size.height != height) {
                    size = IntSize(width, height)
                    setSize(width, height)
                    editor.postInLifecycle(::updateWindowPosition)
                }

                layout(width, height) {
                    placeables.fastForEach { it.place(0, 0) }
                }
            }
        }
    }

    private fun subscribeEvents() {
        eventManager.subscribeEvent<SelectionChangeEvent> { event, _ ->
            if (!isEnabled || editor.isInMouseMode) return@subscribeEvent
            if (event.isSelected ||
                (event.cause != SelectionChangeEvent.CAUSE_TAP &&
                        event.cause != SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)
            ) {
                updateDiagnostic(null, null, null)
                return@subscribeEvent
            }
            updateDiagnostic(event.left)
        }

        eventManager.subscribeEvent<ScrollEvent> { _, _ ->
            if (editor.isInMouseMode) return@subscribeEvent
            if (currentDiagnostic != null && isShowing) {
                if (!isSelectionVisible()) {
                    dismiss()
                } else {
                    updateWindowPosition()
                }
            }
        }

        eventManager.subscribeAlways<HoverEvent> { e ->
            if (!editor.isInMouseMode) return@subscribeAlways
            when (e.causingEvent.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    cancelPendingHover()
                    updateDiagnostic(null, null, null)
                    lastHoverX = e.x; lastHoverY = e.y
                }

                MotionEvent.ACTION_HOVER_EXIT -> {
                    if (!(pointerOverPopup || menuShowing)) {
                        emitHover(null)
                        lastHoverX = e.x; lastHoverY = e.y
                    }
                }

                MotionEvent.ACTION_HOVER_MOVE -> {
                    if (pointerOverPopup || menuShowing) return@subscribeAlways
                    if (!editor.isScreenPointOnText(e.x, e.y)) {
                        emitHover(null)
                        return@subscribeAlways
                    }
                    if (abs(e.x - lastHoverX) > ViewUtils.HOVER_TAP_SLOP ||
                        abs(e.y - lastHoverY) > ViewUtils.HOVER_TAP_SLOP
                    ) {
                        lastHoverX = e.x; lastHoverY = e.y
                        val pos = editor.getPointPositionOnScreen(e.x, e.y)
                        val charPos = editor.text.indexer.getCharPosition(
                            IntPair.getFirst(pos),
                            IntPair.getSecond(pos),
                        )
                        emitHover(charPos)
                    }
                }
            }
        }

        eventManager.subscribeEvent<ColorSchemeUpdateEvent> { event, _ ->
            colorSchemeState = event.colorScheme
        }

        eventManager.subscribeEvent<TextSizeChangeEvent> { _, _ ->
            // Re-measure on next frame; SubcomposeLayout will pick up new metrics.
            if (isShowing) {
                editor.postInLifecycle(::updateWindowPosition)
            }
        }

        eventManager.subscribeEvent<EditorFocusChangeEvent> { event, _ ->
            if (!event.isGainFocus) dismiss()
        }

        eventManager.subscribeEvent<EditorReleaseEvent> { _, _ ->
            isEnabled = false
            scope.cancel()
        }
    }

    @OptIn(FlowPreview::class)
    private fun startHoverPipeline() {
        hoverJob = scope.launch(Dispatchers.Main.immediate) {
            hoverFlow
                .debounce(ViewUtils.HOVER_TOOLTIP_SHOW_TIMEOUT)
                .collectLatest { request ->
                    if (!isEnabled) return@collectLatest
                    if (popup.isShowing && (pointerOverPopup || menuShowing)) {
                        return@collectLatest
                    }
                    val pos = request.pos
                    if (pos == null) {
                        updateDiagnostic(null, null, null)
                    } else {
                        updateDiagnostic(pos)
                    }
                }
        }
    }

    private fun emitHover(pos: CharPosition?) {
        // tryEmit is fine here. extraBufferCapacity is large enough that we won't drop
        // hover updates in practice, and a dropped one would only delay a tooltip slightly.
        hoverFlow.tryEmit(HoverRequest(pos))
    }

    private fun cancelPendingHover() {
        hoverJob?.cancel()
        startHoverPipeline()
    }

    private fun updateDiagnostic(pos: CharPosition) {
        val diagnostics = editor.diagnostics
        if (diagnostics == null) {
            updateDiagnostic(null, null, null)
            return
        }
        diagnostics.queryInRegion(diagnosticList, pos.index - 1, pos.index + 1)
        if (diagnosticList.isEmpty()) {
            updateDiagnostic(null, null, null)
            diagnosticList.clear()
            return
        }
        var minLength = diagnosticList[0].endIndex - diagnosticList[0].startIndex
        var minIndex = 0
        for (i in 1 until diagnosticList.size) {
            val length = diagnosticList[i].endIndex - diagnosticList[i].startIndex
            if (length < minLength) {
                minLength = length
                minIndex = i
            }
        }
        val region = diagnosticList[minIndex]
        updateDiagnostic(region.detail, region, pos)
        if (!editor.getComponent<EditorAutoCompletion>().isCompletionInProgress) {
            show()
        }
        diagnosticList.clear()
    }

    private fun updateDiagnostic(
        diagnostic: DiagnosticDetail?,
        region: DiagnosticRegion?,
        position: CharPosition?,
    ) {
        if (!isEnabled) return

        if (editor.getComponent<EditorAutoCompletion>().isShowing) {
            dismiss()
            return
        }

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
            tooltipState = null
            tooltipStateFlow.value = null
            dismiss()
            return
        }

        tooltipState = DiagnosticTooltipState(
            diagnostic = diagnostic,
            region = region,
            onDismiss = ::dismiss,
            onMenuShowingChanged = { menuShowing = it },
        )
        tooltipStateFlow.value = tooltipState
        // Position will be updated once SubcomposeLayout reports a non-zero size.
        updateWindowPosition()
    }

    private fun isSelectionVisible(): Boolean {
        val selection = editor.cursor.left()
        editor.layout.getCharLayoutOffset(selection.line, selection.column, buffer)
        return buffer[0] >= editor.offsetY &&
                buffer[0] - editor.rowHeight <= editor.offsetY + editor.height &&
                buffer[1] >= editor.offsetX &&
                buffer[1] - 100f <= editor.offsetX + editor.width
    }

    private fun updateWindowPosition() {
        val selection = memorizedPosition ?: return
        val charX = editor.getCharOffsetX(selection.line, selection.column)
        val charY = editor.getCharOffsetY(selection.line, selection.column) - editor.rowHeight
        host.getLocationInWindow(locationBuffer)
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

    override fun show() {
        if (!enabled || !host.isFocused || tooltipState == null) return
        super.show()
    }

    override fun dismiss() {
        if (isShowing) super.dismiss()
    }

    override fun isEnabled(): Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        eventManager.isEnabled = enabled
        if (!enabled) dismiss()
    }

    fun release() {
        if (scope.coroutineContext[Job]?.isActive == true) scope.cancel()
        isEnabled = false
    }
}
