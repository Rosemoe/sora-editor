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
 * - Fabio Zadrozny <fabiofz@gmail.com> - Not adding '\n' on tokenize if it already finished with '\n'
 */
package io.github.rosemoe.sora.textmate.core.internal.grammar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import io.github.rosemoe.sora.textmate.core.grammar.GrammarHelper;
import io.github.rosemoe.sora.textmate.core.grammar.IGrammar;
import io.github.rosemoe.sora.textmate.core.grammar.IGrammarRepository;
import io.github.rosemoe.sora.textmate.core.grammar.ITokenizeLineResult;
import io.github.rosemoe.sora.textmate.core.grammar.ITokenizeLineResult2;
import io.github.rosemoe.sora.textmate.core.grammar.Injection;
import io.github.rosemoe.sora.textmate.core.grammar.StackElement;
import io.github.rosemoe.sora.textmate.core.internal.grammar.parser.Raw;
import io.github.rosemoe.sora.textmate.core.internal.matcher.Matcher;
import io.github.rosemoe.sora.textmate.core.internal.matcher.MatcherWithPriority;
import io.github.rosemoe.sora.textmate.core.internal.oniguruma.OnigString;
import io.github.rosemoe.sora.textmate.core.internal.rule.IRuleFactoryHelper;
import io.github.rosemoe.sora.textmate.core.internal.rule.Rule;
import io.github.rosemoe.sora.textmate.core.internal.rule.RuleFactory;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawRepository;
import io.github.rosemoe.sora.textmate.core.internal.types.IRawRule;
import io.github.rosemoe.sora.textmate.core.theme.IThemeProvider;
import io.github.rosemoe.sora.textmate.core.theme.ThemeTrieElementRule;

/**
 * TextMate grammar implementation.
 *
 * @see https://github.com/Microsoft/vscode-textmate/blob/master/src/grammar.ts
 *
 */
public class Grammar implements IGrammar, IRuleFactoryHelper {

    private final Map<Integer, Rule> ruleId2desc;
    private final Map<String, IRawGrammar> includedGrammars;
    private final IGrammarRepository grammarRepository;
    private final IRawGrammar grammar;
    private final ScopeMetadataProvider scopeMetadataProvider;
    private int rootId;
    private int lastRuleId;
    private List<Injection> injections;

    public Grammar(IRawGrammar grammar, int initialLanguage, Map<String, Integer> embeddedLanguages,
                   IGrammarRepository grammarRepository, IThemeProvider themeProvider) {
        this.scopeMetadataProvider = new ScopeMetadataProvider(initialLanguage, themeProvider, embeddedLanguages);
        this.rootId = -1;
        this.lastRuleId = 0;
        this.includedGrammars = new HashMap<>();
        this.grammarRepository = grammarRepository;
        this.grammar = initGrammar(grammar, null);
        this.ruleId2desc = new HashMap<>();
        this.injections = null;
    }

    public void onDidChangeTheme() {
        this.scopeMetadataProvider.onDidChangeTheme();
    }

    public ScopeMetadata getMetadataForScope(String scope) {
        return this.scopeMetadataProvider.getMetadataForScope(scope);
    }

    public List<Injection> getInjections() {
        if (this.injections == null) {
            this.injections = new ArrayList<>();
            // add injections from the current grammar
            Map<String, IRawRule> rawInjections = this.grammar.getInjections();
            if (rawInjections != null) {
                for (Entry<String, IRawRule> injection : rawInjections.entrySet()) {
                    String expression = injection.getKey();
                    IRawRule rule = injection.getValue();
                    collectInjections(this.injections, expression, rule, this, this.grammar);
                }
            }

            // add injection grammars contributed for the current scope
            if (this.grammarRepository != null) {
                Collection<String> injectionScopeNames = this.grammarRepository
                        .injections(this.grammar.getScopeName());
                if (injectionScopeNames != null) {
                    injectionScopeNames.forEach(injectionScopeName -> {
                        IRawGrammar injectionGrammar = this.getExternalGrammar(injectionScopeName);
                        if (injectionGrammar != null) {
                            String selector = injectionGrammar.getInjectionSelector();
                            if (selector != null) {
                                collectInjections(this.injections, selector, (IRawRule) injectionGrammar, this,
                                        injectionGrammar);
                            }
                        }
                    });
                }
            }
            Collections.sort(this.injections, (i1, i2) -> i1.priority - i2.priority); // sort by priority
        }
        if (this.injections.isEmpty()) {
            return this.injections;
        }
        return this.injections;
    }

    private void collectInjections(List<Injection> result, String selector, IRawRule rule,
                                   IRuleFactoryHelper ruleFactoryHelper, IRawGrammar grammar) {
        Collection<MatcherWithPriority<List<String>>> matchers = Matcher.createMatchers(selector);
        int ruleId = RuleFactory.getCompiledRuleId(rule, ruleFactoryHelper, grammar.getRepository());

        for (MatcherWithPriority<List<String>> matcher : matchers) {
            result.add(new Injection(matcher.matcher, ruleId, grammar, matcher.priority));
        }
    }

    @Override
    public Rule registerRule(IntFunction<Rule> factory) {
        int id = (++this.lastRuleId);
        Rule result = factory.apply(id);
        this.ruleId2desc.put(id, result);
        return result;
    }

    @Override
    public Rule getRule(int patternId) {
        return this.ruleId2desc.get(patternId);
    }

    public IRawGrammar getExternalGrammar(String scopeName) {
        return getExternalGrammar(scopeName, null);
    }

    @Override
    public IRawGrammar getExternalGrammar(String scopeName, IRawRepository repository) {
        if (this.includedGrammars.containsKey(scopeName)) {
            return this.includedGrammars.get(scopeName);
        } else if (this.grammarRepository != null) {
            IRawGrammar rawIncludedGrammar = this.grammarRepository.lookup(scopeName);
            if (rawIncludedGrammar != null) {
                this.includedGrammars.put(scopeName,
                        initGrammar(rawIncludedGrammar, repository != null ? repository.getBase() : null));
                return this.includedGrammars.get(scopeName);
            }
        }
        return null;
    }

    private IRawGrammar initGrammar(IRawGrammar grammar, IRawRule base) {
        grammar = clone(grammar);
        if (grammar.getRepository() == null) {
            ((Raw) grammar).setRepository(new Raw());
        }
        Raw self = new Raw();
        self.setPatterns(grammar.getPatterns());
        self.setName(grammar.getScopeName());
        grammar.getRepository().setSelf(self);
        if (base != null) {
            grammar.getRepository().setBase(base);
        } else {
            grammar.getRepository().setBase(grammar.getRepository().getSelf());
        }
        return grammar;
    }

    private IRawGrammar clone(IRawGrammar grammar) {
        return (IRawGrammar) ((Raw) grammar).clone();
    }

    @Override
    public ITokenizeLineResult tokenizeLine(String lineText) {
        return tokenizeLine(lineText, null);
    }

    @Override
    public ITokenizeLineResult tokenizeLine(String lineText, StackElement prevState) {
        return tokenize(lineText, prevState, false);
    }

    @Override
    public ITokenizeLineResult2 tokenizeLine2(String lineText) {
        return tokenizeLine2(lineText, null);
    }

    @Override
    public ITokenizeLineResult2 tokenizeLine2(String lineText, StackElement prevState) {
        return tokenize(lineText, prevState, true);
    }

    @SuppressWarnings("unchecked")
    private <T> T tokenize(String lineText, StackElement prevState, boolean emitBinaryTokens) {
        if (this.rootId == -1) {
            this.rootId = RuleFactory.getCompiledRuleId(this.grammar.getRepository().getSelf(), this,
                    this.grammar.getRepository());
        }

        boolean isFirstLine;
        if (prevState == null || prevState.equals(StackElement.NULL)) {
            isFirstLine = true;
            ScopeMetadata rawDefaultMetadata = this.scopeMetadataProvider.getDefaultMetadata();
            ThemeTrieElementRule defaultTheme = rawDefaultMetadata.themeData.get(0);
            int defaultMetadata = StackElementMetadata.set(0, rawDefaultMetadata.languageId,
                    rawDefaultMetadata.tokenType, defaultTheme.fontStyle, defaultTheme.foreground,
                    defaultTheme.background);

            String rootScopeName = this.getRule(this.rootId).getName(null, null);
            ScopeMetadata rawRootMetadata = this.scopeMetadataProvider.getMetadataForScope(rootScopeName);
            int rootMetadata = ScopeListElement.mergeMetadata(defaultMetadata, null, rawRootMetadata);

            ScopeListElement scopeList = new ScopeListElement(null, rootScopeName, rootMetadata);

            prevState = new StackElement(null, this.rootId, -1, null, scopeList, scopeList);
        } else {
            isFirstLine = false;
            prevState.reset();
        }

        if (lineText.isEmpty() || lineText.charAt(lineText.length() - 1) != '\n') {
            // Only add \n if the passed lineText didn't have it.
            lineText += '\n';
        }
        OnigString onigLineText = GrammarHelper.createOnigString(lineText);
        int lineLength = lineText.length();
        LineTokens lineTokens = new LineTokens(emitBinaryTokens, lineText);
        StackElement nextState = LineTokenizer.tokenizeString(this, onigLineText, isFirstLine, 0, prevState,
                lineTokens);

        if (emitBinaryTokens) {
            return (T) new TokenizeLineResult2(lineTokens.getBinaryResult(nextState, lineLength), nextState);
        }
        return (T) new TokenizeLineResult(lineTokens.getResult(nextState, lineLength), nextState);
    }

    @Override
    public String getName() {
        return grammar.getName();
    }

    @Override
    public String getScopeName() {
        return grammar.getScopeName();
    }

    @Override
    public Collection<String> getFileTypes() {
        return grammar.getFileTypes();
    }

}
