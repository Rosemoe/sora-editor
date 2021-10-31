/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/CodeEditor
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
package io.github.rosemoe.sora.langs.universal;

import io.github.rosemoe.sora.interfaces.EditorLanguage;

/**
 * An interface to provide information for your language
 *
 * @author Rose
 */
@SuppressWarnings("SameReturnValue")
public interface LanguageDescription {

    /**
     * Check whether given characters is operator
     * Start offset in array is always 0.
     * You should only read characters within length
     *
     * @param characters Character array
     * @param length     Length in array
     */
    boolean isOperator(char[] characters, int length);

    /**
     * Is the two characters leads a single line comment?
     */
    boolean isLineCommentStart(char a, char b);

    /**
     * Is the two characters leads to a multiple line comment?
     */
    boolean isLongCommentStart(char a, char b);

    /**
     * Is the two characters stand for a end of multiple line comment?
     */
    boolean isLongCommentEnd(char a, char b);

    /**
     * Get keywords of your language
     */
    String[] getKeywords();

    /**
     * @see EditorLanguage#useTab()
     */
    boolean useTab();

    /**
     * @see EditorLanguage#getIndentAdvance(String)
     */
    int getOperatorAdvance(String operator);

    boolean isSupportBlockLine();

    /**
     * Whether this is block start
     */
    boolean isBlockStart(String operator);

    /**
     * Whether this is block end
     */
    boolean isBlockEnd(String operator);

}
