/*
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
 */
package io.github.rosemoe.sora.lsp.editor;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.diagnostics.DiagnosticsContainer;
import io.github.rosemoe.sora.lsp.utils.LspUtils;

/**
 * Manager the LspEditor
 */
public class LspEditorManager {

    private final String currentProjectPath;

    private LspEditorManager(String currentProjectPath) {
        this.currentProjectPath = currentProjectPath;
    }

    private final Map<String, LspEditor> editors = new HashMap<>();

    public final DiagnosticsContainer diagnosticsContainer = new DiagnosticsContainer();

    private static final Map<String, LspEditorManager> managers = new HashMap<>();

    public static LspEditorManager getOrCreateEditorManager(String projectPath) {
        var manager = managers.get(projectPath);
        if (manager == null) {
            manager = new LspEditorManager(projectPath);
            managers.put(projectPath, manager);
        }
        return manager;
    }


    public LspEditor getEditor(String currentFileUri) {
        return editors.get(currentFileUri);
    }

    @Nullable
    public LspEditor removeEditor(String currentFileUri) {
        if (editors.get(currentFileUri) == null) {
            return null;
        }
        getEditor(currentFileUri).close();
        return editors.remove(currentFileUri);
    }


    public LspEditor createEditor(String currentFileUri, LanguageServerDefinition serverDefinition) {
        LspEditor editor = editors.get(currentFileUri);
        if (editor == null) {
            editor = new LspEditor(currentProjectPath, currentFileUri, serverDefinition, this);
            editors.put(currentFileUri, editor);
        }
        return editor;
    }

    /**
     * Close all editors under this manager, for scenarios where the project is closed
     */
    public void closeAllEditor() {
        editors.forEach((key, editor) -> {
            editor.close();
        });
        editors.clear();

    }

    /**
     * Close all managers, and close all editors under the manager, for scenarios where the software is closed
     */
    public static void closeAllManager() {
        managers.values().forEach(LspEditorManager::closeAllEditor);
        managers.clear();

        // Maybe the user should be allowed to call the method themselves
        LspUtils.clearVersions();

    }

    public void close() {
        closeAllEditor();
        managers.remove(currentProjectPath);
        diagnosticsContainer.clear();
    }

}
