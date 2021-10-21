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


import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.LineNumberCalculator;

public class CommentSupport {

	private Comments comments;

	public CommentSupport(Comments comments) {
		this.comments = comments;
	}

	public boolean isInComment(Content document, int offset) {
		try {
			if (isInBlockComment((String) document.subSequence(0, offset))) {
				return true;
			}
			LineNumberCalculator calculator=new LineNumberCalculator(document.toString());
			calculator.update(offset);
			int line = calculator.getLine();
			return isInLineComment(document.getLine(line).toString());
		} catch (Exception e) {
			return false;
		}
	}

	public String getLineComment() {
		return comments.getLineComment();
	}

	public CharacterPair getBlockComment() {
		return comments.getBlockComment();
	}

	private boolean isInLineComment(String indexLinePrefix) {
		return indexLinePrefix.contains(comments.getLineComment());
	}

	private boolean isInBlockComment(String indexPrefix) {
		String commentOpen = comments.getBlockComment().getKey();
		String commentClose = comments.getBlockComment().getValue();
		int index = indexPrefix.indexOf(commentOpen);
		while (index != -1 && index < indexPrefix.length()) {
			int closeIndex = indexPrefix.indexOf(commentClose, index + commentOpen.length());
			if (closeIndex == -1) {
				return true;
			}
			index = indexPrefix.indexOf(commentOpen, closeIndex + commentClose.length());
		}
		return false;
	}
}
