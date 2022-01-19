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

import io.github.rosemoe.sora.textmate.core.grammar.IToken;
import io.github.rosemoe.sora.textmate.core.grammar.ITokenizeLineResult;
import io.github.rosemoe.sora.textmate.core.grammar.StackElement;

/**
 *
 * Result of the line tokenization implementation.
 *
 */
public class TokenizeLineResult implements ITokenizeLineResult {

    private final IToken[] tokens;
    private final StackElement ruleStack;

    public TokenizeLineResult(IToken[] tokens, StackElement ruleStack) {
        this.tokens = tokens;
        this.ruleStack = ruleStack;
    }

    @Override
    public IToken[] getTokens() {
        return tokens;
    }

    @Override
    public StackElement getRuleStack() {
        return ruleStack;
    }

}
