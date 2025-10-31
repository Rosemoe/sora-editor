/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.lsp.editor.diagnostics

import io.github.rosemoe.sora.lsp.utils.FileUri
import io.github.rosemoe.sora.lsp.utils.asCharPosition
import io.github.rosemoe.sora.text.CharPosition
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DiagnosticsContainer {
    private val diagnosticsMap by lazy(
        LazyThreadSafetyMode.NONE
    ) {
        ConcurrentHashMap<FileUri, CopyOnWriteArrayList<Diagnostic>>()
    }

    fun setDiagnostics(uri: FileUri, diagnostics: List<Diagnostic>) {
        diagnosticsMap[uri] = CopyOnWriteArrayList(diagnostics)
    }

    fun addDiagnostics(uri: FileUri, diagnostics: List<Diagnostic>) {
        diagnostics.forEach {
            findOrReplaceDiagnostic(uri, it)
        }
    }

    private fun findDiagnostic(uri: FileUri, line: Int): Diagnostic? {
        return diagnosticsMap[uri]?.find {
            it.range.start.line == line || it.range.end.line == line
        }
    }

    private fun findOrReplaceDiagnostic(uri: FileUri, newDiagnostic: Diagnostic) {
        val diagnostics = diagnosticsMap[uri] ?: CopyOnWriteArrayList()
        findDiagnostic(
            uri,
            newDiagnostic.range.start.line
        )?.let { removeDiagnostic(uri, it) }
        diagnostics.add(newDiagnostic)
        diagnosticsMap[uri] = diagnostics
    }

    fun removeDiagnostic(uri: FileUri, diagnostic: Diagnostic) {
        diagnosticsMap[uri]?.remove(diagnostic)
    }

    fun findDiagnostics(uri: FileUri, range: Range): List<Diagnostic>? {
        val diagnostics = diagnosticsMap[uri] ?: return null
        if (diagnostics.isEmpty()) return emptyList()

        val sortedDiagnostics = diagnostics.sortedWith(compareBy({ it.range.start.line }, { it.range.start.character }))

        val index = sortedDiagnostics.binarySearch { diagnostic ->
            val start = diagnostic.range.start
            val end = diagnostic.range.end
            val pos = range.start

            when {
                pos.line < start.line -> 1
                pos.line > end.line -> -1
                pos.line == start.line && pos.character < start.character -> 1
                pos.line == end.line && pos.character > end.character -> -1
                else -> 0
            }
        }

        val result = mutableListOf<Diagnostic>()

        if (index >= 0) {
            result.add(sortedDiagnostics[index])

            var i = index - 1
            while (i >= 0) {
                val diagnostic = sortedDiagnostics[i]
                if (isPositionInRange(range.start, diagnostic.range)) {
                    result.add(0, diagnostic)
                    i--
                } else {
                    break
                }
            }

            i = index + 1
            while (i < sortedDiagnostics.size) {
                val diagnostic = sortedDiagnostics[i]
                if (isPositionInRange(range.start, diagnostic.range)) {
                    result.add(diagnostic)
                    i++
                } else {
                    break
                }
            }
        } else {
            val insertionPoint = -(index + 1)
            for (i in maxOf(0, insertionPoint - 1) until minOf(sortedDiagnostics.size, insertionPoint + 2)) {
                if (i in sortedDiagnostics.indices && isPositionInRange(range.start, sortedDiagnostics[i].range)) {
                    result.add(sortedDiagnostics[i])
                }
            }
        }

        return result.ifEmpty { null }
    }

    private fun isPositionInRange(position: Position, range: Range): Boolean {
        val start = range.start
        val end = range.end

        return when {
            position.line < start.line || position.line > end.line -> false
            position.line == start.line && position.character < start.character -> false
            position.line == end.line && position.character > end.character -> false
            else -> true
        }
    }


    fun addDiagnostic(uri: FileUri, diagnostic: Diagnostic) {
        val diagnostics = diagnosticsMap[uri] ?: CopyOnWriteArrayList()
        findOrReplaceDiagnostic(uri, diagnostic)
        diagnosticsMap[uri] = diagnostics
    }

    fun clearDiagnostics(uri: FileUri) {
        diagnosticsMap.remove(uri)
    }


    fun getDiagnostics(uri: FileUri): List<Diagnostic> {
        return diagnosticsMap.getOrPut(uri) { CopyOnWriteArrayList() }
    }

    fun clear() {
        diagnosticsMap.clear()
    }


}