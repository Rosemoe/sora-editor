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
package org.eclipse.tm4e.core.internal.grammar;

import org.eclipse.tm4e.core.theme.FontStyle;
import org.eclipse.tm4e.core.theme.ThemeTrieElementRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ScopeListElement {

    public final ScopeListElement parent;
    public final String scope;
    public final int metadata;

    public ScopeListElement(ScopeListElement parent, String scope, int metadata) {
        this.parent = parent;
        this.scope = scope;
        this.metadata = metadata;
    }

    private static boolean matchesScope(String scope, String selector, String selectorWithDot) {
        return (selector.equals(scope) || scope.startsWith(selectorWithDot));
    }

    private static boolean matches(ScopeListElement target, List<String> parentScopes) {
        if (parentScopes == null) {
            return true;
        }

        int len = parentScopes.size();
        int index = 0;
        String selector = parentScopes.get(index);
        String selectorWithDot = selector + ".";

        while (target != null) {
            if (matchesScope(target.scope, selector, selectorWithDot)) {
                index++;
                if (index == len) {
                    return true;
                }
                selector = parentScopes.get(index);
                selectorWithDot = selector + '.';
            }
            target = target.parent;
        }

        return false;
    }

    public static int mergeMetadata(int metadata, ScopeListElement scopesList, ScopeMetadata source) {
        if (source == null) {
            return metadata;
        }

        int fontStyle = FontStyle.NotSet;
        int foreground = 0;
        int background = 0;

        if (source.themeData != null) {
            // Find the first themeData that matches
            for (ThemeTrieElementRule themeData : source.themeData) {
                if (matches(scopesList, themeData.parentScopes)) {
                    fontStyle = themeData.fontStyle;
                    foreground = themeData.foreground;
                    background = themeData.background;
                    break;
                }
            }
        }

        return StackElementMetadata.set(metadata, source.languageId, source.tokenType, fontStyle, foreground,
                background);
    }

    public ScopeListElement push(Grammar grammar, String scope) {
        if (scope == null) {
            return this;
        }
        if (scope.indexOf(' ') >= 0) {
            // there are multiple scopes to push
            return ScopeListElement.push(this, grammar, Arrays.asList(scope.split(" ")));// scope.split(/
            // /g));
        }
        // there is a single scope to push
        return ScopeListElement.push(this, grammar, Arrays.asList(scope));
    }

    private static ScopeListElement push(ScopeListElement target, Grammar grammar, List<String> scopes) {
        for (String scope : scopes) {
            ScopeMetadata rawMetadata = grammar.getMetadataForScope(scope);
            int metadata = ScopeListElement.mergeMetadata(target.metadata, target, rawMetadata);
            target = new ScopeListElement(target, scope, metadata);
        }
        return target;
    }

    public List<String> generateScopes() {
        return ScopeListElement.generateScopes(this);
    }

    private static List<String> generateScopes(ScopeListElement scopesList) {
        List<String> result = new ArrayList<>();
        while (scopesList != null) {
            result.add(scopesList.scope);
            scopesList = scopesList.parent;
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, metadata, parent);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof ScopeListElement)) {
            return false;
        }
        return ScopeListElement.equals(this, (ScopeListElement) other);
    }

    private static boolean equals(ScopeListElement a, ScopeListElement b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.scope, b.scope) && a.metadata == b.metadata && equals(a.parent, b.parent);
    }

}
