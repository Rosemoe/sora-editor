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
package io.github.rosemoe.sora.langs.textmate.brackets.ast

internal object EditCombiner {

    fun combine(first: List<EditInfo>, second: List<EditInfo>): List<EditInfo> {
        if (first.isEmpty()) return second
        if (second.isEmpty()) return first

        val firstEdits = first.toTextEdits()
        val secondEdits = second.toTextEdits()

        val s0ToS1Queue = ArrayDeque(toLengthMappings(firstEdits))
        var currentS0Map: LengthMapping? = s0ToS1Queue.poll()

        val result = mutableListOf<TextEdit>()
        var s0Offset = Length.ZERO

        fun nextS0MapsFor(targetLengthAfter: Length?): List<LengthMapping> {
            if (targetLengthAfter == null) {
                val remaining = mutableListOf<LengthMapping>()
                currentS0Map?.let { remaining.add(it); currentS0Map = null }
                while (true) {
                    val next = s0ToS1Queue.poll() ?: break
                    remaining.add(next)
                }
                return remaining
            }

            var remainingAfter = targetLengthAfter!!
            val collected = mutableListOf<LengthMapping>()
            while (!remainingAfter.isZero()) {
                if (currentS0Map == null) {
                    currentS0Map = s0ToS1Queue.poll() ?: break
                }
                val (head, tail) = currentS0Map!!.splitAt(remainingAfter)
                collected.add(head)
                remainingAfter = head.lengthAfter.diffNonNegative(remainingAfter)
                currentS0Map = tail
            }

            if (!remainingAfter.isZero()) {
                collected.add(
                    LengthMapping(
                        modified = false,
                        lengthBefore = remainingAfter,
                        lengthAfter = remainingAfter
                    )
                )
            }

            return collected
        }

        fun pushEdit(startOffset: Length, oldLength: Length, newLength: Length) {
            if (oldLength.isZero() && newLength.isZero()) {
                return
            }
            val last = result.lastOrNull()
            if (last != null && last.endOffset == startOffset) {
                result[result.lastIndex] = TextEdit(
                    startOffset = last.startOffset,
                    oldLength = last.oldLength + oldLength,
                    newLength = last.newLength + newLength
                )
            } else {
                result.add(TextEdit(startOffset, oldLength, newLength))
            }
        }

        fun processMappings(mappings: List<LengthMapping>, modified: Boolean, lengthAfter: Length) {
            if (modified) {
                val s0Length = mappings.sumLengths { it.lengthBefore }
                val s0End = s0Offset + s0Length
                pushEdit(s0Offset, s0Length, lengthAfter)
                s0Offset = s0End
            } else {
                for (map in mappings) {
                    val start = s0Offset
                    s0Offset += map.lengthBefore
                    if (map.modified) {
                        pushEdit(start, map.lengthBefore, map.lengthAfter)
                    }
                }
            }
        }

        val s1ToS2 = toLengthMappings(secondEdits)

        // Mirror VS Code's sentinel workflow: process all real mappings and then
        // run once more with an implicit "copy-tail" entry to flush remaining data.
        for (i in 0..s1ToS2.size) {
            if (i == s1ToS2.size) {
                val s0Tail = nextS0MapsFor(null)
                processMappings(s0Tail, false, Length.ZERO)
                break
            }

            val mapping = s1ToS2[i]
            val s0Maps = nextS0MapsFor(mapping.lengthBefore)
            processMappings(s0Maps, mapping.modified, mapping.lengthAfter)
        }

        val combined = result.map { it.toEditInfo() }
        return combined
    }

    private fun List<EditInfo>.toTextEdits(): List<TextEdit> {
        if (isEmpty()) return emptyList()
        return ArrayList<TextEdit>(size).also { list ->
            for (edit in this) {
                val startOffset = edit.toPosition().toLength()
                val oldLength = edit.toOldLength()
                val endOffset = startOffset + oldLength
                val newLength = edit.toNewLength()
                list.add(TextEdit(startOffset, oldLength, newLength, endOffset))
            }
        }
    }

    private fun toLengthMappings(textEdits: List<TextEdit>): List<LengthMapping> {
        if (textEdits.isEmpty()) return emptyList()

        val result = ArrayList<LengthMapping>(textEdits.size * 2)
        var lastOffset = Length.ZERO
        for (edit in textEdits) {
            val spaceLength = lastOffset.diffNonNegative(edit.startOffset)
            if (!spaceLength.isZero()) {
                result.add(LengthMapping(false, spaceLength, spaceLength))
            }

            result.add(LengthMapping(true, edit.oldLength, edit.newLength))
            lastOffset = edit.endOffset
        }
        return result
    }

    private fun LengthMapping.splitAt(lengthAfter: Length): Pair<LengthMapping, LengthMapping?> {
        val remaining = lengthAfter.diffNonNegative(this.lengthAfter)
        if (remaining.isZero()) {
            return this to null
        }
        return if (modified) {
            val head = LengthMapping(true, lengthBefore, lengthAfter)
            val tail = LengthMapping(true, Length.ZERO, remaining)
            head to tail
        } else {
            val head = LengthMapping(false, lengthAfter, lengthAfter)
            val tail = LengthMapping(false, remaining, remaining)
            head to tail
        }
    }

    private inline fun <T> List<T>.sumLengths(selector: (T) -> Length): Length {
        var acc = Length.ZERO
        for (item in this) {
            acc += selector(item)
        }
        return acc
    }

    private data class TextEdit(
        val startOffset: Length,
        val oldLength: Length,
        val newLength: Length,
        val endOffset: Length = startOffset + oldLength
    ) {
        fun toEditInfo(): EditInfo {
            val start = Position.fromLength(startOffset)
            return EditInfo(
                startLine = start.line,
                startColumn = start.column,
                oldLineCount = oldLength.lineCount,
                oldColumnCount = oldLength.columnCount,
                newLineCount = newLength.lineCount,
                newColumnCount = newLength.columnCount
            )
        }

        fun describe(): String = "Edit(start=$startOffset old=$oldLength new=$newLength)"
    }

    private data class LengthMapping(
        val modified: Boolean,
        val lengthBefore: Length,
        val lengthAfter: Length
    ) {
        fun describe(): String = "${if (modified) "M" else "U"}(before=$lengthBefore, after=$lengthAfter)"
    }

    private fun <T> ArrayDeque<T>.poll(): T? = if (isEmpty()) null else removeFirst()
}
