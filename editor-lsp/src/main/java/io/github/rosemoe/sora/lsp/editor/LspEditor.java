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

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentSyncKind;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.event.SubscriptionReceipt;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import io.github.rosemoe.sora.lsp.editor.event.LspEditorContentChangeEventReceiver;
import io.github.rosemoe.sora.lsp.operations.Feature;
import io.github.rosemoe.sora.lsp.operations.completion.CompletionFeature;
import io.github.rosemoe.sora.lsp.operations.diagnostics.PublishDiagnosticsFeature;
import io.github.rosemoe.sora.lsp.operations.document.ApplyEditsFeature;
import io.github.rosemoe.sora.lsp.operations.document.DocumentChangeFeature;
import io.github.rosemoe.sora.lsp.operations.document.DocumentCloseFeature;
import io.github.rosemoe.sora.lsp.operations.document.DocumentOpenFeature;
import io.github.rosemoe.sora.lsp.operations.document.DocumentSaveFeature;
import io.github.rosemoe.sora.lsp.operations.format.FullFormattingFeature;
import io.github.rosemoe.sora.lsp.operations.format.RangeFormattingFeature;
import io.github.rosemoe.sora.widget.CodeEditor;

@Experimental
public class LspEditor {

    private final String projectPath;

    private final LanguageServerDefinition serverDefinition;

    private final String currentFileUri;

    private WeakReference<CodeEditor> currentEditor;

    private Language wrapperLanguage;

    private LspLanguage currentLanguage;

    private List<Feature<?, ?>> supportedFeatures = new ArrayList<>();

    private List<Object> options = new ArrayList<>();

    private LanguageServerWrapper languageServerWrapper;

    private Boolean isClose = false;

    private Runnable unsubscribeFunction = null;

    private TextDocumentSyncKind textDocumentSyncKind;

    private List<String> completionTriggers = Collections.emptyList();

    private LspEditorContentChangeEventReceiver editorContentChangeEventReceiver;

    private PublishDiagnosticsParams diagnosticsParams = null;


    public LspEditor(String currentProjectPath, String currentFileUri, LanguageServerDefinition serverDefinition) {
        this.currentEditor = new WeakReference<>(null);
        this.currentLanguage = new LspLanguage(this);
        this.currentFileUri = currentFileUri;
        this.projectPath = currentProjectPath;

        this.serverDefinition = serverDefinition;

        this.editorContentChangeEventReceiver = new LspEditorContentChangeEventReceiver(this);
    }

    public String getCurrentFileUri() {
        return currentFileUri;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setEditor(CodeEditor currentEditor) {
        this.currentEditor = new WeakReference<>(currentEditor);

        if (unsubscribeFunction != null) {
            unsubscribeFunction.run();
        }

        currentEditor.setEditorLanguage(currentLanguage);

        var subscriptionReceipt = currentEditor.subscribeEvent(ContentChangeEvent.class, editorContentChangeEventReceiver);

        unsubscribeFunction = subscriptionReceipt::unsubscribe;

    }

    @Nullable
    public CodeEditor getEditor() {
        return currentEditor.get();
    }

    public LspLanguage getLanguage() {
        return currentLanguage;
    }

    @Nullable
    public <T extends Language> T getWrapperLanguage() {
        return (T) wrapperLanguage;
    }

    /**
     * Set the wrapper language, as the language server may not provide all the features, such as highlighting, you can implement the features yourself and integrate with the language server
     */
    public void setWrapperLanguage(Language wrapperLanguage) {
        this.wrapperLanguage = wrapperLanguage;
        currentLanguage.setWrapperLanguage(wrapperLanguage);
        if (currentEditor.get() != null) {
            setEditor(currentEditor.get());
        }
    }

    public void installFeature(Supplier<Feature<?, ?>> featureSupplier) {
        var feature = featureSupplier.get();
        supportedFeatures.add(feature);
        feature.install(this);
    }

    @SafeVarargs
    public final void installFeatures(Supplier<Feature<?, ?>>... featureSupplier) {
        Arrays.stream(featureSupplier).sequential().forEach(this::installFeature);
    }

    public void uninstallFeature(Class<?> featureClass) {
        for (var feature : supportedFeatures) {
            if (feature.getClass() == featureClass) {
                feature.uninstall(this);
                supportedFeatures.remove(feature);
                return;
            }
        }
    }

    @Nullable
    public <T extends Feature> T useFeature(Class<T> featureClass) {
        for (var feature : supportedFeatures) {
            if (feature.getClass() == featureClass) {
                return (T) feature;
            }
        }
        return null;
    }

    public <T extends Feature> Optional<T> safeUseFeature(Class<T> featureClass) {
        return Optional.ofNullable(useFeature(featureClass));
    }

    private void dispose() {

        for (var feature : supportedFeatures) {
            feature.uninstall(this);
        }

        supportedFeatures.clear();
        options.clear();
        currentEditor.clear();
        completionTriggers.clear();

        currentLanguage.destroy();

        currentLanguage = null;
        supportedFeatures = null;
        options = null;
        completionTriggers = null;

        if (unsubscribeFunction != null) {
            unsubscribeFunction.run();
            unsubscribeFunction = null;
        }

        editorContentChangeEventReceiver = null;

    }


    public void installFeatures() {

        //features
        installFeatures(RangeFormattingFeature::new, DocumentOpenFeature::new, DocumentSaveFeature::new,
                DocumentChangeFeature::new, DocumentCloseFeature::new, PublishDiagnosticsFeature::new,
                CompletionFeature::new, FullFormattingFeature::new, ApplyEditsFeature::new);

        //options

        // formatting
        var formattingOptions = new FormattingOptions();
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

    /**
     * Connect to the language server to provide the capabilities, this will cause threads blocking. Note: An error will be thrown if the language server is not connected after some time.
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeouts
     * @see io.github.rosemoe.sora.lsp.requests.Timeout
     */
    @WorkerThread
    public void connect() {
        var languageServerWrapper = LanguageServerWrapper.forProject(projectPath);
        languageServerWrapper = languageServerWrapper != null ? languageServerWrapper : new LanguageServerWrapper(serverDefinition, projectPath);
        languageServerWrapper.serverDefinition = serverDefinition;
        this.languageServerWrapper = languageServerWrapper;
        languageServerWrapper.start();
        //wait for language server start
        languageServerWrapper.getServerCapabilities();
        languageServerWrapper.connect(this);
    }


    public String getEditorContent() {
        return currentEditor.get().getText().toString();
    }


    @Nullable
    public PublishDiagnosticsParams getDiagnostics() {
        return diagnosticsParams;
    }

    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {
        this.diagnosticsParams = publishDiagnosticsParams;
        safeUseFeature(PublishDiagnosticsFeature.class).ifPresent(publishDiagnosticsFeature -> publishDiagnosticsFeature.execute(publishDiagnosticsParams));
    }


    /**
     * Notify the language server to open the document
     */
    public void open() {
        safeUseFeature(DocumentOpenFeature.class).ifPresent(documentOpenFeature -> documentOpenFeature.execute(null));
    }

    /**
     * Get a request manager that can manipulate the language server
     */
    @Nullable
    public RequestManager getRequestManager() {
        LanguageServerWrapper serverWrapper = LanguageServerWrapper.forProject(projectPath);
        return serverWrapper != null ? serverWrapper.getRequestManager() : null;
    }

    public Optional<RequestManager> getRequestManagerOfOptional() {
        return Optional.ofNullable(getRequestManager());
    }

    /**
     * Notify language servers to save document
     */
    public void save() {
        safeUseFeature(DocumentSaveFeature.class).ifPresent(documentSaveFeature -> documentSaveFeature.execute(null));
    }

    /**
     * disconnect to the language server
     */
    public void disconnect() {
        if (languageServerWrapper != null) {
            try {
                var feature = useFeature(DocumentCloseFeature.class);
                if (feature != null) feature.execute(null).get();

                ForkJoinPool.commonPool().execute(() -> languageServerWrapper.disconnect(this));

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close the editor, it will dispose the editor's resources and disconnect to the language server
     */
    public void close() {

        if (isClose) {
            return;
        }

        isClose = true;

        disconnect();
        dispose();
    }


    public TextDocumentSyncKind getSyncOptions() {
        return textDocumentSyncKind == null ? TextDocumentSyncKind.Full : textDocumentSyncKind;
    }

    public void setSyncOptions(TextDocumentSyncKind textDocumentSyncKind) {
        this.textDocumentSyncKind = textDocumentSyncKind;
    }

    public void setCompletionTriggers(List<String> completionTriggers) {
        this.completionTriggers = new ArrayList<>(completionTriggers);
    }


    public String getFileExt() {
        return serverDefinition.ext;
    }

    public List<String> getCompletionTriggers() {
        return this.completionTriggers;
    }
}
