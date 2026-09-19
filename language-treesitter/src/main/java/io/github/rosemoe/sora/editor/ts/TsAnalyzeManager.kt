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

import android.os.Message
import android.util.Log
import com.itsaky.androidide.treesitter.TSInputEdit
import com.itsaky.androidide.treesitter.TSParser
import com.itsaky.androidide.treesitter.TSQueryCursor
import com.itsaky.androidide.treesitter.TSTree
import com.itsaky.androidide.treesitter.string.UTF16String
import com.itsaky.androidide.treesitter.string.UTF16StringFactory
import io.github.rosemoe.sora.editor.ts.spans.DefaultSpanFactory
import io.github.rosemoe.sora.editor.ts.spans.TsSpanFactory
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.util.BaseAnalyzeManager
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import java.util.concurrent.LinkedBlockingQueue

open class TsAnalyzeManager(val languageSpec: TsLanguageSpec, var theme: TsTheme) :
    BaseAnalyzeManager() {

    val currentReceiver: StyleReceiver?
        get() = receiver
    val reference: ContentReference?
        get() = contentRef
    @Volatile
    var thread: TsLooperThread? = null
    var spanFactory: TsSpanFactory = DefaultSpanFactory()

    internal var bracketPairColorization = false

    open var styles = Styles()

    fun updateTheme(theme: TsTheme) {
        this.theme = theme
        val spans = styles.spans
        spans?.let {
            if (it is LineSpansGenerator)
                it.theme = theme
        }
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        val reference = reference ?: return
        thread?.offerMessage(
            MSG_MOD,
            TextModification(
                start.index,
                end.index,
                newTSInputEdit(start, start, end),
                insertedContent.toString(),
                reference.reference.documentVersion
            )
        )
        (styles.spans as LineSpansGenerator?)?.apply {
            lineCount = reference.lineCount
            safeTree.accessTreeIfAvailable {
                it.edit(newTSInputEdit(start, start, end))
            }
        }
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        val reference = reference ?: return
        thread?.offerMessage(
            MSG_MOD,
            TextModification(
                start.index,
                end.index,
                newTSInputEdit(start, end, start),
                null,
                reference.reference.documentVersion
            )
        )
        (styles.spans as LineSpansGenerator?)?.apply {
            lineCount = reference.lineCount
            safeTree.accessTreeIfAvailable {
                it.edit(newTSInputEdit(start, end, start))
            }
        }
    }

    override fun rerun() {
        destroyPreviousRes()
        styles = Styles()
        val initText = reference?.reference?.toString() ?: ""
        val newThread = TsLooperThread()
        newThread.name = "TsDaemon-${nextThreadId()}"
        newThread.offerMessage(MSG_INIT, initText)
        // Publish before starting: the loop treats `thread != this` as outdated.
        thread = newThread
        newThread.start()
    }

    override fun destroy() {
        destroyPreviousRes()
        spanFactory.close()
        super.destroy()
    }

    /**
     * Destroy resources related to previous worker thread, and reset spans.
     */
    protected fun destroyPreviousRes() {
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
                it.abort = true
            }
        }
        val spans = styles.spans
        // IMPORTANT avoid access to the tree after destruction
        styles.spans = null
        if (spans is LineSpansGenerator) {
            spans.safeTree.close()
        }
    }

    companion object {
        private const val MSG_BASE = 11451400
        private const val MSG_INIT = MSG_BASE + 1
        private const val MSG_MOD = MSG_BASE + 2

        @Volatile
        private var threadId = 0

        @Synchronized
        fun nextThreadId() = ++threadId
    }

    inner class TsLooperThread : Thread() {

        private val messageQueue = LinkedBlockingQueue<Message>()
        // Until the first edit arrives the version is unknown; -1 tells TsBracketPairs to skip the
        // check rather than name a revision we cannot vouch for.
        private var documentVersion = contentRef?.reference?.documentVersion ?: -1L
        // Brackets are published independently of the full locals/folding pass.
        private var bracketTree: SafeTsTree? = null

        @Volatile
        var abort: Boolean = false
        val localText: UTF16String = UTF16StringFactory.newString()
        private val parser = TSParser.create().also {
            it.language = languageSpec.language
        }
        /** Assigned by MSG_INIT, which is always the first message handled. */
        lateinit var tree: TSTree

        fun offerMessage(what: Int, obj: Any?) {
            val msg = Message.obtain()
            msg.what = what
            msg.obj = obj
            offerMessage(msg)
        }

        fun offerMessage(msg: Message) {
            // Result ignored: capacity is enough as it is INT_MAX
            messageQueue.offer(msg)
        }

        private fun isOutdated() = abort || isInterrupted || thread != this || messageQueue.isNotEmpty()

        fun updateStyles() {
            if (isOutdated()) return
            val content = reference ?: return
            val newBracketTree = SafeTsTree(tree.copy())
            val oldBracketTree = bracketTree
            bracketTree = newBracketTree
            currentReceiver?.updateBracketProvider(
                this@TsAnalyzeManager,
                TsBracketPairs(newBracketTree, languageSpec, documentVersion)
            )
            oldBracketTree?.close()

            val scopedVariables = try {
                TsScopedVariables(tree, localText, languageSpec, ::isOutdated)
            } catch (_: TsScopedVariables.AnalysisCanceledException) {
                return
            }
            if (isOutdated()) return
            updateCodeBlocks()
            if (isOutdated()) return
            val targetStyles = styles
            val version = documentVersion
            val newTree = SafeTsTree(tree.copy())
            val newSpans = LineSpansGenerator(
                newTree,
                tree.rootNode.endPoint.row + 1,
                content.reference,
                theme,
                languageSpec,
                scopedVariables,
                spanFactory
            )
            val receiver = currentReceiver
            if (receiver == null) {
                newTree.close()
                return
            }
            receiver.setStyles(this@TsAnalyzeManager, targetStyles) {
                if (thread != this || reference?.reference?.documentVersion != version) {
                    newTree.close()
                } else {
                    val oldTree = (targetStyles.spans as LineSpansGenerator?)?.safeTree
                    targetStyles.spans = newSpans
                    oldTree?.close()
                }
            }
        }

        fun updateCodeBlocks() {
            if (languageSpec.blocksQuery.patternCount == 0 || !languageSpec.blocksQuery.canAccess()) {
                return
            }
            val blocks = mutableListOf<CodeBlock>()
            TSQueryCursor.create().use {
                it.exec(languageSpec.blocksQuery, tree.rootNode)
                var match = it.nextMatch()
                while (match != null) {
                    if (isOutdated()) return
                    if (languageSpec.blocksPredicator.doPredicate(
                            languageSpec.predicates,
                            localText,
                            match
                        )
                    ) {
                        match.captures.forEach {
                            val block = CodeBlock().also { block ->
                                var node = it.node
                                val start = node.startPoint
                                block.startLine = start.row
                                block.startColumn = start.column / 2
                                val end = if (languageSpec.blocksQuery.getCaptureNameForId(it.index)
                                        .endsWith(".marked")
                                ) {
                                    // Goto last terminal element
                                    while (node.childCount > 0) {
                                        node = node.getChild(node.childCount - 1)
                                    }
                                    node.startPoint
                                } else {
                                    node.endPoint
                                }
                                block.endLine = end.row
                                block.endColumn = end.column / 2
                            }
                            if (block.endLine - block.startLine > 1) {
                                blocks.add(block)
                            }
                        }
                    }
                    match = it.nextMatch()
                }
            }
            if (isOutdated()) return
            // sequence should be preferred here in order to avoid allocating multiple lists and sets
            val distinct = blocks.asSequence().distinct().toMutableList()
            styles.blocks = distinct
            styles.finishBuilding()
        }

        override fun run() {
            try {
                while (!abort && !isInterrupted) {
                    var msg: Message = messageQueue.take()
                    while (true) {
                        when (msg.what) {
                            MSG_INIT -> localText.append(msg.obj as String)
                            MSG_MOD -> {
                                val modification = msg.obj as TextModification
                                tree.edit(modification.tsEdition)
                                val newText = modification.changedText
                                if (newText == null) {
                                    localText.delete(modification.start, modification.end)
                                } else {
                                    localText.insert(modification.start, newText)
                                }
                                documentVersion = modification.documentVersion
                            }
                        }
                        msg.recycle()
                        if (abort || isInterrupted) return
                        // Drain everything that arrived while the last edit was applied, so the
                        // queued edits share one reparse instead of one reparse per keystroke.
                        msg = messageQueue.poll() ?: break
                    }
                    // Reusing unchanged subtrees, so a reparse per drained batch is cheap.
                    val oldTree = tree
                    tree = parser.parseString(oldTree, localText)
                    oldTree.close()
                    updateStyles()
                }
            } catch (e: InterruptedException) {
                // ignored
            } catch (e: Exception) {
                Log.w("TsAnalyzeManager", "Thread $name exited with an error", e)
            } finally {
                releaseThreadResources()
            }
        }

        fun releaseThreadResources() {
            parser.close()
            if (this::tree.isInitialized) tree.close()
            bracketTree?.close()
            localText.close()
        }

    }

    data class TextModification(
        val start: Int,
        val end: Int,
        val tsEdition: TSInputEdit,
        /**
         * null for deletion
         */
        val changedText: String?,
        val documentVersion: Long
    )
}
