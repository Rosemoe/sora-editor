/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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

package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSQueryCapture
import com.itsaky.androidide.treesitter.TSQueryCursor
import com.itsaky.androidide.treesitter.TSTree
import io.github.rosemoe.sora.editor.ts.spans.TsSpanFactory
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Spans generator for tree-sitter. Results are cached.
 *
 * Note that this implementation does not support external modifications.
 *
 * @author Rosemoe
 */
class LineSpansGenerator(
    internal var tree: TSTree, internal var lineCount: Int,
    private val content: Content, internal var theme: TsTheme,
    private val languageSpec: TsLanguageSpec, var scopedVariables: TsScopedVariables,
    private val spanFactory: TsSpanFactory
) : Spans {

    companion object {
        const val CACHE_THRESHOLD = 60
    }

    private val caches = mutableListOf<SpanCache>()

    fun queryCache(line: Int): MutableList<Span>? {
        for (i in 0 until caches.size) {
            val cache = caches[i]
            if (cache.line == line) {
                caches.removeAt(i)
                caches.add(0, cache)
                return cache.spans
            }
        }
        return null
    }

    fun pushCache(line: Int, spans: MutableList<Span>) {
        while (caches.size >= CACHE_THRESHOLD && caches.size > 0) {
            caches.removeAt(caches.size - 1)
        }
        caches.add(0, SpanCache(spans, line))
    }

    fun captureRegion(startIndex: Int, endIndex: Int): MutableList<Span> {
        val list = mutableListOf<Span>()
        val captures = mutableListOf<TSQueryCapture>()
        TSQueryCursor().use { cursor ->
            cursor.setByteRange(startIndex * 2, endIndex * 2)
            cursor.exec(languageSpec.tsQuery, tree.rootNode)
            var match = cursor.nextMatch()
            while (match != null) {
                if (languageSpec.queryPredicator.doPredicate(languageSpec.predicates, content, match)) {
                    captures.addAll(match.captures)
                }
                match = cursor.nextMatch()
            }
            captures.sortBy { it.node.startByte }
            var lastIndex = 0
            captures.forEach { capture ->
                val startByte = capture.node.startByte
                val endByte = capture.node.endByte
                val start = (startByte / 2 - startIndex).coerceAtLeast(0)
                val pattern = capture.index
                // Do not add span for overlapping regions and out-of-bounds regions
                if (start >= lastIndex && endByte / 2 >= startIndex && startByte / 2 < endIndex
                    && (pattern !in languageSpec.localsScopeIndices && pattern !in languageSpec.localsDefinitionIndices
                            && pattern !in languageSpec.localsDefinitionValueIndices && pattern !in languageSpec.localsMembersScopeIndices)
                ) {
                    if (start != lastIndex) {
                        list.add(
                            createSpan(
                                capture,
                                lastIndex,
                                theme.normalTextStyle
                            )
                        )
                    }
                    var style = 0L
                    if (capture.index in languageSpec.localsReferenceIndices) {
                        val def = scopedVariables.findDefinition(
                            startByte / 2,
                            endByte / 2,
                            content.substring(startByte / 2, endByte / 2)
                        )
                        if (def != null && def.matchedHighlightPattern != -1) {
                            style = theme.resolveStyleForPattern(def.matchedHighlightPattern)
                        }
                        // This reference can not be resolved to its definition
                        // but it can have its own fallback color by other captures
                        // so continue to next capture
                        if (style == 0L) {
                            return@forEach
                        }
                    }
                    if (style == 0L) {
                        style = theme.resolveStyleForPattern(capture.index)
                    }
                    if (style == 0L) {
                        style = theme.normalTextStyle
                    }
                    list.add(createSpan(capture, start, style))
                    lastIndex = (endByte / 2 - startIndex).coerceAtMost(endIndex)
                }
            }
            if (lastIndex != endIndex) {
                list.add(emptySpan(lastIndex))
            }
        }
        if (list.isEmpty()) {
            list.add(emptySpan(0))
        }
        return list
    }

    private fun createSpan(capture: TSQueryCapture, column: Int, style: Long) : Span {
        return spanFactory.createSpan(capture, column, style)
    }

    private fun emptySpan(column: Int) : Span {
        return Span.obtain(column, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
    }

    override fun adjustOnInsert(start: CharPosition, end: CharPosition) {

    }

    override fun adjustOnDelete(start: CharPosition, end: CharPosition) {

    }

    override fun read() = object : Spans.Reader {

        private var spans = mutableListOf<Span>()

        override fun moveToLine(line: Int) {
            if (line < 0 || line >= lineCount) {
                spans = mutableListOf()
                return
            }
            val cached = queryCache(line)
            if (cached != null) {
                spans = cached
                return
            }
            val start = content.indexer.getCharPosition(line, 0).index
            val end = start + content.getColumnCount(line)
            spans = captureRegion(start, end)
            pushCache(line, spans)
        }

        override fun getSpanCount() = spans.size

        override fun getSpanAt(index: Int) = spans[index]

        override fun getSpansOnLine(line: Int): MutableList<Span> {
            val cached = queryCache(line)
            if (cached != null) {
                return ArrayList(cached)
            }
            val start = content.indexer.getCharPosition(line, 0).index
            val end = start + content.getColumnCount(line)
            return captureRegion(start, end)
        }

    }

    override fun supportsModify() = false

    override fun modify(): Spans.Modifier {
        throw UnsupportedOperationException()
    }

    override fun getLineCount() = lineCount
}

data class SpanCache(val spans: MutableList<Span>, val line: Int)
