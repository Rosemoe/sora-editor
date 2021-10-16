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

import java.util.LinkedHashMap;
import java.util.Map;

class DecodeMap {

    int lastAssignedId;
    Map<String /* scope */, int[] /* ids */> scopeToTokenIds;
    Map<String /* token */, Integer /* id */> tokenToTokenId;
    Map<Integer /* id */, String /* id */> tokenIdToToken;
    TMTokenDecodeData prevToken;
    ;

    public DecodeMap() {
        this.lastAssignedId = 0;
        this.scopeToTokenIds = new LinkedHashMap<>();
        this.tokenToTokenId = new LinkedHashMap<>();
        this.tokenIdToToken = new LinkedHashMap<>();
        this.prevToken = new TMTokenDecodeData(new String[0], new LinkedHashMap<Integer, Map<Integer, Boolean>>());
    }

    public int[] getTokenIds(String scope) {
        int[] tokens = this.scopeToTokenIds.get(scope);
        if (tokens != null) {
            return tokens;
        }
        String[] tmpTokens = scope.split("[.]");

        tokens = new int[tmpTokens.length];
        for (int i = 0; i < tmpTokens.length; i++) {
            String token = tmpTokens[i];
            Integer tokenId = this.tokenToTokenId.get(token);
            if (tokenId == null) {
                tokenId = (++this.lastAssignedId);
                this.tokenToTokenId.put(token, tokenId);
                this.tokenIdToToken.put(tokenId, token);
            }
            tokens[i] = tokenId;
        }

        this.scopeToTokenIds.put(scope, tokens);
        return tokens;
    }

    public String getToken(Map<Integer, Boolean> tokenMap) {
        StringBuilder result = new StringBuilder();
        boolean isFirst = true;
        for (int i = 1; i <= this.lastAssignedId; i++) {
            if (tokenMap.containsKey(i)) {
                if (isFirst) {
                    isFirst = false;
                    result.append(this.tokenIdToToken.get(i));
                } else {
                    result.append('.');
                    result.append(this.tokenIdToToken.get(i));
                }
            }
        }
        return result.toString();
    }
}
