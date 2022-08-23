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
package org.eclipse.tm4e.core.internal.grammar;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.internal.grammar.tokenattrs.EncodedTokenAttributes;
import org.eclipse.tm4e.core.internal.theme.FontStyle;
import org.eclipse.tm4e.core.internal.theme.StyleAttributes;

import com.google.common.base.Splitter;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/grammar.ts#L417">
 *      github.com/microsoft/vscode-textmate/blob/main/src/grammar.ts</a>
 */
public final class AttributedScopeStack {

	private static final Splitter BY_SPACE_SPLITTER = Splitter.on(' ');

	public static AttributedScopeStack createRoot(final String scopeName,
		final int /*EncodedTokenAttributes*/ tokenAttributes) {
		return new AttributedScopeStack(null, new ScopeStack(null, scopeName), tokenAttributes);
	}

	public static AttributedScopeStack createRootAndLookUpScopeName(final String scopeName,
		final int encodedTokenAttributes, final Grammar grammar) {
		final var rawRootMetadata = grammar.getMetadataForScope(scopeName);
		final var scopePath = new ScopeStack(null, scopeName);
		final var rootStyle = grammar.themeProvider.themeMatch(scopePath);

		final var resolvedTokenAttributes = AttributedScopeStack.mergeAttributes(
			encodedTokenAttributes,
			rawRootMetadata,
			rootStyle);

		return new AttributedScopeStack(null, scopePath, resolvedTokenAttributes);
	}

	public String scopeName() {
		return this.scopePath.scopeName;
	}

	@Nullable
	private final AttributedScopeStack parent;
	private final ScopeStack scopePath;
	final int tokenAttributes;

	public AttributedScopeStack(
		@Nullable final AttributedScopeStack parent,
		final ScopeStack scopePath,
		final int tokenAttributes) {
		this.parent = parent;
		this.scopePath = scopePath;
		this.tokenAttributes = tokenAttributes;
	}

	public boolean equals(final AttributedScopeStack other) {
		return _equals(this, other);
	}

	private static boolean _equals(
		@Nullable AttributedScopeStack a,
		@Nullable AttributedScopeStack b) {
		do {
			if (a == b) {
				return true;
			}

			if (a == null && b == null) {
				// End of list reached for both
				return true;
			}

			if (a == null || b == null) {
				// End of list reached only for one
				return false;
			}

			if (!Objects.equals(a.scopeName(), b.scopeName()) || a.tokenAttributes != b.tokenAttributes) {
				return false;
			}

			// Go to previous pair
			a = a.parent;
			b = b.parent;
		} while (true);
	}

	public static int mergeAttributes(
		final int existingTokenAttributes,
		final BasicScopeAttributes basicScopeAttributes,
		@Nullable final StyleAttributes styleAttributes) {
		var fontStyle = FontStyle.NotSet;
		var foreground = 0;
		var background = 0;

		if (styleAttributes != null) {
			fontStyle = styleAttributes.fontStyle;
			foreground = styleAttributes.foregroundId;
			background = styleAttributes.backgroundId;
		}

		return EncodedTokenAttributes.set(
			existingTokenAttributes,
			basicScopeAttributes.languageId,
			basicScopeAttributes.tokenType,
			null,
			fontStyle,
			foreground,
			background);
	}

	AttributedScopeStack pushAttributed(@Nullable final String scopePath, final Grammar grammar) {
		if (scopePath == null) {
			return this;
		}

		if (scopePath.indexOf(' ') == -1) {
			// This is the common case and much faster
			return _pushAttributed(this, scopePath, grammar);
		}

		final var scopes = BY_SPACE_SPLITTER.split(scopePath);
		var result = this;
		for (final var scope : scopes) {
			result = _pushAttributed(result, scope, grammar);
		}
		return result;
	}

	private AttributedScopeStack _pushAttributed(
		final AttributedScopeStack target,
		final String scopeName,
		final Grammar grammar) {
		final var rawMetadata = grammar.getMetadataForScope(scopeName);

		final var newPath = target.scopePath.push(scopeName);
		final var scopeThemeMatchResult = grammar.themeProvider.themeMatch(newPath);
		final var metadata = mergeAttributes(
			target.tokenAttributes,
			rawMetadata,
			scopeThemeMatchResult);
		return new AttributedScopeStack(target, newPath, metadata);
	}

	List<String> getScopeNames() {
		return this.scopePath.getSegments();
	}
}