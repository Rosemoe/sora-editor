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
package io.github.rosemoe.sora.textmate.core.internal.rule;

import java.util.List;

import io.github.rosemoe.sora.textmate.core.internal.oniguruma.IOnigCaptureIndex;

public class BeginWhileRule extends Rule {

    public final List<CaptureRule> beginCaptures;
    public final List<CaptureRule> whileCaptures;
    public final boolean whileHasBackReferences;
    public final boolean hasMissingPatterns;
    public final Integer[] patterns;
    private RegExpSource begin;
    private RegExpSource _while;
    private RegExpSourceList cachedCompiledPatterns;
    private RegExpSourceList cachedCompiledWhilePatterns;

    public BeginWhileRule(/* $location:ILocation, */ int id, String name, String contentName, String begin,
                                                     List<CaptureRule> beginCaptures, String _while, List<CaptureRule> whileCaptures,
                                                     ICompilePatternsResult patterns) {
        super(/* $location, */id, name, contentName);
        this.begin = new RegExpSource(begin, this.id);
        this.beginCaptures = beginCaptures;
        this.whileCaptures = whileCaptures;
        this._while = new RegExpSource(_while, -2);
        this.whileHasBackReferences = this._while.hasBackReferences();
        this.patterns = patterns.patterns;
        this.hasMissingPatterns = patterns.hasMissingPatterns;
        this.cachedCompiledPatterns = null;
        this.cachedCompiledWhilePatterns = null;
    }

    public String getWhileWithResolvedBackReferences(String lineText, IOnigCaptureIndex[] captureIndices) {
        return this._while.resolveBackReferences(lineText, captureIndices);
    }

    @Override
    public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
        if (isFirst) {
            Rule rule;
            for (Integer pattern : patterns) {
                rule = grammar.getRule(pattern);
                rule.collectPatternsRecursive(grammar, out, false);
            }
        } else {
            out.push(this.begin);
        }
    }

    @Override
    public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
        this.precompile(grammar);
        return this.cachedCompiledPatterns.compile(grammar, allowA, allowG);
    }

    private void precompile(IRuleRegistry grammar) {
        if (this.cachedCompiledPatterns == null) {
            this.cachedCompiledPatterns = new RegExpSourceList();
            this.collectPatternsRecursive(grammar, this.cachedCompiledPatterns, true);
        }
    }

    public ICompiledRule compileWhile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
        this.precompileWhile();
        if (this._while.hasBackReferences()) {
            this.cachedCompiledWhilePatterns.setSource(0, endRegexSource);
        }
        return this.cachedCompiledWhilePatterns.compile(grammar, allowA, allowG);
    }

    private void precompileWhile() {
        if (this.cachedCompiledWhilePatterns == null) {
            this.cachedCompiledWhilePatterns = new RegExpSourceList();
            this.cachedCompiledWhilePatterns.push(this._while.hasBackReferences() ? this._while.clone() : this._while);
        }
    }

}
