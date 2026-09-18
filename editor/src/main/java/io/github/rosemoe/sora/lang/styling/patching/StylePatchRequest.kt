/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching

/** Describes the editor state for an asynchronous style patch request. */
class StylePatchRequest(
    val visibleStartLine: Int,
    val visibleEndLine: Int,
    /** First changed line, or -1 when no text range was supplied. */
    val changedStartLine: Int,
    /** Last changed line, or -1 when no text range was supplied. */
    val changedEndLine: Int,
    val reason: Reason
) {

    enum class Reason {
        INITIAL,
        VISIBLE_RANGE_CHANGED,
        TEXT_CHANGED,
        MANUAL
    }
}
