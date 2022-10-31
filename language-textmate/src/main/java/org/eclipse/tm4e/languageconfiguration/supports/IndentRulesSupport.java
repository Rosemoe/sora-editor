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
package org.eclipse.tm4e.languageconfiguration.supports;

import org.eclipse.tm4e.core.internal.oniguruma.OnigString;
import org.eclipse.tm4e.languageconfiguration.model.IndentationRules;

/**
 * The "IndentRules" support.
 *
 * @see <a href=
 * "https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/supports/indentRules.ts">
 * https://github.com/microsoft/vscode/blob/main/src/vs/editor/common/languages/supports/indentRules.ts</a>
 */
public class IndentRulesSupport {

    private final IndentationRules indentationRules;

    public IndentRulesSupport(IndentationRules indentationRules) {
        this.indentationRules = indentationRules;
    }

    public boolean shouldIncrease(String text) {
        if (this.indentationRules.increaseIndentPattern == null) {
            return false;
        }
        var searchResult = this.indentationRules.increaseIndentPattern.search(
                OnigString.of(text), 0);

        if (searchResult == null) {
            return false;
        }

        return searchResult.count() > 0;

        // if (this._indentationRules.indentNextLinePattern && this._indentationRules.indentNextLinePattern.test(text)) {
        // 	return true;
        // }
    }

    public boolean shouldDecrease(String text) {
        if (this.indentationRules.decreaseIndentPattern == null) {
            return false;
        }
        var searchResult = this.indentationRules.decreaseIndentPattern.search(
                OnigString.of(text), 0);

        if (searchResult == null) {
            return false;
        }

        return searchResult.count() > 0;
    }

    public boolean shouldIndentNextLine(String text) {
        if (this.indentationRules.indentNextLinePattern == null) {
            return false;
        }
        var searchResult = this.indentationRules.indentNextLinePattern.search(
                OnigString.of(text), 0);

        if (searchResult == null) {
            return false;
        }

        return searchResult.count() > 0;
    }


    public boolean shouldIgnore(String text) {
        if (this.indentationRules.unIndentedLinePattern == null) {
            return false;
        }
        var searchResult = this.indentationRules.unIndentedLinePattern.search(
                OnigString.of(text), 0);

        if (searchResult == null) {
            return false;
        }

        return searchResult.count() > 0;
    }


    public int getIndentMetadata(String text) {
        int ret = 0;
        if (this.shouldIncrease(text)) {
            ret += IndentConsts.INCREASE_MASK;
        }
        if (this.shouldDecrease(text)) {
            ret += IndentConsts.DECREASE_MASK;
        }
        if (this.shouldIndentNextLine(text)) {
            ret += IndentConsts.INDENT_NEXTLINE_MASK;
        }
        if (this.shouldIgnore(text)) {
            ret += IndentConsts.UNINDENT_MASK;
        }
        return ret;
    }

    public boolean packMetadata(int metadata, int mask) {
        return (metadata & mask) != 0;
    }


    public static class IndentConsts {
        public static int INCREASE_MASK = 0b00000001;
        public static int DECREASE_MASK = 0b00000010;
        public static int INDENT_NEXTLINE_MASK = 0b00000100;
        public static int UNINDENT_MASK = 0b00001000;
    }
}
