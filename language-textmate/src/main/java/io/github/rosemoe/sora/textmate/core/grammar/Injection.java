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

import java.util.List;
import java.util.function.Predicate;

import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;

public class Injection {

    public final int priority; // -1 | 0 | 1; // 0 is the default. -1 for 'L' and 1 for 'R'
    public final int ruleId;
    public final IRawGrammar grammar;
    private final Predicate<List<String>> matcher;

    public Injection(Predicate<List<String>> matcher, int ruleId, IRawGrammar grammar, int priority) {
        this.matcher = matcher;
        this.ruleId = ruleId;
        this.grammar = grammar;
        this.priority = priority;
    }

    public boolean match(List<String> states) {
        return matcher.test(states);
    }
}
