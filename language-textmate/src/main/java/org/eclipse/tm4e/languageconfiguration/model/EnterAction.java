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
package org.eclipse.tm4e.languageconfiguration.model;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes what to do when pressing Enter.
 *
 * @see <a href=
 *      "https://github.com/microsoft/vscode/blob/8e2ec5a7ee1ae5500c645c05145359f2a814611c/src/vs/editor/common/languages/languageConfiguration.ts#L232">
 *      github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/languageConfiguration.ts#L232</a>
 */
public class EnterAction {



    public enum IndentAction {
		/**
		 * Insert new line and copy the previous line's indentation.
		 */
		None,
		/**
		 * Insert new line and indent once (relative to the previous line's
		 * indentation).
		 */
		Indent,
		/**
		 * Insert two new lines: - the first one indented which will hold the cursor -
		 * the second one at the same indentation level
		 */
		IndentOutdent,
		/**
		 * Insert new line and outdent once (relative to the previous line's
		 * indentation).
		 */
		Outdent;
	}

	/**
	 * Describe what to do with the indentation.
	 */
	public final IndentAction indentAction;

	/**
	 * Describes text to be appended after the new line and after the indentation.
	 */
	@Nullable
	public String appendText;

	/**
	 * Describes the number of characters to remove from the new line's indentation.
	 */
	@Nullable
	public Integer removeText;

	public EnterAction(final IndentAction indentAction) {
		this.indentAction = indentAction;
	}

	/**
	 * @param appendText the appendText to set
	 */
	EnterAction withAppendText(@Nullable final String appendText) {
		this.appendText = appendText;
		return this;
	}

	/**
	 * @param removeText the removeText to set
	 */
	EnterAction withRemoveText(@Nullable final Integer removeText) {
		this.removeText = removeText;
		return this;
	}

	public EnterAction copy() {
		var copy = new EnterAction(indentAction);
		copy.appendText = appendText;
		copy.removeText = removeText;
		return copy;
	}
}
