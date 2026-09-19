/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching

import io.github.rosemoe.sora.lang.analysis.SequenceUpdateRange
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs
import io.github.rosemoe.sora.widget.CodeEditor

/** Coordinates asynchronous style patch providers and their latest snapshots. */
class StylePatchManager(
    private val editor: CodeEditor, private val onUpdate: StylePatchManagerUpdate
) {

    private val fullUpdateRange = SequenceUpdateRange(0, Int.MAX_VALUE)

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
        request(provider, StylePatchRequest.Reason.INITIAL, EmptyStyleUpdateRange)
    }

    @Synchronized
    fun unregister(provider: StylePatchProvider) {
        providers.remove(provider)
        versions.remove(provider)
        if (merged !== SparseStylePatches.EMPTY) {
            snapshots.remove(provider)?.let { merged.removePatches(it.getPatches()) }
            clearMerged()
        } else {
            snapshots.remove(provider)
        }
        onUpdate.accept(merged, fullUpdateRange)
    }

    @Synchronized
    fun refresh(provider: StylePatchProvider) {
        request(provider, StylePatchRequest.Reason.MANUAL, EmptyStyleUpdateRange)
    }

    @Synchronized
    fun updateVisibleRange(startLine: Int, endLine: Int) {
        val start = startLine.coerceAtLeast(0)
        val end = endLine.coerceAtLeast(start)
        if (hasVisibleRange && visibleStartLine == start && visibleEndLine == end) return
        visibleStartLine = start
        visibleEndLine = end
        hasVisibleRange = true
        // This is a query notification, not an instruction to discard or recompute a provider's
        // cache. Providers own their cache and may answer from it immediately, or start an async
        // computation for the newly visible range. This mirrors VS Code's provider/event split.
        // Providers may register or unregister from inside the callback.
        for (provider in providers.toList()) {
            request(provider, StylePatchRequest.Reason.VISIBLE_RANGE_CHANGED, EmptyStyleUpdateRange)
        }
    }

    private fun request(
        provider: StylePatchProvider,
        reason: StylePatchRequest.Reason,
        changedRange: StyleUpdateRange
    ) {
        if (!providers.contains(provider)) return
        val version = ++versionCounter
        versions[provider] = version
        val request = StylePatchRequest(
            visibleStartLine, visibleEndLine, changedRange, reason
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
                        // A provider may hand back its own snapshot object, in which case only its
                        // entries inside the range are replacements.
                        val incoming = if (patches === snapshot) {
                            patches.getPatches().filter { range.intersects(it.startLine, it.endLine) }
                        } else {
                            patches.getPatches()
                        }
                        val removed = snapshot.removeInRange(range)
                        snapshot.addPatches(incoming)
                        currentMerged.removePatches(removed)
                        currentMerged.addPatches(incoming)
                        clearMerged()
                        onUpdate.accept(merged, range)
                    }
                }
            }
        })
    }

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
        for (snapshot in snapshots.values) {
            snapshot.updateForInsertion(startLine, startColumn, endLine, endColumn)
        }
        merged.rebuildSearchIndex()
        refreshAfterTextChange(startLine, endLine)
    }

    @Synchronized
    fun updateForDeletion(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) {
        for (snapshot in snapshots.values) {
            snapshot.updateForDeletion(startLine, startColumn, endLine, endColumn)
        }
        merged.rebuildSearchIndex()
        refreshAfterTextChange(startLine, endLine)
    }

    private fun refreshAfterTextChange(startLine: Int, endLine: Int) {
        onUpdate.accept(merged, SequenceUpdateRange(startLine, maxOf(endLine, visibleEndLine)))
        for (provider in providers.toList()) {
            request(
                provider,
                StylePatchRequest.Reason.TEXT_CHANGED,
                SequenceUpdateRange(startLine, endLine)
            )
        }
    }

    /** Merge decorations into the token spans used by the renderer. */
    @Synchronized
    fun applyToSpans(line: Int, lineLength: Int, base: List<Span>): List<Span> {
        val patches = merged.getPatchesOnLine(line)
        if (patches.isEmpty() || base.isEmpty()) return base
        var previousEnd = -1
        for (patch in patches) {
            val start = if (patch.startLine < line) 0 else patch.startColumn
            val end = if (patch.endLine > line) lineLength else patch.endColumn
            if (start >= end) continue
            if (start < previousEnd) return overlayPatches(line, lineLength, base, patches)
            previousEnd = end
        }
        return sweepPatches(line, lineLength, base, patches)
    }

    /** Sweep spans and patches together; patches are disjoint here. */
    private fun sweepPatches(
        line: Int, lineLength: Int, base: List<Span>, patches: List<StylePatch>
    ): List<Span> {
        val result = mutableListOf<Span>()
        var patchIndex = 0
        for (spanIndex in base.indices) {
            val span = base[spanIndex]
            val spanStart = span.column
            val spanEnd = if (spanIndex + 1 < base.size) base[spanIndex + 1].column else lineLength
            var cursor = spanStart
            var changed = false
            while (patchIndex < patches.size) {
                val patch = patches[patchIndex]
                val patchStart = if (patch.startLine < line) 0 else patch.startColumn
                val patchEnd = if (patch.endLine > line) lineLength else patch.endColumn
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

    /** Ordered overlay for overlapping patches: later patches win on shared columns. */
    private fun overlayPatches(
        line: Int, lineLength: Int, base: List<Span>, patches: List<StylePatch>
    ): List<Span> {
        var result = base.toMutableList()
        for (patch in patches) {
            val start = if (patch.startLine < line) 0 else patch.startColumn
            val end = if (patch.endLine > line) lineLength else patch.endColumn
            if (start >= end) continue
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
        patch.overrideBold?.let {
            style = if (it) style or TextStyle.BOLD_BIT else style and TextStyle.BOLD_BIT.inv()
        }
        patch.overrideItalics?.let {
            style = if (it) style or TextStyle.ITALICS_BIT else style and TextStyle.ITALICS_BIT.inv()
        }
        span.style = style
        if (patch.overrideForeground != null || patch.overrideBackground != null) {
            val resolver = PatchColorResolver(patch)
            try {
                span.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, resolver)
            } catch (_: UnsupportedOperationException) {
                // Some language spans are deliberately compact and do not implement SpanExt.
                // Replace them with a full span before applying a style patch instead of letting
                // rendering crash in NoExtSpanImpl.setSpanExt.
                val replacement = SpanFactory.obtain(span.column, span.style)
                replacement.extra = span.extra
                replacement.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, resolver)
                return replacement
            }
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
        val incoming = patches.getPatches()
        if (incoming.isEmpty()) {
            snapshots.remove(provider)
        } else {
            snapshots[provider] = patches
            currentMerged.addPatches(incoming)
        }
        clearMerged()
    }

    private fun mutableMerged(): SparseStylePatches {
        if (merged === SparseStylePatches.EMPTY) merged = SparseStylePatches()
        return merged
    }

    private fun clearMerged() {
        if (merged.getPatches().isEmpty()) merged = SparseStylePatches.EMPTY
    }
}

fun interface StylePatchManagerUpdate {
    fun accept(patches: SparseStylePatches, range: StyleUpdateRange)
}
