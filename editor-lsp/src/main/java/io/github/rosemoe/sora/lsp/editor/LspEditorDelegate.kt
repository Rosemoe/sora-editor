package io.github.rosemoe.sora.lsp.editor

import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.AggregatedRequestManager
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import org.eclipse.lsp4j.ServerCapabilities

internal class LspEditorDelegate(private val editor: LspEditor) {

    private data class SessionInfo(
        val definition: LanguageServerDefinition,
        val wrapper: LanguageServerWrapper
    )

    private val sessionInfos = mutableListOf<SessionInfo>()

    val aggregatedRequestManager = AggregatedRequestManager(emptySet())

    private fun refreshSessions() {
        val definitions = editor.project.getServerDefinitions(editor.fileExt).ifEmpty {
            editor.project.getServerDefinition(editor.fileExt)?.let { listOf(it) } ?: emptyList()
        }

        sessionInfos.removeAll { session ->
            definitions.none { it.name == session.definition.name && it.ext == session.definition.ext }
        }

        definitions.forEach { definition ->
            val exists = sessionInfos.any {
                it.definition.name == definition.name && it.definition.ext == definition.ext
            }
            if (!exists) {
                val wrapper =
                    editor.project.getOrCreateLanguageServerWrapper(definition.ext, definition.name)
                sessionInfos.add(SessionInfo(definition, wrapper))
            }
        }
    }

    @WorkerThread
    fun connectAll(): ServerCapabilities? {
        refreshSessions()
        var lastCapabilities: ServerCapabilities? = null

        for (info in sessionInfos) {
            if (info.wrapper.status == ServerStatus.INITIALIZED) {
                continue
            }

            info.wrapper.start()
            val capabilities = info.wrapper.getServerCapabilities()

            if (capabilities != null) {
                info.wrapper.connect(editor)
                lastCapabilities = capabilities
            }
        }

        val initializedWrappers = sessionInfos
            .filter { it.wrapper.status == ServerStatus.INITIALIZED }
            .map { it.wrapper }
            .toSet()

        aggregatedRequestManager.updateSessions(initializedWrappers)

        return aggregatedRequestManager.capabilities ?: lastCapabilities
    }

    fun disconnectAll() {
        sessionInfos.forEach { it.wrapper.disconnect(editor) }
        sessionInfos.clear()
    }

    fun getPrimaryWrapper(): LanguageServerWrapper? {
        return sessionInfos.firstOrNull()?.wrapper
    }
}
