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
package io.github.rosemoe.sora.widget.action

import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Describes where an [EditorAction] was triggered from.
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
enum class ActionTrigger {
    /** The context menu of the editor */
    CONTEXT_MENU,

    /** A click on a gutter marker (fold indicator, breakpoint, ...) */
    GUTTER,

    /** Called from code, e.g. [CodeEditor.performAction] */
    API,

    /** A keyboard shortcut handled by the app */
    KEYBOARD,

    /** A toolbar or other UI button of the app */
    TOOLBAR
}

/**
 * Everything an [EditorAction] needs to know about the place it was triggered from.
 *
 * @property editor The editor that owns the action
 * @property position Text position the action refers to
 * @property trigger Where the action was triggered from
 * @property payload Optional app defined extra data
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class ActionContext @JvmOverloads constructor(
    val editor: CodeEditor,
    val position: CharPosition,
    val trigger: ActionTrigger,
    val payload: Any? = null
) {

    /** Zero based line the action refers to */
    val line: Int
        get() = position.line

    /** Zero based column the action refers to */
    val column: Int
        get() = position.column

    /** Whether text is currently selected in the editor */
    val hasSelection: Boolean
        get() = editor.cursor.isSelected

    /** Currently selected text or null when nothing is selected */
    val selectedText: CharSequence?
        get() {
            val cursor = editor.cursor
            if (!cursor.isSelected) {
                return null
            }
            val left = cursor.left().index
            val right = cursor.right().index
            return editor.text.subSequence(left, right)
        }

}
