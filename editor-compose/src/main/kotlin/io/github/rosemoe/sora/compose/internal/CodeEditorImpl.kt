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

package io.github.rosemoe.sora.compose.internal

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.internal.modifier.editorDragAndDrop
import io.github.rosemoe.sora.compose.internal.modifier.editorMouseEvents
import io.github.rosemoe.sora.compose.internal.modifier.editorTextInput
import io.github.rosemoe.sora.compose.internal.modifier.editorTouchEvents
import io.github.rosemoe.sora.compose.internal.modifier.measureEditorLayout
import io.github.rosemoe.sora.compose.internal.modifier.renderEditor

@Composable
internal fun CodeEditorImpl(
    state: CodeEditorState,
    modifier: Modifier = Modifier
) {
    Layout(
        measurePolicy = CodeEditorMeasurePolicy,
        modifier = modifier
            .editorTouchEvents(state.host, state.delegate)
            .editorMouseEvents(state.delegate)
            .editorDragAndDrop(state.host, state.delegate)
            .focusRequester(state.host.focusRequester)
            .editorTextInput(state)
            .measureEditorLayout(state.host, state.delegate)
            .focusable(interactionSource = remember { MutableInteractionSource() })
            .renderEditor(state)
    )
}

private object CodeEditorMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}
