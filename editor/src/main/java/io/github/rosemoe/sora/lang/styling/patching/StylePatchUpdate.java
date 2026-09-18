/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
 */
package io.github.rosemoe.sora.lang.styling.patching;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import io.github.rosemoe.sora.lang.analysis.StyleUpdateRange;

/** An incremental diff for one provider's style patch set. */
public final class StylePatchUpdate {

    private final List<StylePatch> added;
    private final List<StylePatch> removed;
    private final StyleUpdateRange range;

    public StylePatchUpdate(@NonNull List<StylePatch> added,
                            @NonNull List<StylePatch> removed,
                            @Nullable StyleUpdateRange range) {
        this.added = Collections.unmodifiableList(new java.util.ArrayList<>(added));
        this.removed = Collections.unmodifiableList(new java.util.ArrayList<>(removed));
        this.range = range;
    }

    @NonNull
    public List<StylePatch> getAdded() {
        return added;
    }

    @NonNull
    public List<StylePatch> getRemoved() {
        return removed;
    }

    @Nullable
    public StyleUpdateRange getRange() {
        return range;
    }
}
