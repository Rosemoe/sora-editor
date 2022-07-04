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

import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentSyncKind;

import java.util.ArrayList;
import java.util.List;
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

    private CodeEditor currentEditor;

    private LspLanguage currentLanguage;

    private List<Feature<?, ?>> supportedFeatures = new ArrayList<>();


    private List<Object> options = new ArrayList<>();

    public LspEditor(CodeEditor currentEditor, String currentProjectPath, String currentFileUri, LanguageServerDefinition serverDefinition) {
        this.currentEditor = null;
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
        this.currentEditor = currentEditor;
        currentEditor.setEditorLanguage(currentLanguage);
    }

    public CodeEditor getEditor() {
        return currentEditor;
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
        currentEditor = null;
        currentLanguage.destroy();

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

    public <T> T getOption(Class<T> optionClass) {
        for (Object option : options) {
            if (optionClass.isInstance(option)) {
                return (T) option;
            }
        }
        return null;
    }

    public void connect() {
        LanguageServerWrapper languageServerWrapper = LanguageServerWrapper.forProject(projectPath);
        if (languageServerWrapper != null) {
            return;
        }
        languageServerWrapper = new LanguageServerWrapper(serverDefinition, projectPath);

        languageServerWrapper.start();
        //wait for language server start
        languageServerWrapper.getServer();
        languageServerWrapper.connect(this);


    }


    public void open() {
        LanguageServerWrapper languageServerWrapper = LanguageServerWrapper.forProject(projectPath);

        languageServerWrapper
                .getRequestManager()
                .didOpen(LspUtils.createDidOpenTextDocumentParams(currentLanguage.currentFileUri,
                        languageServerWrapper.serverDefinition.ext, currentEditor.getText().toString()));

    }

    public RequestManager getRequestManager() {
        return LanguageServerWrapper.forProject(projectPath).getRequestManager();
    }

    public void save() {
        getRequestManager()
                .didSave(LspUtils.createDidSaveTextDocumentParams(this.currentLanguage.currentFileUri,
                        this.currentEditor.getText().toString()));
    }

    public void destroy() {
        LanguageServerWrapper languageServerWrapper = LanguageServerWrapper.forProject(projectPath);

        languageServerWrapper.getRequestManager()
                .didClose(new DidCloseTextDocumentParams(
                        new TextDocumentIdentifier(currentLanguage.currentFileUri)
                ));


        if (languageServerWrapper != null) {
            languageServerWrapper.disconnect(this);
        }
    }

    public void setSyncOptions(TextDocumentSyncKind textDocumentSyncKind) {
        currentLanguage.setSyncOptions(textDocumentSyncKind);
    }
}
