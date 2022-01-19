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
package io.github.rosemoe.sora.textmate.core.model;

import java.util.List;

public class LineTokens {

    List<TMToken> tokens;
    int actualStopOffset;
    TMState endState;

    public LineTokens(List<TMToken> tokens, int actualStopOffset, TMState endState) {
        this.tokens = tokens;
        this.actualStopOffset = actualStopOffset;
        this.endState = endState;
    }

    public TMState getEndState() {
        return endState;
    }

    public void setEndState(TMState endState) {
        this.endState = endState;
    }

    public List<TMToken> getTokens() {
        return tokens;
    }
}
