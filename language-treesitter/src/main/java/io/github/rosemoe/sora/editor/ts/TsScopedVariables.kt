/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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

package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSQueryCapture
import com.itsaky.androidide.treesitter.TSQueryCursor
import com.itsaky.androidide.treesitter.TSTree
import java.util.Stack

class TsScopedVariables(tree: TSTree, text: CharSequence, spec: TsLanguageSpec) {

    /**
     * Naturally sorted by start index
     */
    private val collectedVariables = mutableListOf<ScopedVariable>()

    init {
        if (spec.localsDefinitionIndices.isNotEmpty()) {
            TSQueryCursor().use { cursor ->
                cursor.exec(spec.tsQuery, tree.rootNode)
                var match = cursor.nextMatch()
                val captures = mutableListOf<TSQueryCapture>()
                while (match != null) {
                    captures.addAll(match.captures)
                    match = cursor.nextMatch()
                }
                captures.sortBy { it.node.startByte }
                val scopeStack = Stack<Scope>()
                scopeStack.push(Scope(tree.rootNode.endByte / 2))
                captures.forEach {
                    val startIndex = it.node.startByte / 2
                    val endIndex = it.node.endByte / 2
                    while (startIndex >= scopeStack.peek().endIndex) {
                        scopeStack.pop()
                    }
                    val pattern = it.index
                    if (pattern in spec.localsScopeIndices) {
                        scopeStack.push(Scope(endIndex))
                    } else if (pattern in spec.localsDefinitionIndices) {
                        val scopedVar = ScopedVariable(
                            text.substring(startIndex, endIndex),
                            startIndex,
                            scopeStack.peek().endIndex,
                            endIndex
                        )
                        collectedVariables.add(scopedVar)
                        scopeStack.peek().variables.add(scopedVar)
                    } else if (pattern !in spec.localsDefinitionValueIndices && pattern !in spec.localsReferenceIndices) {
                        val topVariables = scopeStack.peek().variables
                        if (topVariables.isNotEmpty()) {
                            val topVariable = topVariables.last()
                            if (topVariable.scopeStartIndex == startIndex && topVariable.nodeEndIndex == endIndex && topVariable.matchedHighlightPattern == -1) {
                                topVariable.matchedHighlightPattern = pattern
                            }
                        }
                    }
                }
            }
            collectedVariables.removeAll { it.matchedHighlightPattern == -1 }
        }
    }

    data class Scope(
        val endIndex: Int,
        val variables: MutableList<ScopedVariable> = mutableListOf()
    )

    data class ScopedVariable(
        var name: String,
        var scopeStartIndex: Int,
        var scopeEndIndex: Int,
        var nodeEndIndex: Int,
        var matchedHighlightPattern: Int = -1
    )

    fun findDefinition(startIndex: Int, endIndex: Int, name: String) : ScopedVariable? {
        val filtered = collectedVariables.filter { it.scopeStartIndex <= startIndex && it.scopeEndIndex >= endIndex && it.name == name }
        if (filtered.isEmpty()) {
            return null
        }
        return filtered.last()
    }

}