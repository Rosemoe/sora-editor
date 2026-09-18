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
import java.util.function.BiConsumer

/** Coordinates asynchronous style patch providers and their latest snapshots. */
class StylePatchManager(
    private val editor: CodeEditor,
    private val onUpdate: BiConsumer<SparseStylePatches, StyleUpdateRange>
) {

    private val fullUpdateRange = SequenceUpdateRange(0, Int.MAX_VALUE)

    private val providers = mutableListOf<StylePatchProvider>()
    private val snapshots = mutableMapOf<StylePatchProvider, SparseStylePatches>()
    private val versions = mutableMapOf<StylePatchProvider, Long>()
    private var merged = SparseStylePatches.EMPTY
    private var visibleStartLine = 0
    private var visibleEndLine = 0
    private var hasVisibleRange = false

    @Synchronized
    fun register(provider: StylePatchProvider) {
        if (providers.contains(provider)) return
        providers.add(provider)
        versions[provider] = 0L
        if (!hasVisibleRange) {
            try {
                visibleStartLine = editor.firstVisibleLine.coerceAtLeast(0)
                visibleEndLine = editor.lastVisibleLine.coerceAtLeast(visibleStartLine)
            } catch (_: RuntimeException) {
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
            snapshots.remove(provider)?.getPatches()?.forEach { merged.removePatch(it) }
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
        providers.toList().forEach { request(it, StylePatchRequest.Reason.VISIBLE_RANGE_CHANGED, -1, -1) }
    }

    private fun request(provider: StylePatchProvider, reason: StylePatchRequest.Reason,
                        changedStartLine: Int, changedEndLine: Int) {
        if (!providers.contains(provider)) return
        val version = (versions[provider] ?: 0L) + 1L
        versions[provider] = version
        val request = StylePatchRequest(
            visibleStartLine, visibleEndLine, changedStartLine, changedEndLine, reason
        )
        provider.provideStylePatches(editor, request, object : StylePatchProvider.Receiver {
            override fun set(patches: SparseStylePatches) {
                editor.postInLifecycle {
                    synchronized(this@StylePatchManager) {
                        if (!isCurrent(provider, version)) return@synchronized
                        replace(provider, patches)
                        onUpdate.accept(merged, fullUpdateRange)
                    }
                }
            }

            override fun update(patches: SparseStylePatches, range: StyleUpdateRange) {
                editor.postInLifecycle {
                    synchronized(this@StylePatchManager) {
                        if (!isCurrent(provider, version)) return@synchronized
                        val snapshot = snapshots.getOrPut(provider) { SparseStylePatches() }
                        val currentMerged = mutableMerged()
                        snapshot.removeInRange(range).forEach { currentMerged.removePatch(it) }
                        patches.getPatches().forEach {
                            snapshot.addPatch(it)
                            currentMerged.addPatch(it)
                        }
                        clearMergedIfEmpty()
                        onUpdate.accept(merged, range)
                    }
                }
            }
        })
    }

    private fun isCurrent(provider: StylePatchProvider, version: Long): Boolean =
        providers.contains(provider) && versions[provider] == version

    @Synchronized
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
        snapshots.values.forEach { it.updateForInsertion(startLine, startColumn, endLine, endColumn) }
        refreshAfterTextChange(startLine, endLine)
    }

    @Synchronized
    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        snapshots.values.forEach { it.updateForDeletion(startLine, startColumn, endLine, endColumn) }
        refreshAfterTextChange(startLine, endLine)
    }

    private fun refreshAfterTextChange(startLine: Int, endLine: Int) {
        onUpdate.accept(merged, SequenceUpdateRange(startLine, maxOf(endLine, visibleEndLine)))
        providers.toList().forEach { request(it, StylePatchRequest.Reason.TEXT_CHANGED, startLine, endLine) }
    }

    /** Merge decorations into the token spans used by the renderer. */
    @Synchronized
    fun applyToSpans(line: Int, lineLength: Int, base: List<Span>): List<Span> {
        var result = mutableListOf<Span>().apply { addAll(base) }
        merged.getPatchesOnLine(line).forEach { patch ->
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

    private fun copyAt(span: Span, column: Int): Span = span.copy().also { it.column = column }

    private fun applyPatch(span: Span, patch: StylePatch): Span {
        var style = span.style
        patch.overrideBold?.let { style = if (it) style or TextStyle.BOLD_BIT else style and TextStyle.BOLD_BIT.inv() }
        patch.overrideItalics?.let { style = if (it) style or TextStyle.ITALICS_BIT else style and TextStyle.ITALICS_BIT.inv() }
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
        snapshots.remove(provider)?.getPatches()?.forEach { currentMerged.removePatch(it) }
        if (patches.getPatches().isEmpty()) {
            snapshots.remove(provider)
        } else {
            snapshots[provider] = patches
            patches.getPatches().forEach { currentMerged.addPatch(it) }
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
