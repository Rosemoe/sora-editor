/*
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
 */
package io.github.rosemoe.sora.lsp.client;

import android.util.Log;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditorManager;
import io.github.rosemoe.sora.lsp.utils.URIUtils;

public class DefaultLanguageClient implements LanguageClient {

    private static String TAG = "DefaultLanguageClient";

    private final ClientContext context;

    public DefaultLanguageClient(@NotNull ClientContext context) {
        this.context = context;
    }

    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
        /* boolean response = WorkspaceEditHandler.applyEdit(params.getEdit(), "LSP edits");*/
        //FIXME: Support it?
        return CompletableFuture.supplyAsync(() -> new ApplyWorkspaceEditResponse(false));
    }


    @Override
    public CompletableFuture<List<Object>> configuration(ConfigurationParams configurationParams) {
        return LanguageClient.super.configuration(configurationParams);
    }

    @Override
    public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
        var workSpaceFolder = new WorkspaceFolder();
        workSpaceFolder.setUri(URIUtils.fileToURI(context.getProjectPath()).toString());
        // Always return the current project path
        return CompletableFuture.completedFuture(List.of(workSpaceFolder));
    }

    @Override
    public CompletableFuture<Void> registerCapability(RegistrationParams params) {
        //Not prepared to support this feature
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
        // Not prepared to support this feature
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void telemetryEvent(Object o) {
        Log.i(TAG, "telemetryEvent: " + o.toString());
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {
        LspEditorManager manager = LspEditorManager
                .getOrCreateEditorManager(getContext().getProjectPath());

        LspEditor editor = manager.getEditor(publishDiagnosticsParams.getUri());

        manager.diagnosticsContainer.addDiagnostics(
                publishDiagnosticsParams.getUri(),
                publishDiagnosticsParams.getDiagnostics());

        if (editor == null) {
            return;
        }

        editor.onDiagnosticsUpdate();
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        context.getEventListener().onShowMessage(messageParams);
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams showMessageRequestParams) {
        return CompletableFuture.completedFuture(new MessageActionItem());
    }


    @Override
    public void logMessage(MessageParams messageParams) {
        context.getEventListener().onLogMessage(messageParams);
    }

    @NotNull
    protected final ClientContext getContext() {
        return context;
    }
}