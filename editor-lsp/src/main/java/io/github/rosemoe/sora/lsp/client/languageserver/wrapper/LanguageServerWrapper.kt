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

package io.github.rosemoe.sora.lsp.client.languageserver.wrapper

import android.util.Log
import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lsp.client.DefaultLanguageClient
import io.github.rosemoe.sora.lsp.client.ServerWrapperBaseClientContext
import io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus
import io.github.rosemoe.sora.lsp.client.languageserver.ShutdownReason
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.DefaultRequestManager
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.events.AsyncEventListener
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.LSPException
import io.github.rosemoe.sora.lsp.utils.override
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.CodeActionCapabilities
import org.eclipse.lsp4j.CodeActionKindCapabilities
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities
import org.eclipse.lsp4j.ColorProviderCapabilities
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.CompletionItemCapabilities
import org.eclipse.lsp4j.DefinitionCapabilities
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.ExecuteCommandCapabilities
import org.eclipse.lsp4j.FormattingCapabilities
import org.eclipse.lsp4j.HoverCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.InlayHintCapabilities
import org.eclipse.lsp4j.OnTypeFormattingCapabilities
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import org.eclipse.lsp4j.RangeFormattingCapabilities
import org.eclipse.lsp4j.ReferencesCapabilities
import org.eclipse.lsp4j.RenameCapabilities
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpCapabilities
import org.eclipse.lsp4j.SignatureInformationCapabilities
import org.eclipse.lsp4j.SymbolCapabilities
import org.eclipse.lsp4j.SynchronizationCapabilities
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.eclipse.lsp4j.WorkspaceEditCapabilities
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.URI
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class LanguageServerWrapper(
    val serverDefinition: LanguageServerDefinition, val project: LspProject
) {
    private val TAG = "LanguageServerWrapper"
    val serverName = serverDefinition.name

    private val connectedEditors = Collections.newSetFromMap(WeakHashMap<LspEditor, Boolean>())

    private var languageServer: LanguageServer? = null

    private var client: LanguageClient? = null
    var requestManager: RequestManager? = null
        private set

    private var effectiveCapabilities: ServerCapabilities? = null

    private var initializeResult: InitializeResult? = null
    private var launcherFuture: Future<*>? = null
    private var initializeFuture: CompletableFuture<InitializeResult>? = null
    private var capabilitiesAlreadyRequested = false

    private var crashCount = 0

    @Volatile
    var status: ServerStatus = ServerStatus.IDLE
        private set(value) {
            if (field == value) return
            serverDefinition.eventListener.onStatusChange(value, field)
            field = value
        }

    private val readyToConnect = Collections.newSetFromMap(WeakHashMap<LspEditor, Boolean>())

    private val commonCoroutineScope = project.coroutineScope

    private var eventHandler: EventHandler? = null

    fun reportEventException(eventListener: AsyncEventListener, exception: Exception) {
        eventHandler?.listener?.onEventException(eventListener, exception)
    }

    /**
     * Warning: this is a long-running operation
     *
     * @return the languageServer capabilities, or null if initialization job didn't complete
     */
    @WorkerThread
    fun getServerCapabilities(): ServerCapabilities? {
        effectiveCapabilities?.let {
            return it
        }

        if (initializeResult != null) {
            return initializeResult?.capabilities
        }

        try {
            start(true)

            initializeFuture?.get(
                if (capabilitiesAlreadyRequested) 0L else Timeout[Timeouts.INIT].toLong(),
                TimeUnit.MILLISECONDS
            )
        } catch (_: TimeoutException) {
            val msg = String.format(
                Locale.getDefault(),
                "%s \n is not initialized after %d seconds",
                serverDefinition.toString(),
                Timeout[Timeouts.INIT] / 1000
            )
            Log.w(TAG, msg)
            serverDefinition.eventListener.onHandlerException(LSPException(msg))
            stop(false, ShutdownReason.TIMEOUT)
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error while waiting for initialization", e)
            serverDefinition.eventListener.onHandlerException(
                LSPException(
                    "Error while waiting for initialization",
                    e
                )
            )
            stop(false, ShutdownReason.CRASH)
            return null
        }

        if (initializeResult != null) {
            capabilitiesAlreadyRequested = true
        }

        return effectiveCapabilities ?: initializeResult?.capabilities
    }

    private fun isOffline(): Boolean {
        return status is ServerStatus.STOPPED || status is ServerStatus.IDLE
    }

    /**
     * Starts the LanguageServer
     *
     * @param throwException Whether to throw a startup failure exception
     */
    @WorkerThread
    fun start(throwException: Boolean = false) {
        synchronized(this) {
            if (!isOffline()) return

            val projectRootPath = project.projectUri.path
            status = ServerStatus.STARTING

            try {
                val streams: Pair<InputStream, OutputStream> =
                    serverDefinition.start(projectRootPath)

                val (inputStream, outputStream) = streams

                val initParams = getInitParams()
                // using for lsp
                val executorService = Executors.newCachedThreadPool()

                val wrapperRef = WeakReference(this@LanguageServerWrapper)
                eventHandler = EventHandler(
                    serverDefinition.eventListener
                ) { wrapperRef.get()?.isOffline()?.not() == true }

                client =
                    DefaultLanguageClient(ServerWrapperBaseClientContext(this@LanguageServerWrapper))

                val launcher = LSPLauncher
                    .createClientLauncher(
                        client, inputStream, outputStream, executorService,
                        eventHandler
                    )

                val connectedLanguageServer = launcher.remoteProxy

                languageServer = connectedLanguageServer

                launcherFuture = launcher.startListening()

                eventHandler?.setLanguageServer(connectedLanguageServer)

                initializeFuture =
                    connectedLanguageServer.initialize(initParams)
                        .thenApply { res ->
                            initializeResult = res
                            Log.i(
                                TAG,
                                "Got initializeResult for $serverDefinition ; $projectRootPath"
                            )

                            val fallbackCapabilities = serverDefinition.expectedCapabilities()
                            val mergedCapabilities =
                                mergeCapabilities(res.capabilities, fallbackCapabilities)
                            effectiveCapabilities = mergedCapabilities
                            res.capabilities = mergedCapabilities

                            requestManager = DefaultRequestManager(
                                this@LanguageServerWrapper,
                                requireNotNull(languageServer),
                                requireNotNull(client),
                                mergedCapabilities,
                                serverDefinition.disabledFeatures
                            )

                            eventHandler?.listener?.initialize(
                                connectedLanguageServer,
                                InitializeResult(mergedCapabilities)
                            )

                            status = ServerStatus.STARTED
                            // send the initialized message since some language servers depends on this message
                            (requestManager as DefaultRequestManager).initialized(InitializedParams())
                            status = ServerStatus.INITIALIZED

                            return@thenApply res
                        }

            } catch (e: IOException) {
                Log.w(TAG, "Failed to start $serverDefinition ; $projectRootPath", e)
                serverDefinition.eventListener.onHandlerException(
                    LSPException("Failed to start $serverDefinition ; $projectRootPath", e)
                )
                if (throwException) {
                    throw RuntimeException(e)
                }
                stop(true, ShutdownReason.CRASH)
            }
        }
    }

    /*
     * The shutdown request is sent from the client to the server. It asks the server to shut down, but to not exit \
     * (otherwise the response might not be delivered correctly to the client).
     * Only if the exit flag is true, particular server instance will exit.
     */
    fun stop(exit: Boolean, reason: ShutdownReason = ShutdownReason.MANUAL) {
        synchronized(this) {
            if (isOffline() || status is ServerStatus.STOPPING) {
                return
            }
            status = ServerStatus.STOPPING(reason)
            initializeFuture?.cancel(true)
            try {
                val shutdown = languageServer?.shutdown()

                shutdown?.get(Timeout[Timeouts.SHUTDOWN].toLong(), TimeUnit.MILLISECONDS)

                if (exit && serverDefinition.callExitForLanguageServer()) {
                    languageServer?.exit()
                }

            } catch (e: java.lang.Exception) {
                // most likely closed externally.
                Log.w(TAG, "Exception occurred while trying to shut down", e)
            } finally {
                launcherFuture?.cancel(true)
                serverDefinition.stop(project.projectUri.path)
                for (ed in connectedEditors.toList()) {
                    disconnect(ed)
                    ed.onWrapperStopped(this)
                }
                launcherFuture = null
                capabilitiesAlreadyRequested = false
                initializeResult = null
                initializeFuture = null
                languageServer = null
                eventHandler = null
                status = ServerStatus.STOPPED(reason)
            }
        }
    }

    private fun getInitParams(): InitializeParams {
        val initParams = InitializeParams().apply {
            rootUri = project.projectUri.toUri().toASCIIString()
        }

        val workspaceClientCapabilities = WorkspaceClientCapabilities().apply {
            applyEdit = true
            didChangeWatchedFiles = DidChangeWatchedFilesCapabilities()
            executeCommand = ExecuteCommandCapabilities()
            workspaceEdit = WorkspaceEditCapabilities()
            symbol = SymbolCapabilities()
            workspaceFolders = true
            configuration = false
        }
        val workspaceFolder = WorkspaceFolder().apply {
            uri = initParams.rootUri
            name = File(URI.create(uri)).name
        }
        // Maybe the user should be allowed to customize the WorkspaceFolder?
        // workspaceFolder.setName("")
        initParams.workspaceFolders = listOf(workspaceFolder)

        val markupKinds = listOf("markdown", "plaintext")
        val textDocumentClientCapabilities = TextDocumentClientCapabilities().apply {
            codeAction = CodeActionCapabilities()
            codeAction.codeActionLiteralSupport = CodeActionLiteralSupportCapabilities().apply {
                codeActionKind = CodeActionKindCapabilities()
            }
            completion =
                CompletionCapabilities(CompletionItemCapabilities(true))
            definition = DefinitionCapabilities()
            documentHighlight = DocumentHighlightCapabilities()
            colorProvider = ColorProviderCapabilities()
            inlayHint = InlayHintCapabilities()
            formatting = FormattingCapabilities()
            hover = HoverCapabilities(markupKinds, true)
            onTypeFormatting = OnTypeFormattingCapabilities()
            rangeFormatting = RangeFormattingCapabilities()
            references = ReferencesCapabilities()
            rename = RenameCapabilities(true, true)
            signatureHelp =
                SignatureHelpCapabilities(SignatureInformationCapabilities(markupKinds), true)
            synchronization =
                SynchronizationCapabilities(true, true, true)
            publishDiagnostics = PublishDiagnosticsCapabilities(true)
        }

        initParams.apply {
            capabilities =
                ClientCapabilities(
                    workspaceClientCapabilities,
                    textDocumentClientCapabilities,
                    null
                )
            initializationOptions =
                serverDefinition.getInitializationOptions(URI.create(initParams.rootUri))
        }
        return initParams
    }

    fun crashed(e: Exception) {
        crashCount += 1
        if (crashCount <= 3) {
            commonCoroutineScope.launch {
                restartAndReconnect()
            }
        } else {
            serverDefinition.eventListener.onHandlerException(
                LSPException(
                    String.format(
                        "LanguageServer for definition %s, project %s keeps crashing due to \n%s\n",
                        serverDefinition.toString(),
                        project.projectUri.path,
                        e.message
                    )
                )
            )
            stop(false, ShutdownReason.CRASH)
            crashCount = 0
        }
    }

    /**
     * Connects an editor to the languageServer
     *
     * @param editor the editor
     */
    @WorkerThread
    fun connect(editor: LspEditor) {
        if (connectedEditors.contains(editor)) {
            return
        }

        start()

        if (initializeFuture == null) {
            synchronized(readyToConnect) { readyToConnect.add(editor) }
            return
        }

        val capabilities = getServerCapabilities()
        if (capabilities == null) {
            Log.w(TAG, "Capabilities are null for $serverDefinition")
            return
        }

        val localInitializeFuture = initializeFuture ?: return

        if (localInitializeFuture.isCancelled) {
            return
        }

        localInitializeFuture.get(Timeout[Timeouts.INIT].toLong(), TimeUnit.MILLISECONDS)

        try {
            val syncOptions =
                capabilities.textDocumentSync ?: return

            connectedEditors.add(editor)
            synchronized(readyToConnect) { readyToConnect.remove(editor) }
            var textDocumentSyncKind =
                if (syncOptions.isLeft) syncOptions.left else syncOptions.right
                    .change

            textDocumentSyncKind =
                textDocumentSyncKind ?: TextDocumentSyncKind.Full

            editor.textDocumentSyncKind =
                if (textDocumentSyncKind != TextDocumentSyncKind.Incremental || editor.textDocumentSyncKind != TextDocumentSyncKind.Incremental)
                    TextDocumentSyncKind.Full else textDocumentSyncKind

            val completionTriggers = capabilities.completionProvider
                ?.triggerCharacters ?: emptyList()
            val signatureHelpTriggers =
                capabilities.signatureHelpProvider
                    ?.triggerCharacters ?: emptyList()
            val signatureHelpReTriggers = capabilities.signatureHelpProvider
                ?.retriggerCharacters ?: emptyList()

            editor.signatureHelpTriggers.addAll(signatureHelpTriggers)
            editor.signatureHelpReTriggers.addAll(signatureHelpReTriggers)
            editor.completionTriggers.addAll(completionTriggers)
            editor.completionTriggers.addAll(arrayOf("[", "{", "(", "<", ".", ","))

            for (ed in readyToConnect) {
                connect(ed)
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param editor The editor
     */
    @WorkerThread
    fun disconnect(editor: LspEditor) {
        if (!connectedEditors.contains(editor)) {
            return
        }

        connectedEditors.remove(editor)
        if (connectedEditors.isEmpty()) {
            stop(false, ShutdownReason.UNUSED)
        }
    }

    /**
     * @return the LanguageServer
     */
    fun getServer(): LanguageServer? {
        start()

        if (initializeFuture?.isDone == false) {
            initializeFuture?.join()
        }

        return languageServer
    }

    /**
     * Reset language server wrapper state so it can be started again if it was failed earlier.
     */
    @WorkerThread
    fun restart() {
        stop(false, ShutdownReason.RESTART)
        start()
    }

    @WorkerThread
    fun restartAndReconnect() {
        val editorsSnapshot = connectedEditors.toList()
        restart()
        for (editor in editorsSnapshot) {
            editor.connectWithTimeoutBlocking()
        }
    }

    private fun mergeCapabilities(
        primary: ServerCapabilities,
        fallback: ServerCapabilities?
    ): ServerCapabilities {
        if (fallback == null) {
            return primary
        }
        val merged = ServerCapabilities()
        merged.override(primary)
        merged.override(fallback)
        return merged
    }
}
