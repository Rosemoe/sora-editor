/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package org.eclipse.tm4e.core.registry;

import static org.eclipse.tm4e.core.internal.utils.MoreCollections.nullToEmpty;
import static org.eclipse.tm4e.core.internal.utils.NullSafetyHelper.castNonNull;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.TMException;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.internal.grammar.BalancedBracketSelectors;
import org.eclipse.tm4e.core.internal.grammar.GrammarReader;
import org.eclipse.tm4e.core.internal.grammar.dependencies.AbsoluteRuleReference;
import org.eclipse.tm4e.core.internal.grammar.dependencies.ScopeDependencyProcessor;
import org.eclipse.tm4e.core.internal.registry.SyncRegistry;
import org.eclipse.tm4e.core.internal.theme.IRawTheme;
import org.eclipse.tm4e.core.internal.theme.Theme;
import org.eclipse.tm4e.core.internal.theme.ThemeReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.rosemoe.sora.util.Logger;

/**
 * The registry that will hold all grammars.
 *
 * @see <a href=
 * "https://github.com/microsoft/vscode-textmate/blob/e8d1fc5d04b2fc91384c7a895f6c9ff296a38ac8/src/main.ts#L51">
 * github.com/microsoft/vscode-textmate/blob/main/src/main.ts</a>
 */
public final class Registry {

    private static final Logger LOGGER = Logger.instance(Registry.class.getName());

    private final IRegistryOptions _options;
    private final SyncRegistry _syncRegistry;
    private final Map<String, Boolean> _ensureGrammarCache = new HashMap<>();

    public Registry() {
        this(new IRegistryOptions() {
        });
    }

    public Registry(final IRegistryOptions options) {
        this._options = options;

        this._syncRegistry = new SyncRegistry(
                Theme.createFromRawTheme(options.getTheme(), options.getColorMap()));
    }

    /**
     * Change the theme. Once called, no previous `ruleStack` should be used anymore.
     */
    public void setTheme(final IThemeSource source) throws TMException {
        try {
            this._syncRegistry.setTheme(Theme.createFromRawTheme(
                    ThemeReader.readTheme(source),
                    _options.getColorMap()));
        } catch (final Exception ex) {
            throw new TMException("Loading theme from '" + source.getFilePath() + "' failed: " + ex.getMessage(), ex);
        }
    }

    public void setTheme(Theme theme) {
        this._syncRegistry.setTheme(theme);
    }


    /**
     * Returns a lookup array for color ids.
     */
    public List<String> getColorMap() {
        return this._syncRegistry.getColorMap();
    }

    /**
     * Load the grammar for `scopeName` and all referenced included grammars asynchronously.
     * Please do not use language id 0.
     */
    @Nullable
    public IGrammar loadGrammarWithEmbeddedLanguages(
            final String initialScopeName,
            final int initialLanguage,
            final Map<String, Integer> embeddedLanguages) {
        return this.loadGrammarWithConfiguration(initialScopeName, initialLanguage,
                new IGrammarConfiguration() {
                    @Override
                    public @Nullable Map<String, Integer> getEmbeddedLanguages() {
                        return embeddedLanguages;
                    }
                });
    }

    /**
     * Load the grammar for `scopeName` and all referenced included grammars asynchronously.
     * Please do not use language id 0.
     */
    @Nullable
    public IGrammar loadGrammarWithConfiguration(
            final String initialScopeName,
            final int initialLanguage,
            final IGrammarConfiguration configuration) {
        return this._loadGrammar(
                initialScopeName,
                initialLanguage,
                configuration.getEmbeddedLanguages(),
                configuration.getTokenTypes(),
                new BalancedBracketSelectors(
                        nullToEmpty(configuration.getBalancedBracketSelectors()),
                        nullToEmpty(configuration.getUnbalancedBracketSelectors())));
    }

    /**
     * Load the grammar for `scopeName` and all referenced included grammars.
     */
    @Nullable
    public IGrammar loadGrammar(final String initialScopeName) {
        return this._loadGrammar(initialScopeName, 0, null, null, null);
    }

    @Nullable
    private IGrammar _loadGrammar(
            final String initialScopeName,
            final int initialLanguage,
            @Nullable final Map<String, Integer> embeddedLanguages,
            @Nullable final Map<String, Integer> tokenTypes,
            @Nullable final BalancedBracketSelectors balancedBracketSelectors) {
        final var dependencyProcessor = new ScopeDependencyProcessor(this._syncRegistry, initialScopeName);
        while (!dependencyProcessor.Q.isEmpty()) {
            for (AbsoluteRuleReference request : dependencyProcessor.Q) {
                this._loadSingleGrammar(request.scopeName);
            }
            dependencyProcessor.processQueue();
        }

        return this._grammarForScopeName(
                initialScopeName,
                initialLanguage,
                embeddedLanguages,
                tokenTypes,
                balancedBracketSelectors);
    }

    private void _loadSingleGrammar(final String scopeName) {
        this._ensureGrammarCache.computeIfAbsent(scopeName, this::_doLoadSingleGrammar);
    }

    private boolean _doLoadSingleGrammar(final String scopeName) {
        final var grammarSource = this._options.getGrammarSource(scopeName);
        if (grammarSource == null) {
            LOGGER.w("No grammar source for scope [%s]", scopeName);
            return false;
        }
        try {
            final var grammar = GrammarReader.readGrammar(grammarSource);
            this._syncRegistry.addGrammar(grammar, this._options.getInjections(scopeName));
        } catch (final Exception ex) {
            LOGGER.w("Loading grammar for scope [%s] failed: {%s}", scopeName, ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public IGrammar addGrammar(final IGrammarSource source) throws TMException {
        return addGrammar(source, null, null, null);
    }

    public IGrammar addGrammar(
            final IGrammarSource source,
            @Nullable final List<String> injections,
            @Nullable final Integer initialLanguage,
            @Nullable final Map<String, Integer> embeddedLanguages) throws TMException {
        try {
            final var rawGrammar = GrammarReader.readGrammar(source);
            this._syncRegistry.addGrammar(rawGrammar,
                    injections == null || injections.isEmpty()
                            ? this._options.getInjections(rawGrammar.getScopeName())
                            : injections);
            return castNonNull(
                    this._grammarForScopeName(rawGrammar.getScopeName(), initialLanguage, embeddedLanguages, null, null));

        } catch (final Exception ex) {
            throw new TMException("Loading grammar from '" + source.getFilePath() + "' failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Lookup a grammar. The grammar must first be registered via `loadGrammar` or `addGrammar`.
     */
    @Nullable
    public IGrammar grammarForScopeName(final String scopeName) {
        return _grammarForScopeName(scopeName, null, null, null, null);
    }

    /**
     * Get the grammar for `scopeName`. The grammar must first be created via `loadGrammar` or `addGrammar`.
     */
    @Nullable
    private IGrammar _grammarForScopeName(
            final String scopeName,
            @Nullable final Integer initialLanguage,
            @Nullable final Map<String, Integer> embeddedLanguages,
            @Nullable final Map<String, Integer> tokenTypes,
            @Nullable final BalancedBracketSelectors balancedBracketSelectors) {
        return this._syncRegistry.grammarForScopeName(
                scopeName,
                initialLanguage == null ? 0 : initialLanguage,
                embeddedLanguages,
                tokenTypes,
                balancedBracketSelectors);
    }


}
