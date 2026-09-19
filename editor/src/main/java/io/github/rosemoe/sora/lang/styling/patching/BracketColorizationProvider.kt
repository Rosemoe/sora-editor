/*
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
 */
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.styling.color.EditorColor
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.IntPair
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Renders bracket pairs as foreground decorations.
 *
 * Runs as a [StylePatchProvider] on the UI thread: the editor asks for a visible line range and the
 * provider returns patches for exactly those lines, plus the sticky lines the editor keeps on
 * screen. The color of a pair is picked from the bracket highlight slots by its nesting level.
 */
internal class BracketColorizationProvider : StylePatchProvider {

    /** Set by the language; `null` while no bracket-aware analyzer is active. */
    var bracketsProvider: BracketsProvider? = null

    // Newest viewport request of the current frame, answered once on the next frame.
    private var pendingRequest: StylePatchRequest? = null
    private var pendingReceiver: StylePatchProvider.Receiver? = null

    override fun provideStylePatches(
        editor: CodeEditor,
        request: StylePatchRequest,
        receiver: StylePatchProvider.Receiver
    ) {
        // Several viewport updates can arrive within one frame. Only the newest one is answered, and
        // only once, so a scroll does not trigger a bracket query per event.
        val alreadyScheduled = pendingReceiver != null
        pendingRequest = request
        pendingReceiver = receiver
        if (alreadyScheduled) return

        editor.postOnAnimation {
            val latestRequest = pendingRequest ?: return@postOnAnimation
            val latestReceiver = pendingReceiver ?: return@postOnAnimation
            pendingRequest = null
            pendingReceiver = null
            if (editor.isReleased) return@postOnAnimation
            val provider = bracketsProvider
            when {
                // A reset can happen before the editor has installed its new text and layout.
                provider == null -> latestReceiver.set(SparseStylePatches.EMPTY)
                // While the analyzer catches up, the previous patches are kept as they are: they
                // were already shifted by the edit, so they are closer to the truth than nothing.
                !provider.isReadyFor(editor.text) -> Unit
                else -> latestReceiver.set(
                    buildPatches(
                        editor.text,
                        editor.colorScheme,
                        latestRequest.visibleStartLine,
                        latestRequest.visibleEndLine,
                        // Sticky lines are queried separately: spanning the gap between them and the
                        // viewport would scan text that is not on screen.
                        editor.stickyLineIndices
                    )
                )
            }
        }
    }

    /**
     * Build the patches for `[firstLine, lastLine]` plus every line in [stickyLines].
     */
    internal fun buildPatches(
        text: Content,
        scheme: EditorColorScheme,
        firstLine: Int,
        lastLine: Int,
        stickyLines: IntArray = intArrayOf()
    ): SparseStylePatches {
        val provider = bracketsProvider ?: return SparseStylePatches.EMPTY
        val colors = (EditorColorScheme.BRACKET_HIGHLIGHTING_FOREGROUND_1
            ..EditorColorScheme.BRACKET_HIGHLIGHTING_FOREGROUND_6)
            .filter { scheme.getColor(it) != 0 }
            .map { EditorColor(it) }
        if (colors.isEmpty()) return SparseStylePatches.EMPTY

        // Sticky lines stay on screen when scrolled away, so each is queried as its own one-line
        // range. Spanning the gap up to the visible window would scan hidden text.
        val start = firstLine.coerceIn(0, text.lineCount - 1)
        val end = lastLine.coerceIn(start, text.lineCount - 1)
        val ranges = listOf(start..end) + stickyLines.distinct()
            .filter { it in 0 until text.lineCount && it !in start..end }
            .map { it..it }

        val patches = ArrayList<StylePatch>()
        val seen = HashSet<Long>()
        fun addBracket(index: Int, length: Int, level: Int) {
            if (index < 0 || length <= 0 || index > text.length - length) return
            val left = text.indexer.getCharPosition(index).fromThis()
            val right = text.indexer.getCharPosition(index + length)
            // A bracket ending exactly at the start of a range does not belong to it.
            val inside = ranges.any {
                left.line <= it.last && right.line >= it.first &&
                    !(right.line == it.first && right.column == 0)
            }
            // Different ranges may report the same pair, and nested pairs repeat delimiters.
            if (!inside || !seen.add(IntPair.pack(index, length))) return
            patches.add(StylePatch(left.line, left.column, right.line, right.column).apply {
                overrideForeground = colors[Math.floorMod(level, colors.size)]
            })
        }

        for (range in ranges) {
            val pairs = provider.queryPairedBracketsForRange(
                text,
                IntPair.pack(range.first, 0),
                IntPair.pack(range.last, text.getColumnCount(range.last))
            ) ?: continue
            for (pair in pairs) {
                addBracket(pair.leftIndex, pair.leftLength, pair.level)
                addBracket(pair.rightIndex, pair.rightLength, pair.level)
            }
        }

        if (patches.isEmpty()) return SparseStylePatches.EMPTY
        patches.sort() // addPatches() requires sorted input, and nested pairs come out of order.
        return SparseStylePatches().apply { addPatches(patches) }
    }
}
