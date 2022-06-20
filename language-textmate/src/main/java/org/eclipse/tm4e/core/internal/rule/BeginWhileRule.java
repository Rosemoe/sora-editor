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
