/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
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
package io.github.rosemoe.sora.langs.textmate;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.registry.IThemeSource;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;

public class TextMateLanguage extends EmptyLanguage {

    private final static String LOG_TAG = "TextMateLanguage";

    private int tabSize = 4;

    private boolean useTab = false;

    private final IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
    boolean autoCompleteEnabled;
    final boolean collectIdentifiers;

    TextMateAnalyzer textMateAnalyzer;

    GrammarRegistry grammarRegistry;

    ThemeRegistry themeRegistry;

    LanguageConfiguration languageConfiguration;

    TextMateNewlineHandler[] newlineHandlers;

    TextMateSymbolPairMatch symbolPairMatch;

    private TextMateNewlineHandler newlineHandler;

    protected TextMateLanguage(IGrammar grammar,
                               LanguageConfiguration languageConfiguration,
                               GrammarRegistry grammarRegistry,
                               ThemeRegistry themeRegistry,
                               boolean collectIdentifiers) {

        this.grammarRegistry = grammarRegistry;
        this.themeRegistry = themeRegistry;
        this.collectIdentifiers = collectIdentifiers;
        autoCompleteEnabled = true;
        symbolPairMatch = new TextMateSymbolPairMatch(this);

        createAnalyzerAndNewlineHandler(grammar, languageConfiguration);
    }

    /**
     * Create a new TextMate language object. Lookup language grammars from default grammar registry.
     *
     * @param languageScopeName  Root language scope name
     * @param collectIdentifiers Collect identifiers for auto-completion
     */
    public static TextMateLanguage create(String languageScopeName, boolean collectIdentifiers) {
        return create(languageScopeName, GrammarRegistry.getInstance(), collectIdentifiers);
    }

    /**
     * Create a new TextMate language object. Lookup language grammars from given grammar registry.
     *
     * @param languageScopeName  Root language scope name
     * @param grammarRegistry    Grammar registry to lookup language grammars
     * @param collectIdentifiers Collect identifiers for auto-completion
     */
    public static TextMateLanguage create(String languageScopeName, GrammarRegistry grammarRegistry, boolean collectIdentifiers) {
        return create(languageScopeName, grammarRegistry, ThemeRegistry.getInstance(), collectIdentifiers);
    }

    /**
     * Create a new TextMate language object. Lookup language grammars from given grammar registry. Use theme from given theme registry.
     *
     * @param languageScopeName  Root language scope name
     * @param grammarRegistry    Grammar registry to lookup language grammars
     * @param themeRegistry      Theme registry for theme management
     * @param collectIdentifiers Collect identifiers for auto-completion
     */
    public static TextMateLanguage create(String languageScopeName, GrammarRegistry grammarRegistry, ThemeRegistry themeRegistry, boolean collectIdentifiers) {
        var grammar = grammarRegistry.findGrammar(languageScopeName);
        if (grammar == null) {
            throw new IllegalArgumentException(String.format("Language with %s scope name %s not found", grammarRegistry, languageScopeName));
        }
        var languageConfiguration = grammarRegistry.findLanguageConfiguration(grammar.getScopeName());
        return new TextMateLanguage(grammar, languageConfiguration, grammarRegistry, themeRegistry, collectIdentifiers);
    }

    /**
     * Create a new TextMate language object. Lookup language grammars from default grammar registry.
     *
     * @param grammarDefinition  Root language grammar definition
     * @param collectIdentifiers Collect identifiers for auto-completion
     */
    public static TextMateLanguage create(GrammarDefinition grammarDefinition, boolean collectIdentifiers) {
        return create(grammarDefinition, GrammarRegistry.getInstance(), collectIdentifiers);
    }

    /**
     * Create a new TextMate language object. Lookup language grammars from given grammar registry.
     *
     * @param grammarDefinition  Root language grammar definition
     * @param grammarRegistry    Grammar registry to lookup language grammars
     * @param collectIdentifiers Collect identifiers for auto-completion
     */
    public static TextMateLanguage create(GrammarDefinition grammarDefinition, GrammarRegistry grammarRegistry, boolean collectIdentifiers) {
        return create(grammarDefinition, grammarRegistry, ThemeRegistry.getInstance(), collectIdentifiers);
    }

    /**
     * Create a new TextMate language object. Lookup language grammars from given grammar registry. Use theme from given theme registry.
     *
     * @param grammarDefinition  Root language grammar definition
     * @param grammarRegistry    Grammar registry to lookup language grammars
     * @param themeRegistry      Theme registry for theme management
     * @param collectIdentifiers Collect identifiers for auto-completion
     */
    public static TextMateLanguage create(GrammarDefinition grammarDefinition, GrammarRegistry grammarRegistry, ThemeRegistry themeRegistry, boolean collectIdentifiers) {
        var grammar = grammarRegistry.loadGrammar(grammarDefinition);
        if (grammar == null) {
            throw new IllegalArgumentException(String.format("Language with %s grammar definition %s not found", grammarRegistry, grammarDefinition));
        }
        var languageConfiguration = grammarRegistry.findLanguageConfiguration(grammar.getScopeName());
        return new TextMateLanguage(grammar, languageConfiguration, grammarRegistry, themeRegistry, collectIdentifiers);
    }


    /**
     * When you update the {@link TextMateColorScheme} for editor, you need to synchronize the updates here
     *
     * @param theme IThemeSource creates from file
     * @deprecated Use {@link ThemeRegistry#setTheme(String)}
     */
    @WorkerThread
    @Deprecated
    public void updateTheme(IThemeSource theme) throws Exception {
        //if (textMateAnalyzer != null) {
        //  textMateAnalyzer.updateTheme(theme);
        //}
        themeRegistry.loadTheme(theme);
    }


    private void createAnalyzerAndNewlineHandler(IGrammar grammar, LanguageConfiguration languageConfiguration) {
        var old = textMateAnalyzer;
        if (old != null) {
            old.setReceiver(null);
            old.destroy();
        }
        try {
            textMateAnalyzer = new TextMateAnalyzer(this, grammar, languageConfiguration, /*grammarRegistry,*/ themeRegistry);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to create analyzer for TextMate", e);
        }
        this.languageConfiguration = languageConfiguration;
        newlineHandler = new TextMateNewlineHandler(this);
        newlineHandlers = new TextMateNewlineHandler[]{newlineHandler};
        if (languageConfiguration != null) {
            // because the editor will only get the symbol pair matcher once
            // (caching object to stop repeated new object created),
            // the symbol pair needs to be updated inside the symbol pair matcher.
            symbolPairMatch.updatePair();
        }
    }

    /**
     * Update the root language to the given scope name.
     * <p>
     * <strong>This should be called when the language is detached from editor.</strong>
     *
     * @param scopeName New language scope name
     */
    public void updateLanguage(@NonNull String scopeName) {
        var grammar = grammarRegistry.findGrammar(scopeName);
        if (grammar == null) {
            throw new IllegalArgumentException(String.format("Language with %s scope name %s not found", grammarRegistry, scopeName));
        }
        var languageConfiguration = grammarRegistry.findLanguageConfiguration(grammar.getScopeName());
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration);
    }

    /**
     * Update the root language to the given grammar definition.
     * <p>
     * <strong>This should be called when the language is detached from editor.</strong>
     *
     * @param grammarDefinition New language grammar definition
     */
    public void updateLanguage(@NonNull GrammarDefinition grammarDefinition) {
        var grammar = grammarRegistry.loadGrammar(grammarDefinition);
        var languageConfiguration = grammarRegistry.findLanguageConfiguration(grammar.getScopeName());
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration);
    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        if (textMateAnalyzer == null) {
            return EmptyAnalyzeManager.INSTANCE;
        }
        return textMateAnalyzer;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Set tab size. The tab size is used to compute code blocks.
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    public int getTabSize() {
        return tabSize;
    }

    @Override
    public boolean useTab() {
        return useTab;
    }

    public void useTab(boolean useTab) {
        this.useTab = useTab;
    }

    public TextMateNewlineHandler getNewlineHandler() {
        return newlineHandler;
    }

    public LanguageConfiguration getLanguageConfiguration() {
        return languageConfiguration;
    }

    @Override
    public TextMateSymbolPairMatch getSymbolPairs() {
        return symbolPairMatch;
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        return newlineHandlers;
    }

    /**
     * Whether auto-completion is enabled.
     * @see #setAutoCompleteEnabled(boolean)
     */
    public boolean isAutoCompleteEnabled() {
        return autoCompleteEnabled;
    }

    /**
     * Set whether auto-completion is enabled.
     * <p>
     * Identifiers are available in auto-completion only when the language instance is set to collect identifiers at the time it is created.
     */
    public void setAutoCompleteEnabled(boolean autoCompleteEnabled) {
        this.autoCompleteEnabled = autoCompleteEnabled;
    }

    @Override
    public void requireAutoComplete(@NonNull ContentReference content, @NonNull CharPosition position, @NonNull CompletionPublisher publisher, @NonNull Bundle extraArguments) {
        if (!autoCompleteEnabled) {
            return;
        }
        var prefix = CompletionHelper.computePrefix(content, position, MyCharacter::isJavaIdentifierPart);
        final var idt = textMateAnalyzer.syncIdentifiers;
        autoComplete.requireAutoComplete(content, position, prefix, publisher, idt);
    }

    public IdentifierAutoComplete getAutoCompleter() {
        return autoComplete;
    }

    /**
     * Set keywords for auto-completion
     */
    public void setCompleterKeywords(String[] keywords) {
        autoComplete.setKeywords(keywords, false);
    }
}
