package io.github.rosemoe.sora.graphics.inline

import android.graphics.Canvas
import android.view.KeyEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorKeyEvent
import io.github.rosemoe.sora.graphics.InlineElementRenderer
import io.github.rosemoe.sora.graphics.Paint
import io.github.rosemoe.sora.lang.completion.inline.InlineCompletionItem
import io.github.rosemoe.sora.lang.completion.inline.InlineCompletionProvider
import io.github.rosemoe.sora.lang.completion.inline.InlineCompletionRequest
import io.github.rosemoe.sora.lang.completion.inline.TriggerKind
import io.github.rosemoe.sora.lang.styling.inline.GhostText
import io.github.rosemoe.sora.lang.styling.inline.InlineElementContainer
import io.github.rosemoe.sora.lang.styling.inline.InlineElementRenderParams
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.subscribeAlways

internal class GhostTextRenderer(private val editor: CodeEditor) : InlineElementRenderer<GhostText>() {

    private val localPaint = Paint().also { it.isAntiAlias = true }

    override val name: String = GhostText.NAME

    init {
        editor.subscribeAlways<EditorKeyEvent> { event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_TAB -> {
                        if (editor.acceptGhostText()) {
                            val cursor = editor.cursor
                            editor.postDelayed({
                                // remove inserted tab (idk what is the best way to do this)
                                editor.text.delete(cursor.right - editor.tabWidth, cursor.right)
                            }, 5)
                        }
                    }

                    KeyEvent.KEYCODE_ESCAPE -> editor.removeGhostText()
                }
            }
        }

        editor.subscribeAlways<ContentChangeEvent> {
            editor.scheduleGhostRecompute()
        }
    }

    private var ghostRunnable: Runnable? = null

    private fun CodeEditor.scheduleGhostRecompute() {
        removeGhostText()

        ghostRunnable?.let { removeCallbacks(it) }
        ghostRunnable = Runnable { computeGhostText() }

        postDelayed(ghostRunnable!!, 80) // debounce typing
    }

    private fun CodeEditor.acceptGhostText(): Boolean {
        val ghost = inlineElements?.toList()?.filterIsInstance<GhostText>()?.firstOrNull() ?: return false
        commitText(ghost.text)

        inlineElements?.remove(ghost)
        setInlineElements(inlineElements)
        return true
    }

    private fun CodeEditor.removeGhostText() {
        val container = inlineElements ?: return
        val list = container.toList().filterIsInstance<GhostText>()
        if (list.isEmpty()) return

        list.forEach { container.remove(it) }
        setInlineElements(container)
    }

    private fun CodeEditor.computeGhostText() {
        val provider = inlineCompletionProvider
            ?: InlineCompletionProvider { editorLanguage.requireInlineCompletion(it) }

        val line = text.getLine(cursor.leftLine)
        val request = InlineCompletionRequest(text, line, cursor, TriggerKind.Typing)

        provider.provideInlineCompletions(request).let { result ->
            if (result == null || result.items.isEmpty()) {
                removeGhostText()
                return
            }

            val current = currentWord()

            val best = pickBestItem(result.items, current)
            if (best != null) {
                showInlineCompletionItem(best)
            } else {
                removeGhostText()
            }
        }
    }

    private fun CodeEditor.showInlineCompletionItem(item: InlineCompletionItem) {
        removeGhostText()

        val suggestion = if (item.range == null) {
            val prefix = item.filterText ?: currentWord()
            item.insertText.removePrefix(prefix)
        } else {
            item.insertText
        }

        if (suggestion.isNotEmpty()) {
            val ghost = GhostText(cursor.leftLine, cursor.leftColumn, suggestion)
            val container = inlineElements ?: InlineElementContainer()
            container.add(ghost)
            setInlineElements(container)
        }
    }

    private fun CodeEditor.currentWord(): CharSequence {
        val offset = cursor.left
        if (offset <= 0) return ""
        return text.take(offset).takeLastWhile { it.isLetterOrDigit() }
    }

    private fun pickBestItem(
        items: List<InlineCompletionItem>,
        currentWord: CharSequence
    ): InlineCompletionItem? {
        var best: InlineCompletionItem? = null
        var bestScore = -1

        for (item in items) {
            val score = matchScore(item, currentWord)
            if (score > bestScore) {
                best = item
                bestScore = score
            }
        }

        return best
    }

    private fun matchScore(item: InlineCompletionItem, currentWord: CharSequence): Int {
        val filter = item.filterText ?: currentWord

        if (!item.insertText.startsWith(filter)) {
            return -1 // not a valid match
        }

        // return how much matched
        return filter.length
    }

    override fun onMeasure(
        element: GhostText,
        paint: Paint,
        params: InlineElementRenderParams
    ) = paint.measureText(element.text)

    override fun onRender(
        element: GhostText,
        canvas: Canvas,
        paint: Paint,
        params: InlineElementRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        localPaint.typeface = paint.typeface
        localPaint.textSize = paint.textSize

        localPaint.color = colorScheme.getColor(EditorColorScheme.GHOST_TEXT_FOREGROUND)

        val baseline = params.textBaseline

        canvas.drawText(
            element.text,
            0f,
            baseline.toFloat(),
            localPaint
        )
    }
}
