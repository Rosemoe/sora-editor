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

/**
 * A breakpoint marker on a single line of the content.
 *
 * The line is not stored inside the container but in the breakpoint itself, so breakpoints are
 * replaced (never mutated) when the content shifts them.
 *
 * @property line Zero based line index of the breakpoint
 * @property enabled Disabled breakpoints are still displayed but drawn as an outline
 * @property condition Optional condition that is evaluated before stopping
 * @property logMessage Optional message that is printed when the line is hit instead of stopping
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class Breakpoint(
    val line: Int,
    val enabled: Boolean = true,
    val condition: String? = null,
    val logMessage: String? = null
) {

    /**
     * Copy this breakpoint onto another line
     */
    fun moveTo(line: Int) = Breakpoint(line, enabled, condition, logMessage)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Breakpoint) {
            return false
        }
        return line == other.line && enabled == other.enabled &&
                condition == other.condition && logMessage == other.logMessage
    }

    override fun hashCode(): Int {
        var result = line
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (condition?.hashCode() ?: 0)
        result = 31 * result + (logMessage?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "Breakpoint{line=$line, enabled=$enabled}"

}
