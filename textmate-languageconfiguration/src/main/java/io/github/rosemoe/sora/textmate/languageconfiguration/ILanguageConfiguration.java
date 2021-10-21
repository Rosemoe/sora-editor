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
package io.github.rosemoe.sora.textmate.languageconfiguration;

import java.util.List;

import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.AutoClosingPairConditional;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.CharacterPair;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.Comments;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.Folding;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.IndentationRule;
import io.github.rosemoe.sora.textmate.languageconfiguration.internal.supports.OnEnterRule;

/**
 * See <a href="https://code.visualstudio.com/api/language-extensions/language-configuration-guide">language-configuration-guide</a>
 *
 */
public interface ILanguageConfiguration {
	/**
	 * Returns the language's comments. The comments are used by
	 * {@link AutoClosingPairConditional} when <code>notIn</code> contains
	 * <code>comment</code>
	 *
	 * @return the language's comments or <code>null</code> if not set
	 */
	Comments getComments();

	/**
	 * Returns the language's brackets. This configuration implicitly affects
	 * pressing Enter around these brackets.
	 *
	 * @return the language's brackets. This configuration implicitly affects
	 *         pressing Enter around these brackets.
	 */
	List<CharacterPair> getBrackets();

	/**
	 * Returns the language's auto closing pairs. The 'close' character is
	 * automatically inserted with the 'open' character is typed. If not set, the
	 * configured brackets will be used.
	 *
	 * @return the language's auto closing pairs. The 'close' character is
	 *         autautomatically inserted with the 'open' character is typed. If not
	 *         set, the configured brackets will be used.
	 */
	List<AutoClosingPairConditional> getAutoClosingPairs();

	/**
	 * By default, VS Code only autocloses pairs if there is whitespace right after the cursor.
	 * Set autoCloseBefore to override that behavior
	 * @return the language's autoCloseBefore
	 */
	String getAutoCloseBefore();
	/**
	 * Returns the language's rules to be evaluated when pressing Enter.
	 *
	 * @return the language's rules to be evaluated when pressing Enter.
	 */
	List<OnEnterRule> getOnEnterRules();

	/**
	 * Returns the language's surrounding pairs. When the 'open' character is typed
	 * on a selection, the selected string is surrounded by the open and close
	 * characters. If not set, the autoclosing pairs settings will be used.
	 *
	 * @return the language's surrounding pairs. When the 'open' character is typed
	 *         on a selection, the selected string is surrounded by the open and
	 *         close characters. If not set, the autoclosing pairs settings will be
	 *         used.
	 */
	List<CharacterPair> getSurroundingPairs();

	/**
	 * Returns the language's folding rules.
	 *
	 * @return the language's folding or <code>null</code> if not set
	 */
	Folding getFolding();

	/**
	 * Returns the language's definition of a word. This is the regex used when
	 * refering to a word.
	 *
	 * @return the language's word pattern or <code>null</code> if not set
	 */
	String getWordPattern();

	/**
	 * Returns the language's indentation rules. This is the regex used when
	 * refering to increase or decrease indent.
	 *
	 * @return the language's indentation rules or <code>null</code> if not set
	 */
	IndentationRule getIndentationRule();
}
