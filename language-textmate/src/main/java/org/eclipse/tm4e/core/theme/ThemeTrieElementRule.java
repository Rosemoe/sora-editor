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
package org.eclipse.tm4e.core.theme;

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
