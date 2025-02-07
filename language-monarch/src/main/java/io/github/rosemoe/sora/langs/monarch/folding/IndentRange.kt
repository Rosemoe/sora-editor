/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.langs.monarch.folding

import io.github.dingyi222666.regex.Regex
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.text.Content

object IndentRange {
    const val MAX_LINE_NUMBER: Int = 0xFFFFFF
    const val MAX_FOLDING_REGIONS: Int = 0xFFFF
    const val MASK_INDENT: Int = -0x1000000

    // START sora-editor note
    // Change String to char[] and int
    // END sora-editor note
    fun computeStartColumn(line: CharArray, len: Int, tabSize: Int): Int {
        var column = 0
        var i = 0

        while (i < len) {
            val chCode = line[i]
            if (chCode == ' ') {
                column++
            } else if (chCode == '\t') {
                column += tabSize
            } else {
                break
            }
            i++
        }

        if (i == len) {
            // line only consists of whitespace
            return -1
        }

        return column
    }

    /**
     * @return :
     * - -1 => the line consists of whitespace
     * - otherwise => the indent level is returned value
     */
    fun computeIndentLevel(line: CharArray, len: Int, tabSize: Int): Int {
        var indent = 0
        var i = 0

        while (i < len) {
            val chCode = line[i]
            if (chCode == ' ') {
                indent++
            } else if (chCode == '\t') {
                indent = indent - indent % tabSize + tabSize
            } else {
                break
            }
            i++
        }

        if (i == len) {
            // line only consists of whitespace
            return -1
        }

        return indent
    }

    fun computeRanges(
        model: Content,
        tabSize: Int,
        offSide: Boolean,
        helper: FoldingHelper,
        pattern: Regex?,
        delegate: AsyncIncrementalAnalyzeManager<*, *>.CodeBlockAnalyzeDelegate
    ): FoldingRegions {
        val result = RangesCollector(/*tabSize*/)
        val previousRegions = mutableListOf<PreviousRegion>().apply {
            add(PreviousRegion(-1, model.getLineCount() + 1, model.getLineCount() + 1))
        }

        for (line in model.lineCount - 1 downTo 0) {
            if (delegate.isCancelled) break
            val indent = helper.getIndentFor(line) // computeIndentLevel(model.getLine(line).getBackingCharArray(), model.getColumnCount(line), tabSize)
            var previous = previousRegions.last()

            if (indent == -1) {
                if (offSide) {
                    // for offSide languages, empty lines are associated to the previous block
                    // note: the next block is already written to the results, so this only
                    // impacts the end position of the block before
                    previous.endAbove = line
                }
                continue // only whitespace
            }
            val m = pattern?.let { helper.getResultFor(line) }

            if (m != null) {
                // folding pattern match
                when {
                    m.count >= 2 -> { // start pattern match
                        // discard all regions until the folding pattern
                        var i = previousRegions.lastIndex
                        while (i > 0 && previousRegions[i].indent != -2) {
                            i--
                        }
                        if (i > 0) {
                            // previousRegions.length = i + 1
                            previous = previousRegions[i]

                            // new folding range from pattern, includes the end line
                            result.insertFirst(line, previous.line, indent)
                            previous.line = line
                            previous.indent = indent
                            previous.endAbove = line
                            continue
                        } else {
                            // no end marker found, treat line as a regular line
                        }
                    }
                    else -> { // end pattern match
                        previousRegions.add(PreviousRegion(-2, line, line))
                        continue
                    }
                }
            }

            if (previous.indent > indent) {
                // discard all regions with larger indent
                while (previousRegions.last().indent > indent) {
                    previousRegions.removeLastOrNull()
                    previous = previousRegions.last()
                }

                // new folding range
                val endLineNumber = previous.endAbove - 1
                if (endLineNumber - line >= 1) { // needs at least size 1
                    result.insertFirst(line, endLineNumber, indent)
                }
            }

            when {
                previous.indent == indent -> previous.endAbove = line
                else -> { // previous.indent < indent
                    // new region with a bigger indent
                    previousRegions.add(PreviousRegion(indent, line, line))
                }
            }
        }
        return result.toIndentRanges(model)
    }
}
