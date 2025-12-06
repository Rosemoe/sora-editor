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
import io.github.rosemoe.sora.annotations.Experimental
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.HoverEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer
import io.github.rosemoe.sora.lang.styling.color.EditorColor
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import io.github.rosemoe.sora.lsp.editor.codeaction.CodeActionWindow
import io.github.rosemoe.sora.lsp.editor.diagnostics.LspDiagnosticTooltipLayout
import io.github.rosemoe.sora.lsp.editor.event.LspEditorContentChangeEvent
import io.github.rosemoe.sora.lsp.editor.event.LspEditorHoverEvent
import io.github.rosemoe.sora.lsp.editor.event.LspEditorScrollEvent
import io.github.rosemoe.sora.lsp.editor.event.LspEditorSelectionChangeEvent
import io.github.rosemoe.sora.lsp.editor.format.LspFormatter
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
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.DefaultDiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.subscribeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.ColorInformation
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightKind
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.messages.Either
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
    private var codeActionWindowWeakReference: WeakReference<CodeActionWindow?> =
        WeakReference(null)
    private var currentLanguage: LspLanguage? = null

    private var subscriptionReceipts: MutableList<SubscriptionReceipt<out Event>> = mutableListOf()

    @Volatile
    private var isClosed = false

    private var cachedInlayHints: List<org.eclipse.lsp4j.InlayHint>? = null

    private var cachedDocumentColors: List<ColorInformation>? = null

    private val disposeLock = Any()

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

            clearSubscriptions()

            currentEditor.setEditorLanguage(currentLanguage)

            if (isEnableSignatureHelp) {
                signatureHelpWindowWeakReference =
                    WeakReference(SignatureHelpWindow(currentEditor, coroutineScope))
            }
            if (isEnableHover) {
                hoverWindowWeakReference = WeakReference(HoverWindow(currentEditor, coroutineScope))
            }
            if (isEnableInlayHint) {
                currentEditor.registerInlayHintRenderer(TextInlayHintRenderer.DefaultInstance)
            }

            codeActionWindowWeakReference = WeakReference(CodeActionWindow(this, currentEditor))

            val currentDiagnosticTooltipWindow =
                currentEditor.getComponent<EditorDiagnosticTooltipWindow>()

            if (currentDiagnosticTooltipWindow.layout is DefaultDiagnosticTooltipLayout) {
                currentDiagnosticTooltipWindow.layout = LspDiagnosticTooltipLayout()
            }
            clearSubscriptions()
            subscriptionReceipts =
                mutableListOf(
                    currentEditor.subscribeEvent<ContentChangeEvent>(
                        LspEditorContentChangeEvent(this)
                    ),
                    currentEditor.subscribeEvent<SelectionChangeEvent>(
                        LspEditorSelectionChangeEvent(this)
                    ),
                    currentEditor.subscribeEvent<HoverEvent>(
                        LspEditorHoverEvent(this)
                    ),
                    currentEditor.subscribeEvent<ScrollEvent>(
                        LspEditorScrollEvent(this)
                    )
                )
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

    var isConnected = false
        private set

    val languageServerWrapper: LanguageServerWrapper
        get() = project.getOrCreateLanguageServerWrapper(fileExt)

    var diagnostics
        get() = project.diagnosticsContainer.getDiagnostics(uri)
        set(value) {
            publishDiagnostics(value)
        }

    val diagnosticsContainer
        get() = project.diagnosticsContainer

    val isShowSignatureHelp
        get() = signatureHelpWindowWeakReference.get()?.isShowing ?: false

    val isShowHover
        get() = hoverWindowWeakReference.get()?.isShowing ?: false

    val isShowCodeActions
        get() = codeActionWindowWeakReference.get()?.isShowing ?: false

    var isEnableHover = true
        set(value) {
            field = value
            if (value) {
                editor?.let {
                    hoverWindowWeakReference = WeakReference(HoverWindow(it, coroutineScope))
                }
            } else {
                hoverWindow?.setEnabled(false)
                hoverWindowWeakReference.clear()
            }
        }

    var isEnableSignatureHelp = true
        set(value) {
            field = value
            if (value) {
                editor?.let {
                    signatureHelpWindowWeakReference = WeakReference(
                        SignatureHelpWindow(
                            it,
                            coroutineScope
                        )
                    )
                }
            } else {
                signatureHelpWindow?.setEnabled(false)
                signatureHelpWindowWeakReference.clear()
            }
        }

    @Experimental
    var isEnableInlayHint = false
        set(value) {
            field = value
            val editorInstance = editor ?: return
            if (value) {
                editorInstance.registerInlayHintRenderers(
                    TextInlayHintRenderer.DefaultInstance,
                    ColorInlayHintRenderer.DefaultInstance
                )
                coroutineScope.launch {
                    this@LspEditor.requestInlayHint(CharPosition(0, 0))
                    this@LspEditor.requestDocumentColor()
                }
            }
        }

    val hoverWindow
        get() = hoverWindowWeakReference.get()

    val codeActionWindow
        get() = codeActionWindowWeakReference.get()

    val signatureHelpWindow
        get() = signatureHelpWindowWeakReference.get()

    val requestManager
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
            val capabilities = languageServerWrapper.getServerCapabilities()
                ?: throw TimeoutException("Unable to connect language server")

            languageServerWrapper.connect(this@LspEditor)

            currentLanguage?.let { language ->
                if (capabilities.documentFormattingProvider?.left != false || capabilities.documentFormattingProvider?.right != null) {
                    language.formatter = LspFormatter(language)
                }
            }

            if (capabilities.inlayHintProvider?.left != false || capabilities.inlayHintProvider?.right != null) {
                requestInlayHint(CharPosition(0, 0))
            }

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
    @Throws(RuntimeException::class)
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

            languageServerWrapper.disconnect(
                this@LspEditor
            )

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

    fun showCodeActions(range: Range?, actions: List<Either<Command, CodeAction>>?) {
        val window = codeActionWindowWeakReference.get() ?: return
        val originEditor = editor ?: return

        val isInCompletion = originEditor.getComponent<EditorAutoCompletion>().isShowing
        val isInSignatureHelp = isShowSignatureHelp

        if (range == null || actions.isNullOrEmpty() || isInCompletion || isInSignatureHelp) {
            originEditor.post { window.dismiss() }
            return
        }

        originEditor.post { window.show(range, actions) }
    }

    fun showDocumentHighlight(highlights: List<DocumentHighlight>?) {
        val editor = editor ?: return

        if (highlights == null || highlights.isEmpty()) {
            editor.highlightTexts = null
            return
        }

        val container = HighlightTextContainer()

        val colors = mapOf(
            DocumentHighlightKind.Write to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BACKGROUND),
            DocumentHighlightKind.Read to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND),
            DocumentHighlightKind.Text to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND)
        )

        val borderColors = mapOf(
            DocumentHighlightKind.Write to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BORDER),
            DocumentHighlightKind.Read to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BORDER),
            DocumentHighlightKind.Text to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BORDER)
        )

        highlights.forEach {
            container.add(
                HighlightTextContainer.HighlightText(
                    it.range.start.line,
                    it.range.start.character,
                    it.range.end.line,
                    it.range.end.character,
                    colors.getValue(it.kind ?: DocumentHighlightKind.Text),
                    borderColors.getValue(it.kind ?: DocumentHighlightKind.Text)
                )
            )
        }

        editor.highlightTexts = container
    }

    internal fun showInlayHints(inlayHints: List<org.eclipse.lsp4j.InlayHint>?) {
        val normalized = inlayHints.normalize()
        if (cachedInlayHints == normalized) {
            return
        }
        cachedInlayHints = normalized
        updateInlinePresentations()
    }

    internal fun showDocumentColors(documentColors: List<ColorInformation>?) {
        val normalized = documentColors.normalize()
        if (cachedDocumentColors == normalized) {
            return
        }
        cachedDocumentColors = normalized
        updateInlinePresentations()
    }

    private fun <T> List<T>?.normalize(): List<T>? {
        return if (this.isNullOrEmpty()) null else this
    }

    private fun updateInlinePresentations() {
        val editorInstance = editor ?: return

        val hasInlayHints = !cachedInlayHints.isNullOrEmpty()
        val hasDocumentColors = !cachedDocumentColors.isNullOrEmpty()

        if (!hasInlayHints && !hasDocumentColors) {
            if (editorInstance.inlayHints != null) {
                editorInstance.inlayHints = null
            }
            return
        }

        val inlayHintsContainer = InlayHintsContainer()
        cachedInlayHints?.inlayHintToDisplay()?.forEach(inlayHintsContainer::add)
        cachedDocumentColors?.colorInfoToDisplay()?.forEach(inlayHintsContainer::add)

        editorInstance.inlayHints = inlayHintsContainer
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

    private fun clearSubscriptions() {
        val iterator = subscriptionReceipts.iterator()

        while (iterator.hasNext()) {
            iterator.next().unsubscribe()
            iterator.remove()
        }
    }

    @WorkerThread
    fun dispose() {
        clearSubscriptions()
        synchronized(disposeLock) {
            if (isClosed) {
                return
                // throw IllegalStateException("Editor is already closed")
            }
            disconnect()
            _currentEditor.clear()
            signatureHelpWindowWeakReference.clear()
            hoverWindowWeakReference.clear()
            codeActionWindowWeakReference.clear()
            clearVersions {
                it == this.uri
            }
            project.removeEditor(this)
            isClosed = true
        }
    }

    suspend fun disposeAsync() = withContext(Dispatchers.IO) {
        dispose()
    }
}
