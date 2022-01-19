/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/atom/node-oniguruma
 * Initial copyright Copyright (c) 2013 GitHub Inc.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - GitHub Inc.: Initial code, written in JavaScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */

package io.github.rosemoe.sora.textmate.core.internal.oniguruma;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OnigSearcher {

    private final List<OnigRegExp> regExps;

    public OnigSearcher(String[] regexps) {
        this.regExps = Arrays.stream(regexps).map(OnigRegExp::new).collect(Collectors.toList());
    }

    public OnigResult search(OnigString source, int charOffset) {
        int byteOffset = source.convertUtf16OffsetToUtf8(charOffset);

        int bestLocation = 0;
        OnigResult bestResult = null;
        int index = 0;

        for (OnigRegExp regExp : regExps) {
            OnigResult result = regExp.search(source, byteOffset);
            if (result != null && result.count() > 0) {
                int location = result.locationAt(0);

                if (bestResult == null || location < bestLocation) {
                    bestLocation = location;
                    bestResult = result;
                    bestResult.setIndex(index);
                }

                if (location == byteOffset) {
                    break;
                }
            }
            index++;
        }
        return bestResult;
    }

}
