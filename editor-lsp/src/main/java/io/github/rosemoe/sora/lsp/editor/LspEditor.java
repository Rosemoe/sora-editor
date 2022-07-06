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
package io.github.rosemoe.sora.lsp.editor;

import androidx.annotation.BinderThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentSyncKind;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import io.github.rosemoe.sora.lsp.operations.Feature;
import io.github.rosemoe.sora.lsp.operations.format.LspFormattingFeature;
import io.github.rosemoe.sora.lsp.utils.LspUtils;
import io.github.rosemoe.sora.widget.CodeEditor;

public class LspEditor {


    private final String projectPath;

    private final LanguageServerDefinition serverDefinition;

    private WeakReference<CodeEditor> currentEditor;

    private LspLanguage currentLanguage;

    private List<Feature<?, ?>> supportedFeatures = new ArrayList<>();

    private List<Object> options = new ArrayList<>();

    private LanguageServerWrapper languageServerWrapper;

    public LspEditor(CodeEditor currentEditor, String currentProjectPath, String currentFileUri, LanguageServerDefinition serverDefinition) {
        this.currentEditor = new WeakReference<>(currentEditor);
        this.currentLanguage = new LspLanguage(currentFileUri, this);
        this.projectPath = currentProjectPath;
        setEditor(currentEditor);
        this.serverDefinition = serverDefinition;
    }

    public String getCurrentFileUri() {
        return currentLanguage.currentFileUri;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setEditor(CodeEditor currentEditor) {
        this.currentEditor = new WeakReference<>(currentEditor);
        currentEditor.setEditorLanguage(currentLanguage);
    }

    @Nullable
    public CodeEditor getEditor() {
        return currentEditor.get();
    }

    public LspLanguage getLanguage() {
        return currentLanguage;
    }

    public void installFeature(Supplier<Feature<?, ?>> featureSupplier) {
        Feature<?, ?> feature = featureSupplier.get();
        supportedFeatures.add(feature);
        feature.install(this);
    }

    public void uninstallFeature(Class<?> featureClass) {
        for (Feature<?, ?> feature : supportedFeatures) {
            if (feature.getClass() == featureClass) {
                feature.uninstall(this);
                supportedFeatures.remove(feature);
                return;
            }
        }
    }

    public <T> T useFeature(Class<T> featureClass) {
        for (Feature<?, ?> feature : supportedFeatures) {
            if (feature.getClass() == featureClass) {
                return (T) feature;
            }
        }
        return null;
    }

    public void dispose() {
        for (Feature<?, ?> feature : supportedFeatures) {
            feature.uninstall(this);
        }
        supportedFeatures.clear();
        currentEditor.clear();
        currentLanguage.destroy();
        currentLanguage = null;
    }


    public void installFeatures() {

        //features
        installFeature(LspFormattingFeature::new);

        //options

        // formatting
        FormattingOptions formattingOptions = new FormattingOptions();
        formattingOptions.setTabSize(4);
        formattingOptions.setInsertSpaces(true);
        options.add(formattingOptions);

    }

    @Nullable
    public <T> T getOption(Class<T> optionClass) {
        for (Object option : options) {
            if (optionClass.isInstance(option)) {
                return (T) option;
            }
        }
        return null;
    }

    @WorkerThread
    public void connect() {
        LanguageServerWrapper languageServerWrapper = LanguageServerWrapper.forProject(projectPath);
        if (languageServerWrapper != null) {
            return;
        }
        languageServerWrapper = new LanguageServerWrapper(serverDefinition, projectPath);

        this.languageServerWrapper = languageServerWrapper;
        languageServerWrapper.start();
        //wait for language server start
        languageServerWrapper.getServer();
        languageServerWrapper.connect(this);

    }


    private String getEditorContent() {
        return currentEditor.get().getText().toString();
    }


    public void open() {
        getRequestManagerOfOptional()
                .ifPresent(requestManager -> requestManager.didOpen(LspUtils.createDidOpenTextDocumentParams(currentLanguage.currentFileUri,
                        serverDefinition.ext, getEditorContent())));
    }

    @Nullable
    public RequestManager getRequestManager() {
        LanguageServerWrapper serverWrapper = LanguageServerWrapper.forProject(projectPath);
        return serverWrapper != null ? serverWrapper.getRequestManager() : null;
    }

    public Optional<RequestManager> getRequestManagerOfOptional() {
        return Optional.ofNullable(getRequestManager());
    }

    public void save() {
        getRequestManagerOfOptional()
                .ifPresent(requestManager ->
                        requestManager.didSave(LspUtils.createDidSaveTextDocumentParams(this.currentLanguage.currentFileUri,
                        getEditorContent())));
    }

    public void destroy() {

        getRequestManagerOfOptional()
                .ifPresent(requestManager -> requestManager.didClose(new DidCloseTextDocumentParams(
                        new TextDocumentIdentifier(currentLanguage.currentFileUri)
                )));

        languageServerWrapper.disconnect(this);

    }

    public void setSyncOptions(TextDocumentSyncKind textDocumentSyncKind) {
        currentLanguage.setSyncOptions(textDocumentSyncKind);
    }
}
