/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * Asynchronously provides decorations for an editor. [StylePatchRequest] carries the current
 * visible line range; results replace the whole snapshot with [Receiver.set] or only the patches
 * intersecting a range with [Receiver.update].
 */
fun interface StylePatchProvider {

    fun provideStylePatches(editor: CodeEditor, request: StylePatchRequest, receiver: Receiver)

    interface Receiver {

        fun set(patches: SparseStylePatches)

        fun update(patches: SparseStylePatches, range: StyleUpdateRange)
    }
}
