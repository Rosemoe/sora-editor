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
package org.eclipse.tm4e.core.internal.grammars;

import org.eclipse.tm4e.core.grammar.GrammarHelper;
import org.eclipse.tm4e.core.theme.IThemeProvider;
import org.eclipse.tm4e.core.theme.Theme;
import org.eclipse.tm4e.core.theme.ThemeTrieElementRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IGrammarRepository;
import org.eclipse.tm4e.core.internal.grammar.Grammar;
import org.eclipse.tm4e.core.internal.grammar.parser.Raw;
import org.eclipse.tm4e.core.internal.types.IRawGrammar;
import org.eclipse.tm4e.core.internal.types.IRawRepository;
import org.eclipse.tm4e.core.internal.types.IRawRule;

public class SyncRegistry implements IGrammarRepository, IThemeProvider {

    private final Map<String, IGrammar> grammars;
    private final Map<String, IRawGrammar> rawGrammars;
    private final Map<String, Collection<String>> injectionGrammars;
    private Theme theme;

    public SyncRegistry(Theme theme) {
        this.theme = theme;
        this.grammars = new HashMap<>();
        this.rawGrammars = new HashMap<>();
        this.injectionGrammars = new HashMap<>();
    }

    private static void collectIncludedScopes(Collection<String> result, IRawGrammar grammar) {
        if (grammar
                .getPatterns() != null /* && Array.isArray(grammar.patterns) */) {
            extractIncludedScopesInPatterns(result, grammar.getPatterns());
        }

        IRawRepository repository = grammar.getRepository();
        if (repository != null) {
            extractIncludedScopesInRepository(result, repository);
        }

        // remove references to own scope (avoid recursion)
        result.remove(grammar.getScopeName());
    }

    /**
     * Fill in `result` all external included scopes in `patterns`
     */
    private static void extractIncludedScopesInPatterns(Collection<String> result, Collection<IRawRule> patterns) {
        for (IRawRule pattern : patterns) {
            Collection<IRawRule> p = pattern.getPatterns();
            if (p != null) {
                extractIncludedScopesInPatterns(result, p);
            }

            String include = pattern.getInclude();
            if (include == null) {
                continue;
            }

            if (include.equals("$base") || include.equals("$self")) {
                // Special includes that can be resolved locally in this grammar
                continue;
            }

            if (include.charAt(0) == '#') {
                // Local include from this grammar
                continue;
            }

            int sharpIndex = include.indexOf('#');
            if (sharpIndex >= 0) {
                addIncludedScope(include.substring(0, sharpIndex), result);
            } else {
                addIncludedScope(include, result);
            }
        }
    }

    private static void addIncludedScope(String scopeName, Collection<String> includedScopes) {
        if (!includedScopes.contains(scopeName)) {
            includedScopes.add(scopeName);
        }
    }

    /**
     * Fill in `result` all external included scopes in `repository`
     */
    private static void extractIncludedScopesInRepository(Collection<String> result, IRawRepository repository) {
        if (!(repository instanceof Raw)) {
            return;
        }
        Raw rawRepository = (Raw) repository;
        for (Entry<String, Object> entry : rawRepository.entrySet()) {
            IRawRule rule = (IRawRule) entry.getValue();
            if (rule.getPatterns() != null) {
                extractIncludedScopesInPatterns(result, rule.getPatterns());
            }
            if (rule.getRepository() != null) {
                extractIncludedScopesInRepository(result, rule.getRepository());
            }
        }
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
        this.grammars.values().forEach(grammar -> ((Grammar) grammar).onDidChangeTheme());
    }

    public Set<String> getColorMap() {
        return this.theme.getColorMap();
    }

    /**
     * Add `grammar` to registry and return a list of referenced scope names
     */
    public Collection<String> addGrammar(IRawGrammar grammar, Collection<String> injectionScopeNames) {
        this.rawGrammars.put(grammar.getScopeName(), grammar);
        Collection<String> includedScopes = new ArrayList<>();
        collectIncludedScopes(includedScopes, grammar);

        if (injectionScopeNames != null) {
            this.injectionGrammars.put(grammar.getScopeName(), injectionScopeNames);
            injectionScopeNames.forEach(scopeName -> addIncludedScope(scopeName, includedScopes));
        }
        return includedScopes;
    }

    @Override
    public IRawGrammar lookup(String scopeName) {
        return this.rawGrammars.get(scopeName);
    }

    @Override
    public Collection<String> injections(String targetScope) {
        return this.injectionGrammars.get(targetScope);
    }

    /**
     * Get the default theme settings
     */
    @Override
    public ThemeTrieElementRule getDefaults() {
        return this.theme.getDefaults();
    }

    /**
     * Match a scope in the theme.
     */
    @Override
    public List<ThemeTrieElementRule> themeMatch(String scopeName) {
        return this.theme.match(scopeName);
    }

    /**
     * Lookup a grammar.
     */
    public IGrammar grammarForScopeName(String scopeName, int initialLanguage,
                                        Map<String, Integer> embeddedLanguages) {
        if (!this.grammars.containsKey(scopeName)) {
            IRawGrammar rawGrammar = lookup(scopeName);
            if (rawGrammar == null) {
                return null;
            }
            this.grammars.put(scopeName,
                    GrammarHelper.createGrammar(rawGrammar, initialLanguage, embeddedLanguages, this, this));
        }
        return this.grammars.get(scopeName);
    }

}
