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


import android.net.Uri;
import android.util.Pair;

import androidx.annotation.Nullable;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import io.github.rosemoe.sora.lsp.client.languageserver.ServerStatus;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspVirtualEditor;

/**
 * The implementation of a LanguageServerWrapper (specific to a serverDefinition and a project)
 */
public class LanguageServerWrapper {

    public LanguageServerDefinition serverDefinition;


    private final HashSet<LspVirtualEditor> toConnect = new HashSet<>();
    private final String projectRootPath;
    private final HashSet<String> urisUnderLspControl = new HashSet<>();
    private final HashSet<LspVirtualEditor> connectedEditors = new HashSet<>();
    private final Map<String, Set<LspVirtualEditor>> uriToEditorManagers = new HashMap<>();
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
    private static final Map<String, LanguageServerWrapper> projectToLanguageServerWrapper = new ConcurrentHashMap<>();


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
    public static LanguageServerWrapper forEditor(LspVirtualEditor editor) {
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

    public void crashed(Exception e) {
        crashCount += 1;
        //TODO: implement reconnection
       /* if (crashCount <= 3) {
            reconnect();
        } else {
            invokeLater(() -> {
                if (alreadyShownCrash) {
                    reconnect();
                } else {
                    int response = Messages.showYesNoDialog(String.format(
                                    "LanguageServer for definition %s, project %s keeps crashing due to \n%s\n"
                                    , serverDefinition.toString(), project.getName(), e.getMessage()),
                            "Language Server Client Warning", "Keep Connected", "Disconnect", PlatformIcons.CHECK_ICON);
                    if (response == Messages.NO) {
                        int confirm = Messages.showYesNoDialog("All the language server based plugin features will be disabled.\n" +
                                "Do you wish to continue?", "", PlatformIcons.WARNING_INTRODUCTION_ICON);
                        if (confirm == Messages.YES) {
                            // Disconnects from the language server.
                            stop(true);
                        } else {
                            reconnect();
                        }
                    } else {
                        reconnect();
                    }
                }
                alreadyShownCrash = true;
                crashCount = 0;
            });
        }*/
    }
}
