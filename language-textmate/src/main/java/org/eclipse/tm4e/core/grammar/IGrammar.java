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
package org.eclipse.tm4e.core.grammar;

import java.util.Collection;

/**
 * TextMate grammar API.
 *
 * @see <a href="https://github.com/Microsoft/vscode-textmate/blob/master/src/main.ts">https://github.com/Microsoft/vscode-textmate/blob/master/src/main.ts</a>
 *
 */
public interface IGrammar {

    /**
     * Returns the name of the grammar.
     *
     * @return the name of the grammar.
     */
    String getName();

    /**
     * Returns the scope name of the grammar.
     *
     * @return the scope name of the grammar.
     */
    String getScopeName();

    /**
     * Returns the supported file types and null otherwise.
     *
     * @return the supported file types and null otherwise.
     */
    Collection<String> getFileTypes();

    /**
     * Tokenize `lineText`.
     *
     * @param lineText
     *            the line text to tokenize.
     * @return the result of the tokenization.
     */
    ITokenizeLineResult tokenizeLine(String lineText);

    /**
     * Tokenize `lineText` using previous line state `prevState`.
     *
     * @param lineText
     *            the line text to tokenize.
     * @param prevState
     *            previous line state.
     * @return the result of the tokenization.
     */
    ITokenizeLineResult tokenizeLine(String lineText, StackElement prevState);

    /**
     * Tokenize `lineText` using previous line state `prevState`.
     * The result contains the tokens in binary format, resolved with the following information:
     *  - language
     *  - token type (regex, string, comment, other)
     *  - font style
     *  - foreground color
     *  - background color
     * e.g. for getting the languageId: `(metadata & MetadataConsts.LANGUAGEID_MASK) >>> MetadataConsts.LANGUAGEID_OFFSET`
     */
    ITokenizeLineResult2 tokenizeLine2(String lineText);

    /**
     * Tokenize `lineText` using previous line state `prevState`.
     * The result contains the tokens in binary format, resolved with the following information:
     *  - language
     *  - token type (regex, string, comment, other)
     *  - font style
     *  - foreground color
     *  - background color
     * e.g. for getting the languageId: `(metadata & MetadataConsts.LANGUAGEID_MASK) >>> MetadataConsts.LANGUAGEID_OFFSET`
     */
    ITokenizeLineResult2 tokenizeLine2(String lineText, StackElement prevState);

}
