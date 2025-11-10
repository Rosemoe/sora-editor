/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.lang.styling.span.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.SpanPool;
import io.github.rosemoe.sora.lang.styling.color.ResolvableColor;
import io.github.rosemoe.sora.lang.styling.span.SpanExt;

/**
 * Span without SpanExt support.
 *
 * @author Rosemoe
 */
public class NoExtSpanImpl implements Span {
    private final static SpanPool<NoExtSpanImpl> pool = new SpanPool<>(NoExtSpanImpl::new);

    private int column;
    private long style;
    private Object extra;

    NoExtSpanImpl() {

    }

    NoExtSpanImpl(int column, long style) {
        this.column = column;
        this.style = style;
    }

    public static NoExtSpanImpl obtain(int column, long style) {
        return pool.obtain(column, style);
    }


    @Override
    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public int getColumn() {
        return this.column;
    }

    @Override
    public void setStyle(long style) {
        this.style = style;
    }

    @Override
    public long getStyle() {
        return this.style;
    }

    @Override
    public void setUnderlineColor(@Nullable ResolvableColor color) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public ResolvableColor getUnderlineColor() {
        return null;
    }

    @Override
    public void setExtra(Object extraData) {
        this.extra = extraData;
    }

    @Override
    @Nullable
    public Object getExtra() {
        return this.extra;
    }

    @Override
    public void setSpanExt(int extType, @Nullable SpanExt ext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSpanExt(int extType) {
        return false;
    }

    @Nullable
    @Override
    public <T> T getSpanExt(int extType) {
        return null;
    }

    @Override
    public void removeAllSpanExt() {

    }

    @Override
    public void reset() {
        setColumn(0);
        setStyle(0L);
        extra = null;
    }

    @NonNull
    @Override
    public Span copy() {
        return new NoExtSpanImpl(this.column, this.style);
    }

    @Override
    public boolean recycle() {
        reset();
        return pool.offer(this);
    }

    @Override
    public String toString() {
        return "NoExtSpanImpl{" +
                "column=" + column +
                ", style=" + style +
                ", extra=" + extra +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NoExtSpanImpl noExtSpan = (NoExtSpanImpl) o;
        return column == noExtSpan.column && style == noExtSpan.style && Objects.equals(extra, noExtSpan.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, style, extra);
    }
}
