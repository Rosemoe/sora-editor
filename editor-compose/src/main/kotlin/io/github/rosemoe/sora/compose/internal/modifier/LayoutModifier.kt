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

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.MeasuredSizeAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import io.github.rosemoe.sora.compose.internal.CodeEditorHostImpl
import io.github.rosemoe.sora.event.EditorAttachStateChangeEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditorDelegate
import io.github.rosemoe.sora.widget.layout.ViewMeasureHelper

private class LayoutModifier(
    var host: CodeEditorHostImpl,
    var delegate: CodeEditorDelegate
) : Modifier.Node(),
    LayoutModifierNode,
    GlobalPositionAwareModifierNode,
    FocusEventModifierNode,
    MeasuredSizeAwareModifierNode {

    // When onSizeChanged changes, we want to invalidate so onRemeasured is called again
    override val shouldAutoInvalidate: Boolean = true
    private var previousSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)

    override fun onRemeasured(size: IntSize) {
        if (previousSize != size) {
            onSizeChanged(size)
            previousSize = size
        }
    }

    private fun onSizeChanged(size: IntSize) {
        val w = size.width
        val h = size.height
        val oldWidth = previousSize.width
        val oldHeight = previousSize.height
        host._width = w
        host._height = h
        delegate.renderer.onSizeChanged(w, h)
        delegate.verticalEdgeEffect?.setSize(w, h)
        delegate.horizontalEdgeEffect?.setSize(h, w)
        delegate.verticalEdgeEffect.finish()
        delegate.horizontalEdgeEffect.finish()

        if (delegate.layout == null || (delegate.isWordwrap && w != oldWidth)) {
            delegate.createLayout()
        } else {
            // If the view got larger, ensure we aren't scrolled past the new maximum bounds
            val scrollDx = if (delegate.offsetX > delegate.scrollMaxX) delegate.scrollMaxX - delegate.offsetX else 0
            val scrollDy = if (delegate.offsetY > delegate.scrollMaxY) delegate.scrollMaxY - delegate.offsetY else 0

            if (scrollDx != 0 || scrollDy != 0) {
                delegate.touchHandler.scrollBy(scrollDx.toFloat(), scrollDy.toFloat())
            }
        }

        delegate.verticalAbsorb = false
        delegate.horizontalAbsorb = false

        if (oldHeight > h && delegate.props.adjustToSelectionOnResize) {
            delegate.ensureSelectionVisible()
        }
    }

    override fun onAttach() {
        delegate.dispatchEvent(EditorAttachStateChangeEvent(delegate, true))
    }

    override fun onDetach() {
        val blink = delegate.cursorBlink
        if (blink != null) {
            blink.valid = false
            blink.visibility = false
            host.removeCallbacks(blink)
        }

        delegate.dispatchEvent(EditorAttachStateChangeEvent(delegate, false))
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        var finalWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        var finalHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight

        if (!constraints.hasFixedWidth || !constraints.hasFixedHeight) {
            val axis = when {
                !constraints.hasFixedWidth && !constraints.hasFixedHeight -> "Width and Height"
                !constraints.hasFixedWidth -> "Width"
                else -> "Height"
            }

            Log.w(
                "CodeEditor",
                "Editor is measured with unconstrained $axis. Use Modifier.fillMaxSize() or provide a fixed size to the editor."
            )
            delegate.anyWrapContentSet = true

            var widthMeasureSpec = constraintsToMeasureSpec(constraints.minWidth, constraints.maxWidth)
            var heightMeasureSpec = constraintsToMeasureSpec(constraints.minHeight, constraints.maxHeight)

            @SuppressLint("RestrictedApi")
            val specs = ViewMeasureHelper.getDesiredSize(
                widthMeasureSpec,
                heightMeasureSpec,
                delegate.measureTextRegionOffset(),
                delegate.rowHeight.toFloat(),
                delegate.isWordwrap,
                delegate.tabWidth,
                delegate.text,
                delegate.renderer.paintGeneral
            )

            widthMeasureSpec = IntPair.getFirst(specs)
            heightMeasureSpec = IntPair.getSecond(specs)

            finalWidth = View.MeasureSpec.getSize(widthMeasureSpec)
            finalHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        } else {
            delegate.anyWrapContentSet = false
        }

        // The final size MUST obey the parent's constraints,
        // so we coerce the desired size to fit safely.
        finalWidth = finalWidth.coerceIn(
            constraints.minWidth,
            if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        )
        finalHeight = finalHeight.coerceIn(
            constraints.minHeight,
            if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        )

        val resolvedConstraints = Constraints.fixed(finalWidth, finalHeight)
        host.constraintsChannel.trySend(resolvedConstraints)
        val placeable = measurable.measure(resolvedConstraints)
        return layout(finalWidth, finalHeight) {
            placeable.placeRelativeWithLayer(x = 0, y = 0, layerBlock = {
                compositingStrategy = CompositingStrategy.Offscreen
            })
        }
    }

    private fun constraintsToMeasureSpec(min: Int, max: Int): Int {
        return if (max == Constraints.Infinity) {
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        } else if (min == max) {
            View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.EXACTLY)
        } else {
            View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        delegate.renderer.onSizeChanged(coordinates.size.width, coordinates.size.height)
        val matrix = Matrix()
        coordinates.transformToScreen(matrix)
        host.matrix.setFrom(matrix)
        host.positionOnScreen = coordinates.positionOnScreen()
        host.positionInWindow = coordinates.positionInWindow()
        host.isAttached = coordinates.isAttached
        //delegate.dispatchEvent(EditorAttachStateChangeEvent(delegate, coordinates.isAttached))
    }

    override fun onFocusEvent(focusState: FocusState) {
        val gainFocus = focusState.isFocused
        if (host._isFocused == gainFocus) return

        host._isFocused = gainFocus

        val blink = delegate.cursorBlink
        if (gainFocus && blink != null) {
            blink.valid = blink.period > 0
            if (blink.valid) {
                host.post(blink)
            }
        } else {
            blink?.valid = false
            blink?.visibility = false
            delegate.touchHandler.hideInsertHandle()
            if (blink != null) host.removeCallbacks(blink)
        }

        delegate.dispatchEvent(EditorFocusChangeEvent(delegate, gainFocus))
        host.invalidate()
    }
}

private class CodeEditorLayoutElement(
    private val host: CodeEditorHostImpl,
    private val delegate: CodeEditorDelegate
) : ModifierNodeElement<LayoutModifier>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "measureEditorLayout"
        properties["host"] = host
        properties["delegate"] = delegate
    }

    override fun create() = LayoutModifier(host, delegate)

    override fun update(node: LayoutModifier) {
        node.host = host
        node.delegate = delegate
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeEditorLayoutElement) return false

        if (host !== other.host) return false
        if (delegate !== other.delegate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(host)
        result = 31 * result + System.identityHashCode(delegate)
        return result
    }
}

internal fun Modifier.measureEditorLayout(
    host: CodeEditorHostImpl,
    delegate: CodeEditorDelegate,
) = then(CodeEditorLayoutElement(host, delegate))
