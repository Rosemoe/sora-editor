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
package io.github.rosemoe.sora.lsp.operations.diagnostics;

import org.eclipse.lsp4j.DocumentDiagnosticParams;
import org.eclipse.lsp4j.FullDocumentDiagnosticReport;
import org.eclipse.lsp4j.UnchangedDocumentDiagnosticReport;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.Provider;
import io.github.rosemoe.sora.lsp.utils.LspUtils;

//TODO: cross-document diagnostic containers
public class QueryDocumentDiagnosticsProvider implements Provider<DocumentDiagnosticParams, CompletableFuture<Map<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>>>> {

    private volatile CompletableFuture<Map<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>>> future;

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
    public CompletableFuture<Map<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>>> execute(DocumentDiagnosticParams params) {
        var manager = editor.getRequestManager();

        if (manager == null) {
            return null;
        }

        future = editor.getRequestManager()
                .diagnostic(
                        LspUtils.createDocumentDiagnosticParams(
                                editor.getCurrentFileUri())
                ).thenApply(either ->
                        either.isRelatedFullDocumentDiagnosticReport()
                                ? either.getRelatedFullDocumentDiagnosticReport().getRelatedDocuments()
                                : either.getRelatedUnchangedDocumentDiagnosticReport().getRelatedDocuments()

                );

        return future;

    }
}
