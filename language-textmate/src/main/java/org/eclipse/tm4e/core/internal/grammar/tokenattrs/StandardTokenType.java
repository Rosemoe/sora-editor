/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.tm4e.core.internal.grammar.tokenattrs;

/**
 * Standard TextMate token type.
 *
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/encodedTokenAttributes.ts#L163">
 *      github.com/microsoft/vscode-textmate/blob/main/src/encodedTokenAttributes.ts</a>
 */
public final class StandardTokenType {

	static public final int Other = 0;
	static public final int Comment = 1;
	static public final int String = 2;
	static public final int RegEx = 3;

	/**
	 * Content should be accessed statically
	 */
	private StandardTokenType() {
	}
}
