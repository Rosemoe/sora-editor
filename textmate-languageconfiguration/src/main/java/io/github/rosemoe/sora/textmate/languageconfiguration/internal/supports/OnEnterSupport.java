/**
 *  Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.EnterAction.IndentAction;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.utils.RegExpUtils;

/**
 * On enter support.
 *
 */
public class OnEnterSupport {

	private static final List<CharacterPair> DEFAULT_BRACKETS = Arrays.asList(new CharacterPair("(", ")"), //$NON-NLS-1$ //$NON-NLS-2$
			new CharacterPair("{", "}"), new CharacterPair("[", "]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private final List<ProcessedBracketPair> brackets;

	private final List<OnEnterRule> regExpRules;

	public OnEnterSupport(List<CharacterPair> brackets, List<OnEnterRule> regExpRules) {
		this.brackets = (brackets != null ? brackets : DEFAULT_BRACKETS).stream().filter(el -> el != null)
				.map((bracket -> {
					return new ProcessedBracketPair(bracket.getKey(), bracket.getValue());
				})).collect(Collectors.toList());

		this.regExpRules = regExpRules != null ? regExpRules : Collections.emptyList();
	}

	public EnterAction onEnter(String oneLineAboveText, String beforeEnterText, String afterEnterText) {
		// (1): `regExpRules`
		for (OnEnterRule rule : regExpRules) {
			if (rule.getBeforeText().matcher(beforeEnterText).find()) {
				if (rule.getAfterText() != null) {
					if (rule.getAfterText().matcher(afterEnterText).find()) {
						return rule.getAction();
					}
				} else {
					return rule.getAction();
				}
			}
		}

		// (2): Special indent-outdent
		if (beforeEnterText.length() > 0 && afterEnterText.length() > 0) {
			for (ProcessedBracketPair bracket : brackets) {
				if (bracket.matchOpen(beforeEnterText) && bracket.matchClose(afterEnterText)) {
					return new EnterAction(IndentAction.IndentOutdent);
				}
			}
		}

		// (3): Open bracket based logic
		if (beforeEnterText.length() > 0) {
			for (ProcessedBracketPair bracket : brackets) {
				if (bracket.matchOpen(beforeEnterText)) {
					return new EnterAction(IndentAction.Indent);
				}
			}
		}
		return null;
	}

	private static class ProcessedBracketPair {

		private static final Pattern B_REGEXP = Pattern.compile("\\B"); //$NON-NLS-1$

		private final Pattern openRegExp;
		private final Pattern closeRegExp;

		public ProcessedBracketPair(String open, String close) {
			openRegExp = createOpenBracketRegExp(open);
			closeRegExp = createCloseBracketRegExp(close);
		}

		public boolean matchOpen(String beforeEnterText) {
			return openRegExp != null && openRegExp.matcher(beforeEnterText).find();
		}

		public boolean matchClose(String afterEnterText) {
			return closeRegExp != null && closeRegExp.matcher(afterEnterText).find();
		}

		private static Pattern createOpenBracketRegExp(String bracket) {
			StringBuilder str = new StringBuilder(RegExpUtils.escapeRegExpCharacters(bracket));
			String c = String.valueOf(str.charAt(0));
			if (!B_REGEXP.matcher(c).find()) {
				str.insert(0, "\\b"); //$NON-NLS-1$
			}
			str.append("\\s*$"); //$NON-NLS-1$
			return RegExpUtils.create(str.toString());
		}

		private static Pattern createCloseBracketRegExp(String bracket) {
			StringBuilder str = new StringBuilder(RegExpUtils.escapeRegExpCharacters(bracket));
			String c = String.valueOf(str.charAt(str.length() - 1));
			if (!B_REGEXP.matcher(c).find()) {
				str.append("\\b"); //$NON-NLS-1$
			}
			str.insert(0, "^\\s*"); //$NON-NLS-1$
			return RegExpUtils.create(str.toString());
		}

	}
}
