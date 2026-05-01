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

package io.github.rosemoe.sora.compose

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import io.github.rosemoe.sora.compose.internal.CodeEditorImpl

/**
 * Composable for displaying and editing code.
 *
 * @param modifier The [Modifier] to be applied to the editor container.
 * @param state The state object to be used to control or observe the editor's state.
 * @param editable Controls if the text in the editor can be modified by the user.
 * @param enabled Controls the enabled state of the editor. When `false`, the editor will not respond to user input.
 */
@Composable
@UiComposable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    editable: Boolean = true,
    enabled: Boolean = true
) {
    LaunchedEffect(enabled, editable) {
        state.host._isEnabled = enabled
        state.editable = editable
    }

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            CodeEditorImpl(
                state = state,
                modifier = Modifier
                    .matchParentSize()
                    .clipToBounds()
                    .pointerHoverIcon(PointerIcon.Text, overrideDescendants = true)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCodeEditor() {
    CodeEditor()
}
