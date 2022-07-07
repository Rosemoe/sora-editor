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

import java.util.LinkedHashMap;
import java.util.Map;

class DecodeMap {

    int lastAssignedId;
    Map<String /* scope */, int[] /* ids */> scopeToTokenIds;
    Map<String /* token */, Integer /* id */> tokenToTokenId;
    Map<Integer /* id */, String /* id */> tokenIdToToken;
    TMTokenDecodeData prevToken;

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
