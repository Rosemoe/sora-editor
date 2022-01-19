/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package io.github.rosemoe.sora.textmate.core.internal.grammar;

import java.util.List;

import io.github.rosemoe.sora.textmate.core.theme.ThemeTrieElementRule;

public class ScopeMetadata {

    public final String scopeName;
    public final int languageId;
    public final int tokenType;
    public final List<ThemeTrieElementRule> themeData;

    public ScopeMetadata(String scopeName, int languageId, int tokenType, List<ThemeTrieElementRule> themeData) {
        this.scopeName = scopeName;
        this.languageId = languageId;
        this.tokenType = tokenType;
        this.themeData = themeData;
    }
}
