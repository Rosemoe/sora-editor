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
package io.github.rosemoe.sora.textmate.core.internal.rule;

public class IncludeOnlyRule extends Rule {

    public final boolean hasMissingPatterns;
    public final Integer[] patterns;
    private RegExpSourceList cachedCompiledPatterns;

    public IncludeOnlyRule(int id, String name, String contentName, ICompilePatternsResult patterns) {
        super(id, name, contentName);
        this.patterns = patterns.patterns;
        this.hasMissingPatterns = patterns.hasMissingPatterns;
        this.cachedCompiledPatterns = null;
    }

    @Override
    public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
        for (Integer pattern : this.patterns) {
            Rule rule = grammar.getRule(pattern);
            rule.collectPatternsRecursive(grammar, out, false);
        }
    }

    @Override
    public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
        if (this.cachedCompiledPatterns == null) {
            this.cachedCompiledPatterns = new RegExpSourceList();
            this.collectPatternsRecursive(grammar, this.cachedCompiledPatterns, true);
        }
        return this.cachedCompiledPatterns.compile(grammar, allowA, allowG);
    }

}
