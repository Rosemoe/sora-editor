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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class CodeSnippet implements Cloneable {

    private final List<SnippetItem> items;
    private final List<PlaceholderDefinition> placeholders;

    public CodeSnippet(@NonNull List<SnippetItem> items, @NonNull List<PlaceholderDefinition> placeholders) {
        this.items = items;
        this.placeholders = placeholders;
    }

    public boolean checkContent() {
        int index = 0;
        for (var item : items) {
            if (item.getStartIndex() != index) {
                return false;
            }
            if (item instanceof PlaceholderItem) {
                if (!placeholders.contains(((PlaceholderItem) item).getDefinition())) {
                    return false;
                }
            }
            index = item.getEndIndex();
        }
        var set = new TreeSet<Integer>();
        for (var placeholder : placeholders) {
            if (!set.contains(placeholder.getId())) {
                set.add(placeholder.getId());
            } else {
                return false;
            }
        }
        return true;
    }

    public List<SnippetItem> getItems() {
        return items;
    }

    public List<PlaceholderDefinition> getPlaceholderDefinitions() {
        return placeholders;
    }

    @NonNull
    @Override
    public CodeSnippet clone() {
        var defs = new ArrayList<PlaceholderDefinition>(placeholders.size());
        var map = new HashMap<PlaceholderDefinition, PlaceholderDefinition>();
        for (PlaceholderDefinition placeholder : placeholders) {
            var n = new PlaceholderDefinition(placeholder.getId(), placeholder.getDefaultValue());
            defs.add(n);
            map.put(placeholder, n);
        }
        var itemsClone = new ArrayList<SnippetItem>(items.size());
        for (SnippetItem item : items) {
            var n = item.clone();
            itemsClone.add(n);
            if (n instanceof PlaceholderItem) {
                if (map.get(((PlaceholderItem) n).getDefinition()) != null) {
                    ((PlaceholderItem) n).setDefinition(map.get(((PlaceholderItem) n).getDefinition()));
                }
            }
        }
        return new CodeSnippet(itemsClone, defs);
    }

    public static class Builder {

        private final List<PlaceholderDefinition> definitions;
        private List<SnippetItem> items = new ArrayList<>();
        private int index;

        public Builder() {
            this(new ArrayList<>());
        }

        public Builder(@NonNull List<PlaceholderDefinition> definitions) {
            this.definitions = definitions;
        }

        public Builder addPlainText(String text) {
            if (!items.isEmpty() && items.get(items.size() - 1) instanceof PlainTextItem) {
                // Merge plain texts
                var item = (PlainTextItem) items.get(items.size() - 1);
                item.setText(item.getText() + text);
                item.setIndex(item.getStartIndex(), item.getEndIndex() + text.length());
                index += text.length();
                return this;
            }
            items.add(new PlainTextItem(text, index));
            index += text.length();
            return this;
        }

        public Builder addPlaceholder(int id) {
            return addPlaceholder(id, null);
        }

        public Builder addPlaceholder(int id, String defaultValue) {
            PlaceholderDefinition def = null;
            for (var definition : definitions) {
                if (definition.getId() == id) {
                    def = definition;
                    break;
                }
            }
            if (def == null) {
                def = new PlaceholderDefinition(id, "");
                definitions.add(def);
            }
            int delta = 0;
            if (defaultValue != null && !defaultValue.equals(def.getDefaultValue())) {
                delta = defaultValue.length() - def.getDefaultValue().length();
                def.setDefaultValue(defaultValue);
            }
            var item = new PlaceholderItem(def, index);
            items.add(item);
            if (delta != 0) {
                for (int i = 0; i < items.size() - 1; i++) {
                    var j = items.get(i);
                    if (j instanceof PlaceholderItem) {
                        var placeholder = (PlaceholderItem) j;
                        if (placeholder.getDefinition() == def) {
                            placeholder.setIndex(placeholder.getStartIndex(), placeholder.getEndIndex() + delta);
                            for (int k = i + 1;k < items.size();k++) {
                                items.get(k).shiftIndex(delta);
                            }
                        }
                    }
                }
                index = items.get(items.size() - 1).getEndIndex();
            } else {
                index += def.getDefaultValue().length();
            }
            return this;
        }

        public Builder addVariable(String name, String defaultValue) {
            items.add(new VariableItem(index, name, defaultValue));
            return this;
        }

        public CodeSnippet build() {
            return new CodeSnippet(items, definitions);
        }

    }

}
