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

    private final String pattern;
    private long nativePtr;

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
        var result = OnigNative.regexSearch(nativePtr, str.getCacheKey(), str.getUtf8Bytes(), startPosition, str.bytesCount);
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

    long getNativePtr() {
        return nativePtr;
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
