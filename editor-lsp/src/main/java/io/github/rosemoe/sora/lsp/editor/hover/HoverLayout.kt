package io.github.rosemoe.sora.lsp.editor.hover

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.lsp4j.Hover

interface HoverLayout {
    /** Bind the host window to this layout. */
    fun attach(window: HoverWindow)
    /** Build the root view that will be shown by the window. */
    fun createView(inflater: LayoutInflater): View
    /** Apply editor colors and fonts to the layout. */
    fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface)
    /** Render the provided hover content. */
    fun renderHover(hover: Hover)
    /** Notify the layout that the editor text size changed. */
    fun onTextSizeChanged(oldSize: Float, newSize: Float)
}
