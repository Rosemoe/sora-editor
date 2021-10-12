/**
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
package io.github.rosemoe.sora.textmate.core.internal.parser;

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
