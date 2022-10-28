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
package io.github.rosemoe.sora.langs.textmate;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.core.registry.IThemeSource;
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration;

import java.io.Reader;
import java.util.Objects;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.completion.CompletionHelper;
import io.github.rosemoe.sora.lang.completion.CompletionPublisher;
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.langs.textmate.registry.LanguageRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultLanguageDefinition;
import io.github.rosemoe.sora.langs.textmate.registry.model.LanguageDefinition;
import io.github.rosemoe.sora.langs.textmate.utils.StringUtils;
import io.github.rosemoe.sora.text.CharPosition;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class TextMateLanguage extends EmptyLanguage {

    private TextMateAnalyzer textMateAnalyzer;
    private int tabSize = 4;
    private final IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
    boolean autoCompleteEnabled;
    final boolean createIdentifiers;

    LanguageRegistry languageRegistry;

    ThemeRegistry themeRegistry;

    //OnEnterSupport ....
    LanguageConfiguration languageConfiguration;

    TextMateNewlineHandler[] newlineHandlers;

    private TextMateNewlineHandler newlineHandler;

    protected TextMateLanguage(IGrammar grammar,
                               LanguageConfiguration languageConfiguration,
                               LanguageRegistry languageRegistry,
                               ThemeRegistry themeRegistry,
                               boolean createIdentifiers) {

        this.languageRegistry = languageRegistry;
        this.themeRegistry = themeRegistry;
        this.languageConfiguration = languageConfiguration;
        //this.grammar = grammar;


        autoCompleteEnabled = true;

        this.createIdentifiers = createIdentifiers;

        createAnalyzerAndNewlineHandler(grammar, languageConfiguration);

    }


    @Deprecated
    public static IGrammar prepareLoad(IGrammarSource grammarSource, @Nullable Reader languageConfiguration, IThemeSource themeSource) {
        var definition = DefaultLanguageDefinition.withGrammarSource(grammarSource, StringUtils.getFileNameWithoutExtension(grammarSource.getFilePath()), null);
        var languageRegistry = LanguageRegistry.getInstance();
        var grammar = languageRegistry.loadLanguage(definition);
        if (languageConfiguration != null) {
            languageRegistry.languageConfigurationToGrammar(LanguageConfiguration.load(languageConfiguration), grammar);
        }
        var themeRegistry = ThemeRegistry.getInstance();
        try {
            themeRegistry.loadTheme(themeSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return grammar;
    }

    @Deprecated
    public static TextMateLanguage create(IGrammarSource grammarSource, Reader languageConfiguration, IThemeSource themeSource) {
        var grammar = prepareLoad(grammarSource, languageConfiguration, themeSource);
        return create(grammar.getScopeName(), true);
    }

    @Deprecated
    public static TextMateLanguage create(IGrammarSource grammarSource, IThemeSource themeSource) {
        var grammar = prepareLoad(grammarSource, null, themeSource);
        return create(grammar.getScopeName(), true);
    }

    @Deprecated
    public static TextMateLanguage createNoCompletion(IGrammarSource grammarSource, Reader languageConfiguration, IThemeSource themeSource) {
        var grammar = prepareLoad(grammarSource, languageConfiguration, themeSource);
        return create(grammar.getScopeName(), false);
    }

    @Deprecated
    public static TextMateLanguage createNoCompletion(IGrammarSource grammarSource, IThemeSource themeSource) {
        var grammar = prepareLoad(grammarSource, null, themeSource);
        return create(grammar.getScopeName(), false);
    }

    public static TextMateLanguage create(String languageScopeName, boolean autoCompleteEnabled) {
        return create(languageScopeName, LanguageRegistry.getInstance(), autoCompleteEnabled);
    }

    public static TextMateLanguage create(String languageScopeName, LanguageRegistry languageRegistry, boolean autoCompleteEnabled) {
        return create(languageScopeName, languageRegistry, ThemeRegistry.getInstance(), autoCompleteEnabled);
    }

    public static TextMateLanguage create(String languageScopeName, LanguageRegistry languageRegistry, ThemeRegistry themeRegistry, boolean autoCompleteEnabled) {
        var grammar = languageRegistry.findGrammar(languageScopeName);

        if (grammar == null) {
            throw new IllegalArgumentException(String.format("Language with %s scope name not found", languageRegistry));
        }

        var languageConfiguration = languageRegistry.findLanguageConfiguration(grammar.getScopeName());


        return new TextMateLanguage(grammar, languageConfiguration, languageRegistry, themeRegistry, autoCompleteEnabled);
    }


    public static TextMateLanguage create(LanguageDefinition languageDefinition, boolean autoCompleteEnabled) {
        return create(languageDefinition, LanguageRegistry.getInstance(), autoCompleteEnabled);
    }

    public static TextMateLanguage create(LanguageDefinition languageDefinition, LanguageRegistry languageRegistry, boolean autoCompleteEnabled) {
        return create(languageDefinition, languageRegistry, ThemeRegistry.getInstance(), autoCompleteEnabled);
    }

    public static TextMateLanguage create(LanguageDefinition languageDefinition, LanguageRegistry languageRegistry, ThemeRegistry themeRegistry, boolean autoCompleteEnabled) {
        var grammar = languageRegistry.loadLanguage(languageDefinition);

        if (grammar == null) {
            throw new IllegalArgumentException(String.format("Language with %s scope name not found", languageRegistry));
        }

        var languageConfiguration = languageRegistry.findLanguageConfiguration(grammar.getScopeName());

        return new TextMateLanguage(grammar, languageConfiguration, languageRegistry, themeRegistry, autoCompleteEnabled);
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
        try {
            textMateAnalyzer = new TextMateAnalyzer(this, grammar, languageConfiguration, /*languageRegistry,*/ themeRegistry);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.languageConfiguration = languageConfiguration;

        newlineHandler = new TextMateNewlineHandler(this);

        newlineHandlers = new TextMateNewlineHandler[]{newlineHandler};

    }

    public void updateLanguage(String scopeName) {
        var grammar = languageRegistry.findGrammar(scopeName);

        var languageConfiguration = languageRegistry.findLanguageConfiguration(grammar.getScopeName());



        createAnalyzerAndNewlineHandler(grammar, languageConfiguration);

    }

    public void updateLanguage(LanguageDefinition languageDefinition) {
        var grammar = languageRegistry.loadLanguage(languageDefinition);

        var languageConfiguration = languageRegistry.findLanguageConfiguration(grammar.getScopeName());

        createAnalyzerAndNewlineHandler(grammar, languageConfiguration);

    }

    @NonNull
    @Override
    public AnalyzeManager getAnalyzeManager() {
        return Objects.requireNonNullElse(textMateAnalyzer, EmptyAnalyzeManager.INSTANCE);
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


    public TextMateNewlineHandler getNewlineHandler() {
        return newlineHandler;
    }

    @Override
    public SymbolPairMatch getSymbolPairs() {
        //TODO: AutoClosePair 
        return new SymbolPairMatch.DefaultSymbolPairs();
    }

    @Override
    public NewlineHandler[] getNewlineHandlers() {
        //TODO: OnEnterSupport
        return newlineHandlers;
    }

    public boolean isAutoCompleteEnabled() {
        return autoCompleteEnabled;
    }

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
        autoComplete.requireAutoComplete(prefix, publisher, idt);
    }

    public IdentifierAutoComplete getAutoCompleter() {
        return autoComplete;
    }

    public void setCompleterKeywords(String[] keywords) {
        autoComplete.setKeywords(keywords, false);
    }
}
