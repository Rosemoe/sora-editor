/**
 * Copyright (c) 2022 Sebastian Thomschke.
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
 * - Sebastian Thomschke - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.internal.theme;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/theme.ts#L163">
 *      github.com/microsoft/vscode-textmate/blob/main/src/theme.ts</a>
 */
public class StyleAttributes {
	public final int fontStyle;
	public final int foregroundId;
	public final int backgroundId;

	public StyleAttributes(final int fontStyle, final int foregroundId, final int backgroundId) {
		this.fontStyle = fontStyle;
		this.foregroundId = foregroundId;
		this.backgroundId = backgroundId;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final StyleAttributes other = (StyleAttributes) obj;
		if (backgroundId != other.backgroundId)
			return false;
		if (fontStyle != other.fontStyle)
			return false;
		if (foregroundId != other.foregroundId)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + backgroundId;
		result = prime * result + fontStyle;
		result = prime * result + foregroundId;
		return result;
	}
}
