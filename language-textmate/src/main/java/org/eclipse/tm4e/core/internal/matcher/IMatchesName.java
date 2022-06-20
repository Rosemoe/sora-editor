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
package org.eclipse.tm4e.core.internal.matcher;

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