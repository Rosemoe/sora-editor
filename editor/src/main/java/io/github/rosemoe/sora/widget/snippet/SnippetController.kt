/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

package io.github.rosemoe.sora.widget.snippet

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.snippet.variable.ClipboardBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.CommentBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.CompositeSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.EditorBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.FileBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.RandomBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.TimeBasedSnippetVariableResolver
import io.github.rosemoe.sora.widget.snippet.variable.WorkspaceBasedSnippetVariableResolver

class SnippetController(private val editor: CodeEditor) {

    val commentVariableResolver = CommentBasedSnippetVariableResolver(null)

    var fileVariableResolver: FileBasedSnippetVariableResolver? = null
        set(value) {
            val old = field
            if (old != null) {
                variableResolver.removeResolver(old)
            }
            field = value
            if (value != null) {
                variableResolver.addResolver(value)
            }
        }

    var workspaceVariableResolver: WorkspaceBasedSnippetVariableResolver? = null
        set(value) {
            val old = field
            if (old != null) {
                variableResolver.removeResolver(old)
            }
            field = value
            if (value != null) {
                variableResolver.addResolver(value)
            }
        }

    private val variableResolver = CompositeSnippetVariableResolver().also {
        it.addResolver(ClipboardBasedSnippetVariableResolver(editor.clipboardManager))
        it.addResolver(EditorBasedSnippetVariableResolver(editor))
        it.addResolver(RandomBasedSnippetVariableResolver())
        it.addResolver(TimeBasedSnippetVariableResolver())
        it.addResolver(commentVariableResolver)
    }



}