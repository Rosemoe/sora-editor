/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.editor.semantic

import io.github.rosemoe.sora.lang.styling.patching.SparseStylePatches
import io.github.rosemoe.sora.lang.styling.patching.StylePatch
import org.eclipse.lsp4j.SemanticTokensEdit
import org.eclipse.lsp4j.SemanticTokensLegend

internal object SemanticTokensDecoder {
    // LSP edits address the previous integer array, not the array after each preceding edit.
    fun applyDelta(previous: List<Int>, edits: List<SemanticTokensEdit>): List<Int> {
        val result = ArrayList<Int>()
        var offset = 0
        for (edit in edits.sortedBy { it.start }) {
            require(edit.start >= offset && edit.start <= previous.size && edit.deleteCount >= 0)
            val end = edit.start.toLong() + edit.deleteCount
            require(end <= previous.size)
            result.addAll(previous.subList(offset, edit.start))
            edit.data?.let(result::addAll)
            offset = end.toInt()
        }
        result.addAll(previous.subList(offset, previous.size))
        return result
    }

    fun decode(
        data: List<Int>, legend: SemanticTokensLegend, lineLengths: IntArray, lineStarts: IntArray,
        provider: SemanticTokenStyleProvider, languageId: String?
    ): SparseStylePatches {
        require(data.size % 5 == 0) { "Semantic tokens must contain groups of five integers" }
        var line = 0L
        var column = 0L
        val patches = ArrayList<StylePatch>(data.size / 5)
        val styles = HashMap<Pair<Int, Int>, SemanticTokenStyle?>()
        for (i in data.indices step 5) {
            val deltaLine = data[i]
            val deltaColumn = data[i + 1]
            val length = data[i + 2]
            val type = data[i + 3]
            val modifiers = data[i + 4]
            require(deltaLine >= 0 && deltaColumn >= 0 && modifiers >= 0)
            line += deltaLine
            column = if (deltaLine == 0) column + deltaColumn else deltaColumn.toLong()
            if (line >= lineLengths.size || type !in legend.tokenTypes.indices || length <= 0) continue
            val startLine = line.toInt()
            if (column > lineLengths[startLine]) continue
            val start = lineStarts[startLine].toLong() + column
            val end = minOf(start + length, lineStarts.last().toLong() + lineLengths.last())
            if (end <= start) continue
            val endLine = lineStarts.binarySearch(end.toInt()).let { if (it >= 0) it else -it - 2 }
            val endColumn = minOf(end.toInt() - lineStarts[endLine], lineLengths[endLine])
            if (endLine == startLine && endColumn <= column) continue
            val key = type to modifiers
            val style = if (styles.containsKey(key)) styles[key] else {
                val names = legend.tokenModifiers.withIndex().filter {
                    it.index < 31 && modifiers and (1 shl it.index) != 0
                }.mapTo(linkedSetOf()) { it.value }
                provider.getStyle(legend.tokenTypes[type], names, languageId).also { styles[key] = it }
            }
            if (style != null) {
                patches.add(StylePatch(startLine, column.toInt(), endLine, endColumn).apply {
                    overrideForeground = style.foreground
                    overrideBold = style.bold
                    overrideItalics = style.italic
                })
            }
        }
        return if (patches.isEmpty()) SparseStylePatches.EMPTY
        // Relative tokens are ordered by start; equal starts may have arbitrary end positions.
        // Keep every overlap and let StylePatchManager compose attributes in range order.
        else SparseStylePatches().apply { addPatches(patches.sorted()) }
    }
}
