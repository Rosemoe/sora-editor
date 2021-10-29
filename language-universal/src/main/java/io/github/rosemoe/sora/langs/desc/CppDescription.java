/*
 *    CodeEditor - the awesome code editor for Android
 *    Copyright (C) 2020-2021  Rosemoe
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

/**
 * @author Rose
 */
@SuppressWarnings("SpellCheckingInspection")
public class CppDescription extends CDescription {

    @Override
    public String[] getKeywords() {
        return new String[]{
                "asm", "auto", "bool", "break", "case", "catch", "char", "class",
                "const", "const_cast", "continue", "default", "delete", "do",
                "double", "dynamic_cast", "else", "enum", "explicit", "export",
                "extern", "false", "float", "for", "friend", "goto", "if", "inline",
                "int", "long", "mutable", "namespace", "new", "operator",
                "private", "protected", "public", "register", "reinterpret_cast",
                "return", "short", "signed", "sizeof", "static", "static_cast",
                "struct", "switch", "template", "this", "throw", "true", "try",
                "typedef", "typeid", "typename", "unsigned", "union",
                "using", "virtual", "void", "volatile", "wchar_t", "while", "define","include",
          "_Bool", "char16_t","char32_t","cl","cin","do"
        };
    }
}
