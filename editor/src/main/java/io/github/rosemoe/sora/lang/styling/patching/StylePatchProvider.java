/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 */
package io.github.rosemoe.sora.lang.styling.patching;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Asynchronously provides decorations for an editor.
 *
 * <p>The provider may call the receiver from any thread. A new result replaces the previous
 * result from this provider, like VS Code's {@code setDecorations} API.</p>
 */
@FunctionalInterface
public interface StylePatchProvider {

    void provideStylePatches(@NonNull CodeEditor editor, @NonNull Receiver receiver);

    @FunctionalInterface
    interface Receiver {
        void setStylePatches(SparseStylePatches patches);
    }
}
