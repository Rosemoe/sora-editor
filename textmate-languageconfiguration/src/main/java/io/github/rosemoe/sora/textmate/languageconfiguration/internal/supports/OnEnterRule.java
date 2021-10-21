/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports;

import java.util.regex.Pattern;

import io.github.rosemoe.sora.textmate.languageconfiguration.internal.utils.RegExpUtils;

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
