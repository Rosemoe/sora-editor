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
package org.eclipse.tm4e.languageconfiguration.supports;

import org.eclipse.jdt.annotation.Nullable;

import org.eclipse.tm4e.languageconfiguration.model.CharacterPair;
import org.eclipse.tm4e.languageconfiguration.model.CommentRule;

import io.github.rosemoe.sora.text.Content;

public final class CommentSupport {

	@Nullable
	private final CommentRule comments;

	public CommentSupport(@Nullable final CommentRule comments) {
		this.comments = comments;
	}

	private boolean isInComment(final Content text, final int offset) {
		try {
			if (isInBlockComment(text.subSequence(0, offset).toString())) {
				return true;
			}
			var indexer = text.getIndexer();
			final int line = indexer.getCharLine(offset);
			final int lineOffset = indexer.getCharIndex(line,0);
			return isInLineComment(text.subSequence(lineOffset, offset - lineOffset).toString());
		} catch (final Exception e) {
			return false;
		}
	}

	@Nullable
	public String getLineComment() {
		final var comments = this.comments;
		return comments == null ? null : comments.lineComment;
	}

	@Nullable
	public CharacterPair getBlockComment() {
		final var comments = this.comments;
		return comments == null ? null : comments.blockComment;
	}

	private boolean isInLineComment(final String indexLinePrefix) {
		final var comments = this.comments;
		if (comments == null)
			return false;
		return indexLinePrefix.indexOf(comments.lineComment) != -1;
	}

	private boolean isInBlockComment(final String indexPrefix) {
		final var comments = this.comments;
		if (comments == null)
			return false;

		final var blockComment = comments.blockComment;
		if (blockComment == null)
			return false;

		final String commentOpen = blockComment.open;
		final String commentClose = blockComment.close;
		int index = indexPrefix.indexOf(commentOpen);
		while (index != -1 && index < indexPrefix.length()) {
			final int closeIndex = indexPrefix.indexOf(commentClose, index + commentOpen.length());
			if (closeIndex == -1) {
				return true;
			}
			index = indexPrefix.indexOf(commentOpen, closeIndex + commentClose.length());
		}
		return false;
	}
}
