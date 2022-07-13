/*
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
 */
package io.github.rosemoe.sora.lsp.client.languageserver.wrapper;

import static io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus.*;
import static io.github.rosemoe.sora.lsp.requests.Timeout.getTimeout;
import static io.github.rosemoe.sora.lsp.requests.Timeouts.INIT;
import static io.github.rosemoe.sora.lsp.requests.Timeouts.SHUTDOWN;


import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.ExecuteCommandCapabilities;
import org.eclipse.lsp4j.FormattingCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.OnTypeFormattingCapabilities;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.ReferencesCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SignatureHelpCapabilities;
import org.eclipse.lsp4j.SymbolCapabilities;
import org.eclipse.lsp4j.SynchronizationCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.lsp.client.DefaultLanguageClient;
import io.github.rosemoe.sora.lsp.client.ServerWrapperBaseClientContext;
import io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.DefaultRequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditorManager;
import io.github.rosemoe.sora.lsp.utils.LSPException;
import io.github.rosemoe.sora.lsp.utils.URIUtils;

/**
 * The implementation of a LanguageServerWrapper (specific to a serverDefinition and a project)
 */
public class LanguageServerWrapper {

    private static final String TAG = "LanguageServerWrapper";

    public LanguageServerDefinition serverDefinition;

    private final String projectRootPath;

    private final HashSet<LspEditor> connectedEditors = new HashSet<>();


    private LanguageServer languageServer;
    private LanguageClient client;
    private RequestManager requestManager;
    private InitializeResult initializeResult;
    private Future<?> launcherFuture;
    private CompletableFuture<InitializeResult> initializeFuture;
    private boolean capabilitiesAlreadyRequested = false;
    private int crashCount = 0;
    private volatile boolean alreadyShownTimeout = false;
    private volatile boolean alreadyShownCrash = false;
    private volatile ServerStatus status = STOPPED;
    private static final Map<Pair<String, String>, LanguageServerWrapper> uriToLanguageServerWrapper =
            new ConcurrentHashMap<>();
    private final HashSet<LspEditor> toConnect = new HashSet<>();
    private static final Map<String, LanguageServerWrapper> projectToLanguageServerWrapper = new ConcurrentHashMap<>();

    private EventHandler eventHandler;

    public LanguageServerWrapper(@NotNull LanguageServerDefinition serverDefinition, @NotNull String projectRootPath) {
        this.serverDefinition = serverDefinition;

        // We need to keep the project rootPath in addition to the project instance, since we cannot get the project
        // base path if the project is disposed.
        this.projectRootPath = projectRootPath;
        projectToLanguageServerWrapper.put(projectRootPath, this);
    }


    /**
     * @param uri             A file uri
     * @param projectRootPath The related project path
     * @return The wrapper for the given uri, or None
     */
    @Nullable
    public static LanguageServerWrapper forUri(String uri, String projectRootPath) {
        return uriToLanguageServerWrapper.get(new Pair<>(uri, Uri.fromFile(new File(projectRootPath)).toString()));
    }

    /**
     * @param editor An editor
     * @return The wrapper for the given editor, or None
     */
    @Nullable
    public static LanguageServerWrapper forEditor(LspEditor editor) {
        return uriToLanguageServerWrapper.get(new Pair<>(editor.getCurrentFileUri(), editor.getCurrentFileUri()));
    }

    @Nullable
    public static LanguageServerWrapper forProject(String projectRootPath) {
        return projectToLanguageServerWrapper.get(projectRootPath);
    }

    public LanguageServerDefinition getServerDefinition() {
        return serverDefinition;
    }

    public String getProjectRootPath() {
        return projectRootPath;
    }

    public ServerStatus getStatus() {
        return status;
    }

    private void setStatus(ServerStatus status) {
        this.status = status;
    }

    /**
     * Warning: this is a long running operation
     *
     * @return the languageServer capabilities, or null if initialization job didn't complete
     */
    @Nullable
    public ServerCapabilities getServerCapabilities() {
        if (initializeResult != null)
            return initializeResult.getCapabilities();
        else {
            try {
                start();
                if (initializeFuture != null) {
                    initializeFuture.get((capabilitiesAlreadyRequested ? 0 : getTimeout(INIT)), TimeUnit.MILLISECONDS);
                }
            } catch (TimeoutException e) {
                String msg = String.format("%s \n is not initialized after %d seconds",
                        serverDefinition.toString(), getTimeout(INIT) / 1000);
                Log.w(TAG, msg);
                serverDefinition.getEventListener().onHandlerException(new LSPException(msg));
                stop(false);
            } catch (Exception e) {
                Log.w(TAG, "Error while waiting for initialization", e);
                serverDefinition.getEventListener().onHandlerException(new LSPException("Error while waiting for initialization", e));
                stop(false);
            }
        }
        capabilitiesAlreadyRequested = true;
        return initializeResult != null ? initializeResult.getCapabilities() : null;
    }

    /**
     * Starts the LanguageServer
     */
    public void start() {
        if (status == STOPPED && !alreadyShownCrash && !alreadyShownTimeout) {
            setStatus(STARTING);
            try {
                Pair<InputStream, OutputStream> streams = serverDefinition.start(projectRootPath);
                InputStream inputStream = streams.first;
                OutputStream outputStream = streams.second;
                InitializeParams initParams = getInitParams();
                ExecutorService executorService = Executors.newCachedThreadPool();
                eventHandler = new EventHandler(serverDefinition.getEventListener(), () -> getStatus() != STOPPED);

                client = new DefaultLanguageClient(new ServerWrapperBaseClientContext(this));
                Launcher<LanguageServer> launcher = LSPLauncher
                        .createClientLauncher(client, inputStream, outputStream, executorService,
                                eventHandler);
                languageServer = launcher.getRemoteProxy();
                launcherFuture = launcher.startListening();

                eventHandler.setLanguageServer(languageServer);

                initializeFuture = languageServer.initialize(initParams).thenApply(res -> {
                    initializeResult = res;
                    Log.i(TAG, "Got initializeResult for " + serverDefinition + " ; " + projectRootPath);

                    requestManager = new DefaultRequestManager(this, languageServer, client, res.getCapabilities());

                    setStatus(STARTED);
                    // send the initialized message since some language servers depends on this message
                    requestManager.initialized(new InitializedParams());
                    setStatus(INITIALIZED);
                    return res;
                });
            } catch (IOException e) {
                Log.w(TAG, "Failed to start " + serverDefinition + " ; " + projectRootPath, e);
                serverDefinition.getEventListener().onHandlerException(new LSPException("Failed to start " +
                        serverDefinition + " ; " + projectRootPath, e));

                stop(true);
            }
        }
    }


    /*
     * The shutdown request is sent from the client to the server. It asks the server to shut down, but to not exit \
     * (otherwise the response might not be delivered correctly to the client).
     * Only if the exit flag is true, particular server instance will exit.
     */
    public void stop(boolean exit) {
        if (this.status == STOPPED || this.status == STOPPING) {
            return;
        }
        setStatus(STOPPING);

        if (initializeFuture != null) {
            initializeFuture.cancel(true);
        }

        try {
            if (languageServer != null) {
                CompletableFuture<Object> shutdown = languageServer.shutdown();
                shutdown.get(getTimeout(SHUTDOWN), TimeUnit.MILLISECONDS);
                if (exit && serverDefinition.callExitForLanguageServer()) {
                    languageServer.exit();
                }
            }
        } catch (Exception e) {
            // most likely closed externally.

            Log.w(TAG, "exception occured while trying to shut down", e);
        } finally {
            if (launcherFuture != null) {
                launcherFuture.cancel(true);
            }
            if (serverDefinition != null) {
                serverDefinition.stop(projectRootPath);
            }
            for (LspEditor ed : new HashSet<>(connectedEditors)) {
                disconnect(ed);
            }

            launcherFuture = null;
            capabilitiesAlreadyRequested = false;
            initializeResult = null;
            initializeFuture = null;
            languageServer = null;
            setStatus(STOPPED);
        }
    }

    /**
     * Disconnects an editor from the LanguageServer
     *
     * @param editor The editor
     */
    public void disconnect(LspEditor editor) {

        if (!connectedEditors.contains(editor)) {
            return;
        }

        String uri = editor.getCurrentFileUri();

        uriToLanguageServerWrapper.remove(new Pair<>(uri, editor.getProjectPath()));

        connectedEditors.remove(editor);

        editor.close();

        if (connectedEditors.isEmpty()) {
            stop(false);
        }
    }

    private InitializeParams getInitParams() {
        InitializeParams initParams = new InitializeParams();
        initParams.setRootUri(URIUtils.fileToURI(projectRootPath).toString());

        WorkspaceClientCapabilities workspaceClientCapabilities = new WorkspaceClientCapabilities();
        workspaceClientCapabilities.setApplyEdit(false); // Not ready to support this feature
        workspaceClientCapabilities.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities());
        workspaceClientCapabilities.setExecuteCommand(new ExecuteCommandCapabilities());
        workspaceClientCapabilities.setWorkspaceEdit(new WorkspaceEditCapabilities());
        workspaceClientCapabilities.setSymbol(new SymbolCapabilities());
        workspaceClientCapabilities.setWorkspaceFolders(false);
        workspaceClientCapabilities.setConfiguration(false);

        TextDocumentClientCapabilities textDocumentClientCapabilities = new TextDocumentClientCapabilities();
        textDocumentClientCapabilities.setCodeAction(new CodeActionCapabilities());
        textDocumentClientCapabilities.getCodeAction().setCodeActionLiteralSupport(new CodeActionLiteralSupportCapabilities());
        textDocumentClientCapabilities.setCompletion(new CompletionCapabilities(new CompletionItemCapabilities(true)));
        textDocumentClientCapabilities.setDefinition(new DefinitionCapabilities());
        textDocumentClientCapabilities.setDocumentHighlight(null); // The feature is not currently supported in the sora-editor
        textDocumentClientCapabilities.setFormatting(new FormattingCapabilities());
        textDocumentClientCapabilities.setHover(new HoverCapabilities());
        textDocumentClientCapabilities.setOnTypeFormatting(new OnTypeFormattingCapabilities());
        textDocumentClientCapabilities.setRangeFormatting(new RangeFormattingCapabilities());
        textDocumentClientCapabilities.setReferences(new ReferencesCapabilities());
        textDocumentClientCapabilities.setRename(new RenameCapabilities());
        textDocumentClientCapabilities.setSignatureHelp(new SignatureHelpCapabilities());
        textDocumentClientCapabilities.setSynchronization(new SynchronizationCapabilities(true, true, true));
        initParams.setCapabilities(
                new ClientCapabilities(workspaceClientCapabilities, textDocumentClientCapabilities, null));
        initParams.setInitializationOptions(
                serverDefinition.getInitializationOptions(URI.create(initParams.getRootUri())));

        return initParams;
    }

    public void crashed(Exception e) {
        crashCount += 1;
        if (crashCount <= 3) {
            reconnect();
        } else {
            serverDefinition.getEventListener().onHandlerException(new LSPException(String.format(
                    "LanguageServer for definition %s, project %s keeps crashing due to \n%s\n"
                    , serverDefinition.toString(), projectRootPath, e.getMessage())));
            alreadyShownCrash = true;
            crashCount = 0;
        }

    }


    /**
     * Connects an editor to the languageServer
     *
     * @param editor the editor
     */
    public void connect(LspEditor editor) {

        String uri = editor.getCurrentFileUri();
        if (connectedEditors.contains(editor)) {
            return;
        }
        Pair<String, String> key = new Pair<>(uri, editor.getProjectPath());

        uriToLanguageServerWrapper.put(key, this);

        start();
        if (initializeFuture != null) {
            ServerCapabilities capabilities = getServerCapabilities();
            if (capabilities == null) {
                Log.w(TAG, "Capabilities are null for " + serverDefinition);
                return;
            }
            initializeFuture.thenRun(() -> {
                if (connectedEditors.contains(editor)) {
                    return;
                }
                try {
                    Either<TextDocumentSyncKind, TextDocumentSyncOptions> syncOptions = capabilities.getTextDocumentSync();
                    if (syncOptions != null) {
                        connectedEditors.add(editor);
                        synchronized (toConnect) {
                            toConnect.remove(editor);
                        }
                        TextDocumentSyncKind textDocumentSyncKind = syncOptions.isLeft() ? syncOptions.getLeft() : syncOptions.getRight().getChange();
                        textDocumentSyncKind = textDocumentSyncKind == null ? TextDocumentSyncKind.Full : textDocumentSyncKind;

                        editor.setSyncOptions(textDocumentSyncKind);

                        editor.installFeatures();

                        editor.open();

                        for (LspEditor ed : new HashSet<>(toConnect)) {
                            connect(ed);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            });

        } else {
            synchronized (toConnect) {
                toConnect.add(editor);
            }
        }
    }

    /**
     * Checks if the wrapper is already connected to the document at the given path.
     *
     * @param location file location
     * @return True if the given file is connected.
     */
    public boolean isConnectedTo(String location) {
        String fileUri = Uri.fromFile(new File(location)).toString();

        return connectedEditors.stream().anyMatch(lspEditor -> fileUri.equals(lspEditor.getCurrentFileUri()));

    }

    /**
     * @return the LanguageServer
     */
    @Nullable
    public LanguageServer getServer() {
        start();
        if (initializeFuture != null && !initializeFuture.isDone()) {
            initializeFuture.join();
        }
        return languageServer;
    }


    private void reconnect() {
        // Need to copy by value since connected editors gets cleared during 'stop()' invocation.
        stop(false);
        for (String uri : connectedEditors.stream().map(LspEditor::getCurrentFileUri).collect(Collectors.toList())) {
            connect(uri);
        }
    }


    /**
     * Is the language server in a state where it can be restartable. Normally language server is
     * restartable if it has timeout or has a startup error.
     */
    public boolean isRestartable() {
        return status == STOPPED && (alreadyShownTimeout || alreadyShownCrash);
    }


    /**
     * Reset language server wrapper state so it can be started again if it was failed earlier.
     */
    public void restart() {
        if (isRestartable()) {
            alreadyShownCrash = false;
            alreadyShownTimeout = false;
        } else {
            stop(false);
        }
        start();
    }

    private void connect(String uri) {
        connect(LspEditorManager.getOrCreateEditorManager(projectRootPath).getEditor(uri));
    }

    public List<String> getConnectedFiles() {
        return connectedEditors.stream().map(LspEditor::getCurrentFileUri).collect(Collectors.toList());
    }


    /**
     * @return The request manager for this wrapper
     */
    public RequestManager getRequestManager() {
        return requestManager;
    }
}
