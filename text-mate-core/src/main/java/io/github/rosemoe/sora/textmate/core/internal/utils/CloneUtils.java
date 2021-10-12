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
package io.github.rosemoe.sora.textmate.core.internal.utils;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.textmate.core.internal.grammar.parser.Raw;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawRepository;

/**
 * Clone utilities.
 *
 */
public class CloneUtils {

    /**
     * Helper class, use methods statically
     */
    private CloneUtils() {

    }

    public static Object clone(Object value) {
        if (value instanceof Raw) {
            Raw rowToClone = (Raw) value;
            Raw raw = new Raw();
            for (Entry<String, Object> entry : rowToClone.entrySet()) {
                raw.put(entry.getKey(), clone(entry.getValue()));
            }
            return raw;
        } else if (value instanceof List) {
            return ((List<?>) value).stream().map(CloneUtils::clone).collect(Collectors.toList());
        } else if (value instanceof String) {
            return value;
        } else if (value instanceof Integer) {
            return value;
        } else if (value instanceof Boolean) {
            return value;
        }
        return value;
    }

    public static IRawRepository mergeObjects(IRawRepository... sources) {
        Raw target = new Raw();
        for (IRawRepository source : sources) {
            Set<Entry<String, Object>> entries = ((Map<String, Object>) source).entrySet();
            for (Entry<String, Object> entry : entries) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }
}
