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

package io.github.rosemoe.sora.editor.ts

import android.os.Bundle
import android.os.Message
import android.util.Log
import com.itsaky.androidide.treesitter.TSInputEdit
import com.itsaky.androidide.treesitter.TSParser
import com.itsaky.androidide.treesitter.TSQueryCursor
import com.itsaky.androidide.treesitter.TSTree
import com.itsaky.androidide.treesitter.string.UTF16String
import com.itsaky.androidide.treesitter.string.UTF16StringFactory
import io.github.rosemoe.sora.data.ObjectAllocator
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import java.util.Collections
import java.util.concurrent.LinkedBlockingQueue

open class TsAnalyzeManager(val languageSpec: TsLanguageSpec, var theme: TsTheme) :
    AnalyzeManager {

    var currentReceiver: StyleReceiver? = null
    var reference: ContentReference? = null
    var extraArguments: Bundle? = null
    var thread: TsLooperThread? = null
    var styles = Styles()

    fun updateTheme(theme: TsTheme) {
        this.theme = theme
        (styles.spans as LineSpansGenerator?)?.let {
            it.theme = theme
        }
    }

    override fun setReceiver(receiver: StyleReceiver?) {
        currentReceiver = receiver
    }

    override fun reset(content: ContentReference, extraArguments: Bundle) {
        reference = content
        this.extraArguments = extraArguments
        rerun()
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {
        thread?.offerMessage(
            MSG_MOD,
            TextModification(
                start.index,
                end.index,
                TSInputEdit(
                    start.index * 2,
                    start.index * 2,
                    end.index * 2,
                    start.toTSPoint(),
                    start.toTSPoint(),
                    end.toTSPoint()
                ),
                insertedContent.toString()
            )
        )
        (styles.spans as LineSpansGenerator?)?.apply {
            lineCount = reference!!.lineCount
            tree.edit(
                TSInputEdit(
                    start.index * 2,
                    start.index * 2,
                    end.index * 2,
                    start.toTSPoint(),
                    start.toTSPoint(),
                    end.toTSPoint()
                )
            )
        }
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {
        thread?.offerMessage(
            MSG_MOD,
            TextModification(
                start.index,
                end.index,
                TSInputEdit(
                    start.index * 2,
                    end.index * 2,
                    start.index * 2,
                    start.toTSPoint(),
                    end.toTSPoint(),
                    start.toTSPoint()
                ),
                null
            )
        )
        (styles.spans as LineSpansGenerator?)?.apply {
            lineCount = reference!!.lineCount
            tree.edit(
                TSInputEdit(
                    start.index * 2,
                    end.index * 2,
                    start.index * 2,
                    start.toTSPoint(),
                    end.toTSPoint(),
                    start.toTSPoint()
                )
            )
        }
    }

    override fun rerun() {
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
                it.abort = true
            }
        }
        (styles.spans as LineSpansGenerator?)?.tree?.close()
        styles.spans = null
        val initText = reference?.reference?.toString() ?: ""
        thread = TsLooperThread().also {
            it.name = "TsDaemon-${nextThreadId()}"
            styles = Styles()
            it.offerMessage(MSG_INIT, initText)
            it.start()
        }
    }

    override fun destroy() {
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
                it.abort = true
            }
        }
        (styles.spans as LineSpansGenerator?)?.tree?.close()
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

        @Volatile
        var abort: Boolean = false
        val localText: UTF16String = UTF16StringFactory.newString()
        private val parser = TSParser().also {
            it.language = languageSpec.language
        }
        var tree: TSTree? = null

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

        fun updateStyles() {
            val scopedVariables = TsScopedVariables(tree!!, localText, languageSpec)
            if (thread == this && messageQueue.isEmpty()) {
                val oldTree = (styles.spans as LineSpansGenerator?)?.tree
                val copied = tree!!.copy()
                styles.spans = LineSpansGenerator(
                    copied,
                    reference!!.lineCount,
                    reference!!.reference,
                    theme,
                    languageSpec,
                    scopedVariables
                )
                val oldBlocks = styles.blocks
                updateCodeBlocks()
                if (oldBlocks != null) {
                    ObjectAllocator.recycleBlockLines(oldBlocks)
                }
                currentReceiver?.setStyles(this@TsAnalyzeManager, styles) {
                    oldTree?.close()
                }
                currentReceiver?.updateBracketProvider(
                    this@TsAnalyzeManager,
                    TsBracketPairs(copied, languageSpec)
                )
            }
        }

        fun updateCodeBlocks() {
            if (languageSpec.blocksQuery.patternCount == 0 || !languageSpec.blocksQuery.isValid) {
                return
            }
            val blocks = mutableListOf<CodeBlock>()
            TSQueryCursor().use {
                it.exec(languageSpec.blocksQuery, tree!!.rootNode)
                var match = it.nextMatch()
                while (match != null) {
                    if (languageSpec.blocksPredicator.doPredicate(
                            languageSpec.predicates,
                            localText,
                            match
                        )
                    ) {
                        match.captures.forEach {
                            val block = ObjectAllocator.obtainBlockLine().also { block ->
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
            val distinct = blocks.distinct().toMutableList()
            Collections.sort(distinct, CodeBlock.COMPARATOR_END)
            styles.blocks = distinct
        }

        override fun run() {
            try {
                while (!abort && !isInterrupted) {
                    val msg = messageQueue.take()
                    if (!handleMessage(msg)) {
                        break
                    }
                    msg.recycle()
                }
            } catch (e: InterruptedException) {
                // ignored
            }
            releaseThreadResources()
        }

        fun handleMessage(msg: Message): Boolean {
            try {
                when (msg.what) {
                    MSG_INIT -> {
                        localText.append(msg.obj!! as String)
                        if (!abort && !isInterrupted) {
                            tree = parser.parseString(localText)
                            updateStyles()
                        }
                    }

                    MSG_MOD -> {
                        if (!abort && !isInterrupted) {
                            val modification = msg.obj!! as TextModification
                            val newText = modification.changedText
                            val t = tree!!
                            t.edit(modification.tsEdition)
                            if (newText == null) {
                                localText.delete(modification.start, modification.end)
                            } else {
                                if (modification.start == localText.length) {
                                    localText.append(newText)
                                } else {
                                    localText.insert(modification.start, newText)
                                }
                            }
                            tree = parser.parseString(t, localText)
                            t.close()
                            updateStyles()
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                Log.w(
                    "TsAnalyzeManager",
                    "Thread $name exited with an error",
                    e
                )
            }
            return false
        }

        fun releaseThreadResources() {
            parser.close()
            tree?.close()
            localText.close()
        }

    }

    private data class TextModification(
        val start: Int,
        val end: Int,
        val tsEdition: TSInputEdit,
        /**
         * null for deletion
         */
        val changedText: String?
    )
}