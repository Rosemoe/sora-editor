/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.span.SpanColorResolver;
import io.github.rosemoe.sora.lang.styling.span.SpanExtAttrs;
import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;
import io.github.rosemoe.sora.widget.CodeEditor;

/** Coordinates asynchronous style patch providers and their latest snapshots. */
public final class StylePatchManager {

    private final CodeEditor editor;
    private final java.util.function.BiConsumer<SparseStylePatches, StyleUpdateRange> onUpdate;
    private final List<StylePatchProvider> providers = new ArrayList<>();
    private final Map<StylePatchProvider, SparseStylePatches> snapshots = new HashMap<>();
    private final Map<StylePatchProvider, Long> versions = new HashMap<>();
    private SparseStylePatches merged;

    public StylePatchManager(@NonNull CodeEditor editor,
                             @NonNull java.util.function.BiConsumer<SparseStylePatches, StyleUpdateRange> onUpdate) {
        this.editor = editor;
        this.onUpdate = onUpdate;
    }

    public synchronized void register(@NonNull StylePatchProvider provider) {
        if (providers.contains(provider)) return;
        providers.add(provider);
        versions.put(provider, 0L);
        refresh(provider);
    }

    public synchronized void unregister(@NonNull StylePatchProvider provider) {
        providers.remove(provider);
        versions.remove(provider);
        var old = snapshots.remove(provider);
        if (merged != null && old != null) for (var patch : old.getPatches()) merged.removePatch(patch);
        if (merged != null && merged.getPatches().isEmpty()) merged = null;
        onUpdate.accept(merged, null);
    }

    public synchronized void refresh(@NonNull StylePatchProvider provider) {
        if (!providers.contains(provider)) return;
        long version = versions.get(provider) + 1;
        versions.put(provider, version);
        provider.provideStylePatches(editor, new StylePatchProvider.Receiver() {
            @Override
            public void setStylePatches(SparseStylePatches patches) {
                editor.postInLifecycle(() -> {
                    synchronized (StylePatchManager.this) {
                        if (!providers.contains(provider) || versions.get(provider) != version) return;
                        replace(provider, patches);
                        onUpdate.accept(merged, null);
                    }
                });
            }

            @Override
            public void updateStylePatches(@NonNull StylePatchUpdate update) {
                editor.postInLifecycle(() -> {
                    synchronized (StylePatchManager.this) {
                        if (!providers.contains(provider) || versions.get(provider) != version) return;
                        var patches = snapshots.computeIfAbsent(provider, ignored -> new SparseStylePatches());
                        if (merged == null) merged = new SparseStylePatches();
                        for (var patch : update.getRemoved()) {
                            patches.removePatch(patch);
                            merged.removePatch(patch);
                        }
                        for (var patch : update.getAdded()) {
                            patches.addPatch(patch);
                            merged.addPatch(patch);
                        }
                        if (merged.getPatches().isEmpty()) merged = null;
                        onUpdate.accept(merged, update.getRange());
                    }
                });
            }
        });
    }

    @NonNull
    public synchronized List<StylePatchProvider> getProviders() {
        return new ArrayList<>(providers);
    }

    @Nullable
    public synchronized SparseStylePatches getPatches() {
        return merged;
    }

    public synchronized void setPatches(@Nullable SparseStylePatches patches) {
        merged = patches;
        onUpdate.accept(merged, null);
    }

    public synchronized void updateForInsertion(int startLine, int startColumn, int endLine, int endColumn) {
        snapshots.values().forEach(it -> it.updateForInsertion(startLine, startColumn, endLine, endColumn));
        onUpdate.accept(merged, null);
    }

    public synchronized void updateForDeletion(int startLine, int startColumn, int endLine, int endColumn) {
        snapshots.values().forEach(it -> it.updateForDeletion(startLine, startColumn, endLine, endColumn));
        onUpdate.accept(merged, null);
    }

    /** Merge decorations into the token spans used by the renderer. */
    @NonNull
    public synchronized List<Span> applyToSpans(int line, int lineLength, @NonNull List<Span> base) {
        var result = new ArrayList<>(base);
        for (var patch : merged == null ? java.util.Collections.<StylePatch>emptyList() : merged.getPatchesOnLine(line)) {
            int start = patch.getStartLine() < line ? 0 : patch.getStartColumn();
            int end = patch.getEndLine() > line ? lineLength : patch.getEndColumn();
            if (start >= end) continue;
            var next = new ArrayList<Span>(result.size() + 2);
            for (int i = 0; i < result.size(); i++) {
                var span = result.get(i);
                int spanStart = span.getColumn();
                int spanEnd = i + 1 < result.size() ? result.get(i + 1).getColumn() : lineLength;
                if (spanEnd <= start || spanStart >= end) {
                    next.add(span);
                    continue;
                }
                if (spanStart < start) next.add(copyAt(span, spanStart));
                int overlapStart = Math.max(spanStart, start);
                int overlapEnd = Math.min(spanEnd, end);
                next.add(applyPatch(copyAt(span, overlapStart), patch));
                if (overlapEnd < spanEnd) next.add(copyAt(span, overlapEnd));
            }
            result = next;
        }
        return result;
    }

    private static Span copyAt(Span span, int column) {
        var copy = span.copy();
        copy.setColumn(column);
        return copy;
    }

    private static Span applyPatch(Span span, StylePatch patch) {
        long style = span.getStyle();
        if (patch.getOverrideBold() != null) style = patch.getOverrideBold() ? style | io.github.rosemoe.sora.lang.styling.TextStyle.BOLD_BIT : style & ~io.github.rosemoe.sora.lang.styling.TextStyle.BOLD_BIT;
        if (patch.getOverrideItalics() != null) style = patch.getOverrideItalics() ? style | io.github.rosemoe.sora.lang.styling.TextStyle.ITALICS_BIT : style & ~io.github.rosemoe.sora.lang.styling.TextStyle.ITALICS_BIT;
        span.setStyle(style);
        if (patch.getOverrideForeground() != null || patch.getOverrideBackground() != null) {
            span.setSpanExt(SpanExtAttrs.EXT_COLOR_RESOLVER, new PatchColorResolver(patch));
        }
        return span;
    }

    private static final class PatchColorResolver implements SpanColorResolver {
        private final StylePatch patch;
        PatchColorResolver(StylePatch patch) { this.patch = patch; }
        @Override public io.github.rosemoe.sora.lang.styling.color.ResolvableColor getForegroundColor(Span span) { return patch.getOverrideForeground(); }
        @Override public io.github.rosemoe.sora.lang.styling.color.ResolvableColor getBackgroundColor(Span span) { return patch.getOverrideBackground(); }
    }

    private void replace(@NonNull StylePatchProvider provider, @Nullable SparseStylePatches patches) {
        if (merged == null) merged = new SparseStylePatches();
        var old = snapshots.get(provider);
        if (old != null) for (var patch : old.getPatches()) merged.removePatch(patch);
        if (patches == null || patches.getPatches().isEmpty()) snapshots.remove(provider);
        else {
            snapshots.put(provider, patches);
            for (var patch : patches.getPatches()) merged.addPatch(patch);
        }
        if (merged.getPatches().isEmpty()) merged = null;
    }
}
