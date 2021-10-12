/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core;

/**
 * TextMate exception.
 *
 */
public class TMException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TMException(String message) {
        super(message);
    }

    public TMException(String message, Throwable cause) {
        super(message, cause);
    }
}
