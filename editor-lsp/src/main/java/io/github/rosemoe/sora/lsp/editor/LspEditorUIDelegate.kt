package io.github.rosemoe.sora.lsp.editor

import io.github.rosemoe.sora.graphics.inlayHint.ColorInlayHintRenderer
import io.github.rosemoe.sora.graphics.inlayHint.TextInlayHintRenderer
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer
import io.github.rosemoe.sora.lang.styling.color.EditorColor
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.rosemoe.sora.lsp.editor.codeaction.CodeActionWindow
import io.github.rosemoe.sora.lsp.editor.diagnostics.LspDiagnosticTooltipLayout
import io.github.rosemoe.sora.lsp.editor.hover.HoverWindow
import io.github.rosemoe.sora.lsp.editor.signature.SignatureHelpWindow
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.DefaultDiagnosticTooltipLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.getComponent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.lsp4j.ColorInformation
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightKind
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.InlayHint
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.lang.ref.WeakReference

internal class LspEditorUIDelegate(private val editor: LspEditor) {

    private var currentEditorRef: WeakReference<CodeEditor?> = WeakReference(null as CodeEditor?)
    private var hoverWindowRef: WeakReference<HoverWindow?> = WeakReference(null as HoverWindow?)
    private var signatureHelpWindowRef: WeakReference<SignatureHelpWindow?> = WeakReference(null as SignatureHelpWindow?)
    private var codeActionWindowRef: WeakReference<CodeActionWindow?> = WeakReference(null as CodeActionWindow?)

    private var cachedInlayHints: List<InlayHint>? = null
    private var cachedDocumentColors: List<ColorInformation>? = null

    var isEnableHover = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value) {
                currentEditorRef.get()?.let {
                    hoverWindowRef.clear()
                    hoverWindowRef = WeakReference(HoverWindow(it, editor.coroutineScope))
                }
            } else {
                hoverWindow?.setEnabled(false)
                hoverWindowRef.clear()
            }
        }

    var isEnableSignatureHelp = true
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value) {
                currentEditorRef.get()?.let {
                    signatureHelpWindowRef.clear()
                    signatureHelpWindowRef = WeakReference(SignatureHelpWindow(it, editor.coroutineScope))
                }
            } else {
                signatureHelpWindow?.setEnabled(false)
                signatureHelpWindowRef.clear()
            }
        }

    var isEnableInlayHint = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            currentEditorRef.get()?.let {
                if (value) {
                    it.registerInlayHintRenderers(
                        TextInlayHintRenderer.DefaultInstance,
                        ColorInlayHintRenderer.DefaultInstance
                    )
                } else {
                    resetInlinePresentations()
                }
            }
        }

    val isShowSignatureHelp
        get() = signatureHelpWindow?.isShowing ?: false

    val isShowHover
        get() = hoverWindow?.isShowing ?: false

    val isShowCodeActions
        get() = codeActionWindow?.isShowing ?: false

    val hoverWindow
        get() = hoverWindowRef.get()

    val signatureHelpWindow
        get() = signatureHelpWindowRef.get()

    val codeActionWindow
        get() = codeActionWindowRef.get()

    fun attachEditor(codeEditor: CodeEditor) {
        currentEditorRef.clear()
        hoverWindowRef.clear()
        signatureHelpWindowRef.clear()
        codeActionWindowRef.clear()

        currentEditorRef = WeakReference(codeEditor)

        if (isEnableSignatureHelp) {
            signatureHelpWindowRef = WeakReference(SignatureHelpWindow(codeEditor, editor.coroutineScope))
        }

        if (isEnableHover) {
            hoverWindowRef = WeakReference(HoverWindow(codeEditor, editor.coroutineScope))
        }

        if (isEnableInlayHint) {
            codeEditor.registerInlayHintRenderers(
                TextInlayHintRenderer.DefaultInstance,
                ColorInlayHintRenderer.DefaultInstance
            )
        }

        codeActionWindowRef = WeakReference(CodeActionWindow(editor, codeEditor))

        val diagnosticWindow = codeEditor.getComponent<EditorDiagnosticTooltipWindow>()
        if (diagnosticWindow.layout is DefaultDiagnosticTooltipLayout) {
            diagnosticWindow.layout = LspDiagnosticTooltipLayout()
        }
    }

    fun detachEditor() {
        hoverWindow?.setEnabled(false)
        hoverWindowRef.clear()

        signatureHelpWindow?.setEnabled(false)
        signatureHelpWindowRef.clear()

        codeActionWindow?.setEnabled(false)
        codeActionWindow?.dismiss()
        codeActionWindowRef.clear()

        resetInlinePresentations()
        currentEditorRef.clear()
    }

    fun showSignatureHelp(signatureHelp: SignatureHelp?) {
        val window = signatureHelpWindow ?: return
        val editorInstance = currentEditorRef.get() ?: return

        if (signatureHelp == null) {
            editorInstance.post { window.dismiss() }
            return
        }

        editorInstance.post { window.show(signatureHelp) }
    }

    fun showHover(hover: Hover?) {
        val window = hoverWindow ?: return
        val editorInstance = currentEditorRef.get() ?: return
        val isInSignatureHelp = isShowSignatureHelp

        if (hover == null || isInSignatureHelp) {
            editorInstance.post { window.dismiss() }
            return
        }

        editorInstance.post { window.show(hover) }
    }

    fun showCodeActions(range: Range?, actions: List<Either<Command, CodeAction>>?) {
        val window = codeActionWindow ?: return
        val editorInstance = currentEditorRef.get() ?: return

        val isInCompletion = editorInstance.getComponent<EditorAutoCompletion>().isShowing
        val isInSignatureHelp = isShowSignatureHelp

        if (range == null || actions.isNullOrEmpty() || isInCompletion || isInSignatureHelp) {
            editorInstance.post { window.dismiss() }
            return
        }

        editorInstance.post { window.show(range, actions) }
    }

    fun showDocumentHighlight(highlights: List<DocumentHighlight>?) {
        val editorInstance = currentEditorRef.get() ?: return

        if (highlights.isNullOrEmpty()) {
            editorInstance.highlightTexts = null
            return
        }

        val container = HighlightTextContainer()

        val colors = mapOf(
            DocumentHighlightKind.Write to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_STRONG_BACKGROUND),
            DocumentHighlightKind.Read to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND),
            DocumentHighlightKind.Text to EditorColor(EditorColorScheme.TEXT_HIGHLIGHT_BACKGROUND)
        )

        highlights.forEach {
            container.add(
                HighlightTextContainer.HighlightText(
                    it.range.start.line,
                    it.range.start.character,
                    it.range.end.line,
                    it.range.end.character,
                    colors.getValue(it.kind ?: DocumentHighlightKind.Text)
                )
            )
        }

        editorInstance.highlightTexts = container
    }

    fun showInlayHints(inlayHints: List<InlayHint>?) {
        val normalized = inlayHints.normalizeList()
        if (cachedInlayHints == normalized) {
            return
        }
        cachedInlayHints = normalized
        updateInlinePresentations()
    }

    fun showDocumentColors(documentColors: List<ColorInformation>?) {
        val normalized = documentColors.normalizeList()
        if (cachedDocumentColors == normalized) {
            return
        }
        cachedDocumentColors = normalized
        updateInlinePresentations()
    }

    private fun updateInlinePresentations() {
        val editorInstance = currentEditorRef.get() ?: return

        val hasInlayHints = !cachedInlayHints.isNullOrEmpty()
        val hasDocumentColors = !cachedDocumentColors.isNullOrEmpty()

        if (!hasInlayHints && !hasDocumentColors) {
            editorInstance.inlayHints = null
            return
        }

        val container = InlayHintsContainer()
        cachedInlayHints?.inlayHintToDisplay()?.forEach(container::add)
        cachedDocumentColors?.colorInfoToDisplay()?.forEach(container::add)

        editorInstance.inlayHints = container
    }

    private fun resetInlinePresentations() {
        cachedInlayHints = null
        cachedDocumentColors = null
        currentEditorRef.get()?.let {
            if (it.inlayHints != null) {
                it.inlayHints = null
            }
        }
    }
}
