package io.github.rosemoe.sora.lsp.editor.hover

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import io.github.rosemoe.sora.lsp.R
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkedString
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.jsonrpc.messages.Either

class DefaultHoverLayout : HoverLayout {
    private lateinit var window: HoverWindow
    private lateinit var root: View
    private lateinit var container: ScrollView
    private lateinit var hoverTextView: TextView
    private var textColor: Int = 0

    override fun attach(window: HoverWindow) {
        this.window = window
    }

    override fun createView(inflater: LayoutInflater): View {
        root = inflater.inflate(R.layout.hover_tooltip_window, null, false)
        container = root.findViewById(R.id.hover_scroll_container)
        hoverTextView = root.findViewById(R.id.hover_text)
        return root
    }

    override fun applyColorScheme(colorScheme: EditorColorScheme, typeface: Typeface) {
        val editor = window.editor
        textColor = colorScheme.getColor(EditorColorScheme.SIGNATURE_TEXT_NORMAL)
        hoverTextView.typeface = typeface
        hoverTextView.setTextColor(textColor)

        val drawable = GradientDrawable().apply {
            cornerRadius = editor.dpUnit * 8
            setColor(colorScheme.getColor(EditorColorScheme.SIGNATURE_BACKGROUND))
        }
        root.background = drawable
    }

    override fun renderHover(hover: Hover) {
        hoverTextView.text = buildHoverText(hover)
        container.post { container.smoothScrollTo(0, 0) }
    }

    private fun buildHoverText(hover: Hover): CharSequence {
        val hoverContents = hover.contents ?: return ""
        return if (hoverContents.isLeft) {
            val items = hoverContents.left.orEmpty()
            items.joinToString("\n\n") { either -> formatMarkedStringEither(either) }
        } else {
            val markup = hoverContents.right
            formatMarkupContent(markup)
        }
    }

    private fun formatMarkedStringEither(either: Either<String, MarkedString>?): String {
        if (either == null) {
            return ""
        }
        return if (either.isLeft) {
            either.left ?: ""
        } else {
            formatMarkedString(either.right)
        }
    }

    private fun formatMarkedString(markedString: MarkedString?): String {
        if (markedString == null) {
            return ""
        }
        val language = markedString.language
        val value = markedString.value ?: return ""
        if (language.isNullOrEmpty()) {
            return value
        }
        return "$language:\n$value"
    }

    private fun formatMarkupContent(markupContent: MarkupContent?): String {
        if (markupContent == null) {
            return ""
        }
        val value = markupContent.value
        return value ?: ""
    }
}
