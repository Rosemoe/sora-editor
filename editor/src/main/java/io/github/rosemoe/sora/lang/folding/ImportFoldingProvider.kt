/*
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
package io.github.rosemoe.sora.lang.folding

import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Folds contiguous blocks of import statements, e.g. the whole import header of a Java or
 * Kotlin file.
 *
 * ```
 * editor.registerFoldingProvider(ImportFoldingProvider())
 * // other languages
 * editor.registerFoldingProvider(ImportFoldingProvider(listOf("import ", "from ")))
 * ```
 *
 * Every run of consecutive lines whose trimmed content starts with one of the
 * [importPrefixes] becomes a region whose first import line is the fold header. A blank
 * line or any other line ends the run, and a run needs at least two lines.
 *
 * @property importPrefixes Line prefixes that mark an import line
 *
 * @author nullij
 * @see <a href="https://github.com/nullij">https://github.com/nullij</a>
 */
class ImportFoldingProvider @JvmOverloads constructor(
    private val importPrefixes: List<String> = DEFAULT_PREFIXES
) : FoldingProvider {

    override fun computeFoldingRegions(editor: CodeEditor, out: MutableList<FoldingRegion>) {
        val text = editor.text ?: return
        if (importPrefixes.isEmpty()) {
            return
        }
        val lineCount = text.lineCount
        var runStart = -1
        for (line in 0 until lineCount) {
            if (isImportLine(text.getLine(line))) {
                if (runStart < 0) {
                    runStart = line
                }
            } else if (runStart >= 0) {
                addRun(runStart, line - 1, out)
                runStart = -1
            }
        }
        if (runStart >= 0) {
            addRun(runStart, lineCount - 1, out)
        }
    }

    private fun addRun(start: Int, end: Int, out: MutableList<FoldingRegion>) {
        if (end > start) {
            out.add(FoldingRegion(start, end))
        }
    }

    private fun isImportLine(line: CharSequence): Boolean {
        var i = 0
        while (i < line.length && (line[i] == ' ' || line[i] == '\t')) {
            i++
        }
        return importPrefixes.any { line.startsWith(it, i) }
    }

    companion object {

        /** Prefixes used for Java and Kotlin sources */
        @JvmField
        val DEFAULT_PREFIXES = listOf("import ")

    }

}
