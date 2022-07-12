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
package io.github.rosemoe.sora.lsp.editor;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.rosemoe.sora.annotations.Experimental;
import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException;
import io.github.rosemoe.sora.lang.completion.CompletionItem;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.format.Formatter;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.lsp.editor.completion.LspCompletionItem;
import io.github.rosemoe.sora.lsp.editor.format.LspFormatter;
import io.github.rosemoe.sora.lsp.operations.completion.CompletionFeature;
import io.github.rosemoe.sora.lsp.operations.document.DocumentChangeFeature;
import io.github.rosemoe.sora.lsp.requests.Timeout;
import io.github.rosemoe.sora.lsp.requests.Timeouts;
import io.github.rosemoe.sora.lsp.utils.LSPException;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

@Experimental
public class LspLanguage implements Language {

    private LspEditor currentEditor;

    private LspFormatter lspFormatter;

    private Language wrapperLanguage = null;

    public LspLanguage(LspEditor editor) {
        this.currentEditor = editor;
        this.lspFormatter = new LspFormatter(this);
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

        publisher.setComparator(new Comparator<>() {
            @Override
            public int compare(CompletionItem o1, CompletionItem o2) {
                if (o1 instanceof LspCompletionItem && o2 instanceof LspCompletionItem) {
                    return ((LspCompletionItem) o1).compareTo(((LspCompletionItem) o2));
                }
                return 0;
            }
        });

        CompletableFuture<Void> documentChangeFuture = currentEditor.useFeature(DocumentChangeFeature.class).getFuture();

        if (documentChangeFuture != null) {
            if (!documentChangeFuture.isDone() || !documentChangeFuture.isCompletedExceptionally() || !documentChangeFuture.isCancelled()) {
                documentChangeFuture.join();
            }
        }

        try {

            currentEditor
                    .useFeature(CompletionFeature.class)
                    .execute(position)
                    .thenAccept(completions -> {
                        completions
                                .forEach(completionItem -> {
                                    publisher.addItem(new LspCompletionItem(
                                            completionItem
                                    ));
                                });

                    })
                    .exceptionally(throwable -> {
                        publisher.cancel();
                        throw new CompletionCancelledException(throwable.getMessage());
                    })
                    .get(Timeout.getTimeout(Timeouts.COMPLETION), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new LSPException(e);
        }

        publisher.updateList();


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
}
