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
package io.github.rosemoe.sora.textmate.core.internal.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.rosemoe.sora.textmate.core.grammar.IToken;
import io.github.rosemoe.sora.textmate.core.grammar.StackElement;

class LineTokens {

    private static final Logger LOGGER = Logger.getLogger(LineTokens.class.getName());

    private final String lineText;

    /**
     * used only if `_emitBinaryTokens` is false.
     */
    private final List<IToken> tokens;
    /**
     * used only if `_emitBinaryTokens` is true.
     */
    private final List<Integer> binaryTokens;
    private boolean emitBinaryTokens;
    private int lastTokenEndIndex;

    LineTokens(boolean emitBinaryTokens, String lineText) {
        this.emitBinaryTokens = emitBinaryTokens;
        this.lineText = LOGGER.isLoggable(Level.FINEST) ? lineText : null; // store line only if it's logged
        if (this.emitBinaryTokens) {
            this.tokens = null;
            this.binaryTokens = new ArrayList<>();
        } else {
            this.tokens = new ArrayList<>();
            this.binaryTokens = null;
        }
        this.lastTokenEndIndex = 0;
    }

    public void produce(StackElement stack, int endIndex) {
        this.produceFromScopes(stack.contentNameScopesList, endIndex);
    }

    public void produceFromScopes(ScopeListElement scopesList, int endIndex) {
        if (this.lastTokenEndIndex >= endIndex) {
            return;
        }

        if (this.emitBinaryTokens) {
            int metadata = scopesList.metadata;
            if (!this.binaryTokens.isEmpty() && this.binaryTokens.get(this.binaryTokens.size() - 1) == metadata) {
                // no need to push a token with the same metadata
                this.lastTokenEndIndex = endIndex;
                return;
            }

            this.binaryTokens.add(this.lastTokenEndIndex);
            this.binaryTokens.add(metadata);

            this.lastTokenEndIndex = endIndex;
            return;
        }

        List<String> scopes = scopesList.generateScopes();

        if (this.lineText != null) {
            LOGGER.info("  token: |" + this.lineText.substring(this.lastTokenEndIndex, endIndex).replaceAll("\n", "\\n") + '|');
            for (String scope : scopes) {
                LOGGER.info("      * " + scope);
            }
        }
        this.tokens.add(new Token(this.lastTokenEndIndex, endIndex, scopes));

        this.lastTokenEndIndex = endIndex;
    }

    public IToken[] getResult(StackElement stack, int lineLength) {
        if (!this.tokens.isEmpty() && this.tokens.get(this.tokens.size() - 1).getStartIndex() == lineLength - 1) {
            // pop produced token for newline
            this.tokens.remove(this.tokens.size() - 1);
        }

        if (this.tokens.isEmpty()) {
            this.lastTokenEndIndex = -1;
            this.produce(stack, lineLength);
            this.tokens.get(this.tokens.size() - 1).setStartIndex(0);
        }

        return this.tokens.toArray(new IToken[0]);
    }

    public int[] getBinaryResult(StackElement stack, int lineLength) {
        if (!this.binaryTokens.isEmpty() && this.binaryTokens.get(this.binaryTokens.size() - 2) == lineLength - 1) {
            // pop produced token for newline
            this.binaryTokens.remove(this.binaryTokens.size() - 1);
            this.binaryTokens.remove(this.binaryTokens.size() - 1);
        }

        if (this.binaryTokens.isEmpty()) {
            this.lastTokenEndIndex = -1;
            this.produce(stack, lineLength);
            this.binaryTokens.set(this.binaryTokens.size() - 2, 0);
        }

        int[] result = new int[this.binaryTokens.size()];
        for (int i = 0, len = this.binaryTokens.size(); i < len; i++) {
            result[i] = this.binaryTokens.get(i);
        }

        return result;
    }
}
