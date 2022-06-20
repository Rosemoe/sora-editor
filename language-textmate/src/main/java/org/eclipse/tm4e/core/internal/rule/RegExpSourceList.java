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
package org.eclipse.tm4e.core.internal.rule;

import org.eclipse.tm4e.core.internal.oniguruma.OnigScanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @see https://github.com/Microsoft/vscode-textmate/blob/master/src/rule.ts
 *
 */
public class RegExpSourceList {

    private final RegExpSourceListAnchorCache _anchorCache;
    private List<RegExpSource> _items;
    private boolean _hasAnchors;
    private ICompiledRule _cached;
    public RegExpSourceList() {
        this._items = new io.github.rosemoe.sora.util.ArrayList<RegExpSource>();
        this._hasAnchors = false;
        this._cached = null;
        this._anchorCache = new RegExpSourceListAnchorCache();
    }

    public void push(RegExpSource item) {
        this._items.add(item);
        this._hasAnchors = this._hasAnchors ? this._hasAnchors : item.hasAnchor();
    }

    public void unshift(RegExpSource item) {
        this._items.add(0, item);
        this._hasAnchors = this._hasAnchors ? this._hasAnchors : item.hasAnchor();
    }

    public int length() {
        return this._items.size();
    }

    public void setSource(int index, String newSource) {
        RegExpSource r = this._items.get(index);
        if (!r.getSource().equals(newSource)) {
            // bust the cache
            this._cached = null;
            this._anchorCache.A0_G0 = null;
            this._anchorCache.A0_G1 = null;
            this._anchorCache.A1_G0 = null;
            this._anchorCache.A1_G1 = null;
            r.setSource(newSource);
        }
    }

    public ICompiledRule compile(IRuleRegistry grammar, boolean allowA, boolean allowG) {
        if (!this._hasAnchors) {
            if (this._cached == null) {
                List<String> regexps = new ArrayList<String>();
                for (RegExpSource regExpSource : _items) {
                    regexps.add(regExpSource.getSource());
                }
                this._cached = new ICompiledRule(createOnigScanner(regexps.toArray(new String[0])), getRules());
            }
            return this._cached;
        } else {
            if (this._anchorCache.A0_G0 == null) {
                this._anchorCache.A0_G0 = (allowA == false && allowG == false) ? this._resolveAnchors(allowA, allowG)
                        : null;
            }
            if (this._anchorCache.A0_G1 == null) {
                this._anchorCache.A0_G1 = (allowA == false && allowG == true) ? this._resolveAnchors(allowA, allowG)
                        : null;
            }
            if (this._anchorCache.A1_G0 == null) {
                this._anchorCache.A1_G0 = (allowA == true && allowG == false) ? this._resolveAnchors(allowA, allowG)
                        : null;
            }
            if (this._anchorCache.A1_G1 == null) {
                this._anchorCache.A1_G1 = (allowA == true && allowG == true) ? this._resolveAnchors(allowA, allowG)
                        : null;
            }
            if (allowA) {
                if (allowG) {
                    return this._anchorCache.A1_G1;
                } else {
                    return this._anchorCache.A1_G0;
                }
            } else {
                if (allowG) {
                    return this._anchorCache.A0_G1;
                } else {
                    return this._anchorCache.A0_G0;
                }
            }
        }

    }

    private ICompiledRule _resolveAnchors(boolean allowA, boolean allowG) {
        List<String> regexps = new ArrayList<String>();
        for (RegExpSource regExpSource : _items) {
            regexps.add(regExpSource.resolveAnchors(allowA, allowG));
        }
        return new ICompiledRule(createOnigScanner(regexps.toArray(new String[0])), getRules());
    }

    private OnigScanner createOnigScanner(String[] regexps) {
        return new OnigScanner(regexps);
    }

    private Integer[] getRules() {
        Collection<Integer> ruleIds = new ArrayList<Integer>();
        for (RegExpSource item : this._items) {
            ruleIds.add(item.getRuleId());
        }
        return ruleIds.toArray(new Integer[0]);
    }

    private class RegExpSourceListAnchorCache {

        public ICompiledRule A0_G0;
        public ICompiledRule A0_G1;
        public ICompiledRule A1_G0;
        public ICompiledRule A1_G1;

    }

}
