package com.rose.editor.langs.universal;

/**
 * An interface to provide information for your language
 */
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

}
