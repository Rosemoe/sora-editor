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
package io.github.rosemoe.sora.lang.debugging

import io.github.rosemoe.sora.event.BreakpointChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.TreeMap

/**
 * Stores the debug breakpoints of an editor and keeps them in sync with content changes.
 *
 * All methods must be called on the UI thread. Every mutation dispatches a
 * [BreakpointChangeEvent] on the host editor.
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class BreakpointContainer(private val editor: CodeEditor) {

    private val breakpoints = TreeMap<Int, Breakpoint>()

    /**
     * All breakpoints ordered by line
     */
    val all: List<Breakpoint>
        get() = ArrayList(breakpoints.values)

    /**
     * Line numbers of all breakpoints ordered by line
     */
    val lines: List<Int>
        get() = ArrayList(breakpoints.keys)

    val size: Int
        get() = breakpoints.size

    fun isEmpty(): Boolean = breakpoints.isEmpty()

    fun contains(line: Int): Boolean = breakpoints.containsKey(line)

    fun get(line: Int): Breakpoint? = breakpoints[line]

    /**
     * Place a breakpoint on the given line, replacing an existing one
     *
     * @return the breakpoint that now sits on the line
     */
    @JvmOverloads
    fun add(
        line: Int,
        enabled: Boolean = true,
        condition: String? = null,
        logMessage: String? = null
    ): Breakpoint {
        val breakpoint = Breakpoint(line, enabled, condition, logMessage)
        breakpoints[line] = breakpoint
        editor.dispatchEvent(BreakpointChangeEvent(editor, breakpoint, true))
        editor.invalidate()
        return breakpoint
    }

    /**
     * Remove the breakpoint on the given line
     *
     * @return the removed breakpoint or null when there was none
     */
    fun remove(line: Int): Breakpoint? {
        val removed = breakpoints.remove(line) ?: return null
        editor.dispatchEvent(BreakpointChangeEvent(editor, null, false))
        editor.invalidate()
        return removed
    }

    /**
     * Add a breakpoint on the line when it has none, remove it otherwise
     *
     * @return the breakpoint after the change, or null when it was removed
     */
    fun toggle(line: Int): Breakpoint? {
        return if (breakpoints.containsKey(line)) {
            remove(line)
        } else {
            add(line)
        }
    }

    /**
     * Remove all breakpoints
     *
     * @return the removed breakpoints
     */
    fun clear(): List<Breakpoint> {
        if (breakpoints.isEmpty()) {
            return emptyList()
        }
        val removed = ArrayList(breakpoints.values)
        breakpoints.clear()
        editor.dispatchEvent(BreakpointChangeEvent(editor, null, false))
        editor.invalidate()
        return removed
    }

    /**
     * Shift breakpoints when lines are inserted into the content
     */
    fun shiftOnInsert(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        val delta = endLine - startLine
        if (delta == 0 || breakpoints.isEmpty()) {
            return
        }
        val updated = TreeMap<Int, Breakpoint>()
        for (breakpoint in breakpoints.values) {
            var line = breakpoint.line
            if (startLine < line || (startLine == line && startColumn == 0)) {
                line += delta
            }
            updated[line] = if (line == breakpoint.line) breakpoint else breakpoint.moveTo(line)
        }
        replaceAll(updated)
    }

    /**
     * Shift breakpoints when lines are deleted from the content. Breakpoints on fully deleted
     * lines are removed.
     */
    fun shiftOnDelete(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        if (startLine == endLine && startColumn == endColumn) {
            return
        }
        if (breakpoints.isEmpty()) {
            return
        }
        val delta = startLine - endLine
        val updated = TreeMap<Int, Breakpoint>()
        for (breakpoint in breakpoints.values) {
            val line = breakpoint.line
            when {
                line > endLine -> {
                    val newLine = line + delta
                    updated[newLine] = breakpoint.moveTo(newLine)
                }
                line < startLine || line == startLine -> updated[line] = breakpoint
                // Otherwise the line is swallowed by the deletion
            }
        }
        replaceAll(updated)
    }

    private fun replaceAll(updated: TreeMap<Int, Breakpoint>) {
        if (updated == breakpoints) {
            return
        }
        breakpoints.clear()
        breakpoints.putAll(updated)
    }

}
