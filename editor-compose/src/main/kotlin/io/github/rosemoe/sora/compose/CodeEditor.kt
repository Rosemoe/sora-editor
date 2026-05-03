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

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.pageDown
import androidx.compose.ui.semantics.pageUp
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import io.github.rosemoe.sora.compose.component.AutoCompletionWindowContent
import io.github.rosemoe.sora.compose.component.DiagnosticTooltipContent
import io.github.rosemoe.sora.compose.component.TextActionWindowContent
import io.github.rosemoe.sora.compose.component.createAutoCompletionWindow
import io.github.rosemoe.sora.compose.component.createDiagnosticTooltipWindow
import io.github.rosemoe.sora.compose.component.createTextActionWindow
import io.github.rosemoe.sora.compose.internal.CodeEditorImpl
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.text.TextUtils
import io.github.rosemoe.sora.widget.SelectionMovement
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.component.EditorTextActionWindow
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.replaceComponent

/**
 * Composable for displaying and editing code.
 *
 * @param modifier The [Modifier] to be applied to the editor container.
 * @param state The state object to be used to control or observe the editor's state.
 * @param editable Controls if the text in the editor can be modified by the user.
 * @param enabled Controls the enabled state of the editor. When `false`, the editor will not respond to user input.
 * @param autoCompletionWindow A composable function to render the auto-completion window.
 * Pass `null` to disable the auto-completion feature. Defaults to [CodeEditorDefaults.AutoCompletionWindow].
 * @param diagnosticTooltipWindow A composable function to render the diagnostic tooltip window.
 * Pass `null` to disable the diagnostic tooltip. Defaults to [CodeEditorDefaults.DiagnosticTooltipWindow].
 * @param textActionWindow A composable function to render the text action window (selection menu/toolbar).
 * Pass `null` to disable the text action window. Defaults to [CodeEditorDefaults.TextActionWindow].
 */
@Composable
@UiComposable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState = rememberCodeEditorState(),
    editable: Boolean = true,
    enabled: Boolean = true,
    autoCompletionWindow: AutoCompletionWindowContent? = CodeEditorDefaults::AutoCompletionWindow,
    diagnosticTooltipWindow: DiagnosticTooltipContent? = CodeEditorDefaults::DiagnosticTooltipWindow,
    textActionWindow: TextActionWindowContent? = { state ->
        CodeEditorDefaults.TextActionWindow(
            state = state,
            modifier = Modifier.fillMaxHeight()
        )
    }
) {
    LaunchedEffect(enabled, editable) {
        state.host._isEnabled = enabled
        state.editable = editable
    }

    val autoCompletion by rememberUpdatedState(autoCompletionWindow)
    val textAction by rememberUpdatedState(textActionWindow)
    val diagnosticTooltip by rememberUpdatedState(diagnosticTooltipWindow)

    LaunchedEffect(autoCompletion) {
        val delegate = state.delegate
        if (autoCompletion == null) {
            delegate.getComponent<EditorAutoCompletion>().isEnabled = false
        } else {
            val component = delegate.createAutoCompletionWindow(autoCompletion!!)
            delegate.replaceComponent<EditorAutoCompletion>(component)
            component.isEnabled = true
        }
    }

    LaunchedEffect(textAction) {
        val delegate = state.delegate
        if (textAction == null) {
            delegate.getComponent<EditorTextActionWindow>().isEnabled = false
            state.textActionWindow?.isEnabled = false
        } else {
            delegate.getComponent<EditorTextActionWindow>().isEnabled = false
            state.textActionWindow = state.createTextActionWindow(textAction!!)
            state.textActionWindow?.isEnabled = true
        }
    }

    LaunchedEffect(diagnosticTooltip) {
        val delegate = state.delegate
        if (diagnosticTooltip == null) {
            delegate.getComponent<EditorDiagnosticTooltipWindow>().isEnabled = false
            state.diagnosticTooltipWindow?.isEnabled = false
        } else {
            delegate.getComponent<EditorDiagnosticTooltipWindow>().isEnabled = false
            state.diagnosticTooltipWindow = state.createDiagnosticTooltipWindow(diagnosticTooltip!!)
            state.diagnosticTooltipWindow?.isEnabled = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            state.release()
        }
    }

    CodeEditorImpl(
        state = state,
        modifier = modifier
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

                        insertTextAtCursor { text ->
                            if (state.isEditable) {
                                state.insertText(text.toString(), state.cursor.left)
                            }
                            state.isEditable
                        }
                    }

                    requestFocus { state.host.requestFocus() }

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

                    pageUp {
                        state.moveSelection(SelectionMovement.PAGE_UP)
                        true
                    }

                    pageDown {
                        state.moveSelection(SelectionMovement.PAGE_DOWN)
                        true
                    }

//                            pageLeft {
//                                state.moveSelection(SelectionMovement.LEFT)
//                                true
//                            }
//
//                            pageRight {
//                                state.moveSelection(SelectionMovement.RIGHT)
//                                true
//                            }
                }
            }
    )
}

@Preview
@Composable
private fun PreviewCodeEditor() {
    CodeEditor()
}
