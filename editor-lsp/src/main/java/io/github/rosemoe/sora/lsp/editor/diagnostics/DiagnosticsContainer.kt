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
import org.eclipse.lsp4j.Diagnostic
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

    private fun findDiagnostic(uri: FileUri, line: Int, column: Int): Diagnostic? {
        return diagnosticsMap[uri]?.find {
            it.range.start.line == line &&
                    it.range.start.character == column ||
                    it.range.end.line == line &&
                    it.range.end.character == column
        }
    }

    private fun findOrReplaceDiagnostic(uri: FileUri, newDiagnostic: Diagnostic) {
        val diagnostics = diagnosticsMap[uri] ?: CopyOnWriteArrayList()
        findDiagnostic(
            uri,
            newDiagnostic.range.start.line,
            newDiagnostic.range.start.character
        )?.let { removeDiagnostic(uri, it) }
        diagnostics.add(newDiagnostic)
        diagnosticsMap[uri] = diagnostics
    }

    fun removeDiagnostic(uri: FileUri, diagnostic: Diagnostic) {
        diagnosticsMap[uri]?.remove(diagnostic)
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