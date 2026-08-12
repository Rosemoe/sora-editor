/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.widget;

import androidx.annotation.NonNull;

import io.github.rosemoe.sora.event.EventReceiver;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.Unsubscribe;
import io.github.rosemoe.sora.widget.style.CursorBlinkingType;

/**
 * This class is used to control cursor visibility
 *
 * @author Rose
 */
final class CursorBlink implements Runnable, EventReceiver<SelectionChangeEvent> {

    /**
     * Frame interval used to refresh the cursor while a smooth animation is active
     */
    private static final long SMOOTH_FRAME_INTERVAL = 16L;

    /**
     * The lowest alpha used by {@link CursorBlinkingType#PHASE}
     */
    private static final float PHASE_MIN_ALPHA = 0.25f;

    /**
     * The lowest size factor used by {@link CursorBlinkingType#EXPAND}
     */
    private static final float EXPAND_MIN_FACTOR = 0.5f;

    final CodeEditor editor;
    public boolean visibility;
    public boolean valid;
    long lastSelectionModificationTime = 0;
    int period;
    private float[] buffer;

    public CursorBlink(CodeEditor editor, int period) {
        visibility = true;
        this.editor = editor;
        this.period = period;
        editor.subscribeEvent(SelectionChangeEvent.class, this);
    }

    @Override
    public void onReceive(@NonNull SelectionChangeEvent event, @NonNull Unsubscribe unsubscribe) {
        onSelectionChanged();
    }

    public void setPeriod(int period) {
        this.period = period;
        if (period <= 0) {
            visibility = true;
            valid = false;
        } else {
            valid = true;
        }
    }

    public void onSelectionChanged() {
        lastSelectionModificationTime = System.currentTimeMillis();
        visibility = true;
    }

    /**
     * Whether the cursor is currently considered visible at all.
     * For smooth animations this is {@code true} whenever the animation is running,
     * since the actual visibility is expressed by {@link #getAlpha()}.
     */
    public boolean isCursorVisible() {
        if (period <= 0 || !valid) {
            return true;
        }
        if (System.currentTimeMillis() - lastSelectionModificationTime < period * 2L) {
            return true;
        }
        switch (editor.getCursorBlinkingType()) {
            case BLINK:
                return visibility;
            case SMOOTH:
            case PHASE:
            case EXPAND:
                return true;
        }
        return true;
    }

    /**
     * The alpha value (0..1) the cursor should be drawn with right now.
     * Always 1 when the cursor must stay fully visible (e.g. blink disabled).
     */
    public float getAlpha() {
        if (period <= 0 || !valid) {
            return 1f;
        }
        if (System.currentTimeMillis() - lastSelectionModificationTime < period * 2L) {
            return 1f;
        }
        switch (editor.getCursorBlinkingType()) {
            case BLINK:
                return visibility ? 1f : 0f;
            case SMOOTH:
                return smoothWave(0f, 1f);
            case PHASE:
                return smoothWave(PHASE_MIN_ALPHA, 1f);
            case EXPAND:
            default:
                return 1f;
        }
    }

    /**
     * The size factor (0..1) used by the expand animation to grow and shrink the cursor.
     */
    public float getExpandFactor() {
        if (period <= 0 || !valid || editor.getCursorBlinkingType() != CursorBlinkingType.EXPAND) {
            return 1f;
        }
        if (System.currentTimeMillis() - lastSelectionModificationTime < period * 2L) {
            return 1f;
        }
        double cycle = period * 2L;
        double t = (System.currentTimeMillis() % (long) cycle) / cycle;
        double v = (1 + Math.cos(t * 2 * Math.PI)) / 2.0;
        return (float) (EXPAND_MIN_FACTOR + (1 - EXPAND_MIN_FACTOR) * v);
    }

    private float smoothWave(float min, float max) {
        double cycle = period * 2L;
        double t = (System.currentTimeMillis() % (long) cycle) / cycle;
        double v = (1 + Math.cos(t * 2 * Math.PI)) / 2.0;
        return (float) (min + (max - min) * v);
    }

    public boolean isSelectionVisible() {
        return (buffer[0] >= editor.getOffsetY() && buffer[0] - editor.getRowHeight() <= editor.getOffsetY() + editor.getHeight()
                && buffer[1] >= editor.getOffsetX() && buffer[1] - 100f/* larger than a single character */ <= editor.getOffsetX() + editor.getWidth());
    }

    @Override
    public void run() {
        if (valid && period > 0) {
            boolean smooth = editor.getCursorBlinkingType() != CursorBlinkingType.BLINK;
            boolean animating = false;
            if (System.currentTimeMillis() - lastSelectionModificationTime >= period * 2L) {
                var left = editor.getCursor().left();
                buffer = editor.getLayout().getCharLayoutOffset(left.line, left.column, buffer);
                animating = !editor.getCursor().isSelected() && isSelectionVisible() && (!smooth || getAlpha() > 0f);
                if (animating) {
                    // Keep refreshing the cursor to drive the continuous animation
                    editor.postInvalidate();
                }
                if (!smooth) {
                    visibility = !visibility;
                }
            } else {
                visibility = true;
            }
            // Only poll at the expensive per-frame rate while the smooth animation is actually
            // visible on screen. Otherwise fall back to the base period so a focused-but-idle or
            // scrolled-away editor doesn't keep waking up ~60 times a second forever.
            editor.postDelayedInLifecycle(this, smooth && animating ? SMOOTH_FRAME_INTERVAL : period);
        } else {
            visibility = true;
        }
    }

}
