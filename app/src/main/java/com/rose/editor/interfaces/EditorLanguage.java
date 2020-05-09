/*
 Copyright 2020 Rose2073

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
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
