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
import io.github.rosemoe.sora.lsp2.client.languageserver.wrapper.LanguageServerWrapper
import io.github.rosemoe.sora.lsp2.editor.diagnostics.DiagnosticsContainer
import io.github.rosemoe.sora.lsp2.events.EventEmitter
import io.github.rosemoe.sora.lsp2.utils.FileUri
import io.github.rosemoe.sora.lsp2.utils.toFileUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ForkJoinPool

class LspProject(
    private val projectPath: String,
) {

    val projectUri = FileUri(projectPath)

    val eventEmitter = EventEmitter()

    private val languageServerWrappers = mutableMapOf<String, LanguageServerWrapper>()

    private val serverDefinitions = mutableMapOf<String, LanguageServerDefinition>()

    private val editors = mutableMapOf<FileUri, LspEditor>()

    val diagnosticsContainer = DiagnosticsContainer()

    val coroutineScope =
        CoroutineScope(ForkJoinPool.commonPool().asCoroutineDispatcher() + SupervisorJob())

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

    fun getEditor(path: String): LspEditor? {
        return editors[path.toFileUri()]
    }

    fun getEditor(uri: FileUri): LspEditor? {
        return editors[uri]
    }

    fun getOrCreateEditor(path: String): LspEditor {
        return getEditor(path) ?: createEditor(path)
    }

    suspend fun closeAllEditors() {
        editors.forEach {
            it.value.dispose()
        }
        editors.clear()
    }

    internal fun getLanguageServerWrapper(ext: String): LanguageServerWrapper? {
        return languageServerWrappers[ext]
    }

    internal fun getOrCreateLanguageServerWrapper(ext: String): LanguageServerWrapper {
        return languageServerWrappers[ext] ?: createLanguageServerWrapper(ext)
    }

    internal fun createLanguageServerWrapper(ext: String): LanguageServerWrapper {
        val definition = serverDefinitions[ext]
            ?: throw IllegalArgumentException("No server definition for extension $ext")
        val wrapper = LanguageServerWrapper(definition, this)
        languageServerWrappers[ext] = wrapper
        return wrapper
    }

    suspend fun dispose() {
        closeAllEditors()
        languageServerWrappers.forEach {
            it.value.stop(false)
        }
    }

    fun init() {
        initEventEmitter()
    }

    private fun initEventEmitter() {

    }
}