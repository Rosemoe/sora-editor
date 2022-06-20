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

import java.util.regex.Pattern;

import org.eclipse.tm4e.languageconfiguration.internal.utils.RegExpUtils;

/**
 * Describes a rule to be evaluated when pressing Enter.
 */
public class OnEnterRule {

	/**
	 * This rule will only execute if the text before the cursor matches this
	 * regular expression.
	 */
	private final Pattern beforeText;

	/**
	 * This rule will only execute if the text after the cursor matches this regular
	 * expression.
	 */
	private final Pattern afterText;

	/**
	 * The action to execute.
	 */
	private final EnterAction action;

	public OnEnterRule(String beforeText, String afterText, EnterAction action) {
		this.beforeText = RegExpUtils.create(beforeText);
		this.afterText = afterText != null ? RegExpUtils.create(afterText) : null;
		this.action = action;
	}

	public Pattern getBeforeText() {
		return beforeText;
	}

	public Pattern getAfterText() {
		return afterText;
	}

	public EnterAction getAction() {
		return action;
	}

}
