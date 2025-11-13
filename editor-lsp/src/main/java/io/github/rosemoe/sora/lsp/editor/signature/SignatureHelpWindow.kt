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

package io.github.rosemoe.sora.lsp.editor.signature

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
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.getComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.SignatureHelp

open class SignatureHelpWindow(
    editor: CodeEditor,
    val coroutineScope: CoroutineScope,
) : EditorPopupWindow(
    editor,
    FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SCROLL_AS_CONTENT or FEATURE_DISMISS_WHEN_OBSCURING_CURSOR
) {

    private lateinit var rootView: View
    private val maxWidth = (editor.width * 0.727).toInt()
    private val maxHeight = (editor.dpUnit * 355).toInt()
    private val buffer = FloatArray(2)
    protected val eventManager = editor.createSubEventManager()
    private var signatureHelp: SignatureHelp? = null

    private var layoutImpl: SignatureHelpLayout = DefaultSignatureHelpLayout()
    private val renderJob = SupervisorJob(coroutineScope.coroutineContext[Job])
    private val renderScope =
        CoroutineScope(coroutineScope.coroutineContext + renderJob + Dispatchers.Main.immediate)

    var layout: SignatureHelpLayout
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
                if (!isSelectionVisible() || !updateWindowPosition()) {
                    dismiss()
                }
            }
        }

        eventManager.subscribeEvent<TextSizeChangeEvent> { event, _ ->
            layout.onTextSizeChanged(event.oldTextSize, event.newTextSize)
            if (isShowing) {
                renderSignatureHelp()
                if (!updateWindowSizeAndLocation()) {
                    dismiss()
                }
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

    open fun show(signatureHelp: SignatureHelp) {
        if (!isEnabled()) {
            return
        }
        val isInDiagnostic = editor.getComponent<EditorDiagnosticTooltipWindow>().isShowing

        if (signatureHelp.signatures.isEmpty() || isInDiagnostic) {
            dismiss()
            return
        }

        this.signatureHelp = signatureHelp
        renderSignatureHelp()
        if (!updateWindowSizeAndLocation()) {
            dismiss()
            return
        }
        show()
    }

    override fun dismiss() {
        cancelRenderJobs()
        super.dismiss()
    }

    protected fun isSelectionVisible(): Boolean {
        val selection = editor.cursor.left()
        editor.layout.getCharLayoutOffset(selection.line, selection.column, buffer)
        return buffer[0] >= editor.offsetY && buffer[0] - editor.rowHeight <= editor.offsetY + editor.height && buffer[1] >= editor.offsetX && buffer[1] - 100f /* larger than a single character */ <= editor.offsetX + editor.width
    }

    private fun updateWindowSizeAndLocation(): Boolean {
        rootView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        )

        val width = rootView.measuredWidth
        val height = rootView.measuredHeight

        setSize(width, height)
        return updateWindowPosition()
    }

    protected open fun updateWindowPosition(): Boolean {
        val selection = editor.cursor.left()
        val charX = editor.getCharOffsetX(selection.line, selection.column)
        val margin = editor.dpUnit * 10
        val anchor =
            editor.getCharOffsetY(selection.line, selection.column) - editor.rowHeight - margin
        val maxY = (editor.height - height).coerceAtLeast(0)
        val aboveTop = anchor - height
        if (aboveTop < 0) {
            // Avoid forcing the popup below the caret so we don't cover completion window
            return false
        }
        val windowY = aboveTop.coerceAtMost(maxY.toFloat())
        val windowX = (charX - width / 2).coerceAtLeast(0f)
        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
        return true
    }

    private fun renderSignatureHelp() {
        val data = signatureHelp ?: return
        layout.renderSignatures(data)
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
            renderSignatureHelp()
        }
    }
}
