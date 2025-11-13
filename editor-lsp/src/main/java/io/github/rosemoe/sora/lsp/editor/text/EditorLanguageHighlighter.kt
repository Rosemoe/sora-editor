package io.github.rosemoe.sora.lsp.editor.text

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EditorMarkdownCodeHighlighterProvider(
    val editorContextProvider: (languageName: LanguageName) -> Pair<Language, EditorColorScheme>?
) : MarkdownCodeHighlighterRegistry.CodeHighlighterProvider {
    override fun provide(language: LanguageName): MarkdownCodeHighlighter? {
        val context = editorContextProvider(language) ?: return null
        return EditorMarkdownCodeHighlighter(context.first, context.second)
    }
}

fun MarkdownCodeHighlighterRegistry.withEditorHighlighter(editorContextProvider: (languageName: LanguageName) -> Pair<Language, EditorColorScheme>?) {
    withProvider(EditorMarkdownCodeHighlighterProvider(editorContextProvider))
}

class EditorMarkdownCodeHighlighter(
    val editorLanguage: Language,
    val editorSchema: EditorColorScheme
) :
    AsyncMarkdownCodeHighlighter() {

    private val mutex = Mutex()

    override suspend fun highlightAsync(
        code: String,
        language: String?,
        codeTypeface: Typeface
    ): Spanned = mutex.withLock {
        val content = Content(code)
        val analyzeManager = editorLanguage.analyzeManager
        try {
            suspendCancellableCoroutine { continuation ->
                val resumed = AtomicBoolean(false)
                val receiver = object : EmptyStyleReceiver() {
                    override fun setStyles(sourceManager: AnalyzeManager, styles: Styles?) {
                        if (styles == null || !resumed.compareAndSet(false, true)) {
                            return
                        }
                        val result = runCatching {
                            styles.toSpanned(
                                content,
                                editorSchema,
                                codeTypeface
                            )
                        }
                        continuation.resumeWith(result)
                    }
                }
                continuation.invokeOnCancellation {
                    analyzeManager.setReceiver(null)
                }
                analyzeManager.setReceiver(receiver)
                runCatching {
                    analyzeManager.reset(ContentReference(content), Bundle())
                }.onFailure { error ->
                    if (resumed.compareAndSet(false, true)) {
                        continuation.resumeWith(Result.failure(error))
                    }
                }
            }
        } finally {
            analyzeManager.setReceiver(null)
        }
    }

    private fun Styles.toSpanned(
        content: Content,
        colorScheme: EditorColorScheme,
        typeface: Typeface
    ): Spanned {
        val builder = SpannableStringBuilder(content.toString())
        val spans = this.spans ?: return builder
        val reader = spans.read()
        val lineCount = content.lineCount
        for (lineIndex in 0 until lineCount) {
            reader.moveToLine(lineIndex)
            val spanCount = reader.getSpanCount()
            val lineStart = content.getCharIndex(lineIndex, 0)
            val lineEnd = content.getCharIndex(lineIndex, content.getColumnCount(lineIndex))
            for (i in 0 until spanCount) {
                val span = reader.getSpanAt(i)
                val start = lineStart + span.column
                val end = if (i < spanCount - 1) {
                    lineStart + reader.getSpanAt(i + 1).column
                } else {
                    lineEnd
                }
                if (start >= end || start >= builder.length) continue
                applySpan(
                    builder,
                    span,
                    start,
                    end.coerceAtMost(builder.length),
                    colorScheme,
                    typeface
                )
            }
        }
        return builder
    }

    private fun applySpan(
        builder: SpannableStringBuilder,
        span: Span,
        start: Int,
        end: Int,
        colorScheme: EditorColorScheme,
        typeface: Typeface
    ) {
        val style = span.style
        val foregroundColorId = TextStyle.getForegroundColorId(style)

        val foregroundColor = colorScheme.getColor(foregroundColorId)

        builder.setSpan(
            ForegroundColorSpan(foregroundColor),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (TextStyle.isBold(style)) {
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (TextStyle.isItalics(style)) {
            builder.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (TextStyle.isStrikeThrough(style)) {
            builder.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

open class EmptyStyleReceiver : StyleReceiver {
    override fun setStyles(
        sourceManager: AnalyzeManager,
        styles: Styles?
    ) {
        setStyles(sourceManager, styles, null)
    }

    override fun setStyles(
        sourceManager: AnalyzeManager,
        styles: Styles?,
        action: Runnable?
    ) {
    }

    override fun setDiagnostics(
        sourceManager: AnalyzeManager,
        diagnostics: DiagnosticsContainer?
    ) {
    }

    override fun updateBracketProvider(
        sourceManager: AnalyzeManager,
        provider: BracketsProvider?
    ) {
    }
}
