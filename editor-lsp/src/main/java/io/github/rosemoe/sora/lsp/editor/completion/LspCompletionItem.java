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

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.TextEdit;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lsp.operations.document.ApplyEditsFeature;
import io.github.rosemoe.sora.lsp.utils.LspUtils;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.CodeEditor;

public class LspCompletionItem extends CompletionItem implements Comparable<LspCompletionItem> {

    private org.eclipse.lsp4j.CompletionItem commitItem;
    private final int prefixLength;

    private ApplyEditsFeature applyEditsFeature;

    private static final String SNIPPET_PLACEHOLDER_REGEX_1 = "\\$\\{\\d+:?([^{^}]*)\\}";

    private static final String SNIPPET_PLACEHOLDER_REGEX_2 = "\\$\\d+";

    public LspCompletionItem(org.eclipse.lsp4j.CompletionItem completionItem, ApplyEditsFeature applyEditsFeature, int prefixLength) {
        super(completionItem.getLabel(), completionItem.getDetail());
        this.commitItem = completionItem;
        this.prefixLength = prefixLength;
        this.applyEditsFeature = applyEditsFeature;
    }

    @Override
    public void performCompletion(@NonNull CodeEditor editor, @NonNull Content text, int line, int column) {

        var textEdit = new TextEdit();

        var cursorPosition = LspUtils.createPosition(line, column);

        textEdit.setRange(LspUtils.createRange(LspUtils.createPosition(line, column - prefixLength), LspUtils.createPosition(line, column)));

        if (commitItem.getInsertText() != null) {
            textEdit.setNewText(commitItem.getInsertText());
        }




        if (commitItem.getTextEdit() != null && commitItem.getTextEdit().isLeft()) {

            //TODO: support InsertReplaceEdit
            textEdit = commitItem.getTextEdit().getLeft();

        }

        if (textEdit.getNewText() == null && commitItem.getLabel() != null) {
            textEdit.setNewText(commitItem.getLabel());
        }


        { // workaround https://github.com/Microsoft/vscode/issues/17036
            var start = textEdit.getRange().getStart();
            var end = textEdit.getRange().getEnd();
            if (start.getLine() > end.getLine() || (start.getLine() == end.getLine() && start.getCharacter() > end.getCharacter())) {
                textEdit.getRange().setEnd(start);
                textEdit.getRange().setStart(end);
            }
        }

        { // allow completion items to be wrong with a too wide range
            var documentEnd = LspUtils.createPosition(
                    text.getLineCount() - 1, text.getColumnCount(line - 1)
            );
            var textEditEnd = textEdit.getRange().getEnd();
            if (documentEnd.getLine() < textEditEnd.getLine()
                    || (documentEnd.getLine() == textEditEnd.getLine() && documentEnd.getCharacter() < textEditEnd.getCharacter())) {
                textEdit.getRange().setEnd(documentEnd);
            }
        }


        SnippetVariable firstSnippetVariable = null;

        if (commitItem.getInsertTextFormat() == InsertTextFormat.Snippet) {
            try {
                var variables = new ArrayList<SnippetVariable>();
                // Extracts variables using placeholder REGEX pattern.
                var varMatcher = Pattern.compile(SNIPPET_PLACEHOLDER_REGEX_1).matcher(textEdit.getNewText());
                while (varMatcher.find()) {
                    variables.add(new SnippetVariable(varMatcher.group(), varMatcher.start(), varMatcher.end()));
                }

                varMatcher = Pattern.compile(SNIPPET_PLACEHOLDER_REGEX_2).matcher(textEdit.getNewText());

                while (varMatcher.find()) {
                    variables.add(new SnippetVariable(varMatcher.group(), varMatcher.start(), varMatcher.end()));
                }

                variables.sort(Comparator.comparingInt(o -> o.startIndex));

                firstSnippetVariable = variables.get(0);

                final String[] finalInsertText = {textEdit.getNewText()};
                variables.forEach(var -> finalInsertText[0] = finalInsertText[0].replace(var.snippetText, "$"));

                String[] splitInsertText = finalInsertText[0].split("\\$");


                finalInsertText[0] = String.join("", splitInsertText);

                textEdit.setNewText(finalInsertText[0]);


            } catch (Exception e) {
                Log.e("aaa", "bbb", e);
                throw e;
            }

        }

        applyEditsFeature.execute(new Pair<>(List.of(textEdit), text));


        if (firstSnippetVariable != null) {
            var startPosition = textEdit.getRange().getStart();
            var targetIndex = text.getCharIndex(startPosition.getLine(), startPosition.getCharacter());

            targetIndex += firstSnippetVariable.startIndex;

            cursorPosition = LspUtils.createPosition(text.getIndexer().getCharPosition(targetIndex));

        }


        text.getCursor().set(cursorPosition.getLine(), cursorPosition.getCharacter());

        if (commitItem.getAdditionalTextEdits() != null) {
            applyEditsFeature.execute(new Pair<>(commitItem.getAdditionalTextEdits(), text));
        }


    }

    @Override
    public int compareTo(LspCompletionItem completionItem) {
        if (commitItem.getSortText() != null && completionItem.commitItem.getSortText() != null) {
            return commitItem.getSortText().compareTo(completionItem.commitItem.getSortText());
        }

        return commitItem.getLabel().compareTo(completionItem.commitItem.getLabel());
    }

    static class SnippetVariable {
        String snippetText;
        int startIndex;
        int endIndex;
        String variableValue;


        SnippetVariable(String text, int start, int end) {
            this.snippetText = text;
            this.startIndex = start;
            this.endIndex = end;
            this.variableValue = getVariableValue(text);
        }

        private String getVariableValue(String lspVarSnippet) {
            if (lspVarSnippet.contains(":")) {
                return lspVarSnippet.substring(lspVarSnippet.indexOf(':') + 1, lspVarSnippet.lastIndexOf('}'));
            }
            return " ";
        }

        @Override
        public String toString() {
            return "SnippetVariable{" +
                    "snippetText='" + snippetText + '\'' +
                    ", startIndex=" + startIndex +
                    ", endIndex=" + endIndex +
                    ", variableValue='" + variableValue + '\'' +
                    '}';
        }
    }

}

