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
package io.github.rosemoe.sora.lsp.editor;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.Comparators;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lsp.editor.completion.CompletionItemProvider;
import io.github.rosemoe.sora.lsp.editor.completion.LspCompletionItem;
import io.github.rosemoe.sora.lsp.editor.format.LspFormatter;
import io.github.rosemoe.sora.lsp.operations.completion.CompletionProvider;
import io.github.rosemoe.sora.lsp.operations.document.ApplyEditsProvider;
import io.github.rosemoe.sora.lsp.operations.document.DocumentChangeProvider;
import io.github.rosemoe.sora.lsp.requests.Timeout;
import io.github.rosemoe.sora.lsp.requests.Timeouts;
import io.github.rosemoe.sora.lsp.utils.LSPException;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

@Experimental
public class LspLanguage implements Language {

    private LspEditor currentEditor;

    private LspFormatter lspFormatter;

    private Language wrapperLanguage = null;

    private CompletionItemProvider<?> completionItemProvider;

    public LspLanguage(LspEditor editor) {
        this.currentEditor = editor;
        this.lspFormatter = new LspFormatter(this);

        completionItemProvider = LspCompletionItem::new;

    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return wrapperLanguage != null ? wrapperLanguage.getAnalyzeManager() : EmptyLanguage.EmptyAnalyzeManager.INSTANCE;
    }

    @Override
    public int getInterruptionLevel() {
        return wrapperLanguage != null ? wrapperLanguage.getInterruptionLevel() : 0;
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content, @NonNull CharPosition position, @NonNull CompletionPublisher publisher, @NonNull Bundle extraArguments) throws CompletionCancelledException {

       /* if (getEditor().hitTrigger(line)) {
            publisher.cancel();
            return;
        }*/

        var prefix = computePrefix(content, position);
        // Log.d("prefix", prefix);

        var prefixLength = prefix.length();

        currentEditor.getProviderManager().safeUseProvider(DocumentChangeProvider.class).ifPresent(documentChangeFeature -> {
            var documentChangeFuture = documentChangeFeature.getFuture();
            if (!documentChangeFuture.isDone() || !documentChangeFuture.isCompletedExceptionally() || !documentChangeFuture.isCancelled()) {
                try {
                    documentChangeFuture.get(1000, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | InterruptedException | TimeoutException ignored) {

                }
            }
        });

        var completionList = new ArrayList<CompletionItem>();

        try {
            var completionFeature = currentEditor.getProviderManager().useProvider(CompletionProvider.class);

            if (completionFeature == null) {
                return;
            }

            completionFeature.execute(position).thenAccept(completions -> completions.forEach(completionItem -> completionList.add(completionItemProvider.createCompletionItem(completionItem, currentEditor.getProviderManager().useProvider(ApplyEditsProvider.class), prefixLength)))).exceptionally(throwable -> {
                publisher.cancel();
                throw new CompletionCancelledException(throwable.getMessage());
            }).get(Timeout.getTimeout(Timeouts.COMPLETION), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new LSPException(e);
        }


        publisher.setComparator(Comparators.getCompletionItemComparator(content, position, completionList));

        publisher.addItems(completionList);

        publisher.updateList();
    }


    public String computePrefix(ContentReference text, CharPosition position) {

        List<String> delimiters = new ArrayList<>(currentEditor.getCompletionTriggers());

        if (delimiters.isEmpty()) {
            return CompletionHelper.computePrefix(text, position, MyCharacter::isJavaIdentifierPart);
        }

        // add whitespace as delimiter, otherwise forced completion does not work
        delimiters.addAll(Arrays.asList(" \t\n\r".split("")));

        var offset = position.index;

        StringBuilder s = new StringBuilder();

        for (int i = 0; i < offset; i++) {
            char singleLetter = text.charAt(offset - i - 1);
            if (delimiters.contains(String.valueOf(singleLetter))) {
                return s.reverse().toString();
            }
            s.append(singleLetter);
        }
        return "";
    }

    @Override
    public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
        return wrapperLanguage != null ? wrapperLanguage.getInterruptionLevel() : 0;
    }

    @Override
    public boolean useTab() {
        return wrapperLanguage != null && wrapperLanguage.useTab();
    }

    @NonNull
    @Override
    public Formatter getFormatter() {
        if (lspFormatter == null) {
            return EmptyLanguage.EmptyFormatter.INSTANCE;
        }
        return lspFormatter;
    }


    @Override
    public SymbolPairMatch getSymbolPairs() {
        return wrapperLanguage != null ? wrapperLanguage.getSymbolPairs() : EmptyLanguage.EMPTY_SYMBOL_PAIRS;
    }

    @Nullable
    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return wrapperLanguage != null ? wrapperLanguage.getNewlineHandlers() : new NewlineHandler[0];
    }


    @Override
    public void destroy() {

        getFormatter().destroy();

        if (wrapperLanguage != null) {
            wrapperLanguage.destroy();
        }

        currentEditor.close();

        currentEditor = null;
        lspFormatter = null;


    }

    public LspEditor getEditor() {
        return currentEditor;
    }


    @Nullable
    public <T extends Language> T getWrapperLanguage() {
        return (T) wrapperLanguage;
    }

    public void setWrapperLanguage(Language wrapperLanguage) {
        this.wrapperLanguage = wrapperLanguage;
    }


    @Nullable
    public <T extends CompletionItem> CompletionItemProvider<T> getCompletionItemProvider() {
        return (CompletionItemProvider<T>) completionItemProvider;
    }

    public void setCompletionItemProvider(CompletionItemProvider<?> completionItemProvider) {
        this.completionItemProvider = completionItemProvider;
    }


}
