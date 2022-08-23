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
import org.eclipse.tm4e.core.internal.utils.StringUtils;

public class Range {

	public final int fromLineNumber;
	public int toLineNumber;

	/**
	 * Constructs a range made of a single line
	 */
	public Range(final int lineNumber) {
		this.fromLineNumber = lineNumber;
		this.toLineNumber = lineNumber;
	}

	public Range(final int fromLineNumber, final int toLineNumber) {
		this.fromLineNumber = fromLineNumber;
		this.toLineNumber = toLineNumber;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Range other = (Range) obj;
		if (fromLineNumber != other.fromLineNumber)
			return false;
		if (toLineNumber != other.toLineNumber)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fromLineNumber;
		result = prime * result + toLineNumber;
		return result;
	}

	@Override
	public String toString() {
		return StringUtils.toString(this, sb -> sb
			.append("from=").append(fromLineNumber).append(", ")
			.append("to=").append(toLineNumber));
	}
}
