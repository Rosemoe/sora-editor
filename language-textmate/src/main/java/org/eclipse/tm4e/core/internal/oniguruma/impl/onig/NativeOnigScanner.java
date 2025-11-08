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
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult;
import org.eclipse.tm4e.core.internal.oniguruma.OnigScanner;
import org.eclipse.tm4e.core.internal.oniguruma.OnigString;
import org.eclipse.tm4e.core.internal.oniguruma.impl.OnigScannerMatchImpl;

import java.util.List;

public class NativeOnigScanner implements OnigScanner {

    private final NativeOnigSearcher searcher;

    public NativeOnigScanner(final List<String> regexps) {
        searcher = new NativeOnigSearcher(regexps);
    }

    @Override
    public @Nullable OnigScannerMatchImpl findNextMatch(final OnigString source, final int startPosition) {
        final OnigResult bestResult = searcher.search(source, startPosition);
        if (bestResult != null) {
            return new OnigScannerMatchImpl(bestResult, source);
        }
        return null;
    }

}
