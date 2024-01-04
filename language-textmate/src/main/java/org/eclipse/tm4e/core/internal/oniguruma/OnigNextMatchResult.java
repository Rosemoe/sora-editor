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

import java.util.Arrays;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-scanner.cc">
 *      github.com/atom/node-oniguruma/blob/master/src/onig-scanner.cc</a>
 */
public final class OnigNextMatchResult {

	private final int index;
	private final OnigCaptureIndex[] captureIndices;

	OnigNextMatchResult(final OnigResult result, final OnigString source) {
		this.index = result.getIndex();
		this.captureIndices = captureIndicesOfMatch(result, source);
	}

	private OnigCaptureIndex[] captureIndicesOfMatch(final OnigResult result, final OnigString source) {
		final int resultCount = result.count();
		final var captures = new OnigCaptureIndex[resultCount];
		for (int i = 0; i < resultCount; i++) {
			final int loc = result.locationAt(i);
			final int captureStart = source.getCharIndexOfByte(loc);
			final int captureEnd = source.getCharIndexOfByte(loc + result.lengthAt(i));
			captures[i] = new OnigCaptureIndex(captureStart, captureEnd);
		}
		return captures;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		final var other = (OnigNextMatchResult) obj;
		return index == other.index
				&& Arrays.equals(captureIndices, other.captureIndices);
	}

	public OnigCaptureIndex[] getCaptureIndices() {
		return captureIndices;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + Arrays.hashCode(captureIndices);
		return result;
	}

	@Override
	public String toString() {
		final var result = new StringBuilder("{\n");
		result.append("  \"index\": ");
		result.append(getIndex());
		result.append(",\n");
		result.append("  \"captureIndices\": [\n");
		int i = 0;
		for (final OnigCaptureIndex captureIndex : getCaptureIndices()) {
			if (i > 0) {
				result.append(",\n");
			}
			result.append("    ");
			result.append(captureIndex);
			i++;
		}
		result.append("\n");
		result.append("  ]\n");
		result.append("}");
		return result.toString();
	}
}
