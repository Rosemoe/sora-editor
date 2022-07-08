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
package io.github.rosemoe.sora.lsp.operations.diagnostics;

import android.util.Log;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.operations.Feature;
import io.github.rosemoe.sora.widget.CodeEditor;

public class PublishDiagnosticsFeature implements Feature<PublishDiagnosticsParams, Void> {


    private LspEditor editor;


    @Override
    public void install(LspEditor editor) {
        this.editor = editor;
    }


    @Override
    public void uninstall(LspEditor editor) {
        this.editor = null;
    }

    private int getIndexForPosition(Position position) {
        CodeEditor currentEditor = editor.getEditor();

        if (currentEditor == null) {
            return 0;
        }

        return currentEditor.getText()
                .getCharIndex(position.getLine(), position.getCharacter());
    }


    @Override
    public Void execute(PublishDiagnosticsParams data) {

        CodeEditor currentEditor = editor.getEditor();

        if (currentEditor == null) {
            return null;
        }

        DiagnosticsContainer diagnosticsContainer = currentEditor.getDiagnostics() != null
                ? currentEditor.getDiagnostics() : new DiagnosticsContainer();

        diagnosticsContainer.reset();

        AtomicInteger id = new AtomicInteger();


        List<DiagnosticRegion> diagnosticRegionList = data
                .getDiagnostics()
                .stream()
                .map(it -> {
                            Log.w("diagnostic message", "diagnostic: " + it.getMessage());
                            return new DiagnosticRegion(
                                    getIndexForPosition(it.getRange().getStart()),
                                    getIndexForPosition(it.getRange().getEnd()),
                                    transformToEditorDiagnosticSeverity(it.getSeverity()), id.incrementAndGet());
                        }
                )
                .collect(Collectors.toList());

        diagnosticsContainer.addDiagnostics(diagnosticRegionList);

        currentEditor.setDiagnostics(diagnosticsContainer);

        return null;
    }

    private short transformToEditorDiagnosticSeverity(DiagnosticSeverity severity) {
        switch (severity) {
            case Hint:
            case Information:
                return DiagnosticRegion.SEVERITY_TYPO;
            case Error:
                return DiagnosticRegion.SEVERITY_ERROR;
            case Warning:
                return DiagnosticRegion.SEVERITY_WARNING;
        }
        return 0;
    }
}
