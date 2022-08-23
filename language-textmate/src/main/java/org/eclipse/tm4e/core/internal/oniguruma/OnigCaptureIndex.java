/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Initial code from https://github.com/atom/node-oniguruma
 * Initial copyright Copyright (c) 2013 GitHub Inc.
 * Initial license: MIT
 *
 * Contributors:
 * - GitHub Inc.: Initial code, written in JavaScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.internal.oniguruma;

import org.eclipse.jdt.annotation.Nullable;

public final class OnigCaptureIndex {

	public final int index;
	public final int start;
	public final int end;

	OnigCaptureIndex(final int index, final int start, final int end) {
		this.index = index;
		this.start = start >= 0 ? start : 0;
		this.end = end >= 0 ? end : 0;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final var other = (OnigCaptureIndex) obj;
		return end == other.end
				&& index == other.index
				&& start == other.start;
	}

	public int getLength() {
		return end - start;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + end;
		result = prime * result + index;
		result = prime * result + start;
		return result;
	}

	@Override
	public String toString() {
		return "{" +
				"\"index\": " + index +
				", \"start\": " + start +
				", \"end\": " + end +
				", \"length\": " + getLength() +
				"}";
	}
}
