/*******************************************************************************
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

package io.github.rosemoe.sora.widget.component

import io.github.rosemoe.sora.event.ContextClickEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.subscribeAlways
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow

class EditorContextMenu(editor: CodeEditor) :
    EditorPopupWindow(editor, FEATURE_SHOW_OUTSIDE_VIEW_ALLOWED), EditorBuiltinComponent {

    val eventManager = editor.createSubEventManager()

    init {
        prepareLayout()
        registerEvents()
    }

    private fun prepareLayout() {

    }

    private fun registerEvents() {
        eventManager.subscribeAlways(::onContextClick)
        eventManager.subscribeAlways(::onEditorRelease)
    }

    private fun onEditorRelease(event: EditorReleaseEvent) {
        isEnabled = false
    }

    private fun onContextClick(event: ContextClickEvent) {

    }

    override fun isEnabled() = eventManager.isEnabled

    override fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

}