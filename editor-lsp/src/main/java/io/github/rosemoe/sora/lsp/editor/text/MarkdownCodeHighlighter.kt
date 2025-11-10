package io.github.rosemoe.sora.lsp.editor.text

import android.graphics.Typeface
import android.text.Spanned

interface MarkdownCodeHighlighter {
    val isAsync: Boolean get() = false

    fun highlight(code: String, language: String?, codeTypeface: Typeface): Spanned

    suspend fun highlightAsync(code: String, language: String?, codeTypeface: Typeface): Spanned = highlight(code, language, codeTypeface)
}

abstract class AsyncMarkdownCodeHighlighter : MarkdownCodeHighlighter {
    override val isAsync: Boolean get() = true

    override fun highlight(code: String, language: String?, codeTypeface: Typeface): Spanned {
        throw UnsupportedOperationException("Use highlightAsync for async highlighter")
    }

    abstract override suspend fun highlightAsync(code: String, language: String?, codeTypeface: Typeface): Spanned
}
