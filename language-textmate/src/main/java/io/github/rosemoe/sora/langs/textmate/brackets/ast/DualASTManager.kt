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

/**
 * Unified dual AST manager with integrated BracketIndexAST logic.
 *
 * Manages two AST versions with copy-on-write semantics:
 *
 * 1. **Text AST**: Built with token information from old/cached snapshot
 *    - Updated immediately in beforeThreadHandled (fast response)
 *    - Uses token analysis but with potentially stale data
 *
 * 2. **Token AST**: Built with fresh token information
 *    - Updated after spans are ready (accurate response)
 *    - After update, textAST points to tokenAST (COW optimization)
 *
 * On initialize, directly builds tokenAST without textAST.
 *
 * This eliminates the need for separate BracketIndexAST instances and provides
 * better memory efficiency through copy-on-write pointer updates.
 */
internal class DualASTManager {

    /**
     * Text AST: Built with old snapshot, fast update.
     */
    @Volatile
    private var textAST: ASTNode = TextAST.EMPTY

    /**
     * Token AST: Built with fresh snapshot, accurate.
     */
    @Volatile
    private var tokenAST: ASTNode = TextAST.EMPTY

    /**
     * Active AST pointer: Points to the most recently updated AST.
     * Queries use this for O(1) access.
     */
    @Volatile
    private var activeAST: ASTNode = TextAST.EMPTY

    @Volatile
    private var textInitialized = false

    @Volatile
    private var tokenInitialized = false

    @Volatile
    private var version = 0L

    /**
     * Rebuilds the Text AST from scratch (used for immediate updates).
     */
    fun rebuildTextAST(tokenizer: ASTTokenizer) {
        textAST = ASTParser.parseFromScratch(tokenizer)
        activeAST = textAST
        textInitialized = true
        version++
    }

    /**
     * Rebuilds the Token AST from scratch.
     * After rebuild, textAST also points to tokenAST (COW optimization).
     */
    fun rebuildTokenAST(tokenizer: ASTTokenizer) {
        tokenAST = ASTParser.parseFromScratch(tokenizer)
        textAST = tokenAST  // Copy-on-write: textAST shares tokenAST
        activeAST = tokenAST
        tokenInitialized = true
        version++
    }

    /**
     * Updates the Text AST with incremental edits.
     * Called in beforeThreadHandled for fast bracket highlighting.
     */
    fun updateTextASTWithEdits(edits: List<EditInfo>, tokenizer: ASTTokenizer) {
        if (edits.isEmpty()) return

        val currentRoot = textAST

        // If empty or first build, just rebuild
        if (currentRoot is TextAST && currentRoot.length.isZero()) {
            rebuildTextAST(tokenizer)
            return
        }

        try {
            val positionMapper = BeforeEditPositionMapper.fromEdits(edits)
            textAST = ASTParser.parseIncremental(tokenizer, currentRoot, positionMapper)
            activeAST = textAST
            textInitialized = true
            version++
        } catch (e: Exception) {
            System.err.println("DualASTManager: Text AST update failed, falling back to rebuild")
            e.printStackTrace()
            rebuildTextAST(tokenizer)
        }
    }

    /**
     * Updates the Token AST with incremental edits.
     * Called in notifySpansChanged for accurate bracket highlighting.
     * After update, textAST also points to tokenAST (COW optimization).
     */
    fun updateTokenASTWithEdits(edits: List<EditInfo>, tokenizer: ASTTokenizer) {
        if (edits.isEmpty()) return

        val currentRoot = tokenAST

        // If empty or first build, just rebuild
        if (currentRoot is TextAST && currentRoot.length.isZero()) {
            rebuildTokenAST(tokenizer)
            return
        }

        try {
            val positionMapper = BeforeEditPositionMapper.fromEdits(edits)
            tokenAST = ASTParser.parseIncremental(tokenizer, currentRoot, positionMapper)
            textAST = tokenAST  // Copy-on-write: textAST shares tokenAST
            activeAST = tokenAST
            tokenInitialized = true
            version++
        } catch (e: Exception) {
            System.err.println("DualASTManager: Token AST update failed, falling back to rebuild")
            e.printStackTrace()
            rebuildTokenAST(tokenizer)
        }
    }

    /**
     * Gets the currently active AST for queries (O(1) operation).
     */
    fun getActiveAST(): ASTNode = activeAST

    /**
     * Gets the type of the currently active AST (for debugging).
     */
    fun getActiveType(): ASTType {
        return when (activeAST) {
            textAST -> if (textAST === tokenAST) ASTType.TOKEN else ASTType.TEXT
            tokenAST -> ASTType.TOKEN
            else -> ASTType.UNKNOWN
        }
    }

    /**
     * Returns true if at least one AST has been initialized.
     */
    fun isInitialized(): Boolean = textInitialized || tokenInitialized

    /**
     * Returns current version number (increments on each update).
     */
    fun currentVersion(): Long = version

    /**
     * Returns true if the AST is empty.
     */
    val isEmpty: Boolean
        get() = activeAST.let { it is TextAST && it.length.isZero() }

    /**
     * Clears all ASTs and resets to empty state.
     */
    fun clear() {
        textAST = TextAST.EMPTY
        tokenAST = TextAST.EMPTY
        activeAST = TextAST.EMPTY
        textInitialized = false
        tokenInitialized = false
        version = 0L
    }
}

private const val TAG = "DualASTManager"

/**
 * AST type enumeration for debugging.
 */
enum class ASTType {
    TEXT,    // Text AST (old snapshot)
    TOKEN,   // Token AST (fresh snapshot)
    UNKNOWN  // Uninitialized or invalid state
}
