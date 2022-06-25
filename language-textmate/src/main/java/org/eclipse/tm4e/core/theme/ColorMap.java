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
package org.eclipse.tm4e.core.theme;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class ColorMap {

    private final Map<String /* color */, Integer /* ID color */> color2id;
    private int lastColorId;

    public ColorMap() {
        this.lastColorId = 0;
        this.color2id = new HashMap<>();
    }

    public int getId(String color) {
        if (color == null) {
            return 0;
        }
        color = color.toUpperCase(Locale.ROOT);
        Integer value = this.color2id.get(color);
        if (value != null) {
            return value;
        }
        value = ++this.lastColorId;
        this.color2id.put(color, value);
        return value;
    }

    public String getColor(int id) {
        for (Entry<String, Integer> entry : color2id.entrySet()) {
            if (id == entry.getValue()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<String> getColorMap() {
        return this.color2id.keySet();
    }

    @Override
    public int hashCode() {
        return Objects.hash(color2id, lastColorId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ColorMap other = (ColorMap) obj;
        return Objects.equals(color2id, other.color2id) && lastColorId == other.lastColorId;
    }
}
