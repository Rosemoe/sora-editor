/**
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

import java.util.List;

import io.github.rosemoe.sora.textmate.core.grammar.IToken;

class Token implements IToken {

    private int startIndex;

    private int endIndex;

    private List<String> scopes;

    public Token(int startIndex, int endIndex, List<String> scopes) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.scopes = scopes;
    }

    @Override
    public int getStartIndex() {
        return startIndex;
    }

    @Override
    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    @Override
    public int getEndIndex() {
        return endIndex;
    }

    @Override
    public List<String> getScopes() {
        return scopes;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("{startIndex: ");
        s.append(startIndex);
        s.append(", endIndex: ");
        s.append(endIndex);
        s.append(", scopes: ");
        s.append(scopes);
        s.append("}");
        return s.toString();
    }
}
