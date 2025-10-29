package io.github.rosemoe.sora.lsp.editor.signature

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.lsp4j.SignatureHelp

interface SignatureHelpLayout {
    /** Bind the host window to this layout. */
    fun attach(window: SignatureHelpWindow)
    /** Build the root view that will be shown by the window. */
    fun createView(inflater: LayoutInflater): View
    /** Apply editor colors and fonts to the layout. */
    fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface)
    /** Render the provided signature help content. */
    fun renderSignatures(signatureHelp: SignatureHelp)
    /** Notify the layout that the editor text size changed. */
    fun onTextSizeChanged(oldSize: Float, newSize: Float)
}
