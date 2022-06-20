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
package org.eclipse.tm4e.languageconfiguration;

import java.util.List;

import org.eclipse.tm4e.languageconfiguration.internal.supports.AutoClosingPairConditional;
import org.eclipse.tm4e.languageconfiguration.internal.supports.CharacterPair;
import org.eclipse.tm4e.languageconfiguration.internal.supports.Comments;
import org.eclipse.tm4e.languageconfiguration.internal.supports.Folding;
import org.eclipse.tm4e.languageconfiguration.internal.supports.IndentationRule;
import org.eclipse.tm4e.languageconfiguration.internal.supports.OnEnterRule;

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
