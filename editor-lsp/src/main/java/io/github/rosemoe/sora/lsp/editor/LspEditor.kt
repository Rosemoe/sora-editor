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

package io.github.rosemoe.sora.lsp.editor

import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.HoverEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import io.github.rosemoe.sora.lsp.editor.event.LspEditorContentChangeEvent
import io.github.rosemoe.sora.lsp.editor.event.LspEditorHoverEvent
import io.github.rosemoe.sora.lsp.editor.event.LspEditorSelectionChangeEvent
import io.github.rosemoe.sora.lsp.editor.hover.HoverWindow
import io.github.rosemoe.sora.lsp.editor.signature.SignatureHelpWindow
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.diagnostics.publishDiagnostics
import io.github.rosemoe.sora.lsp.events.document.documentClose
import io.github.rosemoe.sora.lsp.events.document.documentOpen
import io.github.rosemoe.sora.lsp.events.document.documentSave
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp.utils.FileUri
import io.github.rosemoe.sora.lsp.utils.clearVersions
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.subscribeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.TextDocumentSyncKind
import java.lang.ref.WeakReference
import java.util.concurrent.TimeoutException

class LspEditor(
    val project: LspProject,
    val uri: FileUri,
) {

    private val serverDefinition: LanguageServerDefinition

    private var _currentEditor: WeakReference<CodeEditor?> = WeakReference(null)

    private var signatureHelpWindowWeakReference: WeakReference<SignatureHelpWindow?> =
        WeakReference(null)

    private var hoverWindowWeakReference: WeakReference<HoverWindow?> =
        WeakReference(null)

    private var currentLanguage: LspLanguage? = null

    private var isClosed = false

    private var unsubscribeFunction: Runnable? = null

    val eventManager = LspEventManager(project, this)

    val fileExt = uri.path.substringAfterLast('.')

    var textDocumentSyncKind = TextDocumentSyncKind.Full

    var completionTriggers = emptyList<String>()

    var signatureHelpTriggers = emptyList<String>()

    var signatureHelpReTriggers = emptyList<String>()

    val coroutineScope = project.coroutineScope

    var editor: CodeEditor?
        set(currentEditor) {
            if (currentEditor == null) {
                throw IllegalArgumentException("Editor cannot be null")
            }

            _currentEditor = WeakReference(currentEditor)

            unsubscribeFunction?.run()

            currentEditor.setEditorLanguage(currentLanguage)
            signatureHelpWindowWeakReference = WeakReference(SignatureHelpWindow(currentEditor))
            hoverWindowWeakReference = WeakReference(HoverWindow(currentEditor))

            val editorContentChangeEventReceiver = LspEditorContentChangeEvent(this)
            val editorSelectionChangeEventReceiver = LspEditorSelectionChangeEvent(this)
            val editorHoverEvent = LspEditorHoverEvent(this)

            val subscriptionReceipts =
                mutableListOf(
                    currentEditor.subscribeEvent<ContentChangeEvent>(
                        editorContentChangeEventReceiver
                    ),
                    currentEditor.subscribeEvent<SelectionChangeEvent>(
                        editorSelectionChangeEventReceiver
                    ),
                    currentEditor.subscribeEvent<HoverEvent>(
                        editorHoverEvent
                    )
                )

            unsubscribeFunction =
                Runnable {
                    subscriptionReceipts.forEach {
                        it.unsubscribe()
                    }
                    subscriptionReceipts.clear()
                }
        }
        get() {
            return _currentEditor.get()
        }

    var editorContent: String
        get() = editor?.text?.toString() ?: ""
        set(content) {
            editor?.setText(content)
        }

    var wrapperLanguage: Language? = null
        set(language) {
            field = language
            currentLanguage?.wrapperLanguage = wrapperLanguage
            val editor = _currentEditor.get()
            if (editor != null) {
                this.editor = editor
            }
        }

    var isConnected: Boolean = false
        private set

    val languageServerWrapper: LanguageServerWrapper
        get() = project.getOrCreateLanguageServerWrapper(fileExt)

    var diagnostics: List<Diagnostic>
        get() = project.diagnosticsContainer.getDiagnostics(uri)
        set(value) {
            publishDiagnostics(value)
        }

    val isShowSignatureHelp: Boolean
        get() = signatureHelpWindowWeakReference.get()?.isShowing ?: false

    val signatureHelpWindow: SignatureHelpWindow?
        get() = signatureHelpWindowWeakReference.get()

    val isShowHover: Boolean
        get() = hoverWindowWeakReference.get()?.isShowing ?: false

    val hoverWindow: HoverWindow?
        get() = hoverWindowWeakReference.get()

    var isEnableHover: Boolean = true
        get() = hoverWindow?.isEnabled() ?: false
        set(value) {
            field = value
            if (value) {
                editor?.let { hoverWindowWeakReference = WeakReference(HoverWindow(it)) }
            } else {
                hoverWindow?.setEnabled(false)
                hoverWindowWeakReference.clear()
            }
        }

    var isEnableSignatureHelp: Boolean = true
        get() = signatureHelpWindow?.isEnabled() ?: false
        set(value) {
            field = value
            if (value) {
                editor?.let {
                    signatureHelpWindowWeakReference = WeakReference(SignatureHelpWindow(it))
                }
            } else {
                signatureHelpWindow?.setEnabled(false)
                signatureHelpWindowWeakReference.clear()
            }
        }

    val requestManager: RequestManager?
        get() = languageServerWrapper.requestManager

    init {
        serverDefinition = project.getServerDefinition(fileExt)
            ?: throw Exception("No server definition for extension $fileExt")

        currentLanguage = LspLanguage(this)
    }


    /**
     * Connect to the language server to provide the capabilities, this will cause threads blocking. Note: An error will be thrown if the language server is not connected after some time.
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeouts
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeout
     */

    @Throws(TimeoutException::class)
    suspend fun connect(throwException: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        eventManager.init()
        runCatching {
            languageServerWrapper.start()

            //wait for language server start
            languageServerWrapper.getServerCapabilities()
                ?: throw TimeoutException("Unable to connect language server")

            languageServerWrapper.connect(this@LspEditor)

            isConnected = true
        }.onFailure {
            if (throwException) {
                throw it
            }
            isConnected = false
        }.isSuccess
    }

    @WorkerThread
    fun connectBlocking(throwException: Boolean = true): Boolean = runBlocking {
        connect(throwException)
    }

    /**
     * Try to connect to the language server repeatedly, this will cause threads blocking. Note: An error will be thrown if the language server is not connected after some time.
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeouts
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeout
     */
    @Throws(InterruptedException::class, TimeoutException::class)
    suspend fun connectWithTimeout() {
        var isConnected = false

        var start = System.currentTimeMillis()
        val retryTime = Timeout[Timeouts.INIT]
        val maxRetryTime: Long = start + retryTime

        while (start < maxRetryTime) {
            try {
                connect()
                isConnected = true
                break
            } catch (exception: java.lang.Exception) {
                exception.printStackTrace();
            }
            start = System.currentTimeMillis()
            delay((retryTime / 200).toLong())
        }

        if (!isConnected && start > maxRetryTime) {
            throw TimeoutException("Unable to connect language server")
        } else if (!isConnected) {
            connect()
        }

    }

    @WorkerThread
    fun connectWithTimeoutBlocking() = runBlocking {
        connectWithTimeout()
    }

    /**
     * disconnect to the language server
     */
    @WorkerThread
    fun disconnect() {
        runCatching {
            coroutineScope.future {
                eventManager.emitAsync(EventType.documentClose)
            }.get()

            languageServerWrapper.disconnect(
                this@LspEditor
            )

            isConnected = false
        }.onFailure {
            isConnected = false
            throw it
        }
    }

    /**
     * Notify the language server to open the document
     */
    suspend fun openDocument() {
        eventManager.emitAsync(EventType.documentOpen)
    }

    @WorkerThread
    fun openDocumentBlocking() = runBlocking {
        openDocument()
    }


    /**
     * Notify language servers the document is saved
     */
    suspend fun saveDocument() {
        eventManager.emitAsync(EventType.documentSave)
    }

    @WorkerThread
    fun saveDocumentBlocking() = runBlocking {
        saveDocument()
    }

    fun onDiagnosticsUpdate() {
        publishDiagnostics(diagnostics)
    }

    private fun publishDiagnostics(diagnostics: List<Diagnostic>) {
        eventManager.emit(EventType.publishDiagnostics) {
            put("data", diagnostics)
        }
    }

    fun showSignatureHelp(signatureHelp: SignatureHelp?) {
        val signatureHelpWindow = signatureHelpWindowWeakReference.get() ?: return

        if (signatureHelp == null) {
            editor?.post { signatureHelpWindow.dismiss() }
            return
        }
        editor?.post { signatureHelpWindow.show(signatureHelp) }
    }

    fun showHover(hover: Hover?) {
        val hoverWindow = hoverWindowWeakReference.get() ?: return

        val isInSignatureHelp = isShowSignatureHelp

        if (hover == null || isInSignatureHelp) {
            editor?.post { hoverWindow.dismiss() }
            return
        }

        editor?.post { hoverWindow.show(hover) }
    }

    fun hitReTrigger(eventText: CharSequence): Boolean {
        for (trigger in signatureHelpReTriggers) {
            if (trigger.contains(eventText)) {
                return true
            }
        }
        return false
    }

    fun hitTrigger(eventText: CharSequence): Boolean {
        for (trigger in signatureHelpTriggers) {
            if (trigger.contains(eventText)) {
                return true
            }
        }
        return false
    }


    @WorkerThread
    fun dispose() {
        if (isClosed) {
            return
            // throw IllegalStateException("Editor is already closed")
        }
        disconnect()
        unsubscribeFunction?.run()
        _currentEditor.clear()
        signatureHelpWindowWeakReference.clear()
        hoverWindowWeakReference.clear()
        clearVersions {
            it == this.uri
        }
        project.removeEditor(this)
        isClosed = true
    }

    suspend fun disposeAsync() = withContext(Dispatchers.IO) {
        dispose()
    }
}
