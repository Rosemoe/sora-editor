package android.zero.studio.widget.editor.symbolinput

import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SelectionMovement

object SymbolActionExecutor {

    fun execute(editor: CodeEditor, actionId: Int, text: String?, onOpenManager: (() -> Unit)?) {
        if (!editor.isEditable && actionId in listOf(0, 3, 4, 7, 8, 9, 11, 29, 30, 32)) return

        when (actionId) {
            0 -> insertTextWithMacro(editor, text ?: "")
            3 -> deleteLine(editor)
            4 -> clearLine(editor)
            7 -> toUpperCase(editor)
            8 -> toLowerCase(editor)
            9 -> editor.indentOrCommitTab()
            11 -> toggleComment(editor)
            16 -> editor.moveSelection(SelectionMovement.LINE_START)
            17 -> editor.moveSelection(SelectionMovement.LINE_END)
            18 -> editor.moveSelection(SelectionMovement.LEFT)
            19 -> editor.moveSelection(SelectionMovement.RIGHT)
            20 -> editor.moveSelection(SelectionMovement.UP)
            21 -> editor.moveSelection(SelectionMovement.DOWN)
            22 -> onOpenManager?.invoke()
            23 -> editor.moveSelection(SelectionMovement.TEXT_START)
            24 -> editor.moveSelection(SelectionMovement.TEXT_END)
            25 -> editor.selectAll()
            28 -> editor.copyText()
            29 -> editor.cutText()
            30 -> editor.pasteText()
            32 -> editor.formatCodeAsync()
            33 -> duplicateLine(editor)
            34 -> cutLine(editor)
            35 -> duplicateLine(editor)
            36 -> replaceLine(editor, text)
            37 -> insertTextWithMacro(editor, text ?: "\n")
            38 -> insertTextWithMacro(editor, text ?: "\t")
            39 -> editor.unindentSelection()
            in 40..51 -> insertTextWithMacro(editor, text ?: "<F${actionId - 39}>")
            52 -> editor.copyText() // Ctrl+C
            53 -> editor.cutText() // Ctrl+X
            54 -> editor.pasteText() // Ctrl+V
            55 -> editor.undo()
            56 -> editor.redo()
        }
    }

    private fun insertTextWithMacro(editor: CodeEditor, text: String) {
        val selectedText = if (editor.cursor.isSelected) {
            val left = editor.cursor.left().index
            val right = editor.cursor.right().index
            editor.text.subSequence(left, right).toString()
        } else {
            ""
        }

        val placeholder = "\u0000"
        val normalized = text.replace("$$", placeholder)

        val startIndex = normalized.indexOf("\$S").takeIf { it >= 0 }
        val endIndex = normalized.indexOf("\$E").takeIf { it >= 0 }

        var output = normalized
            .replace("\$S", "")
            .replace("\$E", "")
            .replace("\$T", selectedText)
            .replace(placeholder, "$")

        val insertCursor = startIndex?.coerceIn(0, output.length) ?: output.length
        val selectionEnd = endIndex?.coerceIn(0, output.length) ?: insertCursor

        editor.insertText(output, insertCursor)
        if (selectionEnd != insertCursor) {
            val caret = editor.cursor.left().index
            val delta = selectionEnd - insertCursor
            val target = (caret + delta).coerceIn(0, editor.text.length)
            val start = editor.text.indexer.getCharPosition(caret.coerceAtMost(target))
            val end = editor.text.indexer.getCharPosition(caret.coerceAtLeast(target))
            editor.setSelectionRegion(start.line, start.column, end.line, end.column)
        }
    }

    private fun deleteLine(editor: CodeEditor) {
        val cursor = editor.cursor
        val line = cursor.leftLine
        editor.text.delete(line, 0, line, editor.text.getColumnCount(line))
        if (line < editor.text.lineCount - 1) {
            editor.text.delete(line, editor.text.getColumnCount(line), line + 1, 0)
        } else if (line > 0) {
            editor.text.delete(line - 1, editor.text.getColumnCount(line - 1), line, 0)
        }
    }

    private fun clearLine(editor: CodeEditor) {
        val line = editor.cursor.leftLine
        editor.text.delete(line, 0, line, editor.text.getColumnCount(line))
    }

    private fun cutLine(editor: CodeEditor) {
        deleteLine(editor)
    }

    private fun duplicateLine(editor: CodeEditor) {
        val line = editor.cursor.leftLine
        val content = editor.text.getLineString(line)
        val insertionLine = (line + 1).coerceAtMost(editor.text.lineCount)
        editor.text.insert(insertionLine, 0, content + "\n")
    }

    private fun replaceLine(editor: CodeEditor, replacement: String?) {
        val line = editor.cursor.leftLine
        editor.text.delete(line, 0, line, editor.text.getColumnCount(line))
        editor.text.insert(line, 0, replacement ?: "")
    }

    private fun toggleComment(editor: CodeEditor) {
        val textObj = editor.text
        val startLine = editor.cursor.leftLine
        val endLine = editor.cursor.rightLine
        
        val commentStr = "//"

        textObj.beginBatchEdit()
        for (i in startLine..endLine) {
            val lineStr = textObj.getLineString(i)
            val trimmed = lineStr.trimStart()
            
            if (trimmed.startsWith(commentStr)) {
                val startIdx = lineStr.indexOf(commentStr)
                textObj.delete(i, startIdx, i, startIdx + commentStr.length)
            } else {
                val startIdx = lineStr.length - trimmed.length
                textObj.insert(i, startIdx, commentStr)
            }
        }
        textObj.endBatchEdit()
    }
    
    private fun toUpperCase(editor: CodeEditor) {
        if (editor.cursor.isSelected) {
            val leftIdx = editor.cursor.left().index
            val rightIdx = editor.cursor.right().index
            val text = editor.text.subSequence(leftIdx, rightIdx).toString()
            editor.commitText(text.uppercase())
        }
    }

    private fun toLowerCase(editor: CodeEditor) {
        if (editor.cursor.isSelected) {
            val leftIdx = editor.cursor.left().index
            val rightIdx = editor.cursor.right().index
            val text = editor.text.subSequence(leftIdx, rightIdx).toString()
            editor.commitText(text.lowercase())
        }
    }
    
}
