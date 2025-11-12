package io.github.rosemoe.sora.lsp.editor.codeaction

import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent
import io.github.rosemoe.sora.event.EditorFocusChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.ScrollEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.TextSizeChangeEvent
import io.github.rosemoe.sora.event.subscribeEvent
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceApplyEdit
import io.github.rosemoe.sora.lsp.events.workspace.workSpaceExecuteCommand
import io.github.rosemoe.sora.lsp.utils.asCharPosition
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.base.EditorPopupWindow
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either

class CodeActionWindow(
    private val lspEditor: LspEditor,
    editor: CodeEditor,
) : EditorPopupWindow(
    editor,
    FEATURE_HIDE_WHEN_FAST_SCROLL or FEATURE_SCROLL_AS_CONTENT
) {

    private lateinit var rootView: View
    private val buffer = FloatArray(2)
    private val maxWidth = (editor.width * 0.72f).toInt()
    private val maxHeight = (editor.dpUnit * 260).toInt()
    private val eventManager = editor.createSubEventManager()

    private var layoutImpl: CodeActionLayout = DefaultCodeActionLayout()
    private var anchorPosition: CharPosition? = null
    private var currentActions: List<CodeActionItem> = emptyList()

    var layout: CodeActionLayout
        get() = layoutImpl
        set(value) {
            if (::rootView.isInitialized && layoutImpl === value) {
                return
            }
            layoutImpl = value
            layoutImpl.attach(this)
            rootView = layoutImpl.createView(LayoutInflater.from(editor.context))
            super.setContentView(rootView)
            applyColorScheme()
        }

    init {
        layout = layoutImpl

        eventManager.subscribeEvent<ColorSchemeUpdateEvent> { _, _ ->
            applyColorScheme()
        }
        eventManager.subscribeEvent<EditorReleaseEvent> { _, _ ->
            setEnabled(false)
            dismiss()
        }
        eventManager.subscribeEvent<ScrollEvent> { _, _ ->
            if (editor.isInMouseMode) {
                return@subscribeEvent
            }
            if (isShowing) {
                if (!isAnchorVisible()) {
                    dismiss()
                } else {
                    updateWindowPosition()
                }
            }
        }
        eventManager.subscribeEvent<TextSizeChangeEvent> { event, _ ->
            layout.onTextSizeChanged(event.oldTextSize, event.newTextSize)
            if (isShowing) {
                renderActions()
                updateWindowSizeAndLocation()
            }
        }
        eventManager.subscribeEvent<EditorFocusChangeEvent> { event, _ ->
            if (!event.isGainFocus) {
                dismiss()
            }
        }
        eventManager.subscribeEvent<SelectionChangeEvent> { _, _ ->
            dismiss()
        }
    }

    fun isEnabled(): Boolean = eventManager.isEnabled

    fun setEnabled(enabled: Boolean) {
        eventManager.isEnabled = enabled
        if (!enabled) {
            dismiss()
        }
    }

    fun show(range: Range, entries: List<Either<Command, CodeAction>>) {
        if (!isEnabled() || entries.isEmpty()) {
            dismiss()
            return
        }
        val anchor = range.start.asCharPosition()
        anchorPosition = anchor
        currentActions = entries.map { CodeActionItem.from(it) }
        renderActions()
        updateWindowSizeAndLocation()
        show()
    }

    internal fun onActionSelected(item: CodeActionItem) {
        val action = item.action
        if (action.isLeft) {
            executeCommand(action.left)
        } else {
            applyCodeAction(action.right)
        }
        dismiss()
    }

    private fun executeCommand(command: Command?) {
        val commandId = command?.command ?: return
        val args = command.arguments ?: emptyList()
        lspEditor.coroutineScope.launch {
            lspEditor.eventManager.emitAsync(EventType.workSpaceExecuteCommand) {
                put("command", commandId)
                put("args", args)
            }
        }
    }

    private fun applyCodeAction(action: CodeAction?) {
        if (action == null) {
            return
        }
        val edit = action.edit
        if (edit != null) {
            val params = ApplyWorkspaceEditParams().apply {
                label = action.title
                this.edit = edit
            }
            lspEditor.eventManager.emit(EventType.workSpaceApplyEdit, params)
        }
        val nestedCommand = action.command
        if (nestedCommand != null) {
            executeCommand(nestedCommand)
        }
    }

    private fun renderActions() {
        layout.renderActions(currentActions)
    }

    private fun applyColorScheme() {
        layout.applyColorScheme(editor.colorScheme, editor.typefaceText)
        if (isShowing) {
            renderActions()
        }
    }

    private fun updateWindowSizeAndLocation() {
        val anchor = anchorPosition?.let { normalizePosition(it) } ?: return
        val charY = editor.getCharOffsetY(anchor.line, anchor.column)
        val margin = editor.dpUnit * 6
        val rowHeight = editor.rowHeight

        // Calculate available space below and above cursor (similar to EditorAutoCompletion)
        val spaceBelow = editor.height - (charY + rowHeight + margin)
        val spaceAbove = charY - margin
        val availableHeight = maxOf(spaceBelow, spaceAbove).coerceAtLeast(editor.dpUnit * 100)
        val constrainedMaxHeight = availableHeight.coerceAtMost(maxHeight.toFloat()).toInt()

        // Measure to get actual content height
        rootView.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val width = rootView.measuredWidth.coerceAtLeast((editor.dpUnit * 160).toInt())
        // Like EditorAutoCompletion: use actual height but limit to maxHeight
        val actualHeight = rootView.measuredHeight
        val height = actualHeight.coerceAtMost(constrainedMaxHeight)

        setSize(width, height)
        updateWindowPosition()
    }

    private fun updateWindowPosition() {
        val anchor = anchorPosition?.let { normalizePosition(it) } ?: return
        val charX = editor.getCharOffsetX(anchor.line, anchor.column)
        val charY = editor.getCharOffsetY(anchor.line, anchor.column)
        val margin = editor.dpUnit * 6
        val maxY = (editor.height - height).coerceAtLeast(0)
        val belowTop = charY + editor.rowHeight + margin
        val aboveTop = charY - height - margin
        val windowY = when {
            belowTop + height <= editor.height -> belowTop.coerceAtMost(maxY.toFloat())
            aboveTop >= 0 -> aboveTop.coerceAtMost(maxY.toFloat())
            else -> maxY.toFloat()
        }
        val maxX = (editor.width - width).coerceAtLeast(0)
        val windowX = charX.coerceIn(0f, maxX.toFloat())
        setLocationAbsolutely(windowX.toInt(), windowY.toInt())
    }

    private fun isAnchorVisible(): Boolean {
        val anchor = anchorPosition?.let { normalizePosition(it) } ?: return false
        if (editor.lineCount <= 0) {
            return false
        }
        editor.layout.getCharLayoutOffset(anchor.line, anchor.column, buffer)
        val top = buffer[0]
        val left = buffer[1]
        val visibleTop = editor.offsetY
        val visibleBottom = editor.offsetY + editor.height
        val visibleLeft = editor.offsetX
        val visibleRight = editor.offsetX + editor.width
        return top >= visibleTop && top <= visibleBottom && left >= visibleLeft && left <= visibleRight
    }

    private fun normalizePosition(position: CharPosition): CharPosition {
        if (editor.lineCount <= 0) {
            return CharPosition(0, 0)
        }
        val line = position.line.coerceIn(0, editor.lineCount - 1)
        val columnCount = editor.text.getColumnCount(line)
        val column = position.column.coerceIn(0, columnCount)
        return CharPosition(line, column)
    }
}
