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

package io.github.rosemoe.sora.lsp2.client.languageserver.wrapper

import android.util.Log
import io.github.rosemoe.sora.lsp2.client.DefaultLanguageClient
import io.github.rosemoe.sora.lsp2.client.ServerWrapperBaseClientContext
import io.github.rosemoe.sora.lsp2.client.languageserver.ServerStatus
import io.github.rosemoe.sora.lsp2.client.languageserver.requestmanager.DefaultRequestManager
import io.github.rosemoe.sora.lsp2.client.languageserver.requestmanager.RequestManager
import io.github.rosemoe.sora.lsp2.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp2.editor.LspEditor
import io.github.rosemoe.sora.lsp2.editor.LspProject
import io.github.rosemoe.sora.lsp2.requests.Timeout
import io.github.rosemoe.sora.lsp2.requests.Timeouts
import io.github.rosemoe.sora.lsp2.utils.FileUri
import io.github.rosemoe.sora.lsp2.utils.LSPException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.CodeActionCapabilities
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.CompletionItemCapabilities
import org.eclipse.lsp4j.DefinitionCapabilities
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities
import org.eclipse.lsp4j.ExecuteCommandCapabilities
import org.eclipse.lsp4j.FormattingCapabilities
import org.eclipse.lsp4j.HoverCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.OnTypeFormattingCapabilities
import org.eclipse.lsp4j.RangeFormattingCapabilities
import org.eclipse.lsp4j.ReferencesCapabilities
import org.eclipse.lsp4j.RenameCapabilities
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpCapabilities
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.function.Function
import java.util.stream.Collectors

class LanguageServerWrapper(
    val serverDefinition: LanguageServerDefinition, val project: LspProject
) {
    private val TAG = "LanguageServerWrapper"

    private val connectedEditors = HashSet<LspEditor>()

    private var languageServer: LanguageServer? = null

    private var client: LanguageClient? = null
    var requestManager: RequestManager? = null
        private set

    private var initializeResult: InitializeResult? = null
    private var launcherFuture: Deferred<*>? = null
    private var initializeFuture: Deferred<InitializeResult>? = null
    private var capabilitiesAlreadyRequested = false
    private var crashCount = 0

    @Volatile
    private var alreadyShownTimeout = false

    @Volatile
    private var alreadyShownCrash = false

    @Volatile
    var status = ServerStatus.STOPPED
        private set

    private val readyToConnect = HashSet<LspEditor>()

    private lateinit var eventHandler: EventHandler

    /**
     * Warning: this is a long running operation
     *
     * @return the languageServer capabilities, or null if initialization job didn't complete
     */
    suspend fun getServerCapabilities(): ServerCapabilities? {
        if (initializeResult != null) {
            return initializeResult?.capabilities
        }

        try {
            start();

            withTimeout(
                if (capabilitiesAlreadyRequested) 0 else Timeout[Timeouts.INIT].toLong()
            ) {
                initializeResult = initializeFuture?.await()
            }

        } catch (e: TimeoutCancellationException) {
            val msg = String.format(
                "%s \n is not initialized after %d seconds",
                serverDefinition.toString(),
                Timeout[Timeouts.INIT] / 1000
            )
            Log.w(
                TAG,
                msg
            )
            serverDefinition.eventListener.onHandlerException(LSPException(msg))
            stop(false)
        } catch (e: Exception) {
            Log.w(TAG, "Error while waiting for initialization", e);
            serverDefinition.eventListener
                .onHandlerException(LSPException("Error while waiting for initialization", e));
            stop(false);
        }
        capabilitiesAlreadyRequested = true

        return initializeResult?.capabilities
    }


    /**
     * Starts the LanguageServer
     */
    suspend fun start() {
        start(false)
    }

    /**
     * Starts the LanguageServer
     *
     * @param throwException Whether to throw a startup failure exception
     */
    suspend fun start(throwException: Boolean) = withContext(Dispatchers.Main) {
        if (status != ServerStatus.STOPPED && alreadyShownCrash && alreadyShownTimeout) {
            return@withContext
        }

        val projectRootPath = project.projectUri.path
        status = ServerStatus.STARTING

        try {
            val streams: Pair<InputStream, OutputStream> =
                serverDefinition.start(projectRootPath)

            val (inputStream, outputStream) = streams

            val initParams = getInitParams()
            // using for lsp
            val executorService = Executors.newCachedThreadPool()

            eventHandler = EventHandler(
                serverDefinition.eventListener
            ) { status != ServerStatus.STOPPED }

            client =
                DefaultLanguageClient(ServerWrapperBaseClientContext(this@LanguageServerWrapper))

            val launcher = LSPLauncher
                .createClientLauncher(
                    client, inputStream, outputStream, executorService,
                    eventHandler
                )

            val connectLanguageServer = launcher.remoteProxy

            languageServer = connectLanguageServer
            launcherFuture = async { runInterruptible { launcher.startListening().get() } }

            eventHandler.setLanguageServer(connectLanguageServer)

            initializeFuture = async {
                connectLanguageServer.initialize(initParams)
                    .thenApply { res ->
                        initializeResult = res
                        Log.i(
                            TAG,
                            "Got initializeResult for $serverDefinition ; $projectRootPath"
                        )

                        requestManager = DefaultRequestManager(
                            this@LanguageServerWrapper,
                            requireNotNull(languageServer),
                            requireNotNull(client),
                            res.capabilities
                        )

                        status = ServerStatus.STARTED
                        // send the initialized message since some language servers depends on this message
                        (requestManager as DefaultRequestManager).initialized(InitializedParams())
                        status = ServerStatus.INITIALIZED
                        return@thenApply res
                    }.await()
            }

        } catch (e: IOException) {
            Log.w(
                TAG,
                "Failed to start $serverDefinition ; $projectRootPath", e
            )
            serverDefinition.eventListener.onHandlerException(
                LSPException(
                    "Failed to start " +
                            serverDefinition + " ; " + projectRootPath, e
                )
            )
            if (throwException) {
                throw RuntimeException(e)
            }
            stop(true)
        }

    }

    /*
     * The shutdown request is sent from the client to the server. It asks the server to shut down, but to not exit \
     * (otherwise the response might not be delivered correctly to the client).
     * Only if the exit flag is true, particular server instance will exit.
     */
    suspend fun stop(exit: Boolean) {
        if (status == ServerStatus.STOPPED || status == ServerStatus.STOPPING) {
            return
        }
        status = ServerStatus.STOPPING
        initializeFuture?.cancel("stop", null)
        try {
            val shutdown = languageServer?.shutdown()
            withTimeout(Timeout[Timeouts.SHUTDOWN].toLong()) {
                shutdown?.await()
            }

            if (exit && serverDefinition.callExitForLanguageServer()) {
                languageServer?.exit()
            }

        } catch (e: java.lang.Exception) {
            // most likely closed externally.
            Log.w(
                TAG,
                "exception occured while trying to shut down",
                e
            )
        } finally {
            launcherFuture?.cancel("stop")
            serverDefinition.stop(project.projectUri.path)
            for (ed in connectedEditors) {
                disconnect(ed)
            }
            launcherFuture = null
            capabilitiesAlreadyRequested = false
            initializeResult = null
            initializeFuture = null
            languageServer = null
            status = ServerStatus.STOPPED
        }
    }


    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param editor The editor
     */
    suspend fun disconnect(editor: LspEditor) {
        if (!connectedEditors.contains(editor)) {
            return
        }

        uriToLanguageServerWrapper.remove(
            editor.uri to editor.project.projectUri
        )
        connectedEditors.remove(editor)
        editor.dispose()
        if (connectedEditors.isEmpty()) {
            stop(false)
        }
    }

    private fun getInitParams(): InitializeParams {
        val initParams = InitializeParams().apply {
            rootUri = project.projectUri.toUri().toASCIIString()
        }
        val workspaceClientCapabilities = WorkspaceClientCapabilities().apply {
            applyEdit = false // Not ready to support this feature
            didChangeWatchedFiles = DidChangeWatchedFilesCapabilities()
            executeCommand = ExecuteCommandCapabilities()
            workspaceEdit = WorkspaceEditCapabilities()
            symbol = SymbolCapabilities()
            workspaceFolders = true
            configuration = false
        }
        val workspaceFolder = WorkspaceFolder().apply {
            uri = initParams.rootUri
        }
        // Maybe the user should be allowed to customize the WorkspaceFolder?
        // workspaceFolder.setName("");
        initParams.workspaceFolders = listOf(workspaceFolder)


        val textDocumentClientCapabilities = TextDocumentClientCapabilities().apply {
            codeAction = CodeActionCapabilities()
            codeAction.codeActionLiteralSupport =
                CodeActionLiteralSupportCapabilities()
            completion =
                CompletionCapabilities(CompletionItemCapabilities(true))
            definition = DefinitionCapabilities()
            documentHighlight =
                null // The feature is not currently supported in the sora-editor
            formatting = FormattingCapabilities()
            hover = HoverCapabilities()
            onTypeFormatting = OnTypeFormattingCapabilities()
            rangeFormatting = RangeFormattingCapabilities()
            references = ReferencesCapabilities()
            rename = RenameCapabilities()
            signatureHelp = SignatureHelpCapabilities()
            synchronization =
                SynchronizationCapabilities(true, true, true)
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

    fun crashed(e: java.lang.Exception) {
        crashCount += 1
        if (crashCount <= 3) {
            /*reconnect()*/
        } else {
            serverDefinition.eventListener.onHandlerException(
                io.github.rosemoe.sora.lsp.utils.LSPException(
                    String.format(
                        "LanguageServer for definition %s, project %s keeps crashing due to \n%s\n",
                        serverDefinition.toString(),
                        project.projectUri.path,
                        e.message
                    )
                )
            )
            alreadyShownCrash = true
            crashCount = 0
        }
    }

    /**
     * Connects an editor to the languageServer
     *
     * @param editor the editor
     */
    suspend fun connect(editor: LspEditor) {
        val uri = editor.uri

        if (connectedEditors.contains(editor)) {
            return
        }

        val key = uri to editor.project.projectUri
        uriToLanguageServerWrapper[key] = this
        start()

        if (initializeFuture == null) {
            synchronized(readyToConnect) { readyToConnect.add(editor) }
            return
        }


        val capabilities = getServerCapabilities()
        if (capabilities == null) {
            Log.w(
                TAG,
                "Capabilities are null for $serverDefinition"
            )
            return
        }

        val localInitializeFuture = requireNotNull(initializeFuture)

        localInitializeFuture.await()

        if (connectedEditors.contains(editor)) {
            return
        }

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

            // editor.syncOptions = textDocumentSyncKind

            val completionTriggers =
                if (capabilities.completionProvider != null
                    && capabilities.completionProvider
                        .triggerCharacters != null
                ) capabilities.completionProvider
                    .triggerCharacters else emptyList()
            val signatureHelpTriggers =
                if (capabilities.signatureHelpProvider != null
                    && capabilities.signatureHelpProvider
                        .triggerCharacters != null
                ) capabilities.signatureHelpProvider
                    .triggerCharacters else emptyList()
            val signatureHelpRetriggers =
                if (capabilities.signatureHelpProvider != null
                    && capabilities.signatureHelpProvider
                        .retriggerCharacters != null
                ) capabilities.signatureHelpProvider
                    .retriggerCharacters else emptyList()

            /* editor.signatureHelpTriggers = signatureHelpTriggers
             editor.signatureHelpRetriggers = signatureHelpRetriggers
             editor.completionTriggers = completionTriggers

             editor.open()*/

            for (ed in readyToConnect) {
                connect(ed)
            }

        } catch (e: Exception) {
            Log.w(
                TAG,
                e
            )
        }

    }


    /**
     * Checks if the wrapper is already connected to the document at the given path.
     *
     * @param location file location
     * @return True if the given file is connected.
     */
    fun isConnectedTo(location: String): Boolean {
        val fileUri = FileUri(location)
        return connectedEditors.any {
            it.uri == fileUri
        }
    }

    /**
     * @return the LanguageServer
     */
    suspend fun getServer(): LanguageServer? {
        start()
        if (initializeFuture?.isCompleted == false) {
            initializeFuture?.join()
        }
        return languageServer
    }


    private suspend fun reconnect() {
        // Need to copy by value since connected editors gets cleared during 'stop()' invocation.
        stop(false)
        for (editor in connectedEditors) {
            connect(editor)
        }
    }


    /**
     * Is the language server in a state where it can be restartable. Normally language server is
     * restartable if it has timeout or has a startup error.
     */
    val isRestartable = status == ServerStatus.STOPPED && (alreadyShownTimeout || alreadyShownCrash)


    /**
     * Reset language server wrapper state so it can be started again if it was failed earlier.
     */
    suspend fun restart() {
        if (isRestartable) {
            alreadyShownCrash = false
            alreadyShownTimeout = false
        } else {
            stop(false)
        }
        start()
    }

    fun unregister() {
        projectToLanguageServerWrapper.remove(
            project.projectUri
        )
    }


    fun getConnectedFiles(): List<String> {
        return connectedEditors.map {
            it.uri.path
        }
    }


    companion object {

        private val projectToLanguageServerWrapper =
            ConcurrentHashMap<FileUri, LanguageServerWrapper>()

        private val uriToLanguageServerWrapper =
            ConcurrentHashMap<Pair<FileUri, FileUri>, LanguageServerWrapper>()

        /**
         * @param uri             A file uri
         * @param projectRootPath The related project path
         * @return The wrapper for the given uri, or None
         */
        fun forUri(uri: FileUri, projectRootPath: FileUri): LanguageServerWrapper? {
            return uriToLanguageServerWrapper[Pair(uri, projectRootPath)]
        }

        /**
         * @param editor An editor
         * @return The wrapper for the given editor, or None
         */
        fun forEditor(editor: LspEditor): LanguageServerWrapper? {
            return uriToLanguageServerWrapper[Pair(
                editor.uri,
                editor.project.projectUri
            )]
        }

        fun forProject(project: LspProject): LanguageServerWrapper? {
            return projectToLanguageServerWrapper[project.projectUri]
        }
    }
}