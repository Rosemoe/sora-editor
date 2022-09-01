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

import java.util.List;
import java.util.TreeSet;

public class CodeSnippet {

    private List<SnippetItem> items;
    private List<LiteralDefinition> literals;

    public CodeSnippet(@NonNull List<SnippetItem> items, @NonNull List<LiteralDefinition> literals) {
        this.items = items;
        this.literals = literals;
    }

    public boolean checkContent() {
        int index = 0;
        for (var item : items) {
            if (item.getStartIndex() != index) {
                return false;
            }
            if (item instanceof LiteralItem) {
                if (!literals.contains(((LiteralItem) item).getDefinition())) {
                    return false;
                }
            }
            index = item.getEndIndex();
        }
        var set = new TreeSet<String>();
        for (var literal : literals) {
            if (!set.contains(literal.getId())) {
                set.add(literal.getId());
            } else {
                return false;
            }
        }
        return true;
    }

    public List<SnippetItem> getItems() {
        return items;
    }

    public List<LiteralDefinition> getLiteralDefinitions() {
        return literals;
    }

}
