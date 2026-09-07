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

import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTargetModifierNode
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import io.github.rosemoe.sora.compose.internal.CodeEditorHostImpl
import io.github.rosemoe.sora.util.ClipDataUtils
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditorDelegate

private class DragAndDropModifier(
    var delegate: CodeEditorDelegate,
    var host: CodeEditorHostImpl
) : DelegatingNode() {

    private var dragAndDropNode: DragAndDropTargetModifierNode? = null

    override fun onAttach() {
        createAndAttachDragAndDropModifierNode()
    }

    fun update(delegate: CodeEditorDelegate, host: CodeEditorHostImpl) {
        if (this.delegate != delegate || this.host != host) {
            dragAndDropNode?.let { undelegate(it) }
            this.delegate = delegate
            this.host = host
            createAndAttachDragAndDropModifierNode()
        }
    }

    override fun onDetach() {
        undelegate(dragAndDropNode!!)
    }

    private fun createAndAttachDragAndDropModifierNode() {
        dragAndDropNode = delegate(
            DragAndDropTargetModifierNode(
                shouldStartDragAndDrop = { true },
                target = EditorDragAndDropTarget(host, delegate)
            )
        )
    }
}

private class EditorDragAndDropTarget(
    private val host: CodeEditorHostImpl,
    private val delegate: CodeEditorDelegate
) : DragAndDropTarget {

    override fun onMoved(event: DragAndDropEvent) {
        val androidEvent = event.toAndroidDragEvent()
        val x = androidEvent.x
        val y = androidEvent.y

        val pos = delegate.getPointPositionOnScreen(x, y)
        val line = IntPair.getFirst(pos)
        val column = IntPair.getSecond(pos)

        delegate.touchHandler.draggingSelection = delegate.text.indexer.getCharPosition(line, column)
        host.invalidate()
        delegate.touchHandler.scrollIfReachesEdge(null, x, y)
    }

    override fun onExited(event: DragAndDropEvent) {
        delegate.touchHandler.draggingSelection = null
        host.invalidate()
        //delegate.touchHandler.stopEdgeScroll()
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        val targetPos = delegate.touchHandler.draggingSelection ?: return false
        val androidEvent = event.toAndroidDragEvent()

        delegate.touchHandler.draggingSelection = null
        delegate.setSelection(targetPos.line, targetPos.column)
        delegate.pasteText(ClipDataUtils.clipDataToString(androidEvent.clipData))

        host.requestFocus()
        host.invalidate()

        return true
    }
}

private class DragAndDropElement(
    private val delegate: CodeEditorDelegate,
    private val host: CodeEditorHostImpl
) : ModifierNodeElement<DragAndDropModifier>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "editorDragAndDrop"
        properties["delegate"] = delegate
        properties["host"] = host
    }

    override fun create(): DragAndDropModifier {
        return DragAndDropModifier(delegate, host)
    }

    override fun update(node: DragAndDropModifier) {
        node.update(delegate, host)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DragAndDropElement) return false

        if (delegate !== other.delegate) return false
        if (host !== other.host) return false

        return true
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(delegate)
        result = 31 * result + System.identityHashCode(host)
        return result
    }
}

internal fun Modifier.editorDragAndDrop(
    host: CodeEditorHostImpl,
    delegate: CodeEditorDelegate
) = then(DragAndDropElement(delegate, host))
