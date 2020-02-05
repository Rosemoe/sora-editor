package com.rose.editor.interfaces;

/**
 * Language for editor
 * @author Rose
 */
public interface EditorLanguage
{

    /**
     * Create a CodeAnalyzer
     * @return CodeAnalyzer created
     */
    CodeAnalyzer createAnalyzer();

    /**
     * Create a AutoCompleteProvider
     * @return AutoCompleteProvider created
     */
    AutoCompleteProvider createAutoComplete();

    /**
     * Called by editor to check whether this is a character for auto completion
     * @param ch Character to check
     * @return Whether is character for auto completion
     */
    boolean isAutoCompleteChar(char ch);

    /**
     * Get advance for indent
     * @param content Content of a line
     * @return Advance space count
     */
    int getIndentAdvance(String content);

    /**
     * Whether use tab to format
     * @return Whether use tab
     */
    boolean useTab();

    /**
     * Format the given content
     * @param text Content to format
     * @return Formatted code
     */
    CharSequence format(CharSequence text);

}
