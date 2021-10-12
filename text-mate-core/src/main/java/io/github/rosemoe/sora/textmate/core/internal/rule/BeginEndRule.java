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

public class BeginEndRule extends Rule {

    public final boolean hasMissingPatterns;
    public List<CaptureRule> beginCaptures;
    public boolean endHasBackReferences;
    public List<CaptureRule> endCaptures;
    public boolean applyEndPatternLast;
    public Integer[] patterns;
    private RegExpSource begin;
    private RegExpSource end;
    private RegExpSourceList cachedCompiledPatterns;

    public BeginEndRule(int id, String name, String contentName, String begin, List<CaptureRule> beginCaptures,
                        String end, List<CaptureRule> endCaptures, boolean applyEndPatternLast, ICompilePatternsResult patterns) {
        super(id, name, contentName);
        this.begin = new RegExpSource(begin, this.id);
        this.beginCaptures = beginCaptures;
        this.end = new RegExpSource(end, -1);
        this.endHasBackReferences = this.end.hasBackReferences();
        this.endCaptures = endCaptures;
        this.applyEndPatternLast = applyEndPatternLast;
        this.patterns = patterns.patterns;
        this.hasMissingPatterns = patterns.hasMissingPatterns;
        this.cachedCompiledPatterns = null;
    }

    public String getEndWithResolvedBackReferences(String lineText, IOnigCaptureIndex[] captureIndices) {
        return this.end.resolveBackReferences(lineText, captureIndices);
    }

    @Override
    public void collectPatternsRecursive(IRuleRegistry grammar, RegExpSourceList out, boolean isFirst) {
        if (isFirst) {
            for (Integer pattern : this.patterns) {
                Rule rule = grammar.getRule(pattern);
                rule.collectPatternsRecursive(grammar, out, false);
            }
        } else {
            out.push(this.begin);
        }
    }

    @Override
    public ICompiledRule compile(IRuleRegistry grammar, String endRegexSource, boolean allowA, boolean allowG) {
        RegExpSourceList precompiled = this.precompile(grammar);
        if (this.end.hasBackReferences()) {
            if (this.applyEndPatternLast) {
                precompiled.setSource(precompiled.length() - 1, endRegexSource);
            } else {
                precompiled.setSource(0, endRegexSource);
            }
        }
        return this.cachedCompiledPatterns.compile(grammar, allowA, allowG);
    }

    private RegExpSourceList precompile(IRuleRegistry grammar) {
        if (this.cachedCompiledPatterns == null) {
            this.cachedCompiledPatterns = new RegExpSourceList();

            this.collectPatternsRecursive(grammar, this.cachedCompiledPatterns, true);

            if (this.applyEndPatternLast) {
                this.cachedCompiledPatterns.push(this.end.hasBackReferences() ? this.end.clone() : this.end);
            } else {
                this.cachedCompiledPatterns.unshift(this.end.hasBackReferences() ? this.end.clone() : this.end);
            }
        }
        return this.cachedCompiledPatterns;
    }

}
