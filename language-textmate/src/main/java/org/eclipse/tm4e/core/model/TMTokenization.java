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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IStateStack;
import org.eclipse.tm4e.core.internal.grammar.StateStack;

import com.google.common.base.Splitter;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode/blob/main/src/vs/workbenc/services/textMate/common/TMTokenization.ts">
 *      github.com/microsoft/vscode/blob/main/src/vs/workbenc/services/textMate/common/TMTokenization.ts</a>
 */
public class TMTokenization implements ITokenizationSupport {

	private final IGrammar _grammar;
	private final IStateStack _initialState;
	private final DecodeMap decodeMap = new DecodeMap();

	public TMTokenization(final IGrammar grammar) {
		this(grammar, StateStack.NULL);
	}

	public TMTokenization(final IGrammar grammar, final IStateStack initialState) {
		this._grammar = grammar;
		_initialState = initialState;
	}

	@Override
	public IStateStack getInitialState() {
		return _initialState;
	}

	@Override
	public TokenizationResult tokenize(final String line, @Nullable final IStateStack state) {
		return tokenize(line, state, null, null);
	}

	@Override
	public TokenizationResult tokenize(final String line,
		@Nullable final IStateStack state,
		@Nullable final Integer offsetDeltaOrNull,
		@Nullable final Duration timeLimit) {

		final int offsetDelta = offsetDeltaOrNull == null ? 0 : offsetDeltaOrNull;
		final var tokenizationResult = _grammar.tokenizeLine(line, state, timeLimit);
		final var tokens = tokenizationResult.getTokens();

		// Create the result early and fill in the tokens later
		final var tmTokens = new ArrayList<TMToken>(tokens.length < 10 ? tokens.length : 10);
		String lastTokenType = null;
		for (final var token : tokens) {
			final int tokenStartIndex = token.getStartIndex();
			final var tokenType = decodeTextMateToken(this.decodeMap, token.getScopes());

			// do not push a new token if the type is exactly the same (also helps with ligatures)
			if (!tokenType.equals(lastTokenType)) {
				tmTokens.add(new TMToken(tokenStartIndex + offsetDelta, tokenType));
				lastTokenType = tokenType;
			}
		}

		final var lastToken = tokens[tokens.length - 1];

		return new TokenizationResult(
			tmTokens,

			// TODO Math.min() is a temporary workaround because currently in some cases lastToken.getEndIndex()
			// incorrectly returns larger values than line.length() for some reasons.
			// See for example GrammarTest#testTokenize1IllegalToken()
			offsetDelta + Math.min(line.length(), lastToken.getEndIndex()),

			tokenizationResult.getRuleStack(),
			tokenizationResult.isStoppedEarly());
	}

	private String decodeTextMateToken(final DecodeMap decodeMap, final List<String> scopes) {
		final var prevTokenScopes = decodeMap.prevToken.scopes;
		final int prevTokenScopesLength = prevTokenScopes.size();
		final var prevTokenScopeTokensMaps = decodeMap.prevToken.scopeTokensMaps;

		final var scopeTokensMaps = new LinkedHashMap<Integer, Map<Integer, Boolean>>();
		Map<Integer, Boolean> prevScopeTokensMaps = new LinkedHashMap<>();
		boolean sameAsPrev = true;
		for (int level = 1/* deliberately skip scope 0 */; level < scopes.size(); level++) {
			final String scope = scopes.get(level);

			if (sameAsPrev) {
				if (level < prevTokenScopesLength && prevTokenScopes.get(level).equals(scope)) {
					prevScopeTokensMaps = prevTokenScopeTokensMaps.get(level);
					scopeTokensMaps.put(level, prevScopeTokensMaps);
					continue;
				}
				sameAsPrev = false;
			}

			final Integer[] tokens = decodeMap.getTokenIds(scope);
			prevScopeTokensMaps = new LinkedHashMap<>(prevScopeTokensMaps);
			for (final Integer token : tokens) {
				prevScopeTokensMaps.put(token, true);
			}
			scopeTokensMaps.put(level, prevScopeTokensMaps);
		}

		decodeMap.prevToken = new TMTokenDecodeData(scopes, scopeTokensMaps);
		return decodeMap.getToken(prevScopeTokensMaps);
	}

	@NonNullByDefault({})
	 static class TMTokenDecodeData {

		  Map<Integer, Map<Integer, Boolean>> scopeTokensMaps;
		  List<String> scopes;

		private TMTokenDecodeData(
		@NonNull List<String> scopes,
		@NonNull Map<Integer, Map<Integer, Boolean>> scopeTokensMaps) {
		 this.scopes = scopes;
		 this.scopeTokensMaps = scopeTokensMaps;
	 }
	}

	private static final class DecodeMap {

		private static final Splitter BY_DOT_SPLITTER = Splitter.on('.');

		private int lastAssignedId = 0;
		private final Map<String /* scope */, Integer @Nullable [] /* ids */ > scopeToTokenIds = new LinkedHashMap<>();
		private final Map<String /* token */, @Nullable Integer /* id */ > tokenToTokenId = new LinkedHashMap<>();
		private final Map<Integer /* id */, String /* id */ > tokenIdToToken = new LinkedHashMap<>();
		TMTokenDecodeData prevToken = new TMTokenDecodeData(Collections.emptyList(), new LinkedHashMap<>());

		Integer[] getTokenIds(final String scope) {
			Integer[] tokens = this.scopeToTokenIds.get(scope);
			if (tokens != null) {
				return tokens;
			}
			final String[] tmpTokens = BY_DOT_SPLITTER.splitToList(scope).toArray(new String[0]);

			tokens = new Integer[tmpTokens.length];
			for (int i = 0; i < tmpTokens.length; i++) {
				final String token = tmpTokens[i];
				Integer tokenId = this.tokenToTokenId.get(token);
				if (tokenId == null) {
					tokenId = ++this.lastAssignedId;
					this.tokenToTokenId.put(token, tokenId);
					this.tokenIdToToken.put(tokenId, token);
				}
				tokens[i] = tokenId;
			}

			this.scopeToTokenIds.put(scope, tokens);
			return tokens;
		}

		String getToken(final Map<Integer, Boolean> tokenMap) {
			final StringBuilder result = new StringBuilder();
			boolean isFirst = true;
			for (int i = 1; i <= this.lastAssignedId; i++) {
				if (tokenMap.containsKey(i)) {
					if (isFirst) {
						isFirst = false;
						result.append(this.tokenIdToToken.get(i));
					} else {
						result.append('.');
						result.append(this.tokenIdToToken.get(i));
					}
				}
			}
			return result.toString();
		}
	}
}
