/**
 * Copyright (c) 2023 Vegard IT GmbH and others.
 * <p>
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Sebastian Thomschke (Vegard IT) - initial implementation
 */
package org.eclipse.tm4e.core.internal.parser;

import java.io.Reader;
import java.util.Map;


public final class TMParserYAML extends TMParserJSON {

    public static final TMParserYAML INSTANCE = new TMParserYAML();

    @Override
    @SuppressWarnings({"null", "unchecked"})
    protected Map<String, Object> loadRaw(final Reader source) {
		// wait for a YAML parser ??
        throw new UnsupportedOperationException();
    }
}
