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
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.accessibilityClassName
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import io.github.rosemoe.sora.compose.internal.CodeEditorImpl
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.TextUtils

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
                    .semantics(mergeDescendants = true) {
                        stateDescription = if (enabled) "enabled" else "disabled"

                        if (enabled) {
                            val maxLength = state.props.maxAccessibilityTextLength
                            if (maxLength > 0) {
                                isEditable = state.isEditable
                                textSelectionRange = TextRange(state.cursor.left, state.cursor.right)
                                accessibilityClassName = "CodeEditor"
                                //text = AnnotatedString(TextUtils.trimToSize(state.text, maxLength).toString())
                                //inputText = AnnotatedString(TextUtils.trimToSize(state.text, maxLength).toString())
                                editableText = AnnotatedString(TextUtils.trimToSize(state.text, maxLength).toString())

                                copyText {
                                    state.copyText()
                                    true
                                }

                                cutText {
                                    state.cutText()
                                    true
                                }

                                pasteText {
                                    state.pasteText()
                                    true
                                }

                                setText { text ->
                                    state.setText(text)
                                    true
                                }
                            }

                            onLongClick { true }

                            verticalScrollAxisRange = ScrollAxisRange(
                                value = { state.scrollY.toFloat() },
                                maxValue = { state.maxScrollY.toFloat() },
                                reverseScrolling = false
                            )

                            horizontalScrollAxisRange = ScrollAxisRange(
                                value = { state.scrollX.toFloat() },
                                maxValue = { state.maxScrollX.toFloat() },
                                reverseScrolling = false
                            )

//                            scrollBy { x, y ->
//                                val (line, column) = state.getPointPosition(
//                                    state.scrollX + x,
//                                    state.scrollY + y
//                                )
//                                state.delegate.touchHandler.scrollBy(x, y, true)
//                                state.setSelection(line, column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
//                                true
//                            }

                            scrollByOffset { offset ->
                                val (line, column) = state.getPointPosition(
                                    state.scrollX + offset.x,
                                    state.scrollY + offset.y
                                )
                                state.delegate.touchHandler.scrollBy(offset.x, offset.y, true)
                                state.setSelection(line, column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
                                offset
                            }
                        }
                    }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCodeEditor() {
    CodeEditor()
}
