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
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

import io.github.rosemoe.sora.event.ColorSchemeUpdateEvent;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.StyleReceiver;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.lang.brackets.BracketsProvider;
import io.github.rosemoe.sora.lang.brackets.PairedBracket;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.styling.Styles;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class EditorStyleDelegate implements StyleReceiver {

    private final WeakReference<CodeEditor> editorRef;
    private PairedBracket foundPair;
    private BracketsProvider bracketsProvider;

    // Maybe add can add to editor color scheme?
    private final SparseIntArray collectedBracketPairColors = new SparseIntArray();

    EditorStyleDelegate(@NonNull CodeEditor editor) {
        editorRef = new WeakReference<>(editor);
        editor.subscribeEvent(SelectionChangeEvent.class, (event, __) -> {
            if (!event.isSelected()) {
                postUpdateBracketPair();
            }
        });
        editor.subscribeEvent(ColorSchemeUpdateEvent.class, (event, __) -> {
            computedBracketPairColors();
            editor.invalidate();
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
        final var provider = bracketsProvider;
        if (provider == null) {
            return null;
        }
        return foundPair;
    }

    @Nullable
    public List<PairedBracket> queryPairedBracketsForRange(@NonNull Content text, long leftRange, long rightRange) {
        final var provider = bracketsProvider;

        if (provider == null) {
            return null;
        }

        return provider.queryPairedBracketsForRange(text, leftRange, rightRange);
    }

    public SparseIntArray getCollectedBracketPairColors() {
        return collectedBracketPairColors;
    }

    void reset() {
        foundPair = null;
        bracketsProvider = null;
    }

    private void computedBracketPairColors() {
        final var editor = editorRef.get();

        if (editor == null) {
            return;
        }

        var availableColors = new SparseIntArray();

        for (var index = EditorColorScheme.BRACKET_HIGHLIGHTING_FOREGROUND_1; index <= EditorColorScheme.BRACKET_HIGHLIGHTING_FOREGROUND_6; index++) {
            var color = editor.getColorScheme().getColor(index);

            if (color == 0) continue;
            availableColors.append(availableColors.size(), color);
        }

        if (availableColors.size() < 1) {
            return;
        }

        for (var index = 0; index <= 30; index++) {
            collectedBracketPairColors.put(index, availableColors.get(index % availableColors.size()));
        }
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
            runOnUiThread(() -> editor.setDiagnostics(diagnostics));
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
