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

import io.github.rosemoe.sora.event.ActionPerformEvent
import io.github.rosemoe.sora.event.CreateContextMenuEvent
import io.github.rosemoe.sora.event.InterceptTarget
import io.github.rosemoe.sora.event.subscribeAlways
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Registry and dispatcher of all [EditorAction]s of an editor.
 *
 * The manager ships a few built-in actions (code folding and breakpoints) and automatically
 * appends every action that has [EditorAction.showInContextMenu] enabled to the context menu
 * of the editor.
 *
 * ```
 * editor.registerAction(EditorAction("app.run", "Run", showInContextMenu = true) {
 *     toast("Running line ${it.line + 1}")
 * })
 * editor.performAction("app.run", ActionTrigger.TOOLBAR)
 * ```
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class EditorActionManager(val editor: CodeEditor) {

    private val actions = LinkedHashMap<String, EditorAction>()
    private val menuIds = HashMap<String, Int>()
    private val eventManager = editor.createSubEventManager()
    private var nextMenuId = MENU_ID_BASE

    init {
        eventManager.subscribeAlways(::onCreateContextMenu)
        registerBuiltInActions()
    }

    /**
     * Register an action. An already registered action with the same id is replaced.
     *
     * @return the action that was replaced, or null
     */
    fun register(action: EditorAction): EditorAction? {
        val old = actions.put(action.id, action)
        if (old != null) {
            menuIds.remove(action.id)
        }
        return old
    }

    /**
     * Remove the action with the given id
     *
     * @return the removed action, or null when it was not registered
     */
    fun unregister(id: String): EditorAction? {
        menuIds.remove(id)
        return actions.remove(id)
    }

    /**
     * Remove all app defined actions, keeping the built-in ones
     */
    fun unregisterAll() {
        val builtIn = actions.filterKeys { it.startsWith(BUILT_IN_ID_PREFIX) }
        actions.clear()
        menuIds.clear()
        actions.putAll(builtIn)
    }

    /**
     * Look up an action by its id
     */
    fun find(id: String): EditorAction? = actions[id]

    /**
     * All registered actions in registration order
     */
    fun getActions(): List<EditorAction> = actions.values.toList()

    /**
     * All registered actions that are available in the given context
     */
    fun getActions(context: ActionContext): List<EditorAction> =
        actions.values.filter { it.isEnabled(context) }

    /**
     * Create an [ActionContext] for the current cursor position
     */
    fun createContext(trigger: ActionTrigger, payload: Any? = null): ActionContext =
        ActionContext(editor, editor.cursor.left(), trigger, payload)

    /**
     * Execute the action with the given id
     *
     * @return true when the action has been performed
     */
    fun perform(id: String, context: ActionContext): Boolean {
        val action = actions[id] ?: return false
        return perform(action, context)
    }

    /**
     * Execute the action with the given id, using the current cursor position
     */
    fun perform(id: String, trigger: ActionTrigger = ActionTrigger.API): Boolean =
        perform(id, createContext(trigger))

    /**
     * Execute the given action
     *
     * @return true when the action has been performed. It is false when the action is disabled
     * in this context or when a subscriber intercepted [ActionPerformEvent].
     */
    fun perform(action: EditorAction, context: ActionContext): Boolean {
        if (!action.isEnabled(context)) {
            return false
        }
        val event = ActionPerformEvent(editor, action, context)
        if ((editor.dispatchEvent(event) and InterceptTarget.TARGET_EDITOR) != 0) {
            return false
        }
        action.perform(context)
        return true
    }

    /**
     * Whether the actions should be added to the context menu of the editor
     */
    var addActionsToContextMenu: Boolean = true

    private fun onCreateContextMenu(event: CreateContextMenuEvent) {
        if (!addActionsToContextMenu) {
            return
        }
        val position = event.position
        var order = 0
        for (action in actions.values) {
            if (!action.showInContextMenu) {
                continue
            }
            val context = ActionContext(editor, position, ActionTrigger.CONTEXT_MENU)
            if (!action.isEnabled(context)) {
                continue
            }
            val item = event.menu.add(MENU_GROUP, menuIdOf(action), order, action.title)
            item.setOnMenuItemClickListener {
                perform(action, ActionContext(editor, position, ActionTrigger.CONTEXT_MENU))
                true
            }
            order += MENU_ORDER_STEP
        }
    }

    private fun menuIdOf(action: EditorAction): Int {
        return menuIds.getOrPut(action.id) {
            nextMenuId++
        }
    }

    private fun registerBuiltInActions() {
        register(
            EditorAction(
                ACTION_FOLD_TOGGLE,
                "Toggle folding",
                EditorAction.GROUP_FOLDING,
                handler = { context -> editor.foldingManager.toggle(context.line) }
            )
        )
        register(
            EditorAction(
                ACTION_FOLD_ALL,
                "Fold all",
                EditorAction.GROUP_FOLDING,
                handler = { editor.foldingManager.foldAll() }
            )
        )
        register(
            EditorAction(
                ACTION_FOLD_EXPAND_ALL,
                "Expand all",
                EditorAction.GROUP_FOLDING,
                handler = { editor.foldingManager.expandAll() }
            )
        )
        register(
            EditorAction(
                ACTION_BREAKPOINT_TOGGLE,
                "Toggle breakpoint",
                EditorAction.GROUP_DEBUG,
                handler = { context -> editor.breakpoints.toggle(context.line) }
            )
        )
        register(
            EditorAction(
                ACTION_BREAKPOINT_CLEAR_ALL,
                "Clear all breakpoints",
                EditorAction.GROUP_DEBUG,
                handler = { editor.breakpoints.clear() }
            )
        )
    }

    companion object {

        /** Toggles folding of the region at the context line */
        const val ACTION_FOLD_TOGGLE = "editor.fold.toggle"

        /** Collapses every foldable region */
        const val ACTION_FOLD_ALL = "editor.fold.all"

        /** Expands every collapsed region */
        const val ACTION_FOLD_EXPAND_ALL = "editor.fold.expandAll"

        /** Adds or removes the breakpoint on the context line */
        const val ACTION_BREAKPOINT_TOGGLE = "editor.debug.toggleBreakpoint"

        /** Removes all breakpoints */
        const val ACTION_BREAKPOINT_CLEAR_ALL = "editor.debug.clearBreakpoints"

        private const val BUILT_IN_ID_PREFIX = "editor."
        private const val MENU_GROUP = 0x0f10
        private const val MENU_ID_BASE = 0x0f100000
        private const val MENU_ORDER_STEP = 10

    }

}
