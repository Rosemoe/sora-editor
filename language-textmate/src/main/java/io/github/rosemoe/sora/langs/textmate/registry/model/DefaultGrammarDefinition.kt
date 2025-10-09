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
package io.github.rosemoe.sora.langs.textmate.registry.model

import io.github.rosemoe.sora.langs.textmate.utils.StringUtils
import org.eclipse.tm4e.core.registry.IGrammarSource

class DefaultGrammarDefinition private constructor(
    override val name: String,
    override val scopeName: String?,
    override val grammar: IGrammarSource,
    override val languageConfiguration: String?
) : GrammarDefinition {
    override var embeddedLanguages: Map<String, String> = emptyMap()
        private set


    private constructor(
        name: String,
        scopeName: String?,
        grammarSource: IGrammarSource,
        languageConfigurationPath: String?,
        embeddedLanguages: Map<String, String>
    ) : this(name, scopeName, grammarSource, languageConfigurationPath) {
        this.embeddedLanguages = embeddedLanguages
    }


    fun withEmbeddedLanguages(embeddedLanguages: Map<String, String>?): GrammarDefinition {
        if (embeddedLanguages == null) {
            return this
        }
        return DefaultGrammarDefinition(
            this.name, this.scopeName,
            this.grammar, this.languageConfiguration,
            embeddedLanguages
        )
    }

    companion object {
        @JvmStatic
        fun withGrammarSource(grammarSource: IGrammarSource): DefaultGrammarDefinition {
            val languageNameByPath =
                StringUtils.getFileNameWithoutExtension(grammarSource.getFilePath())
            return withGrammarSource(
                grammarSource,
                languageNameByPath,
                "source.$languageNameByPath"
            )
        }

        @JvmStatic
        fun withLanguageConfiguration(
            grammarSource: IGrammarSource,
            languageConfigurationPath: String?
        ): DefaultGrammarDefinition {
            val languageNameByPath =
                StringUtils.getFileNameWithoutExtension(grammarSource.getFilePath())
            return withLanguageConfiguration(
                grammarSource,
                languageConfigurationPath,
                languageNameByPath,
                "source.$languageNameByPath"
            )
        }


        @JvmStatic
        fun withLanguageConfiguration(
            grammarSource: IGrammarSource,
            languageConfigurationPath: String?,
            languageName: String,
            scopeName: String?
        ): DefaultGrammarDefinition {
            return DefaultGrammarDefinition(
                languageName,
                scopeName,
                grammarSource,
                languageConfigurationPath
            )
        }

        @JvmStatic
        fun withGrammarSource(
            grammarSource: IGrammarSource,
            languageName: String,
            scopeName: String?
        ): DefaultGrammarDefinition {
            return DefaultGrammarDefinition(languageName, scopeName, grammarSource, null)
        }
    }
}
