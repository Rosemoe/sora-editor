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

import android.graphics.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.internal.CodeEditorHostImpl
import io.github.rosemoe.sora.widget.CodeEditorDelegate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private class CodeEditorDrawModifier(
    private var state: CodeEditorState,
    private var host: CodeEditorHostImpl = state.host,
    private var delegate: CodeEditorDelegate = state.delegate
) : Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate = true
    private var invalidateJob: Job? = null

    override fun onAttach() {
        invalidateJob = coroutineScope.launch {
            host.invalidateFlow.collectLatest {
                invalidateDraw()
            }
        }
    }

    override fun onDetach() {
        invalidateJob?.cancel()
        invalidateJob = null
    }

    override fun ContentDrawScope.draw() {
        val scrollX = delegate.offsetX.toFloat()
        val scrollY = delegate.offsetY.toFloat()

        translate(-scrollX, -scrollY) {
            onDraw(drawContext.canvas.nativeCanvas)
        }
    }

    private fun onDraw(canvas: Canvas) {
        delegate.verticalEdgeEffect?.finish()
        delegate.horizontalEdgeEffect?.finish()

        delegate.renderer.draw(canvas)

        // Update magnifier
        if ((delegate.lastCursorState != delegate.cursorBlink?.visibility || !delegate.touchHandler.scroller.isFinished) && delegate.touchHandler.magnifier.isShowing) {
            delegate.lastCursorState = delegate.cursorBlink?.visibility ?: false
            host.post(delegate.touchHandler.magnifier::updateDisplay)
        }
    }

    fun update(state: CodeEditorState) {
        this.state = state
        this.delegate = state.delegate
        this.host = state.host
    }
}

private class CodeEditorDrawElement(
    private val state: CodeEditorState,
) : ModifierNodeElement<CodeEditorDrawModifier>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "renderEditor"
        properties["state"] = state
    }

    override fun create() = CodeEditorDrawModifier(state)

    override fun update(node: CodeEditorDrawModifier) {
        node.update(state)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeEditorDrawElement) return false

        if (state !== other.state) return false

        return true
    }

    override fun hashCode() = System.identityHashCode(state)
}

internal fun Modifier.renderEditor(
    state: CodeEditorState
) = then(CodeEditorDrawElement(state))
