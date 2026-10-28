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
package io.github.rosemoe.sora.widget;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.StyleReceiver;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.brackets.BracketsProvider;
import io.github.rosemoe.sora.lang.brackets.PairedBracket;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticProvider;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.styling.HighlightTextProvider;
import io.github.rosemoe.sora.lang.styling.HighlightTextContainer;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintProvider;
import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer;

public class EditorStyleDelegate implements StyleReceiver, InlayHintProvider, DiagnosticProvider, HighlightTextProvider {

    private final WeakReference<CodeEditor> editorRef;
    private PairedBracket foundPair;
    private BracketsProvider bracketsProvider;
    private DiagnosticsContainer diagnostics;
    private InlayHintsContainer inlayHints;
    private HighlightTextContainer highlightTexts;

    EditorStyleDelegate(@NonNull CodeEditor editor) {
        editorRef = new WeakReference<>(editor);
        editor.subscribeEvent(SelectionChangeEvent.class, (event, __) -> {
            if (!event.isSelected()) {
                postUpdateBracketPair();
            }
        });
    }

    void onTextChange() {
        //  Should we do this?
        //bracketsProvider = null;
        //foundPair = null;
    }

    void postUpdateBracketPair() {
        runOnUiThread(() -> {
            final var provider = bracketsProvider;
            final var editor = editorRef.get();
            if (provider != null && editor != null && !editor.getCursor().isSelected() && editor.isHighlightBracketPair()) {
                foundPair = provider.getPairedBracketAt(editor.getText(), editor.getCursor().getLeft());
                editor.invalidate();
            }
        });
    }

    @Nullable
    public PairedBracket getFoundBracketPair() {
        return foundPair;
    }

    void reset() {
        foundPair = null;
        bracketsProvider = null;
        diagnostics = null;
        inlayHints = null;
        highlightTexts = null;
    }

    private void runOnUiThread(Runnable operation) {
        var editor = editorRef.get();
        if (editor == null) {
            return;
        }
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            operation.run();
        } else {
            editor.postInLifecycle(operation);
        }
    }

    @Override
    public void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles) {
        setStyles(sourceManager, styles, null);
    }

    @Override
    public void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles, @Nullable Runnable action) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> {
                if (action != null) {
                    action.run();
                }
                editor.setStyles(styles);
            });
        }
    }

    @Override
    public void setDiagnostics(@NonNull AnalyzeManager sourceManager, @Nullable DiagnosticsContainer diagnostics) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            this.diagnostics = diagnostics;
            runOnUiThread(editor::invalidateDiagnostics);
        }
    }

    @Override
    public void setInlayHints(@NonNull AnalyzeManager sourceManager, @Nullable InlayHintsContainer inlayHints) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            this.inlayHints = inlayHints;
            runOnUiThread(editor::invalidateInlayHints);
        }
    }

    public void setHighlightTexts(@NonNull AnalyzeManager sourceManager, @Nullable HighlightTextContainer highlightTexts) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            this.highlightTexts = highlightTexts;
            runOnUiThread(editor::invalidateHighlightTexts);
        }
    }

    @Override
    public void provideInlayHints(@NonNull InlayHintsContainer container) {
        if (inlayHints != null) {
            container.addAll(inlayHints);
        }
    }

    @Override
    public void provideDiagnostics(@NonNull DiagnosticsContainer container) {
        if (diagnostics != null) {
            container.addDiagnostics(diagnostics.getRegions());
        }
    }

    @Override
    public void provideHighlightTexts(@NonNull HighlightTextContainer container) {
        if (highlightTexts != null) {
            container.addAll(highlightTexts.asList());
        }
    }

    @Override
    public void updateBracketProvider(@NonNull AnalyzeManager sourceManager, @Nullable BracketsProvider provider) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager() && bracketsProvider != provider) {
            this.bracketsProvider = provider;
            postUpdateBracketPair();
        }
    }

    @Override
    public void updateStyles(@NonNull AnalyzeManager sourceManager, @NonNull Styles styles, @NonNull StyleUpdateRange range) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> editor.updateStyles(styles, range));
        }
    }

    public void clearFoundBracketPair() {
        this.foundPair = null;
    }
}
