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
package org.eclipse.tm4e.languageconfiguration.utils;

import io.github.rosemoe.sora.graphics.SingleCharacterWidths;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.util.IntPair;

public class TextUtils {


    public static String normalizeIndentation(final String str, final int tabSize, final boolean insertSpaces) {
        int firstNonWhitespaceIndex = TextUtils.firstNonWhitespaceIndex(str);
        if (firstNonWhitespaceIndex == -1) {
            firstNonWhitespaceIndex = str.length();
        }
        return TextUtils.normalizeIndentationFromWhitespace(str.substring(0, firstNonWhitespaceIndex), tabSize,
                insertSpaces) + str.substring(firstNonWhitespaceIndex);
    }


    private static String normalizeIndentationFromWhitespace(final String str, final int tabSize,
                                                             final boolean insertSpaces) {
        int spacesCnt = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\t') {
                spacesCnt += tabSize;
            } else {
                spacesCnt++;
            }
        }

        final var result = new StringBuilder();
        if (!insertSpaces) {
            final long tabsCnt = Math.round(Math.floor(spacesCnt / tabSize));
            spacesCnt = spacesCnt % tabSize;
            for (int i = 0; i < tabsCnt; i++) {
                result.append('\t');
            }
        }

        for (int i = 0; i < spacesCnt; i++) {
            result.append(' ');
        }

        return result.toString();
    }

    /**
     * Returns first index of the string that is not whitespace. If string is empty
     * or contains only whitespaces, returns -1
     */
    public static int firstNonWhitespaceIndex(final String str) {
        for (int i = 0, len = str.length(); i < len; i++) {
            final char c = str.charAt(i);
            if (c != ' ' && c != '\t') {
                return i;
            }
        }
        return -1;
    }

    public static String getIndentationFromWhitespace(final String whitespace, final TabSpacesInfo tabSpaces) {
        final var tab = "\t"; //$NON-NLS-1$
        int indentOffset = 0;
        boolean startsWithTab = true;
        boolean startsWithSpaces = true;
        final String spaces = tabSpaces.isInsertSpaces()
                ? " ".repeat(tabSpaces.getTabSize())
                : "";
        while (startsWithTab || startsWithSpaces) {
            startsWithTab = whitespace.startsWith(tab, indentOffset);
            startsWithSpaces = tabSpaces.isInsertSpaces() && whitespace.startsWith(spaces, indentOffset);
            if (startsWithTab) {
                indentOffset += tab.length();
            }
            if (startsWithSpaces) {
                indentOffset += spaces.length();
            }
        }
        return whitespace.substring(0, indentOffset);
    }

    public static String getLinePrefixingWhitespaceAtPosition(final Content d, final CharPosition position) {

        var line = d.getLine(position.line);

        var startIndex = IntPair.getFirst(io.github.rosemoe.sora.text.TextUtils.findLeadingAndTrailingWhitespacePos(
                line
        ));

        return line.subSequence(0, startIndex).toString();

    }


    /**
     * Returns the leading whitespace of the string.
     * If the string contains only whitespaces, returns entire string
     */
    public static String getLeadingWhitespace(String str, int start, int end) {
        for (var i = start; i < end; i++) {
            var chCode = str.charAt(i);
            if (chCode != 32 /*CharCode.Space*/ && chCode != 9/*CharCode.Tab*/) {
                return str.substring(start, i);
            }
        }
        return str.substring(start, end);
    }

    /**
     * Returns the leading whitespace of the string.
     * If the string contains only whitespaces, returns entire string
     */
    public static String getLeadingWhitespace(String str) {
        return getLeadingWhitespace(str, 0, str.length());
    }


    public static TabSpacesInfo getTabSpaces(final TextMateLanguage language) {
        return new TabSpacesInfo(language.getTabSize(), !language.useTab());
    }

    public static String firstCharToUpperCase(String targetString) {
        char[] cs = targetString.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
    }

    public static boolean isEmpty(String text) {
        if (text.length()<1) {
            return true;
        }
        for (var i = 0; i < text.length(); i++) {
            var chCode = text.charAt(i);
            if (chCode != 32 /*CharCode.Space*/ && chCode != 9/*CharCode.Tab*/) {
                return false;
            }
        }
        return true;
    }
}
