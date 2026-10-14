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
 * visible line range and is a query notification. Providers should keep their own cache and
 * avoid recomputing unchanged decorations when the viewport moves. [StylePatchRequest.changedRange]
 * is an empty range unless the reason is [StylePatchRequest.Reason.TEXT_CHANGED]. Results replace
 * the provider snapshot with [Receiver.set], or update only a changed range with [Receiver.update].
 */
fun interface StylePatchProvider {

    fun provideStylePatches(editor: CodeEditor, request: StylePatchRequest, receiver: Receiver)

    interface Receiver {

        fun set(patches: SparseStylePatches)

        fun update(patches: SparseStylePatches, range: StyleUpdateRange)
    }
}
