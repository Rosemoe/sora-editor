/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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

package io.github.rosemoe.sora.lsp2.editor

import io.github.rosemoe.sora.lsp2.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp2.events.EventEmitter
import io.github.rosemoe.sora.lsp2.utils.FileUri
import io.github.rosemoe.sora.lsp2.utils.toFileUri

class LspProject(
    private val projectPath: String,
) {

    val projectUri = "file://$projectPath"

    val eventEmitter = EventEmitter()

    private val serverDefinitions = mutableMapOf<String, LanguageServerDefinition>()

    private val editors = mutableMapOf<FileUri, LspEditor>()

    fun addServerDefinition(definition: LanguageServerDefinition) {
        serverDefinitions[definition.ext] = definition
    }

    fun removeServerDefinition(ext: String) {
        serverDefinitions.remove(ext)
    }

    fun getServerDefinition(ext: String): LanguageServerDefinition? {
        return serverDefinitions[ext]
    }

    fun createEditor(path: String): LspEditor {
        val uri = FileUri(path)
        val editor = LspEditor(this, uri)
        editors[uri] = editor
        return editor
    }

    fun removeEditor(path: String) {
        editors.remove(path.toFileUri())
    }

    fun getEditor(path: String): LspEditor {
        return editors.getValue(path.toFileUri())
    }

    fun closeAllEditors() {
        editors.forEach {
            it.value.dispose()
        }
        editors.clear()
    }

    fun dispose() {

    }

    fun init() {
        initEventEmitter()
    }

    private fun initEventEmitter() {

    }
}