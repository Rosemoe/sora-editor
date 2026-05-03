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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Constraints
import io.github.rosemoe.sora.util.EditorHandler
import io.github.rosemoe.sora.widget.CodeEditorHost
import io.github.rosemoe.sora.widget.EditorInputConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal fun interface Invalidator {
    fun invalidate()
}

@Suppress("PropertyName")
internal class CodeEditorHostImpl(
    override val context: Context,
    private val view: View,
    internal val focusRequester: FocusRequester,
    private val coroutineScope: CoroutineScope,
    private val keyboardController: SoftwareKeyboardController?
) : CodeEditorHost {

    internal val constraintsChannel = Channel<Constraints>(Channel.CONFLATED)
    private val invalidateChannel = Channel<Unit>(Channel.CONFLATED)
    val invalidateFlow = invalidateChannel.receiveAsFlow()

    internal var _width = 0
    internal var _height = 0

    private var _isWrapContentWidth = true
    private var _isWrapContentHeight = true

    internal var _isEnabled = true
    internal var _isFocused = false

    val imm by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    }

    internal lateinit var inputConnection: EditorInputConnection

    /**
     * @see io.github.rosemoe.sora.compose.internal.modifier.createInputRequest
     */
    internal var immView = view

    init {
        coroutineScope.launch {
            constraintsChannel.receiveAsFlow().collectLatest { constraints ->
                _width = constraints.maxWidth
                _height = constraints.maxHeight
                _isWrapContentWidth = !constraints.hasFixedWidth
                _isWrapContentHeight = !constraints.hasFixedHeight
            }
        }
    }

    internal val disallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()

    /**
     * @see io.github.rosemoe.sora.compose.internal.modifier.LayoutModifier.onGloballyPositioned
     */
    internal var positionOnScreen = Offset.Zero
    internal var positionInWindow = Offset.Zero

    override val handler: Handler? get() = view.handler
    override val attachedView: View get() = view

    override val width get() = _width
    override val height get() = _height

    override val measuredWidth get() = _width
    override val measuredHeight get() = _height

    override val isWrapContentWidth get() = _isWrapContentWidth
    override val isWrapContentHeight get() = _isWrapContentHeight

    internal var isAttached: Boolean? = null
    override val isAttachedToWindow: Boolean get() = isAttached ?: view.isAttachedToWindow
    override val isInTouchMode: Boolean get() = view.isInTouchMode

    override val isEnabled get() = _isEnabled
    override val isFocused get() = _isFocused

    override val matrix = Matrix()

    private var _scrollX = 0
    private var _scrollY = 0

    override var scrollX: Int
        get() = _scrollX
        set(value) {
            if (_scrollX != value) {
                _scrollX = value
                invalidate()
            }
        }
    override var scrollY: Int
        get() = _scrollY
        set(value) {
            if (_scrollY != value) {
                _scrollY = value
                invalidate()
            }
        }

    override fun invalidate() {
        val result = invalidateChannel.trySend(Unit)
        if (result.isFailure) {
            Log.d("CodeEditor", "Failed to invalidate", result.exceptionOrNull())
        }
    }

    override fun postInvalidate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate()
        } else {
            post { invalidate() }
        }
    }

    override fun postInvalidateOnAnimation() = invalidate()

    override fun post(action: Runnable): Boolean {
        return view.post(action)
    }

    override fun postInLifecycle(action: Runnable): Boolean {
        return EditorHandler.post(action)
    }

    override fun removeCallbacks(action: Runnable): Boolean {
        EditorHandler.removeCallbacks(action)
        return attachedView.removeCallbacks(action)
    }

    override fun postDelayedInLifecycle(action: Runnable, delayMillis: Long): Boolean {
        return EditorHandler.postDelayed(action, delayMillis)
    }

    override fun requestLayout() {
        invalidate()
        // Layout is handled by Compose; no-op or trigger re-measurement if needed via channels
    }

    override fun getLocationOnScreen(out: IntArray) {
        out[0] = positionOnScreen.x.toInt()
        out[1] = positionOnScreen.y.toInt()
    }

    override fun getLocationInWindow(out: IntArray) {
        out[0] = positionInWindow.x.toInt()
        out[1] = positionInWindow.y.toInt()
    }

    override fun onSuperKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onSuperKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onSuperKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        disallowInterceptTouchEvent(disallowIntercept)
    }

    override fun startSearchActionMode() {
        // TODO
    }

    override fun requestFocus() = focusRequester.requestFocus()
    override fun requestFocusFromTouch() = focusRequester.requestFocus()

    internal val showKeyboardChannel = Channel<Unit>(Channel.CONFLATED)
    internal val hideKeyboardChannel = Channel<Unit>(Channel.CONFLATED)

    override fun showKeyboard() {
        //imm?.showSoftInput(immView, 0)
        //keyboardController?.show()
        showKeyboardChannel.trySend(Unit)
    }

    override fun hideKeyboard() {
        //imm?.hideSoftInputFromWindow(immView.windowToken, 0)
        hideKeyboardChannel.trySend(Unit)
        keyboardController?.hide()
    }

    override fun updateCursorAnchorInfo(info: CursorAnchorInfo) {
        imm?.updateCursorAnchorInfo(immView, info)
    }

    @SuppressLint("RestrictedApi")
    override fun restartInput() {
        if (::inputConnection.isInitialized) {
            inputConnection.reset()
        }
        imm?.restartInput(immView)
    }

    override fun updateSelection(selStart: Int, selEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        imm?.updateSelection(immView, selStart, selEnd, candidatesStart, candidatesEnd)
    }

    override fun updateExtractedText(token: Int, text: ExtractedText) {
        imm?.updateExtractedText(immView, token, text)
    }

    override fun showContextMenu(x: Float, y: Float): Boolean {
        return false
    }

    override fun showContextMenu(): Boolean {
        return false
    }
}
