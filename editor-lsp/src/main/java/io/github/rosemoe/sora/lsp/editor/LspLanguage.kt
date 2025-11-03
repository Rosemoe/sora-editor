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

package io.github.rosemoe.sora.lsp.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.createCompletionItemComparator
import io.github.rosemoe.sora.lang.completion.filterCompletionItems
import io.github.rosemoe.sora.lang.completion.highlightMatchLabel
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lsp.editor.completion.CompletionItemProvider
import io.github.rosemoe.sora.lsp.editor.completion.LspCompletionItem
import io.github.rosemoe.sora.lsp.editor.format.LspFormatter
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.completion.completion
import io.github.rosemoe.sora.lsp.events.document.DocumentChangeEvent
import io.github.rosemoe.sora.lsp.requests.Timeout
import io.github.rosemoe.sora.lsp.requests.Timeouts
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class LspLanguage(var editor: LspEditor) : Language {

    private var _formatter: Formatter? = null

    var wrapperLanguage: Language? = null
    var completionItemProvider: CompletionItemProvider<*>

    init {
        _formatter = LspFormatter(this)
        completionItemProvider =
            CompletionItemProvider { completionItem, eventManager, prefixLength ->
                LspCompletionItem(
                    completionItem,
                    eventManager,
                    prefixLength
                )
            }
    }

    override fun getAnalyzeManager(): AnalyzeManager {
        return wrapperLanguage?.analyzeManager ?: EmptyLanguage.EmptyAnalyzeManager.INSTANCE
    }

    override fun getInterruptionLevel(): Int {
        return wrapperLanguage?.interruptionLevel ?: 0
    }

    @Throws(CompletionCancelledException::class)
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {

        /* if (getEditor().hitTrigger(line)) {
            publisher.cancel();
            return;
        }*/

        if (!editor.isConnected) {
            return
        }

        val prefix = computePrefix(content, position)

        val prefixLength = prefix.length

        val documentChangeEvent =
            editor.eventManager.getEventListener<DocumentChangeEvent>() ?: return

        val documentChangeFuture =
            documentChangeEvent.future

        if (documentChangeFuture?.isDone == false || documentChangeFuture?.isCompletedExceptionally == false || documentChangeFuture?.isCancelled == false) {
            runCatching {
                documentChangeFuture.get(Timeout[Timeouts.WILLSAVE].toLong(), TimeUnit.MILLISECONDS)
            }
        }

        val completionList = ArrayList<CompletionItem>()

        val serverResultCompletionItems =
            editor.coroutineScope.future {
                editor.eventManager.emitAsync(EventType.completion, position)
                    .getOrNull<List<org.eclipse.lsp4j.CompletionItem>>("completion-items")
                    ?: emptyList()
            }

        try {
            serverResultCompletionItems
                .thenAccept { completions ->
                    completions.forEach { completionItem: org.eclipse.lsp4j.CompletionItem ->
                        completionList.add(
                            completionItemProvider.createCompletionItem(
                                completionItem,
                                editor.eventManager,
                                prefixLength
                            )
                        )
                    }
                }.exceptionally { throwable: Throwable ->
                    publisher.cancel()
                    throw CompletionCancelledException(throwable.message)
                }.get(Timeout[Timeouts.COMPLETION].toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            return
        }

        filterCompletionItems(content, position, completionList).let { filteredList ->
            publisher.setComparator(createCompletionItemComparator(filteredList))
            publisher.addItems(filteredList)
        }

        publisher.updateList()
    }

    private fun computePrefix(text: ContentReference, position: CharPosition): String {
        val triggers = editor.completionTriggers
        if (triggers.isEmpty()) {
            return CompletionHelper.computePrefix(text, position) { key: Char ->
                MyCharacter.isJavaIdentifierPart(key)
            }
        }

        val delimiters = triggers.toMutableList().apply {
            addAll(listOf(" ", "\t", "\n", "\r"))
        }

        val s = StringBuilder()

        val line = text.getLine(position.line)
        for (i in position.column - 1 downTo 0) {
            val char = line[i]
            if (delimiters.contains(char.toString())) {
                return s.reverse().toString()
            }
            s.append(char)
        }
        return s.toString()
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return wrapperLanguage?.interruptionLevel ?: 0
    }

    override fun useTab(): Boolean {
        return wrapperLanguage?.useTab() == true
    }

    override fun getFormatter(): Formatter {
        return _formatter ?: wrapperLanguage?.formatter ?: EmptyLanguage.EmptyFormatter.INSTANCE
    }

    fun setFormatter(formatter: Formatter) {
        this._formatter = formatter
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return wrapperLanguage?.symbolPairs ?: EmptyLanguage.EMPTY_SYMBOL_PAIRS
    }

    override fun getNewlineHandlers(): Array<NewlineHandler?> {
        return wrapperLanguage?.newlineHandlers ?: emptyArray()
    }

    override fun destroy() {
        formatter.destroy()
        wrapperLanguage?.destroy()
    }


}

