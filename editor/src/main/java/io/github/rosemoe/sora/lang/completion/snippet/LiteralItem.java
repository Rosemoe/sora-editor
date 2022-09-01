/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
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
package io.github.rosemoe.sora.lang.completion.snippet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LiteralItem extends SnippetItem {

    private final LiteralDefinition definition;
    private String text;

    public LiteralItem(@NonNull LiteralDefinition definition, int index) {
        setIndex(index, index + definition.getDefaultValue().length());
        this.definition = definition;
    }

    private LiteralItem(@NonNull LiteralDefinition definition, @Nullable String text, int start, int end) {
        setIndex(start, end);
        this.text = text;
        this.definition = definition;
    }

    public LiteralDefinition getDefinition() {
        return definition;
    }

    public void setText(@Nullable String text) {
        this.text = text;
    }

    @NonNull
    public String getText() {
        return text == null ? definition.getDefaultValue() : text;
    }

    @NonNull
    @Override
    public LiteralItem clone() {
        return new LiteralItem(definition, text, getStartIndex(), getEndIndex());
    }
}
