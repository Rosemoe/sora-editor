/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.widget

import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.styling.color.EditorColor
import io.github.rosemoe.sora.lang.styling.patching.SparseStylePatches
import io.github.rosemoe.sora.lang.styling.patching.StylePatch
import io.github.rosemoe.sora.lang.styling.patching.StylePatchProvider
import io.github.rosemoe.sora.lang.styling.patching.StylePatchRequest
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/** Converts language bracket queries to foreground decorations on the UI thread. */
internal class BracketColorizationProvider : StylePatchProvider {
    var bracketsProvider: BracketsProvider? = null

    override fun provideStylePatches(
        editor: CodeEditor,
        request: StylePatchRequest,
        receiver: StylePatchProvider.Receiver
    ) {
        // A reset can happen before the editor has installed its new text/layout.
        if (bracketsProvider == null) {
            receiver.set(SparseStylePatches.EMPTY)
            return
        }
        val stickyLines = editor.renderer.stuckCodeBlocks.orEmpty().map { it.startLine }
        receiver.set(buildPatches(editor.text, editor.colorScheme,
            request.visibleStartLine, request.visibleEndLine, stickyLines))
    }

    internal fun buildPatches(
        text: Content,
        scheme: EditorColorScheme,
        firstLine: Int,
        lastLine: Int,
        stickyLines: List<Int>
    ): SparseStylePatches {
        val provider = bracketsProvider ?: return SparseStylePatches.EMPTY
        val colors = (EditorColorScheme.BRACKET_HIGHLIGHTING_FOREGROUND_1..
            EditorColorScheme.BRACKET_HIGHLIGHTING_FOREGROUND_6)
            .filter { scheme.getColor(it) != 0 }
            .map { EditorColor(it) }
        if (colors.isEmpty()) return SparseStylePatches.EMPTY

        val start = firstLine.coerceIn(0, text.lineCount - 1)
        val end = lastLine.coerceIn(start, text.lineCount - 1)
        val ranges = mutableListOf(start..end)
        stickyLines.distinct().filter { it in 0 until text.lineCount && it !in start..end }
            .forEach { ranges.add(it..it) }
        val patches = ArrayList<StylePatch>()
        val seen = HashSet<Long>()

        for (range in ranges) {
            // Include the final line's text, including the last line of the document.
            val pairs = provider.queryPairedBracketsForRange(text,
                IntPair.pack(range.first, 0),
                IntPair.pack(range.last, text.getColumnCount(range.last))) ?: continue
            fun addBracket(index: Int, length: Int, level: Int) {
                if (index < 0 || length <= 0 || index > text.length - length) return
                val left = text.indexer.getCharPosition(index).fromThis()
                val right = text.indexer.getCharPosition(index + length)
                if (left.line > range.last || right.line < range.first ||
                    (right.line == range.first && right.column == 0)) return
                if (!seen.add(IntPair.pack(index, length))) return
                patches.add(StylePatch(left.line, left.column, right.line, right.column).apply {
                    overrideForeground = colors[Math.floorMod(level, colors.size)]
                })
            }
            for (pair in pairs) {
                addBracket(pair.leftIndex, pair.leftLength, pair.level)
                addBracket(pair.rightIndex, pair.rightLength, pair.level)
            }
        }
        return if (patches.isEmpty()) SparseStylePatches.EMPTY else SparseStylePatches().apply {
            addPatches(patches)
        }
    }
}
