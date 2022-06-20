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
package org.eclipse.tm4e.core.model;

import java.util.ArrayList;
import java.util.List;

class ModelTokensChangedEventBuilder {

    private final ITMModel model;
    private final List<Range> ranges;

    public ModelTokensChangedEventBuilder(ITMModel model) {
        this.model = model;
        this.ranges = new ArrayList<>();
    }

    public void registerChangedTokens(int lineNumber) {
        Range previousRange = ranges.isEmpty() ? null : ranges.get(ranges.size() - 1);

        if (previousRange != null && previousRange.toLineNumber == lineNumber - 1) {
            // extend previous range
            previousRange.toLineNumber++;
        } else {
            // insert new range
            ranges.add(new Range(lineNumber));
        }
    }

    public ModelTokensChangedEvent build() {
        if (this.ranges.isEmpty()) {
            return null;
        }
        return new ModelTokensChangedEvent(ranges, model);
    }
}
