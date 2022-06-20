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
package org.eclipse.tm4e.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IToken;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;

public class Tokenizer implements ITokenizationSupport {

    private final IGrammar grammar;
    private final DecodeMap decodeMap;

    public Tokenizer(IGrammar grammar) {
        this.grammar = grammar;
        this.decodeMap = new DecodeMap();
    }

    @Override
    public TMState getInitialState() {
        return new TMState(null, null);
    }

    @Override
    public LineTokens tokenize(String line, TMState state) {
        return tokenize(line, state, null, null);
    }

    @Override
    public LineTokens tokenize(String line, TMState state, Integer offsetDelta, Integer stopAtOffset) {
        if (offsetDelta == null) {
            offsetDelta = 0;
        }
        // Do not attempt to tokenize if a line has over 20k
        // or if the rule stack contains more than 100 rules (indicator of
        // broken grammar that forgets to pop rules)
        // if (line.length >= 20000 || depth(state.ruleStack) > 100) {
        // return new RawLineTokens(
        // [new Token(offsetDelta, '')],
        // [new ModeTransition(offsetDelta, this._modeId)],
        // offsetDelta,
        // state
        // );
        // }
        TMState freshState = state != null ? state.clone() : getInitialState();
        ITokenizeLineResult textMateResult = grammar.tokenizeLine(line, freshState.getRuleStack());
        freshState.setRuleStack(textMateResult.getRuleStack());

        // Create the result early and fill in the tokens later
        List<TMToken> tokens = new ArrayList<>();
        String lastTokenType = null;
        for (int tokenIndex = 0, len = textMateResult.getTokens().length; tokenIndex < len; tokenIndex++) {
            IToken token = textMateResult.getTokens()[tokenIndex];
            int tokenStartIndex = token.getStartIndex();
            String tokenType = decodeTextMateToken(this.decodeMap, token.getScopes().toArray(new String[0]));

            // do not push a new token if the type is exactly the same (also
            // helps with ligatures)
            if (!tokenType.equals(lastTokenType)) {
                tokens.add(new TMToken(tokenStartIndex + offsetDelta, tokenType));
                lastTokenType = tokenType;
            }
        }
        return new LineTokens(tokens, offsetDelta + line.length(), freshState);

    }

    private String decodeTextMateToken(DecodeMap decodeMap, String[] scopes) {
        String[] prevTokenScopes = decodeMap.prevToken.scopes;
        int prevTokenScopesLength = prevTokenScopes.length;
        Map<Integer, Map<Integer, Boolean>> prevTokenScopeTokensMaps = decodeMap.prevToken.scopeTokensMaps;

        Map<Integer, Map<Integer, Boolean>> scopeTokensMaps = new LinkedHashMap<>();
        Map<Integer, Boolean> prevScopeTokensMaps = new LinkedHashMap<>();
        boolean sameAsPrev = true;
        for (int level = 1/* deliberately skip scope 0 */; level < scopes.length; level++) {
            String scope = scopes[level];

            if (sameAsPrev) {
                if (level < prevTokenScopesLength && prevTokenScopes[level].equals(scope)) {
                    prevScopeTokensMaps = prevTokenScopeTokensMaps.get(level);
                    scopeTokensMaps.put(level, prevScopeTokensMaps);
                    continue;
                }
                sameAsPrev = false;
            }

            int[] tokens = decodeMap.getTokenIds(scope);
            prevScopeTokensMaps = new LinkedHashMap<>(prevScopeTokensMaps);
            for (int token : tokens) {
                prevScopeTokensMaps.put(token, true);
            }
            scopeTokensMaps.put(level, prevScopeTokensMaps);
        }

        decodeMap.prevToken = new TMTokenDecodeData(scopes, scopeTokensMaps);
        return decodeMap.getToken(prevScopeTokensMaps);
    }
}
