/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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
import android.widget.TextView
import io.github.rosemoe.sora.R
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.subscribeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorBuiltinComponent
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.subscribeEvent
import org.eclipse.lsp4j.SignatureHelp

open class SignatureHelpWindow(editor: CodeEditor) : EditorPopupWindow(
    editor,
    FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SCROLL_AS_CONTENT
),
    EditorBuiltinComponent {

    private val rootView: View = LayoutInflater.from(editor.context)
        .inflate(io.github.rosemoe.sora.lsp.R.layout.signature_help_tooltip_window, null, false)

    val maxWidth = (editor.width * 0.9).toInt()
    val maxHeight = (editor.dpUnit * 175).toInt()

    private val text =
        rootView.findViewById<TextView>(io.github.rosemoe.sora.lsp.R.id.signature_help_tooltip_text)
    private val locationBuffer = IntArray(2)
    protected val eventManager = editor.createSubEventManager()

    private lateinit var signatureHelp: SignatureHelp

    init {
        super.setContentView(rootView)
        
        eventManager.subscribeEvent<ColorSchemeUpdateEvent> { _, _ ->
            applyColorScheme()
        }

        applyColorScheme()
    }

    override fun isEnabled() = eventManager.isEnabled

    override fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    fun show(signatureHelp: SignatureHelp) {
        this.signatureHelp = signatureHelp
        renderSignatureHelp()
        updateWindowSizeAndLocation()
        show()
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
        val charY = editor.getCharOffsetY(
            selection.line,
            selection.column
        ) - editor.rowHeight - 10 * editor.dpUnit
        editor.getLocationInWindow(locationBuffer)
        val restAbove = charY + locationBuffer[1]
        val restBottom = editor.height - charY - editor.rowHeight
        val windowY = if (restAbove > restBottom) {
            charY - height
        } else {
            charY + editor.rowHeight * 1.5f
        }
        if (windowY < 0) {
            dismiss()
            return
        }
        val windowX = (charX - width / 2).coerceAtLeast(0f)
        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }

    private fun renderSignatureHelp() {

    }

    private fun applyColorScheme() {
        if (isShowing) {
            renderSignatureHelp()
        }
    }


}