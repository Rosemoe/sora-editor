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

package io.github.rosemoe.sora.lsp2.editor

import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lsp.editor.event.LspEditorContentChangeEventReceiver
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.lsp2.client.languageserver.requestmanager.RequestManager
import io.github.rosemoe.sora.lsp2.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp2.client.languageserver.wrapper.LanguageServerWrapper
import io.github.rosemoe.sora.lsp2.editor.signature.SignatureHelpWindow
import io.github.rosemoe.sora.lsp2.utils.FileUri
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.TextDocumentSyncKind
import java.lang.ref.WeakReference
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeoutException

class LspEditor(
    val project: LspProject,
    val uri: FileUri,
) {

    private val serverDefinition: LanguageServerDefinition

    private var _currentEditor: WeakReference<CodeEditor?> = WeakReference(null)

    private var signatureHelpWindowWeakReference: WeakReference<SignatureHelpWindow?> =
        WeakReference(null)

    private val editorContentChangeEventReceiver: LspEditorContentChangeEventReceiver? = null

    private val currentLanguage: LspLanguage? = null

    private var isClose = false

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

            //editor.setEditorLanguage(currentLanguage)
            signatureHelpWindowWeakReference = WeakReference(SignatureHelpWindow(currentEditor))
            /*
                        val subscriptionReceipt =
                            editor.subscribeEvent<ContentChangeEvent>(
                               editorContentChangeEventReceiver
                            )

                        unsubscribeFunction = Runnable { subscriptionReceipt.unsubscribe() }*/
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
            // currentLanguage.setWrapperLanguage(wrapperLanguage)
            this.editor = _currentEditor.get()
        }

    val languageServerWrapper: LanguageServerWrapper
        get() = project.getOrCreateLanguageServerWrapper(fileExt)

    var diagnostics: List<Diagnostic>
        get() = project.diagnosticsContainer.getDiagnostics(uri)
        set(value) {

        }

    val isShowSignatureHelp: Boolean
        get() = signatureHelpWindowWeakReference.get()?.isShowing ?: false

    val requestManager: RequestManager?
        get() = languageServerWrapper.requestManager

    init {
        serverDefinition = project.getServerDefinition(fileExt)
            ?: throw Exception("No server definition for extension $fileExt")
    }


    /**
     * Connect to the language server to provide the capabilities, this will cause threads blocking. Note: An error will be thrown if the language server is not connected after some time.
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeouts
     *
     * @see io.github.rosemoe.sora.lsp.requests.Timeout
     */

    @Throws(TimeoutException::class)
    suspend fun connect(throwException: Boolean = true): Boolean {
        return runCatching {
            languageServerWrapper.start()
            //wait for language server start
            languageServerWrapper.getServer()
                ?: throw TimeoutException("Unable to connect language server")
            languageServerWrapper.connect(this)
        }.onFailure {
            if (throwException) {
                throw it
            }
        }.isSuccess
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
        var start = System.currentTimeMillis()
        val retryTime = Timeout.getTimeout(Timeouts.INIT)
        val maxRetryTime = start + retryTime
        var isConnected = false
        while (start <= maxRetryTime) {
            try {
                isConnected = connect(false)
                break
            } catch (exception: Exception) {
                exception.printStackTrace();
            }
            start = System.currentTimeMillis()
            delay((retryTime / 10).toLong())
        }
        if (start > maxRetryTime && !isConnected) {
            throw TimeoutException("Unable to connect language server")
        } else {
            connect()
        }
    }


    /**
     * disconnect to the language server
     */
    suspend fun disconnect() {
        runCatching {
            /* val feature: DocumentCloseProvider =
                 getProviderManager().useProvider<DocumentCloseProvider>(
                     DocumentCloseProvider::class.java
                 )
             if (feature != null) feature.execute(null).get()*/

            languageServerWrapper.disconnect(
                this
            )

        }.onFailure {
            throw it
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


    suspend fun dispose() {
        if (isClose) {
            throw IllegalStateException("Editor is already closed")
        }
        disconnect()
        unsubscribeFunction?.run()
        _currentEditor.clear()
        isClose = true
    }
}