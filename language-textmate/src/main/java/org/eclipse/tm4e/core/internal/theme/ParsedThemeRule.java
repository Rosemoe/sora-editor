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
package org.eclipse.tm4e.core.internal.theme;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/theme.ts#L267">
 *      github.com/microsoft/vscode-textmate/blob/main/src/theme.ts</a>
 */
public class ParsedThemeRule {

	public final String scope;

	@Nullable
	public final List<String> parentScopes;

	public final int index;

	/**
	 * -1 if not set. An or mask of `FontStyle` otherwise.
	 */
	public final int fontStyle;

	@Nullable
	public final String foreground;

	@Nullable
	public final String background;

	public ParsedThemeRule(final String scope, @Nullable final List<String> parentScopes, final int index,
		final int fontStyle, @Nullable final String foreground, @Nullable final String background) {
		this.scope = scope;
		this.parentScopes = parentScopes;
		this.index = index;
		this.fontStyle = fontStyle;
		this.foreground = foreground;
		this.background = background;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fontStyle;
		result = prime * result + index;
		result = prime * result + Objects.hash(background, foreground, parentScopes, scope);
		return result;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ParsedThemeRule other = (ParsedThemeRule) obj;
		return Objects.equals(background, other.background) && fontStyle == other.fontStyle
			&& Objects.equals(foreground, other.foreground) && index == other.index
			&& Objects.equals(parentScopes, other.parentScopes) && Objects.equals(scope, other.scope);
	}

	@Override
	public String toString() {
		return "ParsedThemeRule [scope=" + scope + ", parentScopes=" + parentScopes + ", index=" + index
			+ ", fontStyle=" + fontStyle + ", foreground=" + foreground + ", background=" + background + "]";
	}
}
