/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package org.eclipse.tm4e.languageconfiguration.internal.supports;


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
