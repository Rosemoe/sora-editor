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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ThemeTrieElementRule {

    // _themeTrieElementRuleBrand: void;

    public final List<String> parentScopes;
    public int scopeDepth;
    public int fontStyle;
    public int foreground;
    public int background;

    public ThemeTrieElementRule(int scopeDepth, List<String> parentScopes, int fontStyle, int foreground,
                                int background) {
        this.scopeDepth = scopeDepth;
        this.parentScopes = parentScopes;
        this.fontStyle = fontStyle;
        this.foreground = foreground;
        this.background = background;
    }

    public static List<ThemeTrieElementRule> cloneArr(List<ThemeTrieElementRule> arr) {
        List<ThemeTrieElementRule> r = new ArrayList<>();
        for (int i = 0, len = arr.size(); i < len; i++) {
            r.add(arr.get(i).clone());
        }
        return r;
    }

    @Override
    public ThemeTrieElementRule clone() {
        return new ThemeTrieElementRule(this.scopeDepth, this.parentScopes, this.fontStyle, this.foreground,
                this.background);
    }

    public void acceptOverwrite(int scopeDepth, int fontStyle, int foreground, int background) {
        if (this.scopeDepth > scopeDepth) {
            // TODO!!!
            // console.log('how did this happen?');
        } else {
            this.scopeDepth = scopeDepth;
        }
        // console.log('TODO -> my depth: ' + this.scopeDepth + ', overwriting
        // depth: ' + scopeDepth);
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
        return Objects.hash(background, fontStyle, foreground, parentScopes, scopeDepth);
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
        ThemeTrieElementRule other = (ThemeTrieElementRule) obj;
        return background == other.background && fontStyle == other.fontStyle && foreground == other.foreground &&
                Objects.equals(parentScopes, other.parentScopes) && scopeDepth == other.scopeDepth;
    }


}
