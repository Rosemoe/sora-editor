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
package org.eclipse.tm4e.core.internal.grammar;

import org.eclipse.tm4e.core.grammar.StackElement;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.tm4e.core.grammar.IToken;

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
