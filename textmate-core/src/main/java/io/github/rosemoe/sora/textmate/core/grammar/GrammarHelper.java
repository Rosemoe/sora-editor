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
package io.github.rosemoe.sora.textmate.core.grammar;

import java.util.Map;

import io.github.rosemoe.sora.textmate.core.internal.grammar.Grammar;
import io.github.rosemoe.sora.textmate.core.internal.oniguruma.OnigString;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;
import io.github.rosemoe.sora.textmate.core.theme.IThemeProvider;

public class GrammarHelper {

    private GrammarHelper() {
        // methods should be accessed statically
    }

    public static IGrammar createGrammar(IRawGrammar grammar, int initialLanguage,
                                         Map<String, Integer> embeddedLanguages, IGrammarRepository repository, IThemeProvider themeProvider) {
        return new Grammar(grammar, initialLanguage, embeddedLanguages, repository, themeProvider);
    }

    public static OnigString createOnigString(String str) {
        return new OnigString(str);
    }

}
