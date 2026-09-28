/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange

/**
 * Describes an asynchronous style-patch query.
 *
 * The visible range tells a provider what the renderer currently needs; it does not imply that
 * the provider must recompute that range. Providers may reuse cached results and only calculate
 * newly visible or text-invalidated lines.
 */
class StylePatchRequest(
    val visibleStartLine: Int,
    val visibleEndLine: Int,
    /** Lines invalidated by a text edit, or [EmptyStyleUpdateRange] for a viewport query. */
    val changedRange: StyleUpdateRange,
    val reason: Reason
) {

    enum class Reason {
        INITIAL,
        VISIBLE_RANGE_CHANGED,
        TEXT_CHANGED,
        MANUAL
    }
}

/** Empty range used when a request has no text-edit interval. */
object EmptyStyleUpdateRange : StyleUpdateRange {

    override fun isInRange(line: Int): Boolean = false

    override fun lineIndexIterator(maxLineIndex: Int): IntIterator = intArrayOf().iterator()
}
