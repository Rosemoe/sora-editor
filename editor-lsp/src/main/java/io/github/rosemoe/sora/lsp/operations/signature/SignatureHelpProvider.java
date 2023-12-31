/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.lsp.operations.signature;

import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import org.eclipse.lsp4j.SignatureHelpParams;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.github.rosemoe.sora.lsp.client.languageserver.requestmanager.RequestManager;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.RunOnlyProvider;
import io.github.rosemoe.sora.lsp.requests.Timeout;
import io.github.rosemoe.sora.lsp.requests.Timeouts;
import io.github.rosemoe.sora.lsp.utils.LSPException;
import io.github.rosemoe.sora.lsp.utils.LspUtils;
import io.github.rosemoe.sora.text.CharPosition;

public class SignatureHelpProvider extends RunOnlyProvider<CharPosition> {
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
    public void run(CharPosition position) {
        RequestManager manager = editor.getRequestManager();

        if (manager == null) {
            return;
        }

        var signatureHelpParams = new SignatureHelpParams(
                LspUtils.createTextDocumentIdentifier(editor.getCurrentFileUri()),
                LspUtils.createPosition(position)
        );

        var future = manager.signatureHelp(signatureHelpParams);


        future = future.thenApply(signatureHelp -> {
            System.out.println(new Gson().toJson(signatureHelp));
            return signatureHelp;
        });


        try {
            var signatureHelp = future.get(Timeout.getTimeout(Timeouts.SIGNATURE), TimeUnit.MILLISECONDS);
            editor.showSignatureHelp(signatureHelp);
        } catch (Exception exception) {
            // throw?
            exception.printStackTrace();
            Log.e("LSP client","show signatureHelp timeout");
        }
    }
}
