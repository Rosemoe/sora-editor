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
package org.eclipse.tm4e.core.internal.rule;

import org.eclipse.tm4e.core.internal.oniguruma.IOnigCaptureIndex;

import java.util.List;

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
