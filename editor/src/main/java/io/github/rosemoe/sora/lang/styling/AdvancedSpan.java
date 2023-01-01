/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
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
package io.github.rosemoe.sora.lang.styling;

import java.util.Objects;

import io.github.rosemoe.sora.lang.styling.inlayHint.InlayHint;

public class AdvancedSpan extends Span {

    public ExternalRenderer renderer = null;
    public InlayHint hintBefore;
    public InlayHint hintAfter;

    public AdvancedSpan(int column, long style) {
        super(column, style);
    }

    public AdvancedSpan setExternalRenderer(ExternalRenderer renderer) {
        this.renderer = renderer;
        return this;
    }

    public AdvancedSpan setInlayHintBefore(InlayHint hintBefore) {
        this.hintBefore = hintBefore;
        return this;
    }

    public AdvancedSpan setInlayHintAfter(InlayHint hintAfter) {
        this.hintAfter = hintAfter;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AdvancedSpan that = (AdvancedSpan) o;
        return Objects.equals(renderer, that.renderer) && Objects.equals(hintBefore, that.hintBefore) && Objects.equals(hintAfter, that.hintAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), renderer, hintBefore, hintAfter);
    }
}
