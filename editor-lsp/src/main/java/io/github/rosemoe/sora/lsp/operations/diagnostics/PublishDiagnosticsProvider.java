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

import android.util.Log;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.RunOnlyProvider;
import io.github.rosemoe.sora.lsp.utils.LspUtils;

public class PublishDiagnosticsProvider extends RunOnlyProvider<List<Diagnostic>> {

    private LspEditor editor;

    @Override
    public void init(LspEditor editor) {
        this.editor = editor;
    }

    @Override
    public void dispose(LspEditor editor) {
        this.editor = null;
    }


    @Override
    public void run(List<Diagnostic> data) {

        var currentEditor = editor.getEditor();

        if (currentEditor == null) {
            return;
        }

        var diagnosticsContainer = currentEditor.getDiagnostics() != null ? currentEditor.getDiagnostics() : new DiagnosticsContainer();

        diagnosticsContainer.reset();

        diagnosticsContainer.addDiagnostics(
                LspUtils.transformToEditorDiagnostics(editor.getEditor(),data)
        );

        currentEditor.setDiagnostics(diagnosticsContainer);

    }


}
