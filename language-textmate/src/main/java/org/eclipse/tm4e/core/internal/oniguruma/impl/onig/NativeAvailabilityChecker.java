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

import io.github.rosemoe.oniguruma.OnigNative;

public class NativeAvailabilityChecker {

    public static boolean isAvailable() {
        try {
            OnigNative.releaseRegex(0L);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

}
