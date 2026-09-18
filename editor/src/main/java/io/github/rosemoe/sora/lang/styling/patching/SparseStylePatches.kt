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

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.analysis.SequenceUpdateRange
import java.util.Collections
import java.util.IdentityHashMap

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
class SparseStylePatches private constructor(initiallyImmutable: Boolean) {

    constructor() : this(false)

    companion object {
        @JvmField
        val EMPTY = SparseStylePatches(true)
    }

    /**
     * Patches stay ordered by start/end position. Keeping this invariant lets insertion use
     * binary search and lets batch updates merge two ordered sequences in O(n), without a global
     * sort after every provider callback.
     */
    private val patches = mutableListOf<StylePatch>()
    // Reuse the read-only view; getPatches() is called for every provider update.
    private val readOnlyPatches: List<StylePatch> = Collections.unmodifiableList(patches)

    /**
     * Prefix maximum of [StylePatch.endLine]. A line query can binary-search the last possible
     * start line, then walk backwards only while a patch may still reach the requested line.
     */
    private val maxEndLines = mutableListOf<Int>()

    private var immutable = initiallyImmutable
    private var searchIndexDirty = false

    private fun getInsertionPoint(patch: StylePatch): Int {
        val result = patches.binarySearch(patch)
        val insertionPoint = if (result < 0) {
            -(result + 1)
        } else {
            result
        }
        return insertionPoint
    }

    fun addPatch(patch: StylePatch) {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        patches.add(getInsertionPoint(patch), patch)
        // The single-item path is uncommon compared with provider batches; rebuilding this small
        // index immediately would make repeated single-item construction O(n²). Defer one rebuild
        // until the next query or batch operation.
        searchIndexDirty = true
    }

    fun removePatch(patch: StylePatch): Boolean {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        val removed = patches.remove(patch)
        if (removed) searchIndexDirty = true
        return removed
    }

    fun clear() {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        patches.clear()
        maxEndLines.clear()
        searchIndexDirty = false
    }

    fun getPatches(): List<StylePatch> = readOnlyPatches

    fun getPatchesOnLine(line: Int): List<StylePatch> {
        if (patches.isEmpty()) return emptyList()
        ensureSearchIndex()
        val result = mutableListOf<StylePatch>()
        // Binary search changes the scan from O(number of all patches) to O(log n + matches).
        var index = upperBoundStartLine(line) - 1
        while (index >= 0 && maxEndLines[index] >= line) {
            val patch = patches[index]
            if (patch.endLine >= line) result.add(patch)
            index--
        }
        result.reverse()
        return result
    }

    /** Remove patches intersecting [range], preserving the ordered storage. */
    fun removeInRange(range: StyleUpdateRange): List<StylePatch> {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        if (patches.isEmpty()) return emptyList()
        ensureSearchIndex()
        if (range is SequenceUpdateRange) {
            return removeSequenceRange(range.startLine, range.endLine)
        }
        // Compact in one pass instead of repeatedly calling removeAt, which would shift the tail
        // for every removed item and could become quadratic.
        return compact { range.intersects(it.startLine, it.endLine) }
    }

    /**
     * Sequence ranges describe one contiguous interval. Prefix maxima locate the first patch
     * ending inside the interval in O(log n), and the ordered start lines locate the upper bound;
     * every item between those bounds intersects the range, so no predicate scan is needed.
     */
    private fun removeSequenceRange(startLine: Int, endLine: Int): List<StylePatch> {
        if (startLine > endLine) return emptyList()
        val first = lowerBoundMaxEnd(startLine)
        val last = upperBoundStartLine(endLine)
        if (first >= last) return emptyList()
        // Prefix maxima only give a safe candidate start: an earlier long patch can keep the
        // candidate before shorter patches that do not intersect. Filter the candidate window
        // once to preserve correctness while still skipping the definitely unrelated prefix.
        return compactRange(first, last) { it.endLine >= startLine }
    }

    fun removePatches(targets: Collection<StylePatch>) {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        if (targets.isEmpty() || patches.isEmpty()) return
        val identity = Collections.newSetFromMap(IdentityHashMap<StylePatch, Boolean>())
        identity.addAll(targets)
        compact { identity.contains(it) }
    }

    fun addPatches(newPatches: Collection<StylePatch>) {
        if (immutable) throw IllegalStateException("the patch list is already set immutable")
        if (newPatches.isEmpty()) return
        // Every SparseStylePatches instance is ordered, so merge in linear time instead of sorting
        // the whole result. This is the hot path for incremental provider updates.
        val incoming = newPatches.toList()
        if (patches.isEmpty()) {
            patches.addAll(incoming)
            searchIndexDirty = true
            ensureSearchIndex()
            return
        }
        val merged = mutableListOf<StylePatch>()
        var existingIndex = 0
        incoming.forEach { patch ->
            while (existingIndex < patches.size && patches[existingIndex] <= patch) {
                merged.add(patches[existingIndex++])
            }
            merged.add(patch)
        }
        while (existingIndex < patches.size) merged.add(patches[existingIndex++])
        patches.clear()
        patches.addAll(merged)
        searchIndexDirty = true
        ensureSearchIndex()
    }

    private fun compact(shouldRemove: (StylePatch) -> Boolean): List<StylePatch> {
        val removed = mutableListOf<StylePatch>()
        var write = 0
        for (read in patches.indices) {
            val patch = patches[read]
            if (shouldRemove(patch)) {
                removed.add(patch)
            } else {
                if (write != read) patches[write] = patch
                write++
            }
        }
        if (removed.isEmpty()) return emptyList()
        for (i in patches.size - 1 downTo write) {
            patches.removeAt(i)
        }
        searchIndexDirty = true
        ensureSearchIndex()
        return removed
    }

    private fun compactRange(first: Int, last: Int, shouldRemove: (StylePatch) -> Boolean): List<StylePatch> {
        val removed = mutableListOf<StylePatch>()
        var write = first
        for (read in first until last) {
            val patch = patches[read]
            if (shouldRemove(patch)) {
                removed.add(patch)
            } else {
                if (write != read) patches[write] = patch
                write++
            }
        }
        if (removed.isEmpty()) return emptyList()
        val tail = patches.subList(last, patches.size).toList()
        while (patches.size > write) patches.removeAt(patches.size - 1)
        patches.addAll(tail)
        searchIndexDirty = true
        ensureSearchIndex()
        return removed
    }

    fun setImmutable() {
        immutable = true
    }

    fun updateForInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val edit = TextEdit(Position(startLine, startColumn), Position(endLine, endColumn))
        patches.forEach { patch ->
            val start = edit.mapInsertion(patch.start)
            val end = edit.mapInsertion(patch.end)
            patch.start = start
            patch.end = end
        }
        rebuildMaxEndLines()
    }

    /**
     * Rebuild only the query index after callers mutate shared [StylePatch] objects in place.
     * Coordinates are shared by provider snapshots and the merged collection, so remapping must
     * happen once while every collection refreshes its own index.
     */
    internal fun rebuildSearchIndex() {
        rebuildMaxEndLines()
    }

    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val edit = TextEdit(Position(startLine, startColumn), Position(endLine, endColumn))
        patches.forEach { patch ->
            val start = edit.mapDeletion(patch.start)
            val end = edit.mapDeletion(patch.end)
            patch.start = start
            patch.end = maxOf(start, end)
        }
        searchIndexDirty = true
        ensureSearchIndex()
    }

    private class TextEdit(val start: Position, val end: Position) {

        private val lineDelta = end.line - start.line

        fun mapInsertion(position: Position): Position = when {
            position < start -> position
            position.line == start.line -> Position(
                end.line,
                end.column + position.column - start.column
            )

            else -> Position(position.line + lineDelta, position.column)
        }

        fun mapDeletion(position: Position): Position = when {
            position <= start -> position
            position <= end -> start
            position.line == end.line -> Position(
                start.line,
                start.column + position.column - end.column
            )

            else -> Position(position.line - lineDelta, position.column)
        }
    }

    private fun upperBoundStartLine(line: Int): Int {
        ensureSearchIndex()
        var low = 0
        var high = patches.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (patches[middle].startLine <= line) low = middle + 1 else high = middle
        }
        return low
    }

    private fun lowerBoundMaxEnd(line: Int): Int {
        ensureSearchIndex()
        var low = 0
        var high = maxEndLines.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (maxEndLines[middle] < line) low = middle + 1 else high = middle
        }
        return low
    }

    private fun rebuildMaxEndLines() {
        maxEndLines.clear()
        var maxEnd = Int.MIN_VALUE
        patches.forEach {
            if (it.endLine > maxEnd) maxEnd = it.endLine
            maxEndLines.add(maxEnd)
        }
        searchIndexDirty = false
    }

    private fun ensureSearchIndex() {
        if (searchIndexDirty) rebuildMaxEndLines()
    }

}
