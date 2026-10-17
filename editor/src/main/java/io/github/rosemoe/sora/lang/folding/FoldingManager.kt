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
 ******************************************************************************/
package io.github.rosemoe.sora.lang.folding

import android.util.SparseArray
import io.github.rosemoe.sora.event.FoldingChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Owns the foldable regions, the collapsed state and the line to row mapping used by folding.
 *
 * Foldable regions are collected from registered [FoldingProvider]s and, when [useCodeBlocks] is
 * true, from the code blocks that the language analyze manager published with the styles.
 * The collapsed state survives analyze results being refreshed, regions are matched by their
 * line range.
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class FoldingManager(val editor: CodeEditor) {

    private val providers = mutableListOf<FoldingProvider>()
    private val collapsed = LinkedHashSet<FoldingRegion>()
    private val lineMap = FoldedLineMap()
    private var regionsCache: List<FoldingRegion>? = null
    private var providerRegionsCache: List<FoldingRegion>? = null
    private var startIndex: SparseArray<FoldingRegion>? = null

    /**
     * Whether the code blocks published by the language should be offered as foldable regions.
     * Disable it when only [FoldingProvider] results should be used.
     */
    var useCodeBlocks: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidateRegions()
            }
        }

    /**
     * Mapping used by layouts to skip the lines hidden by folding
     */
    val foldedLineMap: FoldedLineMap
        get() = lineMap

    /**
     * All foldable regions of the current content, sorted by start line
     */
    val regions: List<FoldingRegion>
        get() = ensureRegions()

    /**
     * Regions that are currently collapsed
     */
    val collapsedRegions: List<FoldingRegion>
        get() = collapsed.toList()

    /**
     * Whether at least one region is collapsed
     */
    val hasCollapsedRegions: Boolean
        get() = collapsed.isNotEmpty()

    init {
        lineMap.update(emptyList(), 0)
    }

    fun registerProvider(provider: FoldingProvider) {
        if (!providers.contains(provider)) {
            providers.add(provider)
            invalidateRegions()
        }
    }

    fun unregisterProvider(provider: FoldingProvider) {
        if (providers.remove(provider)) {
            invalidateRegions()
        }
    }

    fun getProviders(): List<FoldingProvider> = providers.toList()

    /**
     * Mark the cached regions as out of date. They are recomputed lazily on the next query.
     * Call this whenever the analyze results or the source of your provider changed.
     */
    fun invalidateRegions() {
        regionsCache = null
        startIndex = null
        providerRegionsCache = null
    }

    /**
     * Drop only the regions derived from the language code blocks, keeping the results of
     * the [FoldingProvider]s. Used for style updates: providers are derived from the
     * content, which does not change when the analyzer streams style batches into a large
     * file, and re-running them there would scan the whole document on the main thread.
     */
    fun invalidateCodeBlockRegions() {
        regionsCache = null
        startIndex = null
    }

    /**
     * Region that starts on the given line, if any. When multiple regions start on the same line,
     * the innermost one is returned.
     */
    fun regionAt(line: Int): FoldingRegion? = ensureStartIndex().get(line)

    /**
     * Innermost region that contains the given line, if any
     */
    fun regionContaining(line: Int): FoldingRegion? {
        var best: FoldingRegion? = null
        for (region in ensureRegions()) {
            if (region.startLine > line) {
                break
            }
            if (region.contains(line) && (best == null || region.endLine < best.endLine)) {
                best = region
            }
        }
        return best
    }

    /**
     * Region that should be used when the user triggers folding on the given line
     */
    fun regionFor(line: Int): FoldingRegion? = regionAt(line) ?: regionContaining(line)

    /**
     * Whether the given region is collapsed
     */
    fun isCollapsed(region: FoldingRegion): Boolean = collapsed.contains(region)

    /**
     * Whether the given line is hidden by folding
     */
    fun isLineHidden(line: Int): Boolean = lineMap.isHidden(line)

    /**
     * Expand collapsed regions until the given line is visible.
     *
     * Used when the cursor lands on a hidden line, e.g. pressing enter at the end of the
     * header line of a collapsed region, where the newly inserted line would otherwise
     * stay hidden inside the region.
     *
     * @return true when at least one region has been expanded
     */
    fun revealLine(line: Int): Boolean {
        var revealed = false
        while (lineMap.isHidden(line)) {
            val region = collapsedRegionFor(line) ?: break
            if (!unfold(region)) {
                break
            }
            revealed = true
        }
        return revealed
    }

    /**
     * Collapse the region that starts at (or contains) the given line.
     *
     * @return true when the region has been collapsed
     */
    fun fold(line: Int): Boolean {
        val region = regionFor(line) ?: return false
        return fold(region)
    }

    /**
     * Collapse the given region
     *
     * @return true when the state changed
     */
    fun fold(region: FoldingRegion): Boolean {
        if (!editor.isCodeFoldingEnabled || !region.isFoldable || isCollapsed(region)) {
            return false
        }
        collapsed.add(region)
        keepCursorVisible()
        onCollapsedChanged()
        return true
    }

    /**
     * Expand the collapsed region that hides the given line, or the one starting on it
     *
     * @return true when the state changed
     */
    fun unfold(line: Int): Boolean {
        val region = collapsedRegionFor(line) ?: regionAt(line)?.takeIf { isCollapsed(it) } ?: return false
        return unfold(region)
    }

    /**
     * Expand the given region
     *
     * @return true when the state changed
     */
    fun unfold(region: FoldingRegion): Boolean {
        if (!collapsed.remove(region)) {
            return false
        }
        onCollapsedChanged()
        return true
    }

    /**
     * Collapse or expand the foldable region of the given line.
     *
     * @return true when the region is collapsed after the call
     */
    fun toggle(line: Int): Boolean {
        val hidden = collapsedRegionFor(line)
        if (hidden != null) {
            unfold(hidden)
            return false
        }
        val region = regionFor(line) ?: return false
        return if (isCollapsed(region)) {
            unfold(region)
            false
        } else {
            fold(region)
            true
        }
    }

    /**
     * Collapse every foldable region
     */
    fun foldAll() {
        if (!editor.isCodeFoldingEnabled) {
            return
        }
        var changed = false
        for (region in ensureRegions()) {
            if (region.isFoldable && collapsed.add(region)) {
                changed = true
            }
        }
        if (changed) {
            keepCursorVisible()
            onCollapsedChanged()
        }
    }

    /**
     * Expand all collapsed regions
     */
    fun expandAll() {
        if (collapsed.isEmpty()) {
            return
        }
        collapsed.clear()
        onCollapsedChanged()
    }

    /**
     * Innermost collapsed region that hides the given line
     */
    fun collapsedRegionFor(line: Int): FoldingRegion? {
        var best: FoldingRegion? = null
        for (region in collapsed) {
            if (region.containsHiddenLine(line) && (best == null || region.endLine < best.endLine)) {
                best = region
            }
        }
        return best
    }

    /**
     * Called when lines are inserted into the content
     */
    fun onContentInserted(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        invalidateRegions()
        val delta = endLine - startLine
        if (collapsed.isEmpty()) {
            updateLineMap()
            return
        }
        val newCollapsed = LinkedHashSet<FoldingRegion>()
        var changed = false
        for (region in collapsed) {
            var start = region.startLine
            var end = region.endLine
            when {
                startLine < start || (startLine == start && startColumn == 0) -> {
                    start += delta
                    end += delta
                }
                startLine <= end -> end += delta
                else -> Unit
            }
            if (end > start) {
                newCollapsed.add(FoldingRegion(start, end))
            } else {
                changed = true
            }
        }
        changed = changed || newCollapsed.size != collapsed.size
        collapsed.clear()
        collapsed.addAll(newCollapsed)
        updateLineMap()
        editor.layout?.onFoldingRegionsChanged()
        if (changed) {
            dispatchCollapsedChanged()
        }
    }

    /**
     * Called when lines are deleted from the content
     */
    fun onContentDeleted(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        invalidateRegions()
        if (collapsed.isEmpty()) {
            updateLineMap()
            return
        }
        val delta = startLine - endLine
        val newCollapsed = LinkedHashSet<FoldingRegion>()
        var changed = false
        for (region in collapsed) {
            var start = region.startLine
            var end = region.endLine
            when {
                end <= startLine -> Unit
                start > endLine -> {
                    start += delta
                    end += delta
                }
                start >= startLine -> {
                    if (end <= endLine) {
                        changed = true
                        continue
                    }
                    start = startLine
                    end += delta
                }
                else -> {
                    if (end <= endLine) {
                        // The tail of the region was deleted, stay conservative and keep it visible
                        end = startLine - 1
                    } else {
                        end += delta
                    }
                }
            }
            if (end > start) {
                newCollapsed.add(FoldingRegion(start, end))
            } else {
                changed = true
            }
        }
        changed = changed || newCollapsed.size != collapsed.size
        collapsed.clear()
        collapsed.addAll(newCollapsed)
        updateLineMap()
        editor.layout?.onFoldingRegionsChanged()
        if (changed) {
            dispatchCollapsedChanged()
        }
    }

    /**
     * Drop all folding state. Called when the whole content is replaced.
     */
    fun reset() {
        invalidateRegions()
        if (collapsed.isEmpty()) {
            updateLineMap()
            return
        }
        collapsed.clear()
        updateLineMap()
        editor.layout?.onFoldingRegionsChanged()
        dispatchCollapsedChanged()
    }

    /**
     * Rebuild the line to row mapping from the current collapsed state
     */
    fun updateLineMap() {
        val text = editor.text
        lineMap.update(collapsed, text?.lineCount ?: 0)
    }

    private fun ensureRegions(): List<FoldingRegion> {
        regionsCache?.let { return it }
        val regions = LinkedHashSet<FoldingRegion>()
        if (useCodeBlocks) {
            val blocks = editor.styles?.blocks
            if (blocks != null) {
                for (block in blocks) {
                    if (block.endLine > block.startLine) {
                        var endLine = block.endLine
                        if (!block.toBottomOfEndLine && startsWithClosingDelimiter(endLine)) {
                            endLine--
                        }
                        if (endLine > block.startLine) {
                            regions.add(FoldingRegion(block.startLine, endLine))
                        }
                    }
                }
            }
        }
        regions.addAll(ensureProviderRegions())
        val list = regions.toMutableList()
        list.sort()
        regionsCache = list
        return list
    }

    /**
     * Whether the given line starts with a closing bracket (`}`, `]` or `)`) after its
     * indentation. Analyzers whose code blocks have toBottomOfEndLine false end the block
     * on the line of the closing bracket, and folding must keep that line visible.
     */
    private fun startsWithClosingDelimiter(line: Int): Boolean {
        val text = editor.text ?: return false
        if (line < 0 || line >= text.lineCount) {
            return false
        }
        val chars = text.getLine(line)
        var i = 0
        while (i < chars.length) {
            val ch = chars[i]
            if (ch != ' ' && ch != '\t') {
                return ch == '}' || ch == ']' || ch == ')'
            }
            i++
        }
        return false
    }

    private fun ensureProviderRegions(): List<FoldingRegion> {
        providerRegionsCache?.let { return it }
        val provided = ArrayList<FoldingRegion>()
        for (provider in providers) {
            try {
                provider.computeFoldingRegions(editor, provided)
            } catch (e: Exception) {
                // Ignore broken providers so that folding keeps working
                continue
            }
        }
        providerRegionsCache = provided
        return provided
    }

    private fun ensureStartIndex(): SparseArray<FoldingRegion> {
        ensureRegions()
        startIndex?.let { return it }
        val index = SparseArray<FoldingRegion>()
        for (region in regionsCache!!) {
            val current = index.get(region.startLine)
            if (current == null || region.endLine < current.endLine) {
                index.put(region.startLine, region)
            }
        }
        startIndex = index
        return index
    }

    private fun onCollapsedChanged() {
        updateLineMap()
        editor.layout?.onFoldingRegionsChanged()
        dispatchCollapsedChanged()
        editor.invalidate()
    }

    private fun dispatchCollapsedChanged() {
        editor.onFoldingStateChanged()
        editor.dispatchEvent(FoldingChangeEvent(editor))
    }

    private fun keepCursorVisible() {
        val cursor = editor.cursor
        val line = cursor.leftLine
        if (!lineMap.isHidden(line)) {
            return
        }
        val visible = lineMap.clampToVisible(line)
        editor.setSelection(
            visible,
            editor.text.getColumnCount(visible),
            SelectionChangeEvent.CAUSE_UNKNOWN
        )
    }

}
