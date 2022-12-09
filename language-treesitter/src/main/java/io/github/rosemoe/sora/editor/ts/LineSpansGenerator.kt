/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

import com.itsaky.androidide.treesitter.TSNode
import com.itsaky.androidide.treesitter.TSTree
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.lang.Math.max
import java.util.Stack

class LineSpansGenerator(
    private val tree: TSTree, private val lineCount: Int,
    private val content: ContentReference, private val theme: TsTheme
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
        dfsCaptureRegion(tree.rootNode, null, list, startIndex, endIndex, NodeStack(), LastSpanState())
        if (list.isEmpty()) {
            list.add(Span.obtain(0, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)))
        }
        return list
    }

    private fun dfsCaptureRegion(
        node: TSNode,
        nodeName: String?,
        list: MutableList<Span>,
        startIndex: Int,
        endIndex: Int,
        nodeStack: NodeStack,
        lastSpanState: LastSpanState
    ) {
        // Skip this node if it does not contain the given region
        if (node.startByte >= endIndex) {
            // To be consumed by parent
            nodeStack.attributeStack.push(false)
            return
        }
        if (node.type == "ERROR" /* Error node raises too large dfs depth */) {
            nodeStack.attributeStack.push(true)
            val errorStyle = EditorColorScheme.TEXT_NORMAL.toLong()
            list.add(Span.obtain(max(0, node.startByte - startIndex), errorStyle))
            lastSpanState.style = errorStyle
            return
        }
        // Push type stack
        nodeStack.typeStack.push(if (nodeName != null) arrayOf(nodeName, node.type) else arrayOf(node.type))

        // Push color stack if style is set for the rule
        var styled = false
        var nodeStyle = 0L
        if (node.startByte in startIndex..endIndex || (node.startByte < startIndex && node.endByte > startIndex)) {
            val style = theme.resolveStyleForTypeStack(nodeStack.typeStack)
            if (style != 0L && lastSpanState.style != style) {
                styled = true
                list.add(Span.obtain(max(0, node.startByte - startIndex), style))
                lastSpanState.style = style
                nodeStyle = style
            }
        }
        // Visit children
        var childStyled = false
        val cursor = node.walk()
        if (cursor.gotoFirstChild()) {
            do {
                val child = cursor.currentNode
                if (child.startByte >= endIndex) {
                    break
                }
                if (child.endByte >= startIndex) {
                    dfsCaptureRegion(
                        child,
                        if (child.isNamed) cursor.currentFieldName else null,
                        list,
                        startIndex,
                        endIndex,
                        nodeStack,
                        lastSpanState
                    )
                    if (nodeStack.attributeStack.pop()) {
                        childStyled = true
                        if (styled) {
                            list.add(Span.obtain(max(0, child.endByte - startIndex), nodeStyle))
                        }
                    }
                }
            } while (cursor.gotoNextSibling())
        }
        nodeStack.attributeStack.push(styled || childStyled)
        nodeStack.typeStack.pop()
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
            val start = content.getCharPosition(line, 0).index
            val end = start + content.getColumnCount(line)
            spans = captureRegion(start, end)
            if (spans.isEmpty() || spans[0].column > 0) {
                spans.add(0, Span.obtain(0, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)))
            }
            pushCache(line, spans)
        }

        override fun getSpanCount() = spans.size

        override fun getSpanAt(index: Int) = spans[index]

        override fun getSpansOnLine(line: Int): MutableList<Span> {
            val cached = queryCache(line)
            if (cached != null) {
                return ArrayList(cached)
            }
            val start = content.getCharPosition(line, 0).index
            val end = start + content.getColumnCount(line)
            val spans = captureRegion(start, end)
            if (spans.isEmpty() || spans[0].column > 0) {
                spans.add(0, Span.obtain(0, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)))
            }
            return spans
        }

    }

    override fun supportsModify() = false

    override fun modify(): Spans.Modifier {
        throw UnsupportedOperationException()
    }

    override fun getLineCount() = lineCount

}

class NodeStack {

    val typeStack = MyStack<Array<String>>()

    val attributeStack = MyStack<Boolean>()

}

data class SpanCache(val spans: MutableList<Span>, val line: Int)

data class LastSpanState(var style: Long = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))