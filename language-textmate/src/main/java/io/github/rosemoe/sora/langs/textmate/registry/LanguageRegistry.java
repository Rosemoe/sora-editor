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
package io.github.rosemoe.sora.langs.textmate.registry;

import android.util.Pair;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.core.registry.IThemeSource;
import org.eclipse.tm4e.core.registry.Registry;
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
//import io.github.rosemoe.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder;
import io.github.rosemoe.sora.langs.textmate.registry.model.LanguageDefinition;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.FileProvider;

public class LanguageRegistry {

    private static LanguageRegistry instance;

    private Registry registry = new Registry();

    private LanguageRegistry parent;

    private final Map</* scopeName */String, LanguageConfiguration> languageConfigurationMap = new LinkedHashMap<>();

    private final Map</* name */String, String /* scopeName */> grammarFileName2ScopeName = new LinkedHashMap<>();

    public synchronized static LanguageRegistry getInstance() {
        if (instance == null) {
            instance = new LanguageRegistry();
            instance.initThemeListener();
        }
        return instance;
    }

    private LanguageRegistry() {
    }

    public LanguageRegistry(LanguageRegistry parent) {
        this.parent = parent;
    }

    private void initThemeListener() {
        var themeRegistry = ThemeRegistry.getInstance();

        ThemeRegistry.ThemeChangeListener themeChangeListener = newTheme -> {
            try {
                setTheme(newTheme);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        if (!themeRegistry.hasListener(themeChangeListener)) {
            themeRegistry.addListener(themeChangeListener);
        }
    }


    @Nullable
    public IGrammar findGrammar(String scopeName) {
        return findGrammar(scopeName, true);
    }

    public IGrammar findGrammar(String scopeName, boolean findInParent) {
        var grammar = registry.grammarForScopeName(scopeName);

        if (grammar != null) {
            return grammar;
        }

        if (!findInParent) {
            return null;
        }

        if (parent == null) {
            return null;
        }

        return parent.findGrammar(scopeName, true);
    }


    /**
     * Adapted to use streams to read and load language configuration files by yourself {@link TextMateLanguage#create(IGrammarSource, Reader, IThemeSource)}.
     *
     * @param languageConfiguration loaded language configuration
     * @param grammar               Binding to grammar
     * @deprecated The grammar file and language configuration file should in most cases be on local file, use {@link LanguageDefinition#getLanguageConfiguration()} and {@link FileProvider} to read the language configuration file
     */
    @Deprecated
    public void languageConfigurationToGrammar(LanguageConfiguration languageConfiguration, IGrammar grammar) {
        languageConfigurationMap.put(grammar.getScopeName(), languageConfiguration);
    }

    @Nullable
    public LanguageConfiguration findLanguageConfiguration(String scopeName) {
        return findLanguageConfiguration(scopeName, true);
    }

    @Nullable
    public LanguageConfiguration findLanguageConfiguration(String scopeName, boolean findInParent) {
        var languageConfiguration = languageConfigurationMap.get(scopeName);

        if (languageConfiguration != null) {
            return languageConfiguration;
        }

        if (!findInParent) {
            return null;
        }

        if (parent == null) {
            return null;
        }

        return parent.findLanguageConfiguration(scopeName, true);
    }


    public Pair<IGrammar, LanguageConfiguration> loadLanguageAndLanguageConfiguration(LanguageDefinition languageDefinition) {
        var grammar = loadLanguage(languageDefinition);

        var languageConfiguration = findLanguageConfiguration(grammar.getScopeName(), false);

        return Pair.create(grammar, languageConfiguration);
    }

    //public List<IGrammar> loadLanguages(LanguageDefinitionListBuilder builder) {
    //  return builder.build().stream().map(this::loadLanguage).collect(Collectors.toList());
    //}

    public List<IGrammar> loadLanguages(List<LanguageDefinition> list) {
        return list.stream().map(this::loadLanguage).collect(Collectors.toList());
    }

    public IGrammar loadLanguage(LanguageDefinition languageDefinition) {
        var languageName = languageDefinition.getName();

        if (grammarFileName2ScopeName.containsKey(languageName) && languageDefinition.getScopeName() != null) {
            //loaded
            return registry.grammarForScopeName(languageDefinition.getScopeName());
        }

        var grammar = doLoadLanguage(languageDefinition);

        if (languageDefinition.getScopeName() != null) {
            grammarFileName2ScopeName.put(languageName, languageDefinition.getScopeName());
        }

        return grammar;

    }


    private IGrammar doLoadLanguage(LanguageDefinition languageDefinition) {

        var languageConfigurationPath = languageDefinition.getLanguageConfiguration();

        if (languageConfigurationPath != null) {

            var languageConfigurationStream = FileProviderRegistry.getInstance()
                    .tryGetInputStream(languageConfigurationPath);

            if (languageConfigurationStream != null) {


                var languageConfiguration = LanguageConfiguration.load(
                        new InputStreamReader(languageConfigurationStream)
                );

                languageConfigurationMap.put(languageDefinition.getScopeName(), languageConfiguration);

            }
        }

        var grammar = registry.addGrammar(languageDefinition.getGrammar());

        if (languageDefinition.getScopeName() != null && !grammar.getScopeName().equals(languageDefinition.getScopeName())) {
            throw new IllegalStateException(
                    String.format("The scope name loaded by the grammar file does not match the declared scope name, it should be %s instead of %s",
                            grammar.getScopeName(), languageDefinition.getScopeName()));
        }

        return grammar;

    }

    public void setTheme(ThemeModel themeModel) throws Exception {
        if (!themeModel.isLoaded()) {
            themeModel.load();
        }
        registry.setTheme(themeModel.getTheme());
    }


    public void dispose(boolean closeParent) {
        registry = null;
        grammarFileName2ScopeName.clear();
        languageConfigurationMap.clear();

        // if (parent == null) {
        // ? need?
        //FileProviderRegistry.getInstance().dispose();
        // }

        if (parent != null && closeParent) {
            parent.dispose(true);
        }


    }

    public void dispose() {
        dispose(false);
    }


}
