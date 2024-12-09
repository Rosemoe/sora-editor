/*******************************************************************************
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
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.registry.dsl

import io.github.dingyi222666.monarch.language.Language
import io.github.dingyi222666.monarch.loader.json.loadMonarchJson
import io.github.dingyi222666.monarch.types.IMonarchLanguage
import io.github.rosemoe.sora.langs.monarch.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.monarch.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.monarch.theme.toLanguageConfiguration


class MonarchLanguageDefinitionListBuilder :
    LanguageDefinitionListBuilder<Language, MonarchLanguageDefinitionBuilder>() {
    override fun language(name: String, block: MonarchLanguageDefinitionBuilder.() -> Unit) {
        allBuilder.add(MonarchLanguageDefinitionBuilder(name).also(block))
    }

    override fun build(): List<GrammarDefinition<Language>> = allBuilder.map { languageDefinition ->
        val monarchLanguage = languageDefinition.monarchLanguage ?: languageDefinition.runCatching {
            FileProviderRegistry.resolve(languageDefinition.grammar)?.use {
                it.bufferedReader().readText()
            }
        }.getOrNull()?.runCatching {
            loadMonarchJson(this) ?: throw Exception("Failed to load monarch source")
        }?.getOrNull() ?: throw Exception("Failed to load monarch source")

        val language = Language(
            monarchLanguage = monarchLanguage,
            languageName = languageDefinition.name,
            languageId = languageDefinition.scopeName
                ?: "source.${monarchLanguage.tokenPostfix ?: languageDefinition.name}",
            fileExtensions = emptyList(),
            embeddedLanguages = languageDefinition.embeddedLanguages,
        )

        val languageConfiguration = languageDefinition.languageConfiguration?.let { configuration ->
            kotlin.runCatching {
                configuration.toLanguageConfiguration()
            }
                .getOrNull() ?: runCatching {
                FileProviderRegistry.resolve(languageDefinition.languageConfiguration ?: "")?.use {
                    it.bufferedReader().readText()
                }?.toLanguageConfiguration()
            }
                .getOrNull()
        }


        GrammarDefinition(
            name = languageDefinition.name,
            grammar = language,
            embeddedLanguages = languageDefinition.embeddedLanguages ?: emptyMap(),
            languageConfiguration = languageConfiguration,
            scopeName = languageDefinition.scopeName ?: language.languageId
        )


    }
}

class MonarchLanguageDefinitionBuilder(name: String) : LanguageDefinitionBuilder(name) {
    var monarchLanguage: IMonarchLanguage? = null
}

fun monarchLanguages(block: MonarchLanguageDefinitionListBuilder.() -> Unit) =
    MonarchLanguageDefinitionListBuilder().apply(block)