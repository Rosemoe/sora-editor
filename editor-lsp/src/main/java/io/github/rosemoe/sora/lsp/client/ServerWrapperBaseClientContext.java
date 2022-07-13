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
package io.github.rosemoe.sora.lsp.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspEditorManager;

public class ServerWrapperBaseClientContext implements ClientContext {

    private final LanguageServerWrapper wrapper;

    public ServerWrapperBaseClientContext(@NotNull LanguageServerWrapper wrapper) {
        this.wrapper = wrapper;
    }


    @Nullable
    @Override
    public RequestManager getRequestManager() {
        return wrapper.getRequestManager();
    }

    @Override
    public EventHandler.EventListener getEventListener() {
        return wrapper.getServerDefinition().getEventListener();
    }

    @Nullable
    @Override
    public String getProjectPath() {
        return wrapper.getProjectRootPath();
    }

    @Nullable
    @Override
    public LspEditor getEditorEventManagerFor(@NonNull String documentUri) {
        return LspEditorManager.getOrCreateEditorManager(getProjectPath()).getEditor(documentUri);
    }
}