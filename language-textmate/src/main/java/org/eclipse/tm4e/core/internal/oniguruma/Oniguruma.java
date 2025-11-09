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
package org.eclipse.tm4e.core.internal.oniguruma;

import org.eclipse.tm4e.core.internal.oniguruma.impl.joni.JoniOnigRegExp;
import org.eclipse.tm4e.core.internal.oniguruma.impl.joni.JoniOnigScanner;
import org.eclipse.tm4e.core.internal.oniguruma.impl.onig.NativeOnigConfig;
import org.eclipse.tm4e.core.internal.oniguruma.impl.onig.NativeOnigRegExp;
import org.eclipse.tm4e.core.internal.oniguruma.impl.onig.NativeOnigScanner;

import java.util.List;

/**
 * Oniguruma regexp & scanner factory.
 *
 * @author Rosemoe
 */
public class Oniguruma {

    private final static boolean nativeAvailable = NativeOnigConfig.isAvailable();

    private static boolean useJoni = !nativeAvailable;


    public void setUseNativeOniguruma(boolean useNativeOniguruma) {
        if (!nativeAvailable) {
            throw new IllegalStateException("native oniguruma is not available");
        }
        useJoni = !useNativeOniguruma;
    }

    public boolean isUseNativeOniguruma() {
        return !useJoni;
    }

    public static OnigScanner newScanner(final List<String> regexps) {
        return useJoni ? new JoniOnigScanner(regexps) : new NativeOnigScanner(regexps);
    }

    public static OnigRegExp newRegex(final String pattern) {
        return newRegex(pattern, false);
    }

    public static OnigRegExp newRegex(final String pattern, boolean ignoreCase) {
        return useJoni ? new JoniOnigRegExp(pattern, ignoreCase) : new NativeOnigRegExp(pattern, ignoreCase);
    }

}
