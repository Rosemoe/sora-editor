/*
 *   Copyright 2020 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.langs.universal;

import io.github.rosemoe.editor.interfaces.EditorLanguage;

/**
 * An interface to provide information for your language
 * @author Rose
 */
@SuppressWarnings("SameReturnValue")
public interface LanguageDescription {

    /**
     * Check whether given characters is operator
     * Start offset in array is always 0.
     * You should only read characters within length
     * @param characters Character array
     * @param length Length in array
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
