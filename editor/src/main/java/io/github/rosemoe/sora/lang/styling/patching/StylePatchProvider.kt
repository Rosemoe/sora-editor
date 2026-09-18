/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Asynchronously provides decorations for an editor.
 *
 * The editor supplies the visible line range in [StylePatchRequest], so providers do not need to
 * maintain a full-document patch model. Results may replace the visible snapshot with [Receiver.set]
 * or replace only a line range with [Receiver.update].
 */
fun interface StylePatchProvider {

    fun provideStylePatches(editor: CodeEditor, request: StylePatchRequest, receiver: Receiver)

    interface Receiver {
        /** Replace this provider's current visible-window snapshot. */
        fun set(patches: SparseStylePatches)

        /** Replace only the patches intersecting [range]. */
        fun update(patches: SparseStylePatches, range: StyleUpdateRange)
    }
}
