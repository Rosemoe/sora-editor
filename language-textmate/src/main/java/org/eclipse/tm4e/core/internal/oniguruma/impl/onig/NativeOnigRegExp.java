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
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp;
import org.eclipse.tm4e.core.internal.oniguruma.OnigString;
import org.eclipse.tm4e.core.internal.utils.StringUtils;

import io.github.rosemoe.oniguruma.OnigNative;

public class NativeOnigRegExp implements OnigRegExp {

    @Nullable
    private OnigString lastSearchString;

    private int lastSearchPosition = -1;

    @Nullable
    private NativeOnigResult lastSearchResult;

    private final String pattern;
    private long nativePtr;

    private final boolean hasGAnchor;

    /**
     * @throws TMException if parsing fails
     */
    public NativeOnigRegExp(final String pattern) {
        this(pattern, false);
    }

    /**
     * @throws TMException if parsing fails
     */
    public NativeOnigRegExp(final String pattern, final boolean ignoreCase) {
        this.pattern = pattern;
        hasGAnchor = pattern.contains("\\G");
        nativePtr = OnigNative.newRegex(pattern, ignoreCase);
        if (nativePtr == 0L) {
            throw new TMException("Parsing regex pattern \"" + pattern + "\" failed");
        }
    }

    /**
     * @return null if not found
     */
    @Override
    public @Nullable NativeOnigResult search(final OnigString str, final int startPosition) {
        if (hasGAnchor) {
            // Should not use caching, because the regular expression
            // targets the current search position (\G)
            return search(str.getBytesUTF8(), startPosition, str.bytesCount);
        }

        synchronized (this) {
            final var lastSearchResult0 = this.lastSearchResult;
            if (lastSearchString == str
                    && lastSearchPosition <= startPosition
                    && (lastSearchResult0 == null || lastSearchResult0.locationAt(0) >= startPosition)) {
                return lastSearchResult0;
            }
        }

        var result = search(str.getBytesUTF8(), startPosition, str.bytesCount);
        synchronized (this) {
            lastSearchString = str;
            lastSearchPosition = startPosition;
            lastSearchResult = result;
        }
        return lastSearchResult;
    }

    @Nullable
    private NativeOnigResult search(final byte[] data, final int startPosition, final int end) {
        var result = OnigNative.regexSearch(nativePtr, data, startPosition, end);
        if (result != null) {
            return new NativeOnigResult(result);
        }
        return null;
    }

    @Override
    public String pattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this, sb -> {
            sb.append("pattern=").append(pattern);
        });
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (nativePtr != 0L) {
                OnigNative.releaseRegex(nativePtr);
                nativePtr = 0L;
            }
        } finally {
            super.finalize();
        }
    }
}
