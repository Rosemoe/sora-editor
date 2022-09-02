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
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.tm4e.core.internal.grammar;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/basicScopesAttributeProvider.ts#L10">
 *      github.com/microsoft/vscode-textmate/blob/main/src/basicScopesAttributeProvider.ts</a>
 */
final class BasicScopeAttributes {

	final int languageId;
	final int /*OptionalStandardTokenType*/ tokenType;

	BasicScopeAttributes(final int languageId, final int /*OptionalStandardTokenType*/ tokenType) {
		this.languageId = languageId;
		this.tokenType = tokenType;
	}
}
