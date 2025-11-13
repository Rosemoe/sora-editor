package io.github.rosemoe.sora.lsp.editor.hover

import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.event.subscribeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.jsonrpc.messages.Either

open class HoverWindow(
    editor: CodeEditor,
    internal val coroutineScope: CoroutineScope
) : EditorPopupWindow(
    editor,
    FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SCROLL_AS_CONTENT or FEATURE_DISMISS_WHEN_OBSCURING_CURSOR
) {

    private lateinit var rootView: View
    private val maxWidth = (editor.width * 0.8).toInt()
    private val maxHeight = (editor.dpUnit * 280).toInt()
    private val buffer = FloatArray(2)
    protected val eventManager = editor.createSubEventManager()
    private var hover: Hover? = null
    private var pendingHover: Hover? = null
    private val showRunnable = Runnable {
        val nextHover = pendingHover
        pendingHover = null
        if (nextHover != null) {
            performShow(nextHover)
        }
    }

    private var layoutImpl: HoverLayout = DefaultHoverLayout()
    private val renderJob = SupervisorJob(coroutineScope.coroutineContext[Job])
    private val renderScope =
        CoroutineScope(coroutineScope.coroutineContext + renderJob + Dispatchers.Main.immediate)

    var alwaysShowOnTouchHover = true

    var HOVER_TOOLTIP_SHOW_TIMEOUT = 1000L

    var layout: HoverLayout
        get() = layoutImpl
        set(value) {
            if (::rootView.isInitialized && layoutImpl === value) {
                return
            }
            cancelRenderJobs()
            layoutImpl = value
            layoutImpl.attach(this)
            rootView = layoutImpl.createView(LayoutInflater.from(editor.context))
            super.setContentView(rootView)
            applyColorScheme()
        }

    init {
        layout = layoutImpl
        eventManager.subscribeEvent<ColorSchemeUpdateEvent> { _, _ ->
            applyColorScheme()
        }
        eventManager.subscribeEvent<EditorReleaseEvent> { _, _ ->
            setEnabled(false)
            dismiss()
        }
        eventManager.subscribeEvent<ScrollEvent> { _, _ ->
            if (editor.isInMouseMode) {
                return@subscribeEvent
            }
            if (isShowing) {
                if (!isSelectionVisible()) {
                    dismiss()
                } else {
                    updateWindowPosition()
                }
            }
        }
        eventManager.subscribeEvent<TextSizeChangeEvent> { event, _ ->
            layout.onTextSizeChanged(event.oldTextSize, event.newTextSize)
            if (isShowing) {
                renderHover()
                updateWindowSizeAndLocation()
            }
        }
        eventManager.subscribeEvent<EditorFocusChangeEvent> { event, _ ->
            if (!event.isGainFocus) {
                dismiss()
            }
        }
    }

    fun isEnabled() = eventManager.isEnabled

    fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    open fun show(hover: Hover) {
        editor.removeCallbacks(showRunnable)
        pendingHover = null
        dismiss()
        pendingHover = hover
        editor.postDelayedInLifecycle(showRunnable, HOVER_TOOLTIP_SHOW_TIMEOUT)
    }

    override fun dismiss() {
        editor.removeCallbacks(showRunnable)
        pendingHover = null
        cancelRenderJobs()
        super.dismiss()
    }

    protected fun isSelectionVisible(): Boolean {
        val selection = editor.cursor.left()
        editor.layout.getCharLayoutOffset(selection.line, selection.column, buffer)
        return buffer[0] >= editor.offsetY && buffer[0] - editor.rowHeight <= editor.offsetY + editor.height && buffer[1] >= editor.offsetX && buffer[1] - 100f <= editor.offsetX + editor.width
    }

    private fun updateWindowSizeAndLocation() {
        rootView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        )

        val width = rootView.measuredWidth
        val height = rootView.measuredHeight

        setSize(width, height)
        updateWindowPosition()
    }

    protected open fun updateWindowPosition() {
        val selection = editor.cursor.left()
        val charX = editor.getCharOffsetX(selection.line, selection.column)
        val margin = editor.dpUnit * 10
        val anchor =
            editor.getCharOffsetY(selection.line, selection.column) - editor.rowHeight - margin
        val maxY = (editor.height - height).coerceAtLeast(0)
        val aboveTop = anchor - height
        val belowTop = (anchor + editor.rowHeight * 1.5f).coerceAtLeast(0f)
        val windowY = if (aboveTop >= 0) {
            aboveTop.coerceAtMost(maxY.toFloat())
        } else {
            belowTop.coerceAtMost(maxY.toFloat())
        }
        val windowX = (charX - width / 2).coerceAtLeast(0f)
        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }

    private fun renderHover() {
        val data = hover ?: return
        layout.renderHover(data)
    }

    internal fun launchRender(block: suspend CoroutineScope.() -> Unit): Job {
        return renderScope.launch(block = block)
    }

    private fun cancelRenderJobs() {
        renderJob.cancelChildren()
    }

    private fun applyColorScheme() {
        layout.applyColorScheme(editor.colorScheme, editor.typefaceText)
        if (isShowing) {
            renderHover()
        }
    }

    private fun performShow(hover: Hover) {
        val isInCompletion = editor.getComponent<EditorAutoCompletion>().isShowing
        val isInDiagnostic = editor.getComponent<EditorDiagnosticTooltipWindow>().isShowing

        if (!hover.hasContent() || !isEnabled() || isInCompletion || isInDiagnostic) {
            dismiss()
            return
        }

        this.hover = hover
        renderHover()
        updateWindowSizeAndLocation()
        super.show()
    }


}
