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
package io.github.rosemoe.sora.langs.desc;

import io.github.rosemoe.sora.langs.universal.LanguageDescription;

public class ShellDescription implements LanguageDescription {
    public boolean isLineCommentStart(char c, char c2) {
        return c == '#' && c2 == '#';
    }

    public boolean isLongCommentEnd(char c, char c2) {
        return c == '#' && c2 == '#';
    }

    public boolean isLongCommentStart(char c, char c2) {
        return c == '#' && c2 == '#';
    }

    public boolean isSupportBlockLine() {
        return true;
    }

    public boolean useTab() {
        return false;
    }

    public boolean isOperator(char[] cArr, int i) {
        if (i != 1) {
            return false;
        }
        char c = cArr[0];
        return c == '+' || c == '-' || c == '{' || c == '}' || c == '[' || c == ']' || c == '(' || c == ')' || c == '|' || c == ':' || c == '.' || c == ',' || c == ';' || c == '*' || c == '/' || c == '&' || c == '^' || c == '%' || c == '!' || c == '~' || c == '<' || c == '>' || c == '=' || c == '\\' || c == '#';
    }

    public String[] getKeywords() { 
    return new String[]{"alias","case","hash","wait","bg","bind","break","builtin","debugger","cd","command","compgen","declare","cat","dirs","disown","do","done","fc","fg","fi","for","function","getopts","jobs","kill","break","umask","let","local","logout","alias","else","elif","ulimit","complete","until","echo","continue","source","while","wh","times","exec","exit","export","zi","eval","pwd","popd","printf","pushd","if","del"};
    }

    public int getOperatorAdvance(String str) {
        str.hashCode();
        if (!str.equals("{")) {
            return !str.equals("}") ? 0 : -4;
        }
        return 4;
    }

    public boolean isBlockStart(String str) {
        return str.equals("{");
    }

    public boolean isBlockEnd(String str) {
        return str.equals("}");
    }
}
///code by ninja coder
