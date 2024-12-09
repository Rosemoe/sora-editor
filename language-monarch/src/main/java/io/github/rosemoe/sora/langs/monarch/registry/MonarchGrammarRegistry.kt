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

package io.github.rosemoe.sora.langs.monarch.registry

import com.squareup.moshi.Moshi
import io.github.dingyi222666.monarch.language.Language
import io.github.dingyi222666.monarch.language.LanguageRegistry
import io.github.dingyi222666.monarch.loader.json.addLast
import io.github.dingyi222666.monarch.types.IThemeService
import io.github.dingyi222666.monarch.types.ITokenTheme
import io.github.rosemoe.sora.langs.monarch.registry.grammardefinition.MonarchGrammarDefinitionReader
import io.github.rosemoe.sora.langs.monarch.registry.grammardefinition.ParsedGrammarDefinitionList
import io.github.rosemoe.sora.langs.monarch.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.monarch.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.monarch.theme.adapter
import java.io.File

class MonarchGrammarRegistry(
    internal val languageRegistry: LanguageRegistry = LanguageRegistry(),
    parent: GrammarRegistry<Language>? = null
) : GrammarRegistry<Language>(parent), IThemeService {

    val moshi: Moshi = Moshi.Builder()
        .apply {
            addLast<ParsedGrammarDefinitionList<Language>>(MonarchGrammarDefinitionReader())
        }
        .build()


    private var currentTheme = ThemeModel.EMPTY

    override fun doLoadGrammar(grammarDefinition: GrammarDefinition<Language>): Language {
        return grammarDefinition.grammar.also {
            languageRegistry.registerLanguage(it, true, this)
        }
    }

    override fun doSetGrammarRegistryTheme(themeModel: ThemeModel) {
        currentTheme = themeModel
    }

    override fun doLoadGrammarsFromJsonPath(jsonPath: String): List<GrammarDefinition<Language>> {
        val adapter = moshi.adapter<ParsedGrammarDefinitionList<Language>>()
        return (adapter.fromJson(File(jsonPath).readText())
            ?: ParsedGrammarDefinitionList.empty()).grammarDefinition
    }

    override fun doSearchGrammar(scopeName: String): Language? {
        // fast search?
        return languageRegistry.getRegisteredLanguages().find {
            "source${it.monarchLanguage.tokenPostfix}" == scopeName ||
                    "source.${it.monarchLanguage.tokenPostfix}" == scopeName ||
                    "source.${it.languageId}" == scopeName ||
                    it.languageId == scopeName

        }
    }


    override fun currentColorTheme(): ITokenTheme {
        return currentTheme.value
    }

    companion object {
        val INSTANCE by lazy {
            MonarchGrammarRegistry()
        }
    }
}

