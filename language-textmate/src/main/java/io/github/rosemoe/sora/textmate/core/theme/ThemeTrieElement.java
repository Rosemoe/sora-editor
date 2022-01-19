/*
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.github.rosemoe.sora.textmate.core.internal.utils.CompareUtils;

public class ThemeTrieElement {

    // _themeTrieElementBrand: void;

    private final ThemeTrieElementRule mainRule;
    private final List<ThemeTrieElementRule> rulesWithParentScopes;
    private final Map<String /* segment */, ThemeTrieElement> children;

    public ThemeTrieElement(ThemeTrieElementRule mainRule) {
        this(mainRule, new ArrayList<>(), new HashMap<>());
    }

    public ThemeTrieElement(ThemeTrieElementRule mainRule, List<ThemeTrieElementRule> rulesWithParentScopes) {
        this(mainRule, rulesWithParentScopes, new HashMap<>());
    }

    public ThemeTrieElement(ThemeTrieElementRule mainRule, List<ThemeTrieElementRule> rulesWithParentScopes,
                            Map<String /* segment */, ThemeTrieElement> children) {
        this.mainRule = mainRule;
        this.rulesWithParentScopes = rulesWithParentScopes;
        this.children = children;
    }

    private static List<ThemeTrieElementRule> sortBySpecificity(List<ThemeTrieElementRule> arr) {
        if (arr.size() == 1) {
            return arr;
        }
        arr.sort(ThemeTrieElement::cmpBySpecificity);
        return arr;
    }

    private static int cmpBySpecificity(ThemeTrieElementRule a, ThemeTrieElementRule b) {
        if (a.scopeDepth == b.scopeDepth) {
            List<String> aParentScopes = a.parentScopes;
            List<String> bParentScopes = b.parentScopes;
            int aParentScopesLen = aParentScopes == null ? 0 : aParentScopes.size();
            int bParentScopesLen = bParentScopes == null ? 0 : bParentScopes.size();
            if (aParentScopesLen == bParentScopesLen) {
                for (int i = 0; i < aParentScopesLen; i++) {
                    int aLen = aParentScopes.get(i).length();
                    int bLen = bParentScopes.get(i).length();
                    if (aLen != bLen) {
                        return bLen - aLen;
                    }
                }
            }
            return bParentScopesLen - aParentScopesLen;
        }
        return b.scopeDepth - a.scopeDepth;
    }

    public List<ThemeTrieElementRule> match(String scope) {
        if ("".equals(scope)) {
            List<ThemeTrieElementRule> arr = new ArrayList<>();
            arr.add(this.mainRule);
            arr.addAll(this.rulesWithParentScopes);
            return ThemeTrieElement.sortBySpecificity(arr);
        }

        int dotIndex = scope.indexOf('.');
        String head;
        String tail;
        if (dotIndex == -1) {
            head = scope;
            tail = "";
        } else {
            head = scope.substring(0, dotIndex);
            tail = scope.substring(dotIndex + 1);
        }

        if (this.children.containsKey(head)) {
            return this.children.get(head).match(tail);
        }

        List<ThemeTrieElementRule> arr = new ArrayList<>();
        arr.add(this.mainRule);
        arr.addAll(this.rulesWithParentScopes);
        return ThemeTrieElement.sortBySpecificity(arr);
    }

    public void insert(int scopeDepth, String scope, List<String> parentScopes, int fontStyle, int foreground,
                       int background) {
        if ("".equals(scope)) {
            this.doInsertHere(scopeDepth, parentScopes, fontStyle, foreground, background);
            return;
        }

        int dotIndex = scope.indexOf('.');
        String head;
        String tail;
        if (dotIndex == -1) {
            head = scope;
            tail = "";
        } else {
            head = scope.substring(0, dotIndex);
            tail = scope.substring(dotIndex + 1);
        }

        ThemeTrieElement child;
        if (this.children.containsKey(head)) {
            child = this.children.get(head);
        } else {
            child = new ThemeTrieElement(this.mainRule.clone(),
                    ThemeTrieElementRule.cloneArr(this.rulesWithParentScopes));
            this.children.put(head, child);
        }

        child.insert(scopeDepth + 1, tail, parentScopes, fontStyle, foreground, background);
    }

    private void doInsertHere(int scopeDepth, List<String> parentScopes, int fontStyle, int foreground,
                              int background) {

        if (parentScopes == null) {
            // Merge into the main rule
            this.mainRule.acceptOverwrite(scopeDepth, fontStyle, foreground, background);
            return;
        }

        // Try to merge into existing rule
        for (ThemeTrieElementRule rule : this.rulesWithParentScopes) {
            if (CompareUtils.strArrCmp(rule.parentScopes, parentScopes) == 0) {
                // bingo! => we get to merge this into an existing one
                rule.acceptOverwrite(scopeDepth, fontStyle, foreground, background);
                return;
            }
        }

        // Must add a new rule

        // Inherit from main rule
        if (fontStyle == FontStyle.NotSet) {
            fontStyle = this.mainRule.fontStyle;
        }
        if (foreground == 0) {
            foreground = this.mainRule.foreground;
        }
        if (background == 0) {
            background = this.mainRule.background;
        }

        this.rulesWithParentScopes
                .add(new ThemeTrieElementRule(scopeDepth, parentScopes, fontStyle, foreground, background));
    }

    @Override
    public int hashCode() {
        return Objects.hash(children, mainRule, rulesWithParentScopes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ThemeTrieElement other = (ThemeTrieElement) obj;
        return Objects.equals(children, other.children) && Objects.equals(mainRule, other.mainRule) && Objects.equals(rulesWithParentScopes, other.rulesWithParentScopes);
    }

}
