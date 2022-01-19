/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/Microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.grammar;

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
