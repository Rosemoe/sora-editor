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
package io.github.rosemoe.sora.langs.textmate.registry.model;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.languageconfiguration.model.LanguageConfiguration;

import java.util.function.Supplier;

import io.github.rosemoe.sora.langs.textmate.StringUtil;

public class DefaultLanguageDefinition implements LanguageDefinition {

    private String name;

    private String languageConfigurationPath = null;

    private IGrammarSource grammarSource;

    private String scopeName = null;


    private DefaultLanguageDefinition(String name, String scopeName,
                                      IGrammarSource grammarSource, String languageConfigurationPath) {

        this.name = name;
        this.scopeName = scopeName;
        this.grammarSource = grammarSource;
        this.languageConfigurationPath = languageConfigurationPath;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable String getLanguageConfiguration() {
        return null;
    }

    @Override
    public @Nullable String getScopeName() {
        return scopeName;
    }

    @Override
    public IGrammarSource getGrammar() {
        return grammarSource;
    }

    public static LanguageDefinition withGrammarSource(IGrammarSource grammarSource) {
        var languageNameByPath = StringUtil.getFileNameWithoutExtension(grammarSource.getFilePath());
        return withGrammarSource(grammarSource, languageNameByPath, "source." + languageNameByPath);
    }

    public static LanguageDefinition withLanguageConfiguration(IGrammarSource grammarSource, String languageConfigurationPath) {
        var languageNameByPath = StringUtil.getFileNameWithoutExtension(grammarSource.getFilePath());
        return withLanguageConfiguration(grammarSource, languageConfigurationPath, languageNameByPath, "source." + languageNameByPath);
    }


    public static LanguageDefinition withLanguageConfiguration(IGrammarSource grammarSource, String languageConfigurationPath, String languageName, String scopeName) {
        return new DefaultLanguageDefinition(languageName, scopeName, grammarSource, languageConfigurationPath);
    }

    public static LanguageDefinition withGrammarSource(IGrammarSource grammarSource, String languageName, String scopeName) {
        return new DefaultLanguageDefinition(languageName, scopeName, grammarSource, null);
    }


}
