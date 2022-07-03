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
package io.github.rosemoe.sora.lsp.operations.format;

import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import java.util.concurrent.CompletableFuture;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.Feature;

public class LspFormattingFeature implements Feature<String, String> {

    private CompletableFuture<String> future;
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
        }
    }

    @Override
    public String execute(String uri) {
        RequestManager manager = editor.getRequestManager();
        DocumentFormattingParams formattingParams = new DocumentFormattingParams();

        formattingParams.setOptions(editor.getOption(FormattingOptions.class));
        formattingParams.setTextDocument(new TextDocumentIdentifier(uri));
        future = manager.formatting(formattingParams)
                .thenApply(list -> {
                    //get 0
                    return list.get(0).getNewText();
                });

        //block
        return future.join();
    }

}
