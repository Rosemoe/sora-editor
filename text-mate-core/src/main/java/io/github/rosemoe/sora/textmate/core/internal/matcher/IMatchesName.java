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
package io.github.rosemoe.sora.textmate.core.internal.matcher;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public interface IMatchesName<T> {

    public static final IMatchesName<List<String>> NAME_MATCHER = new IMatchesName<List<String>>() {

        @Override
        public boolean match(Collection<String> identifers, List<String> scopes) {
            if (scopes.size() < identifers.size()) {
                return false;
            }
            AtomicInteger lastIndex = new AtomicInteger();
            // every
            return identifers.stream().allMatch(identifier -> {
                for (int i = lastIndex.get(); i < scopes.size(); i++) {
                    if (scopesAreMatching(scopes.get(i), identifier)) {
                        lastIndex.incrementAndGet();
                        return true;
                    }
                }
                return false;
            });
        }

        private boolean scopesAreMatching(String thisScopeName, String scopeName) {
            if (thisScopeName == null) {
                return false;
            }
            if (thisScopeName.equals(scopeName)) {
                return true;
            }
            int len = scopeName.length();
            return thisScopeName.length() > len && thisScopeName.substring(0, len).equals(scopeName)
                    && thisScopeName.charAt(len) == '.';
        }

    };

    boolean match(Collection<String> names, T scopes);

}