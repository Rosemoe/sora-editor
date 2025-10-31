package io.github.rosemoe.sora.widget.component

import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Defines the contract between [EditorDiagnosticTooltipWindow] and its layout implementation.
 */
interface DiagnosticTooltipLayout {
    /** Bind the host window so the layout can interact with it. */
    fun attach(window: EditorDiagnosticTooltipWindow)
    /** Build the root view that will be shown inside the tooltip popup. */
    fun createView(inflater: LayoutInflater): View
    /** Apply the editor color scheme to all relevant views. */
    fun applyColorScheme(colorScheme: EditorColorScheme)
    /** Render the provided diagnostic information inside the layout. */
    fun renderDiagnostic(diagnostic: DiagnosticDetail?)

    /**
     * Render diagnostic information with its associated region metadata.
     * The default implementation delegates to the legacy overload for backwards compatibility.
     */
    fun renderDiagnostic(diagnostic: DiagnosticDetail?, region: DiagnosticRegion?) {
        renderDiagnostic(diagnostic)
    }

    /**
     * Notify the layout that the editor text size changed (values are in pixels).
     * Default implementation is a no-op for backwards compatibility.
     */
    fun onTextSizeChanged(oldSizePx: Float, newSizePx: Float) {}
    /** Measure the layout and return the width/height that should be used for the popup window. */
    fun measureContent(maxWidth: Int, maxHeight: Int): Pair<Int, Int>
    /** Returns true if the pointer is currently hovering over the popup content. */
    fun isPointerOverPopup(): Boolean
    /** Returns true if the overflow popup/menu is currently showing. */
    fun isMenuShowing(): Boolean
    /** Called when the tooltip window itself is dismissed. */
    fun onWindowDismissed()
}
