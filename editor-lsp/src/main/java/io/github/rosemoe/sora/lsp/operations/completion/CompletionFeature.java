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
package io.github.rosemoe.sora.lsp.operations.completion;

import org.eclipse.lsp4j.CompletionItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.Feature;
import io.github.rosemoe.sora.lsp.utils.LspUtils;
import io.github.rosemoe.sora.text.CharPosition;

public class CompletionFeature implements Feature<CharPosition, CompletableFuture<List<CompletionItem>>> {

    private CompletableFuture<List<CompletionItem>> future;
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


    @Override
    public CompletableFuture<List<CompletionItem>> execute(CharPosition data) {
        if (future != null) {
            future.cancel(true);
            future = null;
        }

        RequestManager manager = editor.getRequestManager();

        if (manager == null) {
            return null;
        }

        future = editor.getRequestManager().completion(
                LspUtils.createCompletionParams(
                        editor.getCurrentFileUri(),
                        LspUtils.createPosition(data)
                )
        ).thenApply(listCompletionListEither ->
                listCompletionListEither.isLeft() ? listCompletionListEither.getLeft() :
                        listCompletionListEither.getRight().getItems());


        return future;
    }
}
