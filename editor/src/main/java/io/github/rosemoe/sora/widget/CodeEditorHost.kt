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

package io.github.rosemoe.sora.widget

import android.content.Context
import android.graphics.Matrix
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedText
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CodeEditorHost {

    val context: Context
    val handler: Handler?

    val attachedView: View

    val width: Int
    val height: Int

    val measuredWidth: Int
    val measuredHeight: Int

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isWrapContentWidth: Boolean

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isWrapContentHeight: Boolean

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isAttachedToWindow: Boolean

    val matrix: Matrix

    var scrollX: Int
    var scrollY: Int

    fun invalidate()
    fun postInvalidate()
    fun postInvalidateOnAnimation()

    fun post(action: Runnable): Boolean
    fun postInLifecycle(action: Runnable): Boolean
    fun removeCallbacks(action: Runnable): Boolean
    fun postDelayedInLifecycle(action: Runnable, delayMillis: Long): Boolean

    fun requestLayout()

    fun getLocationOnScreen(out: IntArray)
    fun getLocationInWindow(out: IntArray)

    fun onSuperKeyDown(keyCode: Int, event: KeyEvent): Boolean
    fun onSuperKeyUp(keyCode: Int, event: KeyEvent): Boolean
    fun onSuperKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun startSearchActionMode()

    val isEnabled: Boolean
    val isInTouchMode: Boolean
    val isFocused: Boolean

    fun requestFocus(): Boolean
    fun requestFocusFromTouch(): Boolean

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun showKeyboard()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun hideKeyboard()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun updateCursorAnchorInfo(info: CursorAnchorInfo)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun restartInput()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun updateSelection(selStart: Int, selEnd: Int, candidatesStart: Int, candidatesEnd: Int)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun updateExtractedText(token: Int, text: ExtractedText)

    fun showContextMenu(x: Float, y: Float): Boolean
    fun showContextMenu(): Boolean
}
