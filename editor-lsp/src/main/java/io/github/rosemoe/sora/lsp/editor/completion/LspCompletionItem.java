/*
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
 */
package io.github.rosemoe.sora.lsp.editor.completion;

import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.TextEdit;

import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.CodeEditor;

public class LspCompletionItem extends CompletionItem implements Comparable<LspCompletionItem> {


    private final org.eclipse.lsp4j.CompletionItem commitItem;

    public LspCompletionItem(org.eclipse.lsp4j.CompletionItem completionItem) {
        super(completionItem.getLabel(), completionItem.getDetail());
        this.commitItem = completionItem;
    }

    @Override
    public void performCompletion(CodeEditor editor, Content text, int line, int column) {

        var insertText = commitItem.getInsertText();


        //var cursor = text.getCursor();

        if (insertText != null) {
            //TODO: Support InsertTextFormat.Snippet

            //always insert text
            text.insert(line, column, insertText);
            return;
        }

        if (commitItem.getTextEdit() != null) {

            //TODO: support InsertReplaceEdit

            if (commitItem.getTextEdit().isLeft()) {

                var textEdit = commitItem.getTextEdit().getLeft();
                String commitText = textEdit.getNewText();
                text.replace(textEdit.getRange().getStart().getLine(), textEdit.getRange().getStart().getCharacter(),
                        textEdit.getRange().getEnd().getLine(), textEdit.getRange().getEnd().getCharacter(),
                        commitText);
            }

            return;

        }

        insertText = commitItem.getLabel();

        text.replace(text.getCharIndex(line, column), text.getCharIndex(line,
                text.getColumnCount(line) - 1), insertText);


    }


    @Override
    public int compareTo(LspCompletionItem completionItem) {
        if (commitItem.getSortText() != null && completionItem.commitItem.getSortText() != null) {
            return commitItem.getSortText().compareTo(completionItem.commitItem.getSortText());
        }
        return commitItem.getLabel().compareTo(completionItem.commitItem.getLabel());
    }
}

