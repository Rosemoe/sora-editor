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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @see <a href=
 *      "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/theme.ts#L403">
 *      github.com/microsoft/vscode-textmate/blob/main/src/theme.ts</a>
 */
public class ThemeTrieElementRule {

	public int scopeDepth;

	@Nullable
	public final List<String> parentScopes;

	public int fontStyle;
	public int foreground;
	public int background;

	public ThemeTrieElementRule(final int scopeDepth, @Nullable final List<String> parentScopes, final int fontStyle,
		final int foreground,
		final int background) {

		this.scopeDepth = scopeDepth;
		this.parentScopes = parentScopes;
		this.fontStyle = fontStyle;
		this.foreground = foreground;
		this.background = background;
	}

	@Override
	public ThemeTrieElementRule clone() {
		return new ThemeTrieElementRule(this.scopeDepth, this.parentScopes, this.fontStyle, this.foreground,
			this.background);
	}

	public static List<ThemeTrieElementRule> cloneArr(final List<ThemeTrieElementRule> arr) {
		final var r = new ArrayList<ThemeTrieElementRule>(arr.size());
		for (final var e : arr) {
			r.add(e.clone());
		}
		return r;
	}

	public void acceptOverwrite(final int scopeDepth, final int fontStyle, final int foreground, final int background) {
		if (this.scopeDepth > scopeDepth) {
			// TODO!!!
			// console.log('how did this happen?');
		} else {
			this.scopeDepth = scopeDepth;
		}
		// console.log('TODO -> my depth: ' + this.scopeDepth + ', overwriting depth: ' + scopeDepth);
		if (fontStyle != FontStyle.NotSet) {
			this.fontStyle = fontStyle;
		}
		if (foreground != 0) {
			this.foreground = foreground;
		}
		if (background != 0) {
			this.background = background;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + background;
		result = prime * result + fontStyle;
		result = prime * result + foreground;
		result = prime * result + Objects.hashCode(parentScopes);
		result = prime * result + scopeDepth;
		return result;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ThemeTrieElementRule other = (ThemeTrieElementRule) obj;
		return background == other.background && fontStyle == other.fontStyle && foreground == other.foreground &&
			Objects.equals(parentScopes, other.parentScopes) && scopeDepth == other.scopeDepth;
	}
}
