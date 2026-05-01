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

package io.github.rosemoe.sora.compose

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Defines which non-printable characters or markers should be drawn in the editor.
 *
 * This is a value class wrapping bitwise flags used by [CodeEditor].
 *
 * @see CodeEditorState.nonPrintableMarks
 */
@JvmInline
value class NonPrintableMarks internal constructor(val flag: Int) {

    /**
     * Combines this set of marks with another set.
     */
    infix fun or(other: NonPrintableMarks) = NonPrintableMarks(flag or other.flag)

    companion object {
        /**
         * No non-printable marks are drawn.
         */
        val None = NonPrintableMarks(0)

        /**
         * Draw symbols for whitespace characters at the beginning of a line.
         */
        val LeadingWhitespace = NonPrintableMarks(CodeEditor.FLAG_DRAW_WHITESPACE_LEADING)

        /**
         * Draw symbols for whitespace characters between non-whitespace characters.
         */
        val InnerWhitespace = NonPrintableMarks(CodeEditor.FLAG_DRAW_WHITESPACE_INNER)

        /**
         * Draw symbols for whitespace characters at the end of a line.
         */
        val TrailingWhitespace = NonPrintableMarks(CodeEditor.FLAG_DRAW_WHITESPACE_TRAILING)

        /**
         * Draw symbols for whitespace characters on lines that contain only whitespace.
         */
        val EmptyLineWhitespace = NonPrintableMarks(CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE)

        /**
         * Draw symbols for line separators (e.g. carriage return or line feed).
         */
        val LineSeparator = NonPrintableMarks(CodeEditor.FLAG_DRAW_LINE_SEPARATOR)

        /**
         * Draw whitespace symbols even when they are within a text selection.
         * If not set, whitespace symbols may be hidden by selection highlights.
         */
        val WhitespaceInSelection = NonPrintableMarks(CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION)

        /**
         * Draw symbols indicating where a line has been soft-wrapped.
         */
        val SoftWrap = NonPrintableMarks(CodeEditor.FLAG_DRAW_SOFT_WRAP)
    }
}
