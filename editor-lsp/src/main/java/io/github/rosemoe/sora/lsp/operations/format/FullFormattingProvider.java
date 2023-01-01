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
package io.github.rosemoe.sora.lsp.operations.format;

import android.util.Pair;

import androidx.annotation.WorkerThread;

import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.RunOnlyProvider;
import io.github.rosemoe.sora.lsp.operations.document.ApplyEditsProvider;
import io.github.rosemoe.sora.lsp.requests.Timeout;
import io.github.rosemoe.sora.lsp.requests.Timeouts;
import io.github.rosemoe.sora.lsp.utils.LSPException;
import io.github.rosemoe.sora.lsp.utils.LspUtils;
import io.github.rosemoe.sora.text.Content;

public class FullFormattingProvider extends RunOnlyProvider<Content> {

    private CompletableFuture<Void> future;
    private LspEditor editor;

    @Override
    public void init(LspEditor editor) {
        this.editor = editor;
    }

    @Override
    public void dispose(LspEditor editor) {
        this.editor = null;
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    @Override
    @WorkerThread
    public void run(Content content) {
        RequestManager manager = editor.getRequestManager();

        if (manager == null) {
            return;
        }

        var formattingParams = new DocumentFormattingParams();
        formattingParams.setOptions(editor.getProviderManager().getOption(FormattingOptions.class));

        formattingParams.setTextDocument(LspUtils.createTextDocumentIdentifier(editor.getCurrentFileUri()));

        var formattingFuture = manager.formatting(formattingParams);

        if (formattingFuture == null) {
            future = CompletableFuture.completedFuture(null);
            return;
        }

        future = formattingFuture.thenApply(list -> Optional.ofNullable(list).orElse(List.of())).thenAccept(list -> {
            editor.getProviderManager().safeUseProvider(ApplyEditsProvider.class)
                    .ifPresent(applyEditsFeature -> applyEditsFeature
                            .execute(new Pair<>(list, content)));
        });


        try {
            future.get(Timeout.getTimeout(Timeouts.FORMATTING), TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            throw new LSPException("Formatting code timeout");
        }
    }


}
