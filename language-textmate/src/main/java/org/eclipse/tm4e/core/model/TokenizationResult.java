/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Initial code from https://github.com/microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 *
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.model;

import java.util.List;

import org.eclipse.tm4e.core.grammar.IStateStack;

/**
 * @see <a href="https://github.com/microsoft/vscode/blob/d81ca6dfcae29a9bf0f648b94dff145b3665fac1/src/vs/editor/common/languages.ts#L53">
 *      github.com/microsoft/vscode/main/src/vs/editor/common/languages.ts <code>#TokenizationResult</code></a>
 */
public final class TokenizationResult {

	final List<TMToken> tokens;
	int actualStopOffset;

	IStateStack endState;

	final boolean stoppedEarly;

	public TokenizationResult(final List<TMToken> tokens, final int actualStopOffset, final IStateStack endState,
			final boolean stoppedEarly) {
		this.tokens = tokens;
		this.actualStopOffset = actualStopOffset;
		this.endState = endState;
		this.stoppedEarly = stoppedEarly;
	}

	public IStateStack getEndState() {
		return endState;
	}

	public List<TMToken> getTokens() {
		return tokens;
	}

	/**
	 * Did tokenization stop early due to reaching the time limit.
	 */
	public boolean isStoppedEarly() {
		return stoppedEarly;
	}
}
