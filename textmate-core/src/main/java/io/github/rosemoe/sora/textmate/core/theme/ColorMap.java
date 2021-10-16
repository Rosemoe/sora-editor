/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.theme;

import java.util.HashMap;
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
        color = color.toUpperCase();
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
