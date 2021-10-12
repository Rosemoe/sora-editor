/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.theme;

import java.util.List;

public class ParsedThemeRule {

    // _parsedThemeRuleBrand: void;

    public final String scope;
    public final List<String> parentScopes;
    public final int index;

    /**
     * -1 if not set. An or mask of `FontStyle` otherwise.
     */
    public final int fontStyle;
    public final String foreground;
    public final String background;

    public ParsedThemeRule(String scope, List<String> parentScopes, int index, int fontStyle, String foreground,
                           String background) {
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
        result = prime * result + ((background == null) ? 0 : background.hashCode());
        result = prime * result + fontStyle;
        result = prime * result + ((foreground == null) ? 0 : foreground.hashCode());
        result = prime * result + index;
        result = prime * result + ((parentScopes == null) ? 0 : parentScopes.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParsedThemeRule other = (ParsedThemeRule) obj;
        if (background == null) {
            if (other.background != null)
                return false;
        } else if (!background.equals(other.background))
            return false;
        if (fontStyle != other.fontStyle)
            return false;
        if (foreground == null) {
            if (other.foreground != null)
                return false;
        } else if (!foreground.equals(other.foreground))
            return false;
        if (index != other.index)
            return false;
        if (parentScopes == null) {
            if (other.parentScopes != null)
                return false;
        } else if (!parentScopes.equals(other.parentScopes))
            return false;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        return true;
    }

}
