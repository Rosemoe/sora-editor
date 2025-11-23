package io.github.rosemoe.sora.lsp.editor

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.AggregatedRequestManager
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import org.eclipse.lsp4j.ServerCapabilities
import java.util.concurrent.TimeoutException

internal class LspEditorDelegate(private val editor: LspEditor) {

    private data class SessionInfo(
        val definition: LanguageServerDefinition,
        val wrapper: LanguageServerWrapper
    )

    private val sessionInfos = mutableListOf<SessionInfo>()

    val aggregatedRequestManager = AggregatedRequestManager(emptyList())

    /** Ensure every definition for the current extension has a wrapper and an aggregated entry. */
    private fun refreshSessions() {
        val definitions = editor.project.getServerDefinitions(editor.fileExt).ifEmpty {
            editor.project.getServerDefinition(editor.fileExt)?.let { listOf(it) } ?: emptyList()
        }
        sessionInfos.clear()
        definitions.forEach { definition ->
            val wrapper = editor.project.getOrCreateLanguageServerWrapper(definition.ext, definition.name)
            sessionInfos.add(SessionInfo(definition, wrapper))
        }
        aggregatedRequestManager.updateSessions(
            sessionInfos.map { info ->
                AggregatedRequestManager.SessionEntry(
                    info.definition.name,
                    { info.wrapper.requestManager },
                    { info.wrapper.getServerCapabilities() }
                )
            }
        )
    }

    fun connectAll(): ServerCapabilities? {
        refreshSessions()
        var lastCapabilities: ServerCapabilities? = null
        for (info in sessionInfos) {
            info.wrapper.start()
            val capabilities = info.wrapper.getServerCapabilities()
            capabilities?.let { lastCapabilities = it }
            info.wrapper.connect(editor)
        }
        return aggregatedRequestManager.capabilities ?: lastCapabilities
    }

    fun disconnectAll() {
        sessionInfos.forEach { it.wrapper.disconnect(editor) }
    }

    fun getPrimaryWrapper(): LanguageServerWrapper? {
        return sessionInfos.firstOrNull()?.wrapper
    }
}
