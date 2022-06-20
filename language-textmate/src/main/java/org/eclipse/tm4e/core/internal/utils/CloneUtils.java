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
package org.eclipse.tm4e.core.internal.utils;

import org.eclipse.tm4e.core.internal.grammar.parser.Raw;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tm4e.core.internal.types.IRawRepository;

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
