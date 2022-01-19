/**
 *  Copyright (c) 2018 Red Hat Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Lucas Bullen (Red Hat Inc.) - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports;

public class Comments {

	private final String lineComment;

	private final CharacterPair blockComment;

	public Comments(String lineComment, CharacterPair blockComment) {
		this.lineComment = lineComment;
		this.blockComment = blockComment;
	}

	public String getLineComment() {
		return lineComment;
	}

	public CharacterPair getBlockComment() {
		return blockComment;
	}
}