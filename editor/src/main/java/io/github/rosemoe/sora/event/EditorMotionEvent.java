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
package io.github.rosemoe.sora.event;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.TextRange;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.RegionResolverKt;

/**
 * Base class for click events
 *
 * @author Rosemoe
 * @see ClickEvent
 * @see DoubleClickEvent
 * @see LongPressEvent
 * @see ContextClickEvent
 * @see HoverEvent
 */
public abstract class EditorMotionEvent extends Event {

    /**
     * Motion occurred outside of editor.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int REGION_OUTBOUND = RegionResolverKt.REGION_OUTBOUND;

    /**
     * Motion occurred in line number region.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int REGION_LINE_NUMBER = RegionResolverKt.REGION_LINE_NUMBER;

    /**
     * Motion occurred in side icon region.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int REGION_SIDE_ICON = RegionResolverKt.REGION_SIDE_ICON;

    /**
     * Motion occurred in divider margin region.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int REGION_DIVIDER_MARGIN = RegionResolverKt.REGION_DIVIDER_MARGIN;

    /**
     * Motion occurred in line divider region.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int REGION_DIVIDER = RegionResolverKt.REGION_DIVIDER;

    /**
     * Motion occurred in text region.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int REGION_TEXT = RegionResolverKt.REGION_TEXT;

    /**
     * Motion occurred in editor bounds on the Y-axis.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int IN_BOUND = RegionResolverKt.IN_BOUND;

    /**
     * Motion occurred outside of editor bounds on the Y-axis.
     *
     * @see EditorMotionEvent#getMotionRegion()
     */
    public static final int OUT_BOUND = RegionResolverKt.OUT_BOUND;

    private final CharPosition pos;
    private final MotionEvent event;
    private final Span span;
    private final TextRange spanRange;
    private final int motionRegion;
    private final int motionBound;

    public EditorMotionEvent(@NonNull CodeEditor editor, @NonNull CharPosition position,
                             @NonNull MotionEvent event, @Nullable Span span, @Nullable TextRange spanRange,
                             int motionRegion, int motionBound) {
        super(editor);
        this.pos = position;
        this.event = event;
        this.span = span;
        this.spanRange = spanRange;
        this.motionRegion = motionRegion;
        this.motionBound = motionBound;
    }

    @Override
    public boolean canIntercept() {
        return true;
    }

    public boolean isFromMouse() {
        return event.isFromSource(InputDevice.SOURCE_MOUSE);
    }

    public int getLine() {
        return pos.line;
    }

    public int getColumn() {
        return pos.column;
    }

    public int getIndex() {
        return pos.index;
    }

    public CharPosition getCharPosition() {
        return pos.fromThis();
    }

    public float getX() {
        return event.getX();
    }

    public float getY() {
        return event.getY();
    }

    /**
     * Get original event object from Android framework
     */
    @NonNull
    public MotionEvent getCausingEvent() {
        return event;
    }

    /**
     * Get span at event character position, maybe null.
     */
    @Nullable
    public Span getSpan() {
        return span;
    }

    /**
     * Get span range at event character position, maybe null
     */
    @Nullable
    public TextRange getSpanRange() {
        return spanRange;
    }

    /**
     * Get the X-axis region where the motion event occurred. Check
     * {@link #getMotionBound()} to get the Y-axis bound.
     *
     * @return The region within editor where the motion event occurred.
     * @see #REGION_OUTBOUND
     * @see #REGION_LINE_NUMBER
     * @see #REGION_SIDE_ICON
     * @see #REGION_DIVIDER_MARGIN
     * @see #REGION_DIVIDER
     * @see #REGION_TEXT
     * @see #getMotionBound()
     */
    public int getMotionRegion() {
        return motionRegion;
    }

    /**
     * Get the Y-axis bounds of the motion event. Check {@link #getMotionRegion()} to get the
     * X-axis region.
     *
     * @return The bound of the motion event.
     * @see #IN_BOUND
     * @see #OUT_BOUND
     * @see #getMotionRegion()
     */
    public int getMotionBound() {
        return motionBound;
    }
}
