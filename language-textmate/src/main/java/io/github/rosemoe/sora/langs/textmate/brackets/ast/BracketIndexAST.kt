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
 */
package io.github.rosemoe.sora.langs.textmate.brackets.ast

internal class BracketASTManager {

    @Volatile
    private var tokenAST: ASTNode = TextAST.EMPTY

    @Volatile
    private var tokenInitialized = false

    @Volatile
    private var version = 0L

    fun rebuildTokenAST(tokenizer: ASTTokenizer) {
        tokenAST = ASTParser.parseFromScratch(tokenizer)
        tokenInitialized = true
        version++
    }

    fun updateTokenASTWithEdits(edits: List<EditInfo>, tokenizer: ASTTokenizer) {
        if (edits.isEmpty()) return

        val currentRoot = tokenAST

        if (currentRoot is TextAST && currentRoot.length.isZero()) {
            rebuildTokenAST(tokenizer)
            return
        }

        try {
            val positionMapper = BeforeEditPositionMapper.fromEdits(edits)
            tokenAST = ASTParser.parseIncremental(tokenizer, currentRoot, positionMapper)
            tokenInitialized = true
            version++
        } catch (e: Exception) {
            rebuildTokenAST(tokenizer)
        }
    }

    fun getActiveAST(): ASTNode = tokenAST

    fun isInitialized(): Boolean = tokenInitialized

    fun currentVersion(): Long = version

    val isEmpty: Boolean
        get() = tokenAST.let { it is TextAST && it.length.isZero() }

    fun clear() {
        tokenAST = TextAST.EMPTY
        tokenInitialized = false
        version = 0L
    }
}
