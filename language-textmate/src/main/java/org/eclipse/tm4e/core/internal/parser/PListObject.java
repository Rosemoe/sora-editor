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
package org.eclipse.tm4e.core.internal.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PListObject {

    public final PListObject parent;
    private final List<Object> arrayValues;
    private final Map<String, Object> mapValues;

    private String lastKey;

    public PListObject(PListObject parent, boolean valueAsArray) {
        this.parent = parent;
        if (valueAsArray) {
            this.arrayValues = new ArrayList<>();
            this.mapValues = null;
        } else {
            this.arrayValues = null;
            this.mapValues = createRaw();
        }
    }

    public String getLastKey() {
        return lastKey;
    }

    public void setLastKey(String lastKey) {
        this.lastKey = lastKey;
    }

    public void addValue(Object value) {
        if (isValueAsArray()) {
            arrayValues.add(value);
        } else {
            mapValues.put(getLastKey(), value);
        }
    }

    public boolean isValueAsArray() {
        return arrayValues != null;
    }

    public Object getValue() {
        if (isValueAsArray()) {
            return arrayValues;
        }
        return mapValues;
    }

    protected abstract Map<String, Object> createRaw();
}
