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
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.establishTextInputSession
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.event.BuildEditorInfoEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private class CodeEditorInputModifier(
    var state: CodeEditorState,
) : Modifier.Node(),
    PlatformTextInputModifierNode,
    KeyInputModifierNode,
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {

    private var channelJob: Job? = null
    private var sessionJob: Job? = null
    val delegate = state.delegate

    override fun onFocusEvent(focusState: FocusState) {
        // If we LOSE focus entirely, clean up everything
        if (!focusState.isFocused) {
            channelJob?.cancel()
            channelJob = null

            sessionJob?.cancel()
            sessionJob = null

            val keyboardController = currentValueOf(LocalSoftwareKeyboardController)
            keyboardController?.hide()
            return
        }

        if (delegate.isEditable && state.host.isEnabled && channelJob == null) {
            channelJob = coroutineScope.launch {
                launch {
                    state.host.showKeyboardChannel.receiveAsFlow().collect {
                        val keyboardController = currentValueOf(LocalSoftwareKeyboardController)

                        if (sessionJob?.isActive == true) {
                            // The session is already running (e.g., user hid IME and tapped again, or is scrolling).
                            keyboardController?.show()
                        } else {
                            sessionJob = launch {
                                establishTextInputSession {
                                    startInputMethod(createInputRequest(state))
                                }
                            }
                        }
                    }
                }

                launch {
                    state.host.hideKeyboardChannel.receiveAsFlow().collect {
                        // DO NOT cancel the sessionJob here!
                        // Canceling the session destroys the connection and causes flickers during scroll.
                        // We only want to visually hide the keyboard.
                        val keyboardController = currentValueOf(LocalSoftwareKeyboardController)
                        keyboardController?.hide()
                    }
                }
            }
        }
    }

    override fun onDetach() {
        channelJob?.cancel()
        channelJob = null
        sessionJob?.cancel()
        sessionJob = null
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val nativeKeyEvent = event.nativeKeyEvent
        return when (event.type) {
            KeyEventType.KeyDown -> delegate.keyEventHandler.onKeyDown(nativeKeyEvent.keyCode, nativeKeyEvent)
            KeyEventType.KeyUp -> delegate.keyEventHandler.onKeyUp(nativeKeyEvent.keyCode, nativeKeyEvent)

            KeyEventType.Unknown -> {
                @Suppress("DEPRECATION")
                if (nativeKeyEvent.action == android.view.KeyEvent.ACTION_MULTIPLE) {
                    delegate.keyEventHandler.onKeyMultiple(
                        nativeKeyEvent.keyCode,
                        nativeKeyEvent.repeatCount,
                        nativeKeyEvent
                    )
                } else {
                    false
                }
            }

            else -> false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean {
        return false
    }
}

private class CodeEditorInputElement(
    private val state: CodeEditorState
) : ModifierNodeElement<CodeEditorInputModifier>() {

    override fun InspectorInfo.inspectableProperties() {
        name = "editorTextInput"
        properties["state"] = state
    }

    override fun create(): CodeEditorInputModifier {
        return CodeEditorInputModifier(state)
    }

    override fun update(node: CodeEditorInputModifier) {
        node.state = state
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeEditorInputElement) return false
        return state === other.state
    }

    override fun hashCode() = state.hashCode()
}

internal fun Modifier.editorTextInput(state: CodeEditorState) = then(CodeEditorInputElement(state))

@SuppressLint("RestrictedApi")
private fun PlatformTextInputSessionScope.createInputRequest(
    state: CodeEditorState,
): PlatformTextInputMethodRequest {
    val hostImpl = state.host
    val delegate = state.delegate
    val inputConnection = hostImpl.inputConnection

    return PlatformTextInputMethodRequest { outAttrs ->
        hostImpl.immView = view

        if (delegate.checkSoftInputEnabled()) {
            outAttrs.inputType =
                if (delegate.inputType != 0) delegate.inputType else EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            outAttrs.inputType = InputType.TYPE_NULL
        }

        outAttrs.initialSelStart = delegate.cursor?.left ?: 0
        outAttrs.initialSelEnd = delegate.cursor?.right ?: 0
        outAttrs.initialCapsMode = inputConnection.getCursorCapsMode(0)

        // Prevent fullscreen when the screen height is too small
        // Especially in landscape mode
        if (!delegate.props.allowFullscreen) {
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }

        delegate.dispatchEvent(BuildEditorInfoEvent(delegate, outAttrs))
        //@SuppressLint("RestrictedApi")
        inputConnection.reset()
        delegate.text.resetBatchEdit()
        delegate.setExtracting(null)

        inputConnection
    }
}
