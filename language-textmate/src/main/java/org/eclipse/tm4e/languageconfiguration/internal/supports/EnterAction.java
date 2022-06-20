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

/**
 * Describes what to do when pressing Enter.
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
	private final IndentAction indentAction;

	/**
	 * Describe whether to outdent current line.
	 */
	private Boolean outdentCurrentLine;
	/**
	 * Describes text to be appended after the new line and after the indentation.
	 */
	private String appendText;
	/**
	 * Describes the number of characters to remove from the new line's indentation.
	 */
	private Integer removeText;

	public EnterAction(IndentAction indentAction) {
		this.indentAction = indentAction;
	}

	public IndentAction getIndentAction() {
		return indentAction;
	}

	/**
	 * @return the outdentCurrentLine
	 */
	public Boolean getOutdentCurrentLine() {
		return outdentCurrentLine;
	}

	/**
	 * @param outdentCurrentLine
	 *            the outdentCurrentLine to set
	 * @return
	 */
	public EnterAction setOutdentCurrentLine(Boolean outdentCurrentLine) {
		this.outdentCurrentLine = outdentCurrentLine;
		return this;
	}

	/**
	 * @return the appendText
	 */
	public String getAppendText() {
		return appendText;
	}

	/**
	 * @param appendText
	 *            the appendText to set
	 * @return
	 */
	public EnterAction setAppendText(String appendText) {
		this.appendText = appendText;
		return this;
	}

	/**
	 * @return the removeText
	 */
	public Integer getRemoveText() {
		return removeText;
	}

	/**
	 * @param removeText
	 *            the removeText to set
	 * @return
	 */
	public EnterAction setRemoveText(Integer removeText) {
		this.removeText = removeText;
		return this;
	}

}
