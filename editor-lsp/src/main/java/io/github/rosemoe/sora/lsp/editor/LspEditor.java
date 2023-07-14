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
import androidx.annotation.WorkerThread;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.TextDocumentSyncKind;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import io.github.rosemoe.sora.lsp.editor.event.LspEditorContentChangeEventReceiver;
import io.github.rosemoe.sora.lsp2.editor.signature.SignatureHelpWindow;
import io.github.rosemoe.sora.lsp.operations.Provider;
import io.github.rosemoe.sora.lsp.operations.completion.CompletionProvider;
import io.github.rosemoe.sora.lsp.operations.diagnostics.PublishDiagnosticsProvider;
import io.github.rosemoe.sora.lsp.operations.diagnostics.QueryDocumentDiagnosticsProvider;
import io.github.rosemoe.sora.lsp.operations.document.ApplyEditsProvider;
import io.github.rosemoe.sora.lsp.operations.document.DocumentChangeProvider;
import io.github.rosemoe.sora.lsp.operations.document.DocumentCloseProvider;
import io.github.rosemoe.sora.lsp.operations.document.DocumentOpenProvider;
import io.github.rosemoe.sora.lsp.operations.document.DocumentSaveProvider;
import io.github.rosemoe.sora.lsp.operations.format.FullFormattingProvider;
import io.github.rosemoe.sora.lsp.operations.format.RangeFormattingProvider;
import io.github.rosemoe.sora.lsp.operations.signature.SignatureHelpProvider;
import io.github.rosemoe.sora.lsp.requests.Timeout;
import io.github.rosemoe.sora.lsp.requests.Timeouts;
import io.github.rosemoe.sora.widget.CodeEditor;

@Experimental
public class LspEditor {

    private final String projectPath;

    private final LanguageServerDefinition serverDefinition;

    private final String currentFileUri;
    private final LspEditorManager lspEditorManager;

    private WeakReference<CodeEditor> currentEditor;

    private WeakReference<SignatureHelpWindow> signatureHelpWindowWeakReference;

    private Language wrapperLanguage;

    private LspLanguage currentLanguage;

    private LspProviderManager providerManager;

    private LanguageServerWrapper languageServerWrapper;

    private Boolean isClose = false;

    private Runnable unsubscribeFunction = null;

    private TextDocumentSyncKind textDocumentSyncKind;

    private List<String> completionTriggers = Collections.emptyList();

    private List<String> signatureHelpTriggers = Collections.emptyList();

    private List<String> signatureHelpRetriggers = Collections.emptyList();

    private LspEditorContentChangeEventReceiver editorContentChangeEventReceiver;


    public LspEditor(String currentProjectPath, String currentFileUri, LanguageServerDefinition serverDefinition, LspEditorManager lspEditorManager) {
        this.currentEditor = new WeakReference<>(null);
        this.providerManager = new LspProviderManager(this);
        this.currentLanguage = new LspLanguage(this);

        this.lspEditorManager = lspEditorManager;
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
        signatureHelpWindowWeakReference = new WeakReference<>(new SignatureHelpWindow(currentEditor));

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
        var editor = currentEditor.get();
        if (editor != null) {
            setEditor(editor);
        }
    }

    /**
     * Return the provider manager to manage this editor
     */
    public LspProviderManager getProviderManager() {
        return providerManager;
    }

    /**
     * @deprecated recommended to use {@link LspProviderManager#addProvider(Supplier)}
     */
    @Deprecated
    public void installFeature(Supplier<Provider<?, ?>> featureSupplier) {
        providerManager.addProvider(featureSupplier);
    }

    /**
     * @deprecated recommended to use {@link LspProviderManager#addProviders(Supplier[])}
     */
    @Deprecated
    @SafeVarargs
    public final void installFeatures(Supplier<Provider<?, ?>>... featureSupplier) {
        providerManager.addProviders(featureSupplier);
    }

    /**
     * Remove a feature for the given class
     *
     * @deprecated The internal implementation has been migrated to {@link LspProviderManager} and  recommended to use {@link LspProviderManager#removeProvider(Class)}
     */
    @Deprecated
    public void uninstallFeature(Class<?> featureClass) {
        providerManager.removeProvider(featureClass);
    }

    /**
     * Return a feature instance for the given class
     *
     * @deprecated The internal implementation has been migrated to {@link LspProviderManager} and recommended to use {@link LspProviderManager#useProvider(Class)}
     */
    @Nullable
    @Deprecated
    public <T extends Provider> T useFeature(Class<T> featureClass) {
        return providerManager.useProvider(featureClass);
    }

    /**
     * Return feature instances based on Optional wrappers.
     *
     * @deprecated The internal implementation has been migrated to {@link LspProviderManager} and recommended to use {@link LspProviderManager#safeUseProvider(Class)}
     */
    @Deprecated
    public <T extends Provider> Optional<T> safeUseFeature(Class<T> featureClass) {
        return providerManager.safeUseProvider(featureClass);
    }


    public void installFeatures() {

        //features
        providerManager.addProviders(RangeFormattingProvider::new, DocumentOpenProvider::new, DocumentSaveProvider::new, DocumentChangeProvider::new, DocumentCloseProvider::new, PublishDiagnosticsProvider::new, CompletionProvider::new, FullFormattingProvider::new, ApplyEditsProvider::new,
                QueryDocumentDiagnosticsProvider::new, SignatureHelpProvider::new);

        //options

        // formatting
        var formattingOptions = new FormattingOptions();
        formattingOptions.setTabSize(4);
        formattingOptions.setInsertSpaces(true);
        providerManager.addOption(formattingOptions);

    }


    /**
     * @deprecated recommended to use {@link LspProviderManager#getOption(Class)}
     */
    @Deprecated
    @Nullable
    public <T> T getOption(Class<T> optionClass) {
        return providerManager.getOption(optionClass);
    }


    private void setupLanguageServerWrapper() {
        var languageServerWrapper = LanguageServerWrapper.forProject(projectPath);
        languageServerWrapper = languageServerWrapper != null ? languageServerWrapper : new LanguageServerWrapper(serverDefinition, projectPath);
        languageServerWrapper.serverDefinition = serverDefinition;
        this.languageServerWrapper = languageServerWrapper;
    }

    /**
     * Connect to the language server to provide the capabilities, this will cause threads blocking. Note: An error will be thrown if the language server is not connected after some time.
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeouts
     * @see io.github.rosemoe.sora.lsp.requests.Timeout
     */
    @WorkerThread
    public void connect() throws TimeoutException {

        setupLanguageServerWrapper();

        languageServerWrapper.start();
        //wait for language server start
        var server = languageServerWrapper.getServer();
        if (server == null) {
            throw new TimeoutException("Unable to connect language server");
        }
        languageServerWrapper.connect(this);
    }

    /**
     * Try to connect to the language server repeatedly, this will cause threads blocking. Note: An error will be thrown if the language server is not connected after some time.
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeouts
     * @see io.github.rosemoe.sora.lsp.requests.Timeout
     */
    @WorkerThread
    public void connectWithTimeout() throws InterruptedException, TimeoutException {

        setupLanguageServerWrapper();

        var start = System.currentTimeMillis();
        var retryTime = Timeout.getTimeout(Timeouts.INIT);
        long maxRetryTime = start + retryTime;

        while (start < maxRetryTime) {
            try {
                connect();
                break;
            } catch (Exception exception) {
                //exception.printStackTrace();
            }
            start = System.currentTimeMillis();

            Thread.sleep(retryTime / 10);

        }

        if (start > maxRetryTime) {
            throw new TimeoutException("Unable to connect language server");
        } else {
            connect();
        }

    }


    public String getEditorContent() {
        return currentEditor.get().getText().toString();
    }


    @Nullable
    public List<Diagnostic> getDiagnostics() {
        return lspEditorManager.diagnosticsContainer.getDiagnostics(currentFileUri);
    }

    public void onDiagnosticsUpdate() {
        publishDiagnostics(getDiagnostics());
    }

    private void publishDiagnostics(List<Diagnostic> diagnostics) {
        getProviderManager().safeUseProvider(PublishDiagnosticsProvider.class)
                .ifPresent(publishDiagnosticsFeature -> publishDiagnosticsFeature.execute(diagnostics));
    }


    /**
     * Notify the language server to open the document
     */
    public void open() {
        getProviderManager().safeUseProvider(DocumentOpenProvider.class).ifPresent(documentOpenFeature -> documentOpenFeature.execute(null));
    }


    /**
     * Notify language servers to save document
     */
    public void save() {
        getProviderManager().safeUseProvider(DocumentSaveProvider.class).ifPresent(documentSaveFeature -> documentSaveFeature.execute(null));
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
     * disconnect to the language server
     */
    public void disconnect() {
        if (languageServerWrapper != null) {
            try {
                var feature = getProviderManager().useProvider(DocumentCloseProvider.class);
                if (feature != null) feature.execute(null).get();

                ForkJoinPool.commonPool().execute(() -> languageServerWrapper.disconnect(this));

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void showSignatureHelp(SignatureHelp signatureHelp) {
        var signatureHelpWindow = signatureHelpWindowWeakReference.get();
        if (signatureHelpWindow == null) {
            return;
        }
        var editor = currentEditor.get();
        if (editor == null) {
            return;
        }
        if (signatureHelp == null) {
            editor.post(signatureHelpWindow::dismiss);
            return;
        }
        editor.post(() -> signatureHelpWindow.show(signatureHelp));
    }


    private void dispose() {
        if (languageServerWrapper != null) {
            languageServerWrapper.unregister();
        }
        providerManager.dispose();

        currentEditor.clear();
        completionTriggers.clear();

        currentLanguage.destroy();

        currentLanguage = null;
        providerManager = null;
        completionTriggers = null;

        if (unsubscribeFunction != null) {
            unsubscribeFunction.run();
            unsubscribeFunction = null;
        }

        editorContentChangeEventReceiver = null;
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

    public void setSignatureHelpTriggers(List<String> signatureHelpTriggers) {
        this.signatureHelpTriggers = new ArrayList<>(signatureHelpTriggers);
    }

    public List<String> getSignatureHelpTriggers() {
        return this.signatureHelpTriggers;
    }

    public void setSignatureHelpRetriggers(List<String> signatureHelpRetriggers) {
        if (signatureHelpRetriggers.size() < 1 && signatureHelpTriggers.size() > 0 && signatureHelpTriggers.contains("(")) {
            this.signatureHelpRetriggers = List.of(")");
            return;
        }
        this.signatureHelpRetriggers = signatureHelpRetriggers;
    }

    public List<String> getSignatureHelpRetriggers() {
        return this.signatureHelpRetriggers;
    }

    public boolean hitRetrigger(CharSequence eventText) {
        for (var trigger : signatureHelpRetriggers) {
            if (trigger.contains(eventText)) {
                return true;
            }
        }
        return false;
    }

    public boolean hitTrigger(CharSequence eventText) {
        for (var trigger : signatureHelpTriggers) {
            if (trigger.contains(eventText)) {
                return true;
            }
        }
        return false;
    }

    public boolean isShowSignatureHelp() {
        var signatureHelpWindow = signatureHelpWindowWeakReference.get();
        if (signatureHelpWindow == null) {
            return false;
        }
        return signatureHelpWindow.isShowing();
    }


}
