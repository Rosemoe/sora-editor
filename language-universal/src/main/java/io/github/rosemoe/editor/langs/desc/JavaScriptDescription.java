/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *     Please contact Rosemoe by email roses2020@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.editor.langs.desc;

import io.github.rosemoe.editor.langs.universal.LanguageDescription;

/**
 * @author Rose
 */
public class JavaScriptDescription implements LanguageDescription {

    @Override
    public boolean isOperator(char[] characters, int length) {
        if (length == 1) {
            char c = characters[0];
            return (c == '+' || c == '-' || c == '{' || c == '}' || c == '[' || c == ']' ||
                    c == '(' || c == ')' || c == '|' || c == ':' || c == '.' || c == ',' ||
                    c == ';' || c == '*' || c == '/' || c == '&' || c == '^' || c == '%' ||
                    c == '!' || c == '~' || c == '<' || c == '>' || c == '=');
        }
        return false;
    }

    @Override
    public boolean isLineCommentStart(char a, char b) {
        return a == '/' && b == '/';
    }

    @Override
    public boolean isLongCommentStart(char a, char b) {
        return a == '/' && b == '*';
    }

    @Override
    public boolean isLongCommentEnd(char a, char b) {
        return a == '*' && b == '/';
    }

    @Override
    public String[] getKeywords() {
        return new String[]{
                "abstract", "else", "instanceof", "super", "boolean", "enum", "int", "switch",
                "break", "export", "interface", "synchronized", "byte", "extends", "let", "this",
                "case", "false", "long", "throw", "catch", "final", "native", "throws",
                "char", "finally", "new", "transient", "class", "float", "null", "true",
                "const", "for", "package", "try", "continue", "function", "private", "typeof",
                "debugger", "goto", "protected", "var", "default", "if", "public", "void",
                "delete", "implements", "return", "volatile", "do", "import", "short", "while",
                "double", "in", "static", "with"
        };
    }

    @Override
    public boolean useTab() {
        return false;
    }

    @Override
    public int getOperatorAdvance(String operator) {
        switch (operator) {
            case "{":
                return 4;
            case "}":
                return -4;
        }
        return 0;
    }

    @Override
    public boolean isSupportBlockLine() {
        return true;
    }

    @Override
    public boolean isBlockStart(String operator) {
        return operator.equals("{");
    }

    @Override
    public boolean isBlockEnd(String operator) {
        return operator.equals("}");
    }

}
