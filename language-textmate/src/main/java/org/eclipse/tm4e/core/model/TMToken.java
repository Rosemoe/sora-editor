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
package org.eclipse.tm4e.core.model;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @see <a href="https://github.com/microsoft/vscode/blob/d81ca6dfcae29a9bf0f648b94dff145b3665fac1/src/vs/editor/common/languages.ts#L35">
 *      github.com/microsoft/vscode/main/src/vs/editor/common/languages.ts <code>#Token</code></a>
 */
public final class TMToken {

	public final int startIndex;
	public final String type;

	public TMToken(final int startIndex, final String type) {
		this.startIndex = startIndex;
		this.type = type;
	}

	@Override
	public boolean equals(final @Nullable Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof final TMToken other) {
			return startIndex == other.startIndex
					&& type.equals(other.type);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * (31 + startIndex) + type.hashCode();
	}

	@Override
	public String toString() {
		return "(" + startIndex + ", " + (type.isEmpty() ? "<empty>" : type) + ")";
	}
}
