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
package org.eclipse.tm4e.core.model;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.grammar.IGrammar;

/**
 * TextMate model API.
 *
 * @see <a href="https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/model/tokenizationTextModelPart.ts">
 *      github.com/microsoft/vscode/blob/main/src/vs/editor/common/model/tokenizationTextModelPart.ts</a>
 */
public interface ITMModel {

	/**
	 * Returns the TextMate grammar to use to parse for each lines of the document the TextMate tokens.
	 *
	 * @return the TextMate grammar to use to parse for each lines of the document the TextMate tokens.
	 */
	@Nullable
	IGrammar getGrammar();

	/**
	 * Set the TextMate grammar to use to parse for each lines of the document the TextMate tokens.
	 */
	void setGrammar(IGrammar grammar);

	/**
	 * Add model tokens changed listener.
	 *
	 * @param listener to add
	 */
	void addModelTokensChangedListener(IModelTokensChangedListener listener);

	/**
	 * Remove model tokens changed listener.
	 *
	 * @param listener to remove
	 */
	void removeModelTokensChangedListener(IModelTokensChangedListener listener);

	void dispose();

	/**
	 * @param lineIndex 0-based
	 *
	 * @throws IndexOutOfBoundsException
	 */
	@Nullable
	List<TMToken> getLineTokens(int lineIndex);
}
