/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/Microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.model;

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
