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
package io.github.rosemoe.sora.lsp.operations.document;

import androidx.annotation.Nullable;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentSyncKind;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.Feature;
import io.github.rosemoe.sora.lsp.requests.Timeout;
import io.github.rosemoe.sora.lsp.requests.Timeouts;
import io.github.rosemoe.sora.lsp.utils.LspUtils;

public class DocumentChangeFeature implements Feature<ContentChangeEvent, Void> {

    private CompletableFuture<Void> future;
    private LspEditor editor;


    @Override
    public void install(LspEditor editor) {
        this.editor = editor;
    }

    @Override
    public void uninstall(LspEditor editor) {
        this.editor = null;
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }


    @Nullable
    public CompletableFuture<Void> getFuture() {
        return future;
    }

    @Override
    public Void execute(ContentChangeEvent data) {

        DidChangeTextDocumentParams params = createDidChangeTextDocumentParams(data);

        editor.getRequestManagerOfOptional()
                .ifPresent(requestManager -> {
                    future = CompletableFuture.runAsync(() -> requestManager.didChange(
                            params));

                    ForkJoinPool.commonPool().execute(() -> {
                                future.join();
                                future = null;
                            }
                    );

                });


        return null;
    }

    private List<TextDocumentContentChangeEvent> createFullTextDocumentContentChangeEvent() {
        return List.of(LspUtils.createTextDocumentContentChangeEvent(
                editor.getEditorContent()
        ));
    }

    private List<TextDocumentContentChangeEvent> createIncrementTextDocumentContentChangeEvent(ContentChangeEvent data) {
        String text = data.getChangedText().toString();
        return List.of(
                LspUtils.createTextDocumentContentChangeEvent(
                        LspUtils.createRange(data.getChangeStart(), data.getChangeEnd()),
                        data.getAction() == ContentChangeEvent.ACTION_DELETE ? text.length() : 0,
                        data.getAction() == ContentChangeEvent.ACTION_DELETE ? "" : text
                )
        );
    }


    private DidChangeTextDocumentParams createDidChangeTextDocumentParams(ContentChangeEvent data) {
        TextDocumentSyncKind kind = editor.getSyncOptions();

        boolean isFullSync = kind == TextDocumentSyncKind.None || kind == TextDocumentSyncKind.Full;

        return LspUtils
                .createDidChangeTextDocumentParams(editor.getCurrentFileUri(),
                        isFullSync ? createFullTextDocumentContentChangeEvent() : createIncrementTextDocumentContentChangeEvent(data));
    }
}
