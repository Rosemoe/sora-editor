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
package org.eclipse.tm4e.core.registry;

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.internal.theme.IRawTheme;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/main.ts#L39">
 *      github.com/microsoft/vscode-textmate/blob/main/src/main.ts</a>
 */
public interface IRegistryOptions {

	@Nullable
	default IRawTheme getTheme() {
		return null;
	}

	@Nullable
	default List<String> getColorMap() {
		return null;
	}

	@Nullable
	default IGrammarSource getGrammarSource(@SuppressWarnings("unused") final String scopeName) {
		return null;
	}

	@Nullable
	default Collection<@NonNull String> getInjections(@SuppressWarnings("unused") final String scopeName) {
		return null;
	}
}
