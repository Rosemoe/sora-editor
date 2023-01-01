 /*
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
 */
 package io.github.rosemoe.sora.lsp.editor.completion;

 import android.util.Pair;

 import androidx.annotation.NonNull;

 import org.eclipse.lsp4j.InsertTextFormat;
 import org.eclipse.lsp4j.TextEdit;

 import java.util.List;

 import io.github.rosemoe.sora.lang.completion.CompletionItem;
 import io.github.rosemoe.sora.lang.completion.CompletionItemKind;
 import io.github.rosemoe.sora.lang.completion.SimpleCompletionIconDrawer;
 import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser;
 import io.github.rosemoe.sora.lsp.operations.document.ApplyEditsProvider;
 import io.github.rosemoe.sora.lsp.utils.LspUtils;
 import io.github.rosemoe.sora.text.CharPosition;
 import io.github.rosemoe.sora.text.Content;
 import io.github.rosemoe.sora.widget.CodeEditor;

 public class LspCompletionItem extends CompletionItem {

     private org.eclipse.lsp4j.CompletionItem commitItem;

     private ApplyEditsProvider applyEditsFeature;


     public LspCompletionItem(org.eclipse.lsp4j.CompletionItem completionItem, ApplyEditsProvider applyEditsFeature, int prefixLength) {
         super(completionItem.getLabel(), completionItem.getDetail());
         this.commitItem = completionItem;
         this.prefixLength = prefixLength;
         this.applyEditsFeature = applyEditsFeature;
         this.kind = completionItem.getKind() == null ? CompletionItemKind.Text : CompletionItemKind.valueOf(completionItem.getKind().name());
         this.sortText = completionItem.getSortText();
         var labelDetails = commitItem.getLabelDetails();
         if (labelDetails != null && labelDetails.getDescription() != null) {
             this.desc = labelDetails.getDescription();
         }
         this.icon = SimpleCompletionIconDrawer.draw(kind);
     }


     @Override
     public void performCompletion(@NonNull CodeEditor editor, @NonNull Content text, CharPosition position) {

         var textEdit = new TextEdit();


         textEdit.setRange(LspUtils.createRange(LspUtils.createPosition(position.line, position.column - prefixLength), LspUtils.createPosition(position)));

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
                     text.getLineCount() - 1, text.getColumnCount(Math.max(0, position.line - 1))
             );
             var textEditEnd = textEdit.getRange().getEnd();
             if (documentEnd.getLine() < textEditEnd.getLine()
                     || (documentEnd.getLine() == textEditEnd.getLine() && documentEnd.getCharacter() < textEditEnd.getCharacter())) {
                 textEdit.getRange().setEnd(documentEnd);
             }
         }


         var finalTextEdit = textEdit;

         Runnable runnable = () -> applyEditsFeature.execute(new Pair<>(List.of(finalTextEdit), text));

         if (commitItem.getInsertTextFormat() == InsertTextFormat.Snippet) {
             var codeSnippet = CodeSnippetParser.parse(textEdit.getNewText());
             var startIndex = text.getCharIndex(textEdit.getRange().getStart().getLine(), textEdit.getRange().getStart().getCharacter());
             var endIndex = text.getCharIndex(textEdit.getRange().getEnd().getLine(), textEdit.getRange().getEnd().getCharacter());
             var selectedText = text.subSequence(startIndex, endIndex).toString();

             text.delete(startIndex, endIndex);
             runnable = () -> editor.getSnippetController()
                     .startSnippet(startIndex, codeSnippet, selectedText);
         }

         runnable.run();

         if (commitItem.getAdditionalTextEdits() != null) {
             applyEditsFeature.execute(new Pair<>(commitItem.getAdditionalTextEdits(), text));
         }


     }

     @Override
     public void performCompletion(@NonNull CodeEditor editor, @NonNull Content text, int line, int column) {
         // do nothing
     }


 }

