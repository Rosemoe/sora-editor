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

package io.github.rosemoe.sora.lsp2.utils

import android.health.connect.datatypes.units.Length
import android.util.Log
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.TextRange
import io.github.rosemoe.sora.util.ArrayList
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.lsp4j.CompletionContext
import org.eclipse.lsp4j.CompletionParams
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

fun FileUri.createDidCloseTextDocumentParams(
    languageId: String,
    text: String
): DidCloseTextDocumentParams {
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

fun String.createTextDocumentContentChangeEvent(): TextDocumentContentChangeEvent {
    return TextDocumentContentChangeEvent(this)
}

fun FileUri.createTextDocumentContentChangeEvent(
    range: Range,
    rangeLength: Int,
    text: String
): TextDocumentContentChangeEvent {
    return TextDocumentContentChangeEvent(range, rangeLength, text)
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
    return createRange(start.toPosition(), end.toPosition())
}

fun createPosition(line: Int, character: Int): Position {
    return Position(line, character)
}

fun CharPosition.toPosition(): Position {
    return Position(this.line, this.column)
}

fun TextRange.toRange(): Range {
    return Range(this.start.toPosition(), this.end.toPosition())
}

fun FileUri.createDidOpenTextDocumentParams(
    languageId: String,
    text: String
): DidOpenTextDocumentParams {
    val params = DidOpenTextDocumentParams()
    params.textDocument = TextDocumentItem(this.toFileUri(), languageId, getVersion(this), text)
    return params
}

fun FileUri.createDocumentDiagnosticParams(): DocumentDiagnosticParams {
    return DocumentDiagnosticParams(this.createTextDocumentIdentifier())
}

fun FileUri.createCompletionParams(
    position: Position?,
    context: CompletionContext?
): CompletionParams {
    val params = CompletionParams()
    params.textDocument = this.createTextDocumentIdentifier()
    params.position = position
    params.context = context
    return params
}

fun FileUri.createDidSaveTextDocumentParams(
    text: String
): DidSaveTextDocumentParams {
    val params = DidSaveTextDocumentParams()
    params.textDocument = this.createTextDocumentIdentifier()
    params.text = text
    return params
}

fun Position.getIndex(editor: CodeEditor): Int {
    return editor.text.getCharIndex(this.line, this.character)
}

fun List<Diagnostic>.transformToEditorDiagnostics(editor: CodeEditor): List<DiagnosticRegion> {
    val result = ArrayList<DiagnosticRegion>()
    var id = 0L
    for (diagnosticSource in this) {
        Log.w("diagnostic message", "diagnostic: " + diagnosticSource.message)
        val diagnostic = DiagnosticRegion(
            diagnosticSource.range.start.getIndex(editor),
            diagnosticSource.range.end.getIndex(editor),
            diagnosticSource.severity.toEditorLevel(), id++,
            DiagnosticDetail(
                diagnosticSource.severity.name,
                diagnosticSource.message,
                null,
                null
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
    versionMap.keys.filter(func).forEach { versionMap.remove(it) }
}