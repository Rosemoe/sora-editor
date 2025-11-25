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

package io.github.rosemoe.sora.lsp.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.lsp4j.CompletionContext
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.CompletionTriggerKind
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentDiagnosticParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import java.util.concurrent.locks.ReentrantReadWriteLock

private val versionMap = HashMap<FileUri, Int>()

private var lock: ReentrantReadWriteLock? = null

fun FileUri.createDidCloseTextDocumentParams(): DidCloseTextDocumentParams {
    val params = DidCloseTextDocumentParams()
    params.textDocument = this.createTextDocumentIdentifier()
    return params
}

fun FileUri.createTextDocumentIdentifier(): TextDocumentIdentifier {
    val identifier = TextDocumentIdentifier()
    identifier.uri = this.toFileUri()
    return identifier
}

fun FileUri.createDidChangeTextDocumentParams(
    events: List<TextDocumentContentChangeEvent>
): DidChangeTextDocumentParams {
    val params = DidChangeTextDocumentParams()
    params.contentChanges = events
    params.textDocument = VersionedTextDocumentIdentifier(this.toFileUri(), getVersion(this))
    return params
}

fun FileUri.createTextDocumentContentChangeEvent(
    range: Range, text: String
): TextDocumentContentChangeEvent {
    return TextDocumentContentChangeEvent(range, text)
}

fun FileUri.createTextDocumentContentChangeEvent(
    text: String
): TextDocumentContentChangeEvent {
    return TextDocumentContentChangeEvent(text)
}

fun createRange(start: Position, end: Position): Range {
    return Range(start, end)
}

fun createRange(start: CharPosition, end: CharPosition): Range {
    return createRange(start.asLspPosition(), end.asLspPosition())
}

fun createPosition(line: Int, character: Int): Position {
    return Position(line, character)
}

fun CharPosition.asLspPosition(): Position {
    return Position(this.line, this.column)
}

fun Position.asCharPosition(): CharPosition {
    return CharPosition(this.line, this.character)
}

fun TextRange.asLspRange(): Range {
    return Range(this.start.asLspPosition(), this.end.asLspPosition())
}

fun LspEditor.createDidOpenTextDocumentParams(): DidOpenTextDocumentParams {
    val params = DidOpenTextDocumentParams()
    params.textDocument = TextDocumentItem(
        this.uri.toFileUri(), this.fileExt, getVersion(this.uri), editorContent
    )
    return params
}

fun FileUri.createDocumentDiagnosticParams(): DocumentDiagnosticParams {
    return DocumentDiagnosticParams(this.createTextDocumentIdentifier())
}

fun FileUri.createCompletionParams(
    position: Position, context: CompletionContext
): CompletionParams {
    val params = CompletionParams()
    params.textDocument = this.createTextDocumentIdentifier()
    params.position = position
    params.context = context
    context.triggerKind = CompletionTriggerKind.TriggerCharacter
    return params
}


fun LspEditor.createDidSaveTextDocumentParams(): DidSaveTextDocumentParams {
    val params = DidSaveTextDocumentParams()
    params.textDocument = uri.createTextDocumentIdentifier()
    params.text = editorContent
    return params
}

fun Position.getIndex(editor: CodeEditor): Int {
    val line = line.coerceAtMost(editor.lineCount - 1)
    return editor.text.getCharIndex(
        line,
        editor.text.getColumnCount(line).coerceAtMost(this.character)
    )
}

internal fun List<Diagnostic>.transformToEditorDiagnostics(editor: CodeEditor): List<DiagnosticRegion> {
    val result = ArrayList<DiagnosticRegion>()
    var id = 0L
    for (diagnosticSource in this) {
        val diagnostic = DiagnosticRegion(
            diagnosticSource.range.start.getIndex(editor),
            diagnosticSource.range.end.getIndex(editor),
            diagnosticSource.severity.toEditorLevel(),
            id++,
            DiagnosticDetail(
                diagnosticSource.severity.name, diagnosticSource.message, null, diagnosticSource
            )
        )
        result.add(diagnostic)
    }
    return result
}

fun DiagnosticSeverity.toEditorLevel(): Short {
    return when (this) {
        DiagnosticSeverity.Hint, DiagnosticSeverity.Information -> DiagnosticRegion.SEVERITY_TYPO
        DiagnosticSeverity.Error -> DiagnosticRegion.SEVERITY_ERROR
        DiagnosticSeverity.Warning -> DiagnosticRegion.SEVERITY_WARNING
    }
}

private fun getVersion(fileUri: FileUri): Int {
    val notNullLock = lock ?: ReentrantReadWriteLock()
    if (lock == null) {
        lock = notNullLock
    }

    val readLock = notNullLock.readLock()
    readLock.lock()
    var version = versionMap.getOrDefault(fileUri, 0)
    version++
    readLock.unlock()
    val writeLock = notNullLock.writeLock()
    writeLock.lock()
    versionMap[fileUri] = version
    writeLock.unlock()
    return version
}

fun clearVersions(func: (FileUri) -> Boolean) {
    val keysToDelete = versionMap.keys.filter(func)
    keysToDelete.forEach { versionMap.remove(it) }
}

fun blendARGB(
    @ColorInt color1: Int, @ColorInt color2:Int,
    @FloatRange(from = 0.0, to = 1.0) ratio: Float
): Int {
    val inverseRatio = 1 - ratio;
    val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio;
    val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio;
    val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio;
    val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio;
    return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
}