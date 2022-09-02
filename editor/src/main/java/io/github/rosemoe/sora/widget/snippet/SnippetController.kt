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

package io.github.rosemoe.sora.widget.snippet

import android.util.Log
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.completion.snippet.*
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.snippet.variable.*
import io.github.rosemoe.sorakt.getComponent
import io.github.rosemoe.sorakt.subscribeEvent

class SnippetController(private val editor: CodeEditor) {

    val commentVariableResolver = CommentBasedSnippetVariableResolver(null)

    var fileVariableResolver: FileBasedSnippetVariableResolver? = null
        set(value) {
            val old = field
            if (old != null) {
                variableResolver.removeResolver(old)
            }
            field = value
            if (value != null) {
                variableResolver.addResolver(value)
            }
        }

    var workspaceVariableResolver: WorkspaceBasedSnippetVariableResolver? = null
        set(value) {
            val old = field
            if (old != null) {
                variableResolver.removeResolver(old)
            }
            field = value
            if (value != null) {
                variableResolver.addResolver(value)
            }
        }

    private var currentSnippet: CodeSnippet? = null
    var snippetIndex = -1
    private var tabStops: MutableList<SnippetItem>? = null
    private var currentTabStopIndex = -1
    private var inSequenceEdits = false

    private val variableResolver = CompositeSnippetVariableResolver().also {
        it.addResolver(ClipboardBasedSnippetVariableResolver(editor.clipboardManager))
        it.addResolver(EditorBasedSnippetVariableResolver(editor))
        it.addResolver(RandomBasedSnippetVariableResolver())
        it.addResolver(TimeBasedSnippetVariableResolver())
        it.addResolver(commentVariableResolver)
    }

    init {
        editor.subscribeEvent<SelectionChangeEvent> { event, _ ->
            if (isInSnippet()) {
                if (!checkIndex(event.left.index) || !checkIndex(event.right.index)) {
                    stopSnippet()
                }
            }
        }
        editor.subscribeEvent<ContentChangeEvent> { event, _ ->
            if (isInSnippet() && !inSequenceEdits) {
                if (event.action == ContentChangeEvent.ACTION_SET_NEW_TEXT) {
                    stopSnippet()
                } else if (event.action == ContentChangeEvent.ACTION_INSERT) {
                    if (checkIndex(event.changeStart.index)) {
                        var exitOnEnd = false;
                        val addedTextLength = if (event.changedText.contains(lineSeparatorRegex)) {
                            exitOnEnd = true
                            event.changedText.indexOfFirst { it == '\r' || it == '\n' }
                        } else {
                            event.changedText.length
                        }
                        // Shift current text
                        val editing = getEditingTabStop()!!
                        editing.setIndex(editing.startIndex, editing.endIndex + addedTextLength)
                        var metEditing = false
                        currentSnippet!!.items.forEach {
                            if (metEditing) {
                                it.shiftIndex(event.changedText.length)
                            } else if (it == editing) {
                                metEditing = true
                            }
                        }
                        inSequenceEdits = true
                        val text = editor.text
                        var hasChangedText = false
                        val replacement = text.substring(editing.startIndex, editing.endIndex)
                        text.beginBatchEdit()
                        currentSnippet!!.items.forEachIndexed { index, snippetItem ->
                            if (isEditingRelated(snippetItem)) {
                                hasChangedText = true
                                val deltaIndex =
                                    replacement.length - (snippetItem.endIndex - snippetItem.startIndex)
                                text.replace(
                                    snippetItem.startIndex,
                                    snippetItem.endIndex,
                                    replacement
                                )
                                snippetItem.setIndex(
                                    snippetItem.startIndex,
                                    snippetItem.endIndex + deltaIndex
                                )
                                shiftItemsFrom(index + 1, deltaIndex)
                            }
                        }
                        text.endBatchEdit()
                        inSequenceEdits = false
                        if (exitOnEnd) {
                            stopSnippet()
                        } else if(hasChangedText) {
                            editor.getComponent<EditorAutoCompletion>().requireCompletion()
                        }
                    } else {
                        stopSnippet()
                    }
                } else if (event.action == ContentChangeEvent.ACTION_DELETE) {
                    if (!checkIndex(event.changeStart.index) || !checkIndex(event.changeEnd.index)) {
                        stopSnippet()
                    } else {
                        editor.text.undoManager.exitReplaceMode()
                        val deletedTextLength = event.changedText.length
                        val editing = getEditingTabStop()!!
                        editing.setIndex(editing.startIndex, editing.endIndex - deletedTextLength)
                        var metEditing = false
                        currentSnippet!!.items.forEach {
                            if (metEditing) {
                                it.shiftIndex(-deletedTextLength)
                            } else if (it == editing) {
                                metEditing = true
                            }
                        }
                        inSequenceEdits = true
                        val text = editor.text
                        val replacement = text.substring(editing.startIndex, editing.endIndex)
                        text.beginBatchEdit()
                        currentSnippet!!.items.forEachIndexed { index, snippetItem ->
                            if (isEditingRelated(snippetItem)) {
                                val deltaIndex =
                                    replacement.length - (snippetItem.endIndex - snippetItem.startIndex)
                                text.replace(
                                    snippetItem.startIndex,
                                    snippetItem.endIndex,
                                    replacement
                                )
                                snippetItem.setIndex(
                                    snippetItem.startIndex,
                                    snippetItem.endIndex + deltaIndex
                                )
                                shiftItemsFrom(index + 1, deltaIndex)
                            }
                        }
                        text.endBatchEdit()
                        inSequenceEdits = false
                    }
                }
            }
        }
    }

    private fun checkIndex(index: Int): Boolean {
        val editing = getEditingTabStop()!!
        return index >= editing.startIndex && index <= editing.endIndex
    }

    fun startSnippet(index: Int, snippet: CodeSnippet, selectedText: String = "") {
        if (snippetIndex != -1) {
            stopSnippet()
        }
        // Stage 1: verify the snippet structure
        if (!snippet.checkContent() || snippet.items.size == 0) {
            Log.e("SnippetController", "invalid code snippet")
            return
        }
        val clonedSnippet = snippet.clone()
        currentSnippet = clonedSnippet
        currentTabStopIndex = -1
        snippetIndex = index
        // Stage 2: resolve the variables
        clonedSnippet.placeholderDefinitions.forEach {
            if (variableResolver.canResolve(it.id)) {
                it.defaultValue = variableResolver.resolve(it.id)
                // resolved, and we edit the items
                val items = clonedSnippet.items!!
                for (i in 0 until items.size) {
                    val snippetItem = items[i]
                    if ((snippetItem is PlaceholderItem) && snippetItem.definition == it) {
                        // replace with plain text, and shift items after
                        val deltaIndex = it.defaultValue.length - snippetItem.text.length
                        items[i] = PlainTextItem(
                            it.defaultValue,
                            snippetItem.startIndex,
                            snippetItem.endIndex + deltaIndex
                        )
                        shiftItemsFrom(i + 1, deltaIndex)
                    }
                }
            }
        }
        // Stage 3: apply selected text, clean useless items and shift all items to editor index
        val items1 = clonedSnippet.items!!
        for (i in 0 until items1.size) {
            val snippetItem = items1[i]
            if (snippetItem is SelectedTextItem) {
                // replace with plain text, and shift items after
                val deltaIndex = selectedText.length
                items1[i] = PlainTextItem(
                    selectedText,
                    snippetItem.startIndex,
                    snippetItem.endIndex + deltaIndex
                )
                shiftItemsFrom(i + 1, deltaIndex)
            }
        }
        val itr = clonedSnippet.items.iterator()
        while (itr.hasNext()) {
            val item = itr.next()
            if ((item is PlaceholderItem && item.text.isEmpty()) || (item is PlainTextItem && item.text.isEmpty())) {
                itr.remove()
            }
        }
        shiftItemsFrom(0, index)
        // Stage 4: make correct indentation
        val text = editor.text
        val pos = text.indexer.getCharPosition(index)
        val line = text.getLine(pos.line)
        var indentEnd = 0
        for (i in 0 until pos.column) {
            if (line.value[i] == ' ' || line.value[i] == '\t') {
                indentEnd++;
            } else {
                break
            }
        }
        val indentText = line.substring(0, indentEnd)
        val items = clonedSnippet.items
        for (i in 0 until items.size) {
            val snippetItem = items[i]
            if (snippetItem is PlainTextItem && snippetItem.text.contains(lineSeparatorRegex)) {
                var first = true
                val sb = StringBuilder()

                snippetItem.text.split(lineSeparatorRegex).forEach {
                    if (first) {
                        sb.append(it)
                        first = false
                    } else {
                        sb.append(editor.lineSeparator.content)
                            .append(indentText)
                            .append(it)
                    }
                }
                val deltaIndex = sb.length - snippetItem.text.length
                snippetItem.text = sb.toString()
                snippetItem.setIndex(snippetItem.startIndex, snippetItem.endIndex + deltaIndex)
                shiftItemsFrom(i + 1, deltaIndex)
            }
        }
        // Stage 5: collect tab stops and placeholders
        val tabStops = mutableListOf<SnippetItem>()
        clonedSnippet.items.forEach { item ->
            if (item is TabStopItem) {
                if (tabStops.find { it is TabStopItem && it.ordinal == item.ordinal } == null) {
                    tabStops.add(item)
                }
            } else if (item is PlaceholderItem) {
                if (tabStops.find { it is PlaceholderItem && it.definition == item.definition } == null) {
                    tabStops.add(item)
                }
            }
        }
        val end = clonedSnippet.items.find { it is SelectionEndItem } ?: SelectionEndItem(
            clonedSnippet.items.last().endIndex
        )
        tabStops.add(end)
        this.tabStops = tabStops
        // Stage 6: insert the text
        val sb = StringBuilder()
        clonedSnippet.items.forEach {
            if (it is PlainTextItem) {
                sb.append(it.text)
            } else if (it is PlaceholderItem) {
                sb.append(it.text)
            }
        }
        text.insert(pos.line, pos.column, sb)
        // Stage 7: shift to the first tab stop
        shiftToTabStop(0)
    }

    fun isInSnippet() = snippetIndex != -1 && currentTabStopIndex != -1

    fun getEditingTabStop() = if (snippetIndex == -1) null else tabStops!![currentTabStopIndex]

    fun getEditingRelatedTabStops(): List<SnippetItem> {
        val editing = getEditingTabStop()
        if (editing != null) {
            if (editing is TabStopItem) {
                return currentSnippet!!.items!!.filter { it is TabStopItem && it.ordinal == editing.ordinal && it != editing }
            } else if (editing is PlaceholderItem) {
                return currentSnippet!!.items!!.filter { it is PlaceholderItem && it.definition == editing.definition && it != editing }
            }
        }
        return emptyList()
    }

    fun isEditingRelated(it: SnippetItem): Boolean {
        val editing = getEditingTabStop()
        if (editing != null) {
            if (editing is TabStopItem) {
                return it is TabStopItem && it.ordinal == editing.ordinal && it != editing
            } else if (editing is PlaceholderItem) {
                return it is PlaceholderItem && it.definition == editing.definition && it != editing
            }
        }
        return false
    }

    fun getInactiveTabStops(): List<SnippetItem> {
        val editing = getEditingTabStop()
        if (editing != null) {
            return if (editing is TabStopItem) {
                currentSnippet!!.items!!.filter {
                    (it is PlaceholderItem) || (it is TabStopItem && it.ordinal != editing.ordinal)
                }
            } else if (editing is PlaceholderItem) {
                currentSnippet!!.items!!.filter { (it is TabStopItem) || (it is PlaceholderItem && it.definition != editing.definition) }
            } else {
                emptyList()
            }
        }
        return emptyList()
    }

    fun shiftToPreviousTabStop() {
        if (snippetIndex != -1 && currentTabStopIndex > 0) {
            shiftToTabStop(--currentTabStopIndex)
        }
    }

    fun shiftToNextTabStop() {
        if (snippetIndex != -1 && currentTabStopIndex < tabStops!!.size - 1) {
            shiftToTabStop(++currentTabStopIndex)
        }
    }

    private fun shiftToTabStop(index: Int) {
        if (snippetIndex == -1) {
            return
        }
        val tabStop = tabStops!![index]
        val indexer = editor.text.indexer
        val left = indexer.getCharPosition(tabStop.startIndex)
        val right = indexer.getCharPosition(tabStop.endIndex)
        currentTabStopIndex = index
        editor.setSelectionRegion(left.line, left.column, right.line, right.column)
        if (index == tabStops!!.size - 1) {
            stopSnippet()
        }
    }

    private fun shiftItemsFrom(itemIndex: Int, deltaIndex: Int) {
        val items = currentSnippet!!.items!!
        for (i in itemIndex until items.size) {
            items[i].shiftIndex(deltaIndex)
        }
    }

    fun stopSnippet() {
        currentSnippet = null
        snippetIndex = -1
        tabStops = null
        currentTabStopIndex = -1
        editor.invalidate()
    }

    companion object {
        val lineSeparatorRegex = Regex("\\r|\\n|\\r\\n")
    }


}