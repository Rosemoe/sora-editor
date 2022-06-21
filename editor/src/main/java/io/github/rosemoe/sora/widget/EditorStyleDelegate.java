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
package io.github.rosemoe.sora.widget;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.analysis.StyleReceiver;
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.rosemoe.sora.lang.styling.Styles;

public class EditorStyleDelegate implements StyleReceiver {

    private final WeakReference<CodeEditor> editorRef;

    EditorStyleDelegate(@NonNull CodeEditor editor) {
        editorRef = new WeakReference<>(editor);
    }

    private void runOnUiThread(Runnable operation) {
        var editor = editorRef.get();
        if (editor == null) {
            return;
        }
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            operation.run();
        } else {
            editor.post(operation);
        }
    }

    @Override
    public void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> editor.setStyles(styles));
        }
    }

    @Override
    public void setDiagnostics(@NonNull AnalyzeManager sourceManager, @Nullable DiagnosticsContainer diagnostics) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> editor.setDiagnostics(diagnostics));
        }
    }
}
