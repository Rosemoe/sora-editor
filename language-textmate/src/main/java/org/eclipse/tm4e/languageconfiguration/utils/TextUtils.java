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

public class TextUtils {


    public static String normalizeIndentation(final String str, final int tabSize, final boolean insertSpaces) {
        int firstNonWhitespaceIndex = TextUtils.firstNonWhitespaceIndex(str);
        if (firstNonWhitespaceIndex == -1) {
            firstNonWhitespaceIndex = str.length();
        }
        return TextUtils.normalizeIndentationFromWhitespace(str.substring(0, firstNonWhitespaceIndex), tabSize,
                insertSpaces) + str.substring(firstNonWhitespaceIndex);
    }


    public static int invisibleColumnFromColumn(Content content, int line, int column, TabSpacesInfo tabSpacesInfo) {
        var lineContent = content.getLine(line);

        var currentIndex = 0;
        var result = 0;
        while (currentIndex < lineContent.length()) {
            var codePoint = lineContent.charAt(currentIndex);
            if (codePoint != 32 /*CharCode.Space*/ && codePoint != 9/*CharCode.Tab*/) {
                return result;
            }
            currentIndex++;
            result++;
        }

        return result;

    }


    private static int nextVisibleColumn(int codePoint, int visibleColumn, int tabSize) {
        if (codePoint == 9 /*CharCode.Tab*/) {
            return visibleColumn + tabSize - visibleColumn % tabSize;
        }
        if (SingleCharacterWidths.isEmoji((char) codePoint)) {
            return visibleColumn + 2;
        }
        return visibleColumn + 1;
    }

    public static int visibleColumnFromColumn(Content content, int line, int column, TabSpacesInfo tabSpacesInfo) {
        var lineContent = content.getLine(line);
        var textLen = Math.max(0,Math.min(column - 1, lineContent.length()));
        var text = lineContent.subSequence(0, textLen);
        var currentIndex = 0;
        var result = 0;
        while (currentIndex < textLen) {
            var codePoint = text.charAt(currentIndex);
            result = nextVisibleColumn(codePoint, result, tabSpacesInfo.getTabSize());
            currentIndex++;
        }

        return result;

    }


    public static String shiftIndent(int visibleColumn, TabSpacesInfo tabSpacesInfo) {

        // https://github.com/microsoft/vscode/blob/bf63ea1932dd253745f38a4cbe26bb9be01801b1/src/vs/editor/common/commands/shiftCommand.ts#L60
        var tabSize = tabSpacesInfo.getTabSize();
        var desiredTabStop = visibleColumn + tabSize - visibleColumn % tabSize;
        var indentCount = desiredTabStop / tabSize; // will be an integer

        if (tabSpacesInfo.isInsertSpaces()) {
            return " ".repeat(indentCount);
        } else {
            return "\t".repeat(indentCount);
        }

    }


    public static String unShiftIndent(int visibleColumn, TabSpacesInfo tabSpacesInfo) {

        var tabSize = tabSpacesInfo.getTabSize();
        var desiredTabStop = Math.max(0, visibleColumn - 1 - (visibleColumn - 1) % tabSize);
       // var indentCount = desiredTabStop; /// tabSize; // will be an integer

        if (tabSpacesInfo.isInsertSpaces()) {
            return " ".repeat(desiredTabStop);
        } else {
            return "\t".repeat(desiredTabStop);
        }

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
     * Returns the start of the string at the offset in the text. If the string is
     * not in the text at the offset, returns -1.</br>
     * Ex: </br>
     * text = "apple banana", offset=8, string="banana" returns=6
     */
    public static int startIndexOfOffsetTouchingString(final String text, final int offset, final String string) {
        int start = offset - string.length();
        start = start < 0 ? 0 : start;
        int end = offset + string.length();
        end = end >= text.length() ? text.length() : end;
        try {
            final int indexInSubtext = text.substring(start, end).indexOf(string);
            return indexInSubtext == -1 ? -1 : start + indexInSubtext;
        } catch (final IndexOutOfBoundsException e) {
            return -1;
        }
    }

    /**
     * Returns first index of the string that is not whitespace. If string is empty
     * or contains only whitespaces, returns -1
     */
    private static int firstNonWhitespaceIndex(final String str) {
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

        // find start of line
        final int p = position.index;

        final int start = d.getIndexer().getCharIndex(position.line, 0);

        // find white spaces
        final int end = findEndOfWhiteSpace(d, start, p);

        return d.subContent(position.line, 0,
                d.getIndexer().getCharLine(end), d.getIndexer().getCharColumn(end)).toString();

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


    /**
     * Returns the first offset greater than <code>offset</code> and smaller than
     * <code>end</code> whose character is not a space or tab character. If no such
     * offset is found, <code>end</code> is returned.
     *
     * @param document the document to search in
     * @param offset   the offset at which searching start
     * @param end      the offset at which searching stops
     * @return the offset in the specified range whose character is not a space or
     * tab
     */
    private static int findEndOfWhiteSpace(final Content document, int offset, final int end) {
        while (offset < end) {
            final char c = document.charAt(offset);
            if (c != ' ' && c != '\t') {
                return offset;
            }
            offset++;
        }
        return end;
    }

    public static TabSpacesInfo getTabSpaces(final TextMateLanguage language) {
        return new TabSpacesInfo(language.getTabSize(), !language.useTab());
    }

    public static String firstCharToUpperCase(String targetString) {
        char[] cs = targetString.toCharArray();
        cs[0] -= 32;
        return String.valueOf(cs);
    }
}
