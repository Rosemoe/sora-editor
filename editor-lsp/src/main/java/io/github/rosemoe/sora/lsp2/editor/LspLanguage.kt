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

package io.github.rosemoe.sora.lsp2.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.getCompletionItemComparator
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lsp2.editor.completion.CompletionItemProvider
import io.github.rosemoe.sora.lsp2.editor.completion.LspCompletionItem
import io.github.rosemoe.sora.lsp2.editor.format.LspFormatter

import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.SymbolPairMatch
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer


class LspLanguage(var editor: LspEditor) : Language {

    var lspFormatter: LspFormatter? = null
        private set

    var wrapperLanguage: Language? = null
    var completionItemProvider: CompletionItemProvider<*>

    init {
        lspFormatter = LspFormatter(this)
        completionItemProvider =
            CompletionItemProvider { completionItem: org.eclipse.lsp4j.CompletionItem, eventManager: LspEventManager, prefixLength: Int ->
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

        /* *//* if (getEditor().hitTrigger(line)) {
            publisher.cancel();
            return;
        }*//*
        val prefix = computePrefix(content, position)
        // Log.d("prefix", prefix);
        val prefixLength = prefix.length
        editor!!.providerManager.safeUseProvider(
            DocumentChangeProvider::class.java
        ).ifPresent { documentChangeFeature: DocumentChangeProvider ->
            val documentChangeFuture =
                documentChangeFeature.future
            if (!documentChangeFuture.isDone || !documentChangeFuture.isCompletedExceptionally || !documentChangeFuture.isCancelled) {
                try {
                    documentChangeFuture[1000, TimeUnit.MILLISECONDS]
                } catch (ignored: ExecutionException) {
                } catch (ignored: InterruptedException) {
                } catch (ignored: TimeoutException) {
                }
            }
        }
        val completionList = ArrayList<CompletionItem>()
        try {
            val completionFeature = editor!!.providerManager.useProvider(
                CompletionProvider::class.java
            )
                ?: return
            completionFeature.execute(position)
                .thenAccept { completions: List<org.eclipse.lsp4j.CompletionItem?> ->
                    completions.forEach(
                        Consumer { completionItem: org.eclipse.lsp4j.CompletionItem? ->
                            completionList.add(
                                completionItemProvider.createCompletionItem(
                                    completionItem,
                                    editor!!.providerManager.useProvider(
                                        ApplyEditsProvider::class.java
                                    ),
                                    prefixLength
                                )
                            )
                        })
                }.exceptionally { throwable: Throwable ->
                    publisher.cancel()
                    throw CompletionCancelledException(throwable.message)
                }[Timeout.getTimeout(Timeouts.COMPLETION).toLong(), TimeUnit.MILLISECONDS]
        } catch (e: Exception) {
            throw LSPException(e)
        }
        publisher.setComparator(getCompletionItemComparator(content, position, completionList))
        publisher.addItems(completionList)
        publisher.updateList()*/
    }

    fun computePrefix(text: ContentReference, position: CharPosition): String {
        val delimiters: MutableList<String> = ArrayList(
            editor.completionTriggers
        )
        if (delimiters.isEmpty()) {
            return CompletionHelper.computePrefix(text, position) { key: Char ->
                MyCharacter.isJavaIdentifierPart(
                    key
                )
            }
        }

        // add whitespace as delimiter, otherwise forced completion does not work
        delimiters.addAll(" \t\n\r".split(""))
        val offset = position.index
        val s = StringBuilder()
        for (i in 0 until offset) {
            val singleLetter = text[offset - i - 1]
            if (delimiters.contains(singleLetter.toString())) {
                return s.reverse().toString()
            }
            s.append(singleLetter)
        }
        return ""
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return wrapperLanguage?.interruptionLevel ?: 0
    }

    override fun useTab(): Boolean {
        return wrapperLanguage?.useTab() == true
    }

    override fun getFormatter(): Formatter {
        return lspFormatter ?: EmptyLanguage.EmptyFormatter.INSTANCE
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
        editor.project.coroutineScope.launch {
            editor.dispose()
        }
        lspFormatter = null
    }


}

