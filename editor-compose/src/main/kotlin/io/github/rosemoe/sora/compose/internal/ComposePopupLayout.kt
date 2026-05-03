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

@file:OptIn(ExperimentalUuidApi::class)

package io.github.rosemoe.sora.compose.internal

import android.annotation.SuppressLint
import android.graphics.Outline
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun View.createPopupLayout(content: @Composable () -> Unit): View {
    val parent = findViewTreeCompositionContext()
        ?: error("Unable to find a CompositionContext in the view tree. Ensure the View is attached to a window.")

    return ComposePopupLayout(this).apply {
        setContent(parent = parent) {
            val layoutDirection = LocalLayoutDirection.current

            SideEffect {
                superSetLayoutDirection(layoutDirection)
            }

            content()
        }
    }
}

@SuppressLint("ViewConstructor")
private class ComposePopupLayout(composeView: View) : AbstractComposeView(composeView.context), ViewRootForInspector {

    private val density = Density(context)

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services.
    private val maxSupportedElevation = 8.dp

    override val subCompositionView: AbstractComposeView
        get() = this

    private val snapshotStateObserver =
        SnapshotStateObserver(
            onChangedExecutor = { command ->
                // This is the same executor logic used by AndroidComposeView's
                // OwnerSnapshotObserver, which
                // drives most of the state observation in compose UI.
                if (handler?.looper === Looper.myLooper()) {
                    command()
                } else {
                    handler?.post(command)
                }
            }
        )

    init {
        id = android.R.id.content
        setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        setViewTreeSavedStateRegistryOwner(composeView.findViewTreeSavedStateRegistryOwner())
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

        // Set unique id for AbstractComposeView. This allows state restoration for the state
        // defined inside the Popup via rememberSaveable()
        setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "Popup:${Uuid.random()}")

        // Enable children to draw their shadow by not clipping them
        clipChildren = false
        // Allocate space for elevation
        with(density) { elevation = maxSupportedElevation.toPx() }
        // Simple outline to force window manager to allocate space for shadow.
        // Note that the outline affects clickable area for the dismiss listener. In case of shapes
        // like circle the area for dismiss might be to small (rectangular outline consuming clicks
        // outside of the circle).
        outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, result: Outline) {
                    result.setRect(0, 0, view.width, view.height)
                    // We set alpha to 0 to hide the view's shadow and let the composable to draw
                    // its
                    // own shadow. This still enables us to get the extra space needed in the
                    // surface.
                    result.alpha = 0f
                }
            }
    }

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    @Composable
    @UiComposable
    override fun Content() {
        content()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        snapshotStateObserver.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapshotStateObserver.stop()
        snapshotStateObserver.clear()
    }

    override fun setLayoutDirection(layoutDirection: Int) {
        // Do nothing. ViewRootImpl will call this method attempting to set the layout direction
        // from the context's locale, but we have one already from the parent composition.
    }

    // Sets the "real" layout direction for our content that we obtain from the parent composition.
    fun superSetLayoutDirection(layoutDirection: LayoutDirection) {
        val direction =
            when (layoutDirection) {
                LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
                LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
            }
        super.setLayoutDirection(direction)
    }
}
