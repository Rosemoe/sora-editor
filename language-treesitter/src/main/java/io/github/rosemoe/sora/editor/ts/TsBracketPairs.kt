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

package io.github.rosemoe.sora.editor.ts

import com.itsaky.androidide.treesitter.TSQueryCursor
import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import java.lang.Math.max

class TsBracketPairs(
    private val safeTree: SafeTsTree,
    private val languageSpec: TsLanguageSpec
) : BracketsProvider {

    companion object {

        val OPEN_NAME = "editor.brackets.open"
        val CLOSE_NAME = "editor.brackets.close"

    }

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        if (languageSpec.bracketsQuery.canAccess() && languageSpec.bracketsQuery.patternCount > 0) {
            TSQueryCursor.create().use { cursor ->
                cursor.setByteRange(max(0, index - 1) * 2, index * 2 + 1)

                return safeTree.accessTree { tree ->
                    if (tree.closed) return@accessTree null
                    val rootNode = tree.rootNode
                    if (!rootNode.canAccess() || rootNode.hasChanges()) {
                        return@accessTree null
                    }
                    cursor.exec(languageSpec.bracketsQuery, rootNode)
                    var match = cursor.nextMatch()
                    var matched = false
                    val pos = IntArray(4)
                    while (match != null && !matched) {
                        if (languageSpec.bracketsPredicator.doPredicate(
                                languageSpec.predicates,
                                text,
                                match
                            )
                        ) {
                            pos.fill(-1)
                            for (capture in match.captures) {
                                val captureName =
                                    languageSpec.bracketsQuery.getCaptureNameForId(capture.index)
                                if (captureName == OPEN_NAME || captureName == CLOSE_NAME) {
                                    val node = capture.node
                                    if (index >= node.startByte / 2 && index <= node.endByte / 2) {
                                        matched = true
                                    }
                                    if (captureName == OPEN_NAME) {
                                        pos[0] = node.startByte
                                        pos[1] = node.endByte
                                    } else {
                                        pos[2] = node.startByte
                                        pos[3] = node.endByte
                                    }
                                }
                            }
                            if (matched && pos[0] != -1 && pos[2] != -1) {
                                return@accessTree PairedBracket(
                                    pos[0] / 2,
                                    (pos[1] - pos[0]) / 2,
                                    pos[2] / 2,
                                    (pos[3] - pos[2]) / 2
                                )
                            }
                        }
                        match = cursor.nextMatch()
                    }
                    null
                }
            }
        }
        return null
    }

}