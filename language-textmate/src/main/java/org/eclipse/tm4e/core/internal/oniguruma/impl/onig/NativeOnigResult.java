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
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 */
package org.eclipse.tm4e.core.internal.oniguruma.impl.onig;

import org.eclipse.tm4e.core.internal.oniguruma.OnigResult;

public class NativeOnigResult implements OnigResult {

    private final int[] ranges;
    int index = -1;

    NativeOnigResult(int[] ranges) {
        this(ranges, false);
    }

    NativeOnigResult(int[] ranges, boolean batchResult) {
        this.ranges = ranges;
        if (batchResult) {
            index = ranges[ranges.length - 1];
        }
    }

    @Override
    public int getIndexOfRegex() {
        return index;
    }

    @Override
    public int locationAt(int index) {
        return Math.max(0, ranges[2 * index]);
    }

    @Override
    public int lengthAt(int index) {
        return Math.max(0, ranges[2 * index + 1] - ranges[2 * index]);
    }

    @Override
    public int count() {
        return ranges.length / 2;
    }
}
