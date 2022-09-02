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

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Describes a rule to be evaluated when pressing Enter.
 *
 * @see <a href=
 *      "https://github.com/microsoft/vscode/blob/8e2ec5a7ee1ae5500c645c05145359f2a814611c/src/vs/editor/common/languages/languageConfiguration.ts#L157">
 *      github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/languageConfiguration.ts#L157</a>
 */
public final class OnEnterRule {

	/**
	 * This rule will only execute if the text before the cursor matches this regular expression.
	 */
	public final Pattern beforeText;

	/**
	 * This rule will only execute if the text after the cursor matches this regular expression.
	 */
	@Nullable
	public final Pattern afterText;

	// TODO @Nullable public final Pattern previousLineText;

	/**
	 * The action to execute.
	 */
	public final EnterAction action;

	public OnEnterRule(final Pattern beforeText, @Nullable final Pattern afterText, final EnterAction action) {
		this.beforeText = beforeText;
		this.afterText = afterText;
		this.action = action;
	}

	/**
	 * Only for unit tests
	 *
	 * @throws PatternSyntaxException if beforeText or afterText contain invalid regex pattern
	 */
	OnEnterRule(final String beforeText, @Nullable final String afterText, final EnterAction action) {
		this.beforeText = Pattern.compile(beforeText);
		this.afterText = afterText == null ? null : Pattern.compile(afterText);
		this.action = action;
	}
}
