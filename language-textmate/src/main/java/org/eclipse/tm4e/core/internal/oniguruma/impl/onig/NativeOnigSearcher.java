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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.TMException;
import org.eclipse.tm4e.core.internal.oniguruma.OnigString;

import java.util.List;
import java.util.stream.Collectors;

import io.github.rosemoe.oniguruma.OnigNative;

class NativeOnigSearcher {

    private final List<NativeOnigRegExp> regExps;
    private final long[] pointers;

    public NativeOnigSearcher(final List<String> regExps) {
        this.regExps = regExps.stream().map(NativeOnigSearcher::createRegExp).collect(Collectors.toList());
        this.pointers = this.regExps.stream().mapToLong(NativeOnigRegExp::getNativePtr).toArray();
    }

    private static NativeOnigRegExp createRegExp(String exp) {
        // workaround for regular expressions that are unsupported by joni
        // from https://github.com/JetBrains/intellij-community/blob/881c9bc397b850bad1d393a67bcbc82861d55d79/plugins/textmate/core/src/org/jetbrains/plugins/textmate/regex/joni/JoniRegexFactory.kt#L32
        try {
            return new NativeOnigRegExp(exp);
        } catch (TMException e) {
            if (e.getCause() == null) {
                e.printStackTrace();
                return new NativeOnigRegExp("^$");
            } else {
                throw e;
            }
        }
    }

    @Nullable
    public NativeOnigResult search(final OnigString source, final int charOffset) {
        final int byteOffset = source.getByteIndexOfChar(charOffset);

        if (NativeOnigConfig.isSearchInBatch()) {
            var result = OnigNative.regexSearchBatch(pointers, source.getCacheKey(), source.getUtf8Bytes(), byteOffset, source.bytesCount);
            return result == null ? null : new NativeOnigResult(result, true);
        }

        int bestLocation = 0;
        NativeOnigResult bestResult = null;
        int index = 0;

        for (final var regExp : regExps) {
            final var result = regExp.search(source, byteOffset);
            if (result != null && result.count() > 0) {
                final int location = result.locationAt(0);

                if (bestResult == null || location < bestLocation) {
                    bestLocation = location;
                    bestResult = result;
                    bestResult.index = index;
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
