/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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

package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.analysis.SequenceUpdateRange
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import java.lang.IllegalStateException
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Ordered patch storage. [patches] stays sorted by start/end position and [maxEndLines] keeps the
 * prefix maximum of [StylePatch.endLine], so line queries and ordered merges avoid full rescans.
 */
class SparseStylePatches private constructor(initiallyImmutable: Boolean) {

    constructor() : this(false)

    companion object {
        @JvmField
        val EMPTY = SparseStylePatches(true)
    }

    private val patches = mutableListOf<StylePatch>()
    private val readOnlyPatches: List<StylePatch> = Collections.unmodifiableList(patches)
    private val maxEndLines = mutableListOf<Int>()
    private var immutable = initiallyImmutable
    private var indexDirty = false

    private fun checkMutable() {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
    }

    private fun index() {
        if (!indexDirty) return
        maxEndLines.clear()
        var max = Int.MIN_VALUE
        for (patch in patches) {
            if (patch.endLine > max) max = patch.endLine
            maxEndLines.add(max)
        }
        indexDirty = false
    }

    private fun upperBound(line: Int): Int {
        index()
        var low = 0
        var high = patches.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (patches[mid].startLine <= line) low = mid + 1 else high = mid
        }
        return low
    }

    private fun lowerBoundMaxEnd(line: Int): Int {
        index()
        var low = 0
        var high = maxEndLines.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (maxEndLines[mid] < line) low = mid + 1 else high = mid
        }
        return low
    }

    /** Drop every item of [from, to) matching [drop], packing survivors at the front. */
    private fun compact(from: Int, to: Int, drop: (StylePatch) -> Boolean): List<StylePatch> {
        val removed = mutableListOf<StylePatch>()
        var write = from
        for (read in from until to) {
            val patch = patches[read]
            if (drop(patch)) {
                removed.add(patch)
            } else {
                if (write != read) patches[write] = patch
                write++
            }
        }
        if (removed.isEmpty()) return removed
        val tail = if (to < patches.size) patches.subList(to, patches.size).toList() else emptyList()
        while (patches.size > write) patches.removeAt(patches.size - 1)
        patches.addAll(tail)
        indexDirty = true
        return removed
    }

    fun addPatch(patch: StylePatch) {
        checkMutable()
        val at = patches.binarySearch(patch)
        patches.add(if (at < 0) -at - 1 else at, patch)
        indexDirty = true
    }

    /** Linear merge of two ordered sequences; [newPatches] is assumed to be sorted. */
    fun addPatches(newPatches: Collection<StylePatch>) {
        checkMutable()
        if (newPatches.isEmpty()) return
        val merged = ArrayList<StylePatch>(patches.size + newPatches.size)
        var index = 0
        for (patch in newPatches) {
            while (index < patches.size && patches[index] <= patch) merged.add(patches[index++])
            merged.add(patch)
        }
        while (index < patches.size) merged.add(patches[index++])
        patches.clear()
        patches.addAll(merged)
        indexDirty = true
    }

    fun removePatch(patch: StylePatch): Boolean {
        checkMutable()
        if (!patches.remove(patch)) return false
        indexDirty = true
        return true
    }

    fun removePatches(targets: Collection<StylePatch>) {
        checkMutable()
        if (targets.isEmpty() || patches.isEmpty()) return
        val identity = Collections.newSetFromMap(IdentityHashMap<StylePatch, Boolean>())
        identity.addAll(targets)
        compact(0, patches.size) { identity.contains(it) }
    }

    fun removeInRange(range: StyleUpdateRange): List<StylePatch> {
        checkMutable()
        if (patches.isEmpty()) return emptyList()
        if (range !is SequenceUpdateRange) {
            return compact(0, patches.size) { range.intersects(it.startLine, it.endLine) }
        }
        if (range.startLine > range.endLine) return emptyList()
        val from = lowerBoundMaxEnd(range.startLine)
        val to = upperBound(range.endLine)
        if (from >= to) return emptyList()
        return compact(from, to) { it.endLine >= range.startLine }
    }

    fun clear() {
        checkMutable()
        patches.clear()
        maxEndLines.clear()
        indexDirty = false
    }

    fun setImmutable() {
        immutable = true
    }

    fun getPatches(): List<StylePatch> = readOnlyPatches

    fun getPatchesOnLine(line: Int): List<StylePatch> {
        if (patches.isEmpty()) return emptyList()
        val result = mutableListOf<StylePatch>()
        var index = upperBound(line) - 1
        while (index >= 0 && maxEndLines[index] >= line) {
            val patch = patches[index]
            if (patch.endLine >= line) result.add(patch)
            index--
        }
        result.reverse()
        return result
    }

    /** Refresh the query index after shared [StylePatch] coordinates were remapped in place. */
    internal fun rebuildSearchIndex() {
        indexDirty = true
    }

    fun updateForInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val edit = TextEdit(Position(startLine, startColumn), Position(endLine, endColumn))
        for (patch in patches) {
            val start = edit.insert(patch.start)
            val end = edit.insert(patch.end)
            patch.start = start
            patch.end = end
        }
        indexDirty = true
    }

    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val edit = TextEdit(Position(startLine, startColumn), Position(endLine, endColumn))
        for (patch in patches) {
            val start = edit.delete(patch.start)
            val end = edit.delete(patch.end)
            patch.start = start
            patch.end = maxOf(start, end)
        }
        indexDirty = true
    }
}

private class TextEdit(val start: Position, val end: Position) {

    private val lineDelta = end.line - start.line

    fun insert(position: Position): Position = when {
        position < start -> position
        position.line == start.line -> Position(end.line, end.column + position.column - start.column)
        else -> Position(position.line + lineDelta, position.column)
    }

    fun delete(position: Position): Position = when {
        position <= start -> position
        position <= end -> start
        position.line == end.line -> Position(start.line, start.column + position.column - end.column)
        else -> Position(position.line - lineDelta, position.column)
    }
}
