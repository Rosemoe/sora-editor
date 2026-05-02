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

package io.github.rosemoe.sora.compose.component

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rosemoe.sora.compose.LocalEditorColorScheme
import io.github.rosemoe.sora.compose.LocalEditorFontFamily
import io.github.rosemoe.sora.compose.internal.createPopupLayout
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.event.Unsubscribe
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.highlightMatchLabel
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.KeyboardUtils
import io.github.rosemoe.sora.widget.CodeEditorDelegate
import io.github.rosemoe.sora.widget.component.CompletionLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * The state of the auto-completion window, used for rendering the UI in Compose.
 */
@Immutable
data class CompletionState(
    /**
     * The list of completion items to be displayed.
     */
    val items: List<CompletionItem> = emptyList(),
    /**
     * The index of the currently selected item in [items]. Defaults to -1 if no item is selected.
     */
    val selectedIndex: Int = -1,
    /**
     * Whether the completion items are currently being loaded/fetched.
     */
    val isLoading: Boolean = false,
    /**
     * Whether animations (like sliding or fading) should be enabled for the completion window.
     */
    val enableAnimation: Boolean = true,
)

internal fun CodeEditorDelegate.createAutoCompletionWindow(
    content: @Composable (CompletionState, onSelect: (Int) -> Unit) -> Unit
): EditorAutoCompletion {
    val window = AutoCompletionWindow(this)
    window.content = content
    return window
}

private class AutoCompletionWindow(
    delegate: CodeEditorDelegate
) : EditorAutoCompletion(delegate, delegate.host) {

    private val density = Density(context)

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var completionJob: Job? = null

    private val state = MutableStateFlow(CompletionState())
    private var colorSchemeState: MutableState<EditorColorScheme>? = null

    var content: @Composable (CompletionState, onSelect: (Int) -> Unit) -> Unit by mutableStateOf({ _, _ -> })

    @Composable
    private fun Content() {
        val currentState by state.collectAsStateWithLifecycle()
        val scheme = colorSchemeState?.value ?: editor.colorScheme

        CompositionLocalProvider(
            LocalEditorColorScheme provides scheme,
            LocalEditorFontFamily provides FontFamily(editor.typefaceText),
        ) {
            content(currentState) { select(it) }
        }
    }

    override fun setLayout(layout: CompletionLayout) {
        setContentView(host.attachedView.createPopupLayout(::Content))
        applyColorScheme()
    }

    override fun onKeyEvent(event: EditorKeyEvent, unsubscribe: Unsubscribe) {
        if (event.eventType != EditorKeyEvent.Type.DOWN ||
            event.isAltPressed || event.isCtrlPressed || event.isShiftPressed ||
            !isShowing
        ) return

        val st = state.value

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                moveUp()
                event.markAsConsumed()
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                moveDown()
                event.markAsConsumed()
            }

            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_PAGE_UP -> hide()

            KeyEvent.KEYCODE_TAB -> {
                if (st.selectedIndex == -1) moveDown()
                if (select(st.selectedIndex)) event.markAsConsumed()
            }

            KeyEvent.KEYCODE_ENTER -> {
                if (st.selectedIndex == -1 &&
                    editor.props.selectCompletionItemOnEnterForSoftKbd
                ) {
                    moveDown()
                }
                if (select(state.value.selectedIndex)) event.markAsConsumed()
            }
        }
    }

    override fun getCurrentPosition() = state.value.selectedIndex

    override fun isCompletionInProgress(): Boolean {
        val job = completionJob
        return isShowing || requestShow > requestHide || job?.isActive == true
    }

    override fun setEnabledAnimation(enabledAnimation: Boolean) {
        state.update { it.copy(enableAnimation = enabledAnimation) }
    }

    override fun setAdapter(adapter: EditorCompletionAdapter?) {}

    private fun ensureColorSchemeState() {
        if (colorSchemeState == null) {
            colorSchemeState = mutableStateOf(editor.colorScheme)
        }
    }

    override fun applyColorScheme() {
        ensureColorSchemeState()
        colorSchemeState?.value = editor.colorScheme
    }

    private var loadingJob: Job? = null

    override fun setLoading(state: Boolean) {
        loading = state

        loadingJob?.cancel()

        if (state) {
            loadingJob = scope.launch {
                delay(50)
                if (loading) {
                    this@AutoCompletionWindow.state.update {
                        it.copy(isLoading = true)
                    }
                }
            }
        } else {
            this@AutoCompletionWindow.state.update {
                it.copy(isLoading = false)
            }
        }
    }

    override fun moveDown() {
        state.update { current ->
            val next = (current.selectedIndex + 1).coerceAtMost(current.items.lastIndex)
            current.copy(selectedIndex = next)
        }

        ensurePosition()
    }

    override fun moveUp() {
        state.update { current ->
            val next = (current.selectedIndex - 1).coerceAtLeast(0)
            current.copy(selectedIndex = next)
        }

        ensurePosition()
    }

    override fun ensurePosition() {
        // Compose UI should scroll to selectedIndex.
    }

    override fun select() = select(state.value.selectedIndex)

    override fun select(pos: Int): Boolean {
        if (pos == -1) return false

        val items = state.value.items
        val item = items.getOrNull(pos) ?: return false

        val cursor = editor.cursor
        val job = completionJob

        if (!cursor.isSelected && job != null) {
            cancelShowUp = true

            editor.beginComposingTextRejection()
            editor.text.beginBatchEdit()
            editor.restartInput()

            try {
                item.performCompletion(editor, editor.text, editor.cursor.left())
                editor.updateCursor()
            } finally {
                editor.text.endBatchEdit()
                editor.endComposingTextRejection()
                cancelShowUp = false
            }

            editor.restartInput()
        }

        hide()
        return true
    }

    override fun resetScrollPosition() {
        // UI should scroll to index 0 when items change.
    }

    override fun requireCompletion() {
        if (cancelShowUp || !isEnabled) return

        val text = editor.text
        if (text.cursor.isSelected || checkNoCompletion()) {
            hide()
            return
        }

        val now = System.nanoTime()
        if (now - requestTime < editor.props.cancelCompletionNs) {
            hide()
            requestTime = now
            return
        }

        cancelCompletion()
        requestTime = now
        state.update { it.copy(selectedIndex = -1) }

        val requestTimestamp = requestTime
        val requestPosition = editor.cursor.left()
        val language = editor.editorLanguage
        val extra = editor.extraArguments

        val contentRef = ContentReference(editor.text).apply {
            setValidator {
                if (!scope.isActive || requestTime != requestTimestamp) {
                    throw CompletionCancelledException()
                }
            }
        }

        publisher = CompletionPublisher(
            host.handler ?: Handler(Looper.getMainLooper()),
            {
                val items = publisher!!.items

                if (highlightMatchedLabel) {
                    items.highlightMatchLabel(editor.colorScheme)
                }

                if (lastAttachedItems == null || lastAttachedItems!!.get() != items) {
                    state.update {
                        it.copy(
                            items = items,
                            selectedIndex = if (items.isNotEmpty()) 0 else -1
                        )
                    }
                    lastAttachedItems = WeakReference(items)
                }

                if (editor.props.moveSelectionToFirstForKeyboard
                    && KeyboardUtils.isHardKeyboardConnected(context)
                    && state.value.selectedIndex == -1
                ) {
                    moveDown()
                }

                val newHeight = with(density) { 45.dp.toPx() } * items.size
                if (newHeight == 0f) hide()

                updateCompletionWindowPosition()
                setSize(width, minOf(newHeight.toInt(), maxHeight))
                resetScrollPosition()
                if (!isShowing) show()
            },
            editor.editorLanguage.interruptionLevel
        )

        setLoading(true)

        completionJob = scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    language.requireAutoComplete(
                        contentRef,
                        requestPosition,
                        publisher!!,
                        extra
                    )
                }

                if (!isActive || requestTime != requestTimestamp) return@launch

                if (publisher!!.hasData()) {
                    publisher!!.updateList(true)
                } else {
                    editor.postInLifecycle(::hide)
                }

                editor.postInLifecycle { setLoading(false) }
            } catch (_: CompletionCancelledException) {
                Log.v("AutoCompletionWindow", "Completion is cancelled")
            } catch (_: CancellationException) {
                // expected
            } catch (e: Throwable) {
                Log.e("AutoCompletionWindow", "Completion failed", e)
            }
        }
    }

    override fun cancelCompletion() {
        completionJob?.cancel()
        completionJob = null
    }
}
