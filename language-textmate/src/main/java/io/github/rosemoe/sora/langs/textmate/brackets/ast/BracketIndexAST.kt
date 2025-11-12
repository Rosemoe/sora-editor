/*******************************************************************************
 * ---------------------------------------------------------------------------------------------
 *  *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  *  Licensed under the MIT License. See License.txt in the project root for license information.
 *  *--------------------------------------------------------------------------------------------
 ******************************************************************************/
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
