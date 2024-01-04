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

import org.eclipse.tm4e.languageconfiguration.model.IndentationRules;
import org.eclipse.tm4e.languageconfiguration.utils.TextUtils;

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
        if (this.indentationRules.increaseIndentPattern == null || TextUtils.isEmpty(text)) {
            return false;
        }
        return indentationRules.increaseIndentPattern.matcher(text).matches();

        // if (this._indentationRules.indentNextLinePattern && this._indentationRules.indentNextLinePattern.test(text)) {
        // 	return true;
        // }
    }

    public boolean shouldDecrease(String text) {
        if (this.indentationRules.decreaseIndentPattern == null || TextUtils.isEmpty(text)) {
            return false;
        }
        return indentationRules.decreaseIndentPattern.matcher(text).matches();
    }

    public boolean shouldIndentNextLine(String text) {
        if (this.indentationRules.indentNextLinePattern == null || TextUtils.isEmpty(text)) {
            return false;
        }
        return indentationRules.indentNextLinePattern.matcher(text).matches();
    }


    public boolean shouldIgnore(String text) {
        if (this.indentationRules.unIndentedLinePattern == null || TextUtils.isEmpty(text)) {
            return false;
        }
        return indentationRules.unIndentedLinePattern.matcher(text).matches();
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


    public static class IndentConsts {
        public static final int INCREASE_MASK = 0b00000001;
        public static final int DECREASE_MASK = 0b00000010;
        public static final int INDENT_NEXTLINE_MASK = 0b00000100;
        public static final int UNINDENT_MASK = 0b00001000;
    }
}
