/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Initial code from https://github.com/microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 *
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.internal.theme;

import org.eclipse.tm4e.core.internal.parser.PListParser;
import org.eclipse.tm4e.core.internal.parser.PListParserJSON;
import org.eclipse.tm4e.core.internal.parser.PListParserXML;
import org.eclipse.tm4e.core.internal.parser.PListParserYAML;
import org.eclipse.tm4e.core.internal.parser.PListPath;
import org.eclipse.tm4e.core.internal.parser.PropertySettable;
import org.eclipse.tm4e.core.registry.IThemeSource;

/**
 * TextMate Theme reader utilities.
 */
public final class ThemeReader {

	private static final PropertySettable.Factory<PListPath> OBJECT_FACTORY = path -> new ThemeRaw();

	private static final PListParser<ThemeRaw> JSON_PARSER = new PListParserJSON<>(OBJECT_FACTORY);
	private static final PListParser<ThemeRaw> XML_PARSER = new PListParserXML<>(OBJECT_FACTORY);
	private static final PListParser<ThemeRaw> YAML_PARSER = new PListParserYAML<>(OBJECT_FACTORY);

	public static IRawTheme readTheme(final IThemeSource source) throws Exception {
		try (var reader = source.getReader()) {
			switch (source.getContentType()) {
			case JSON:
				return JSON_PARSER.parse(reader);
			case YAML:
				return YAML_PARSER.parse(reader);
			case XML:
			default:
				return XML_PARSER.parse(reader);
			}
		}
	}

	/**
	 * methods should be accessed statically
	 */
	private ThemeReader() {
	}
}
