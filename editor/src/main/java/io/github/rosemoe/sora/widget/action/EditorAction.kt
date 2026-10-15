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

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

/**
 * A named command that can be triggered by the user or by code, e.g. "Run", "Toggle breakpoint"
 * or "Fold all".
 *
 * Actions are registered on the editor with [io.github.rosemoe.sora.widget.CodeEditor.registerAction]
 * and are executed by [io.github.rosemoe.sora.widget.action.EditorActionManager].
 *
 * ```
 * editor.registerAction(EditorAction("app.run", "Run", showInContextMenu = true) { context ->
 *     runFile(context.line)
 * })
 * ```
 *
 * The action can carry an icon ([iconRes] or [icon]). The icon is shown in the context menu
 * and, when the action is bound to a line with
 * [EditorActionManager.bindIconForLine], in the side icon column of the gutter.
 *
 * @property id Stable unique identifier of the action
 * @property title Text shown to the user (context menu, action list, ...)
 * @property group One of the [GROUP_*] constants, used by apps to group their actions
 * @property showInContextMenu Whether the editor should append this action to its context menu
 * @property handler Executes the action. Subclasses may override [perform] instead.
 * @property enabledPredicate Custom availability check, null means always enabled.
 * @property iconRes Drawable resource of the icon, 0 for none
 * @property icon Icon drawn for the action, takes precedence over [iconRes]
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
open class EditorAction @JvmOverloads constructor(
    val id: String,
    val title: CharSequence,
    val group: Int = GROUP_GENERAL,
    var showInContextMenu: Boolean = false,
    private val handler: ((ActionContext) -> Unit)? = null,
    private val enabledPredicate: ((ActionContext) -> Boolean)? = null,
    @DrawableRes val iconRes: Int = 0,
    val icon: Drawable? = null
) {

    /**
     * Whether the action can be performed in the given context
     */
    open fun isEnabled(context: ActionContext): Boolean = enabledPredicate?.invoke(context) ?: true

    /**
     * Execute the action
     */
    open fun perform(context: ActionContext) {
        handler?.invoke(context)
    }

    /**
     * Resolve the icon of this action, null when the action has none
     */
    fun iconOrNull(context: Context): Drawable? =
        icon ?: if (iconRes != 0) context.resources.getDrawable(iconRes, null) else null

    override fun toString(): String = "EditorAction{$id, title=$title}"

    companion object {

        /** Actions that do not fit any other group */
        const val GROUP_GENERAL = 0

        /** Actions that modify the text */
        const val GROUP_EDIT = 1

        /** Code folding actions */
        const val GROUP_FOLDING = 2

        /** Debugging actions, e.g. breakpoints */
        const val GROUP_DEBUG = 3

        /** Actions that run or build the current document */
        const val GROUP_RUN = 4

        /**
         * Create an action from a lambda
         */
        @JvmStatic
        @JvmOverloads
        fun of(
            id: String,
            title: CharSequence,
            group: Int = GROUP_GENERAL,
            showInContextMenu: Boolean = false,
            @DrawableRes iconRes: Int = 0,
            icon: Drawable? = null,
            handler: (ActionContext) -> Unit
        ) = EditorAction(id, title, group, showInContextMenu, handler, null, iconRes, icon)

    }

}
