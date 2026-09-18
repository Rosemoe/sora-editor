/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.analysis.SequenceUpdateRange
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.widget.CodeEditor

/** Coordinates asynchronous style patch providers and their latest snapshots. */
class StylePatchManager(
    private val editor: CodeEditor, private val onUpdate: StylePatchManagerUpdate
) {

    private val fullUpdateRange = SequenceUpdateRange(0, Int.MAX_VALUE)

    /** Provider order is stable; callbacks update only the affected provider snapshot. */
    private val providers = mutableListOf<StylePatchProvider>()
    private val snapshots = mutableMapOf<StylePatchProvider, SparseStylePatches>()
    private val versions = mutableMapOf<StylePatchProvider, Long>()
    private var versionCounter = 0L
    private var merged = SparseStylePatches.EMPTY
    private var visibleStartLine = 0
    private var visibleEndLine = 0
    private var hasVisibleRange = false

    @Synchronized
    fun register(provider: StylePatchProvider) {
        if (providers.contains(provider)) return
        providers.add(provider)
        if (!hasVisibleRange) {
            runCatching {
                visibleStartLine = editor.firstVisibleLine.coerceAtLeast(0)
                visibleEndLine = editor.lastVisibleLine.coerceAtLeast(visibleStartLine)
            }.onFailure {
                visibleStartLine = 0
                visibleEndLine = 0
            }
            hasVisibleRange = true
        }
        request(provider, StylePatchRequest.Reason.INITIAL, -1, -1)
    }

    @Synchronized
    fun unregister(provider: StylePatchProvider) {
        providers.remove(provider)
        versions.remove(provider)
        if (merged !== SparseStylePatches.EMPTY) {
            snapshots.remove(provider)?.let { merged.removePatches(it.getPatches()) }
            clearMergedIfEmpty()
        } else {
            snapshots.remove(provider)
        }
        onUpdate.accept(merged, fullUpdateRange)
    }

    @Synchronized
    fun refresh(provider: StylePatchProvider) {
        request(provider, StylePatchRequest.Reason.MANUAL, -1, -1)
    }

    @Synchronized
    fun updateVisibleRange(startLine: Int, endLine: Int) {
        val normalizedStart = startLine.coerceAtLeast(0)
        val normalizedEnd = endLine.coerceAtLeast(normalizedStart)
        if (hasVisibleRange && visibleStartLine == normalizedStart && visibleEndLine == normalizedEnd) return
        visibleStartLine = normalizedStart
        visibleEndLine = normalizedEnd
        hasVisibleRange = true
        // The provider is external code and may register/unregister providers synchronously.
        // Iterate over a shallow snapshot to avoid concurrent modification and skipped entries;
        // this copies only O(P) provider references, never the patch collections.
        providers.toList().forEach {
            request(it, StylePatchRequest.Reason.VISIBLE_RANGE_CHANGED, -1, -1)
        }
    }

    private fun request(
        provider: StylePatchProvider,
        reason: StylePatchRequest.Reason,
        changedStartLine: Int,
        changedEndLine: Int
    ) {
        if (!providers.contains(provider)) return
        val version = ++versionCounter
        versions[provider] = version
        val request = StylePatchRequest(
            visibleStartLine, visibleEndLine, changedStartLine, changedEndLine, reason
        )
        provider.provideStylePatches(editor, request, object : StylePatchProvider.Receiver {
            override fun set(patches: SparseStylePatches) {
                editor.postInLifecycle {
                    synchronized(this@StylePatchManager) {
                        if (versions[provider] != version) return@synchronized
                        replace(provider, patches)
                        onUpdate.accept(merged, fullUpdateRange)
                    }
                }
            }

            override fun update(patches: SparseStylePatches, range: StyleUpdateRange) {
                editor.postInLifecycle {
                    synchronized(this@StylePatchManager) {
                        if (versions[provider] != version) return@synchronized
                        val snapshot = snapshots.getOrPut(provider) { SparseStylePatches() }
                        val currentMerged = mutableMerged()
                        // Replace only this range. The rest of the provider snapshot and merged
                        // collection remain in place, so an async partial result is proportional
                        // to the changed range rather than to the whole document.
                        val incoming = if (patches === snapshot) {
                            // A provider may reuse its snapshot object. In that case only its
                            // entries in the requested range are replacements; re-adding the
                            // whole object would duplicate every entry outside the range.
                            patches.getPatches().filter {
                                range.intersects(it.startLine, it.endLine)
                            }
                        } else {
                            patches.getPatches()
                        }
                        val removed = snapshot.removeInRange(range)
                        snapshot.addPatches(incoming)
                        currentMerged.removePatches(removed)
                        currentMerged.addPatches(incoming)
                        clearMergedIfEmpty()
                        onUpdate.accept(merged, range)
                    }
                }
            }
        })
    }

    @Synchronized
    /** Return a stable public snapshot; callers cannot mutate the manager's provider list. */
    fun getProviders(): List<StylePatchProvider> = providers.toList()

    @Synchronized
    fun getPatches(): SparseStylePatches = merged

    @Synchronized
    fun setPatches(patches: SparseStylePatches) {
        merged = if (patches.getPatches().isEmpty()) SparseStylePatches.EMPTY else patches
        onUpdate.accept(merged, fullUpdateRange)
    }

    @Synchronized
    fun updateForInsertion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        snapshots.values.forEach {
            it.updateForInsertion(
                startLine, startColumn, endLine, endColumn
            )
        }
        // Snapshots and merged share patch objects. Their coordinates are already remapped above;
        // refresh the merged search index without applying the edit a second time.
        merged.rebuildSearchIndex()
        refreshAfterTextChange(startLine, endLine)
    }

    @Synchronized
    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        snapshots.values.forEach {
            it.updateForDeletion(
                startLine, startColumn, endLine, endColumn
            )
        }
        merged.rebuildSearchIndex()
        refreshAfterTextChange(startLine, endLine)
    }

    private fun refreshAfterTextChange(startLine: Int, endLine: Int) {
        onUpdate.accept(merged, SequenceUpdateRange(startLine, maxOf(endLine, visibleEndLine)))
        // See updateVisibleRange: provider callbacks are allowed to mutate registration state.
        providers.toList().forEach {
            request(it, StylePatchRequest.Reason.TEXT_CHANGED, startLine, endLine)
        }
    }

    /** Merge decorations into the token spans used by the renderer. */
    @Synchronized
    fun applyToSpans(line: Int, lineLength: Int, base: List<Span>): List<Span> {
        val linePatches = merged.getPatchesOnLine(line)
        if (linePatches.isEmpty() || base.isEmpty()) return base
        return if (hasOverlappingPatches(linePatches, line, lineLength)) {
            applyOverlappingPatches(line, lineLength, base, linePatches)
        } else {
            applyNonOverlappingPatches(line, lineLength, base, linePatches)
        }
    }

    /**
     * The common case is sparse, non-overlapping decorations. Sweep spans and patches together in
     * O(span count + patch count + output count), instead of rescanning every span for every patch.
     */
    private fun applyNonOverlappingPatches(
        line: Int,
        lineLength: Int,
        base: List<Span>,
        patches: List<StylePatch>
    ): List<Span> {
        val result = mutableListOf<Span>()
        var patchIndex = 0
        base.forEachIndexed { spanIndex, span ->
            val spanStart = span.column
            val spanEnd = base.getOrNull(spanIndex + 1)?.column ?: lineLength
            var cursor = spanStart
            var changed = false
            while (patchIndex < patches.size) {
                val patch = patches[patchIndex]
                val patchStart = patchStart(patch, line)
                val patchEnd = patchEnd(patch, line, lineLength)
                if (patchEnd <= spanStart) {
                    patchIndex++
                    continue
                }
                if (patchStart >= spanEnd) break
                if (cursor < patchStart) result.add(copyAt(span, cursor))
                val overlapStart = maxOf(cursor, patchStart)
                val overlapEnd = minOf(spanEnd, patchEnd)
                if (overlapStart < overlapEnd) {
                    result.add(applyPatch(copyAt(span, overlapStart), patch))
                    cursor = overlapEnd
                    changed = true
                }
                if (patchEnd <= spanEnd) patchIndex++ else break
            }
            if (!changed) {
                result.add(span)
            } else if (cursor < spanEnd) {
                result.add(copyAt(span, cursor))
            }
        }
        return result
    }

    /** Preserve the old ordered-overlay semantics only for the uncommon overlapping case. */
    private fun applyOverlappingPatches(
        line: Int,
        lineLength: Int,
        base: List<Span>,
        patches: List<StylePatch>
    ): List<Span> {
        var result = base.toMutableList()
        patches.forEach { patch ->
            val start = if (patch.startLine < line) 0 else patch.startColumn
            val end = if (patch.endLine > line) lineLength else patch.endColumn
            if (start >= end) return@forEach
            val next = mutableListOf<Span>()
            result.forEachIndexed { index, span ->
                val spanStart = span.column
                val spanEnd = if (index + 1 < result.size) result[index + 1].column else lineLength
                if (spanEnd <= start || spanStart >= end) {
                    next.add(span)
                } else {
                    if (spanStart < start) next.add(copyAt(span, spanStart))
                    val overlapStart = maxOf(spanStart, start)
                    val overlapEnd = minOf(spanEnd, end)
                    next.add(applyPatch(copyAt(span, overlapStart), patch))
                    if (overlapEnd < spanEnd) next.add(copyAt(span, overlapEnd))
                }
            }
            result = next
        }
        return result
    }

    private fun hasOverlappingPatches(patches: List<StylePatch>, line: Int, lineLength: Int): Boolean {
        var previousEnd = -1
        patches.forEach { patch ->
            val start = patchStart(patch, line)
            val end = patchEnd(patch, line, lineLength)
            if (start < end) {
                if (start < previousEnd) return true
                previousEnd = end
            }
        }
        return false
    }

    private fun patchStart(patch: StylePatch, line: Int): Int =
        if (patch.startLine < line) 0 else patch.startColumn

    private fun patchEnd(patch: StylePatch, line: Int, lineLength: Int): Int =
        if (patch.endLine > line) lineLength else patch.endColumn

    private fun copyAt(span: Span, column: Int): Span = span.copy().also { it.column = column }

    private fun applyPatch(span: Span, patch: StylePatch): Span {
        var style = span.style
        patch.overrideBold?.let {
            style = if (it) style or TextStyle.BOLD_BIT else style and TextStyle.BOLD_BIT.inv()
        }
        patch.overrideItalics?.let {
            style =
                if (it) style or TextStyle.ITALICS_BIT else style and TextStyle.ITALICS_BIT.inv()
        }
        span.style = style
        if (patch.overrideForeground != null || patch.overrideBackground != null) {
            span.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, PatchColorResolver(patch))
        }
        return span
    }

    private class PatchColorResolver(private val patch: StylePatch) : SpanColorResolver {
        override fun getForegroundColor(span: Span) = patch.overrideForeground
        override fun getBackgroundColor(span: Span) = patch.overrideBackground
    }

    private fun replace(provider: StylePatchProvider, patches: SparseStylePatches) {
        val currentMerged = mutableMerged()
        snapshots.remove(provider)?.let { currentMerged.removePatches(it.getPatches()) }
        // VS Code-like set semantics replace one provider's snapshot. Removing the old snapshot
        // and linearly merging the ordered incoming list avoids rebuilding every provider.
        val incoming = patches.getPatches()
        if (incoming.isEmpty()) {
            snapshots.remove(provider)
        } else {
            snapshots[provider] = patches
            currentMerged.addPatches(incoming)
        }
        clearMergedIfEmpty()
    }

    private fun mutableMerged(): SparseStylePatches {
        if (merged === SparseStylePatches.EMPTY) merged = SparseStylePatches()
        return merged
    }

    private fun clearMergedIfEmpty() {
        if (merged.getPatches().isEmpty()) merged = SparseStylePatches.EMPTY
    }
}

fun interface StylePatchManagerUpdate {
    fun accept(patches: SparseStylePatches, range: StyleUpdateRange)
}
