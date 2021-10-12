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
package io.github.rosemoe.sora.textmate.core.registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.rosemoe.sora.textmate.core.TMException;
import io.github.rosemoe.sora.textmate.core.grammar.IGrammar;
import io.github.rosemoe.sora.textmate.core.internal.grammar.reader.GrammarReader;
import io.github.rosemoe.sora.textmate.core.internal.grammars.SyncRegistry;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;
import io.github.rosemoe.sora.textmate.core.theme.IRawTheme;
import io.github.rosemoe.sora.textmate.core.theme.Theme;

/**
 * The registry that will hold all grammars.
 *
 * @see https://github.com/Microsoft/vscode-textmate/blob/master/src/main.ts
 *
 */
public class Registry {

    private final IRegistryOptions locator;
    private final SyncRegistry syncRegistry;

    public Registry() {
        this(IRegistryOptions.DEFAULT_LOCATOR);
    }

    public Registry(IRegistryOptions locator) {
        this.locator = locator;
        this.syncRegistry = new SyncRegistry(Theme.createFromRawTheme(locator.getTheme()));
    }

    /**
     * Change the theme. Once called, no previous `ruleStack` should be used
     * anymore.
     */
    public void setTheme(IRawTheme theme) {
        this.syncRegistry.setTheme(Theme.createFromRawTheme(theme));
    }

    /**
     * Returns a lookup array for color ids.
     */
    public Set<String> getColorMap() {
        return this.syncRegistry.getColorMap();
    }

    public IGrammar loadGrammar(String initialScopeName) {
        return _loadGrammar(initialScopeName);
    }

    private IGrammar _loadGrammar(String initialScopeName) {

        List<String> remainingScopeNames = new ArrayList<>();
        remainingScopeNames.add(initialScopeName);

        List<String> seenScopeNames = new ArrayList<>();
        seenScopeNames.add(initialScopeName);

        while (!remainingScopeNames.isEmpty()) {
            String scopeName = remainingScopeNames.remove(0); // shift();

            if (this.syncRegistry.lookup(scopeName) != null) {
                continue;
            }

            String filePath = this.locator.getFilePath(scopeName);
            if (filePath == null) {
                if (scopeName.equals(initialScopeName)) {
                    throw new TMException("Unknown location for grammar <" + initialScopeName + ">");
                    // callback(new Error('Unknown location for grammar <' +
                    // initialScopeName + '>'), null);
                    // return;
                }
                continue;
            }

            try {
                InputStream in = this.locator.getInputStream(scopeName);
                IRawGrammar grammar = GrammarReader.readGrammarSync(filePath, in);
                Collection<String> injections = this.locator.getInjections(scopeName);

                Collection<String> deps = this.syncRegistry.addGrammar(grammar, injections);
                for (String dep : deps) {
                    if (!seenScopeNames.contains(dep)) {
                        seenScopeNames.add(dep);
                        remainingScopeNames.add(dep);
                    }
                }
            } catch (Throwable e) {
                if (scopeName.equals(initialScopeName)) {
                    // callback(new Error('Unknown location for grammar <' +
                    // initialScopeName + '>'), null);
                    // return;
                    throw new TMException("Unknown location for grammar <" + initialScopeName + ">", e);
                }
            }
        }
        return this.grammarForScopeName(initialScopeName);
    }

    public IGrammar loadGrammarFromPathSync(File file) throws Exception {
        return loadGrammarFromPathSync(file.getPath(), new FileInputStream(file));
    }

    public IGrammar loadGrammarFromPathSync(String path, InputStream in) throws Exception {
        return loadGrammarFromPathSync(path, in, 0, null);
    }

    /**
     * Load the grammar at `path` synchronously.
     *
     * @throws Exception
     */
    public IGrammar loadGrammarFromPathSync(String path, InputStream in, int initialLanguage,
                                            Map<String, Integer> embeddedLanguages) throws Exception {
        IRawGrammar rawGrammar = GrammarReader.readGrammarSync(path, in);
        Collection<String> injections = this.locator.getInjections(rawGrammar.getScopeName());
        this.syncRegistry.addGrammar(rawGrammar, injections);
        return this.grammarForScopeName(rawGrammar.getScopeName(), initialLanguage, embeddedLanguages);
    }

    public IGrammar grammarForScopeName(String scopeName) {
        return grammarForScopeName(scopeName, 0, null);
    }

    /**
     * Get the grammar for `scopeName`. The grammar must first be created via
     * `loadGrammar` or `loadGrammarFromPathSync`.
     */
    public IGrammar grammarForScopeName(String scopeName, int initialLanguage, Map<String, Integer> embeddedLanguages) {
        return this.syncRegistry.grammarForScopeName(scopeName, initialLanguage, embeddedLanguages);
    }

    public IRegistryOptions getLocator() {
        return locator;
    }
}
