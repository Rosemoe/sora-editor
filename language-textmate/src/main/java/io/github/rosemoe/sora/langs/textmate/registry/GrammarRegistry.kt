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
package io.github.rosemoe.sora.langs.textmate.registry

import android.util.Pair
import io.github.rosemoe.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.reader.LanguageDefinitionReader
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.registry.Registry
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.io.InputStreamReader

class GrammarRegistry {
    private var registry: Registry? = Registry()

    private var parent: GrammarRegistry? = null

    private val languageConfigurationMap = mutableMapOf</* scopeName */ String, LanguageConfiguration>()

    private val scopeName2GrammarId = mutableMapOf<String , Int>()

    private val grammarFileName2ScopeName = mutableMapOf<String, String>()

    private val scopeName2GrammarDefinition = mutableMapOf<String, GrammarDefinition>()

    private constructor()

    constructor(parent: GrammarRegistry?) {
        this.parent = parent
    }

    private fun initThemeListener() {
        val themeRegistry = ThemeRegistry.getInstance()

        val themeChangeListener = ThemeRegistry.ThemeChangeListener { newTheme: ThemeModel? ->
            try {
                setTheme(newTheme!!)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        themeRegistry.addListener(themeChangeListener)
    }


    @JvmOverloads
    fun findGrammar(scopeName: String?, findInParent: Boolean = true): IGrammar? {
        val grammar = registry!!.grammarForScopeName(scopeName)
        if (grammar != null) {
            return grammar
        }
        if (!findInParent) {
            return null
        }
        return parent?.findGrammar(scopeName, true)
    }

    /**
     * Adapted to use streams to read and load language configuration files by yourself
     * [io.github.rosemoe.sora.langs.textmate.TextMateLanguage.create].
     *
     * @param languageConfiguration loaded language configuration
     * @param grammar               Binding to grammar
     */
    @Deprecated("The grammar file and language configuration file should in most cases be on local file, " +
            "use [GrammarDefinition#getLanguageConfiguration()] and [FileResolver] to read the language configuration file")
    @Synchronized
    fun languageConfigurationToGrammar(
        languageConfiguration: LanguageConfiguration,
        grammar: IGrammar
    ) {
        languageConfigurationMap.put(grammar.getScopeName(), languageConfiguration)
    }

    @JvmOverloads
    fun findLanguageConfiguration(
        scopeName: String?,
        findInParent: Boolean = true
    ): LanguageConfiguration? {
        return languageConfigurationMap[scopeName] ?: if (findInParent) {
            parent?.findLanguageConfiguration(scopeName, true)
        } else {
            null
        }
    }

    fun loadLanguageAndLanguageConfiguration(grammarDefinition: GrammarDefinition): Pair<IGrammar, LanguageConfiguration> {
        val grammar = loadGrammar(grammarDefinition)
        val languageConfiguration = findLanguageConfiguration(grammar.getScopeName(), false)
        return Pair.create<IGrammar, LanguageConfiguration>(grammar, languageConfiguration)
    }

    fun loadGrammars(builder: LanguageDefinitionListBuilder): List<IGrammar> {
        return loadGrammars(builder.build())
    }

    fun loadGrammars(list: List<GrammarDefinition>): List<IGrammar> {
        prepareLoadGrammars(list)
        return list.map(this::loadGrammar)
    }

    fun loadGrammars(jsonPath: String): List<IGrammar> {
        return loadGrammars(LanguageDefinitionReader.read(jsonPath))
    }

    @Synchronized
    fun loadGrammar(grammarDefinition: GrammarDefinition): IGrammar {
        val languageName = grammarDefinition.name

        if (grammarFileName2ScopeName.containsKey(languageName)) {
            //loaded
            val grammarForScopeName = registry!!.grammarForScopeName(grammarDefinition.scopeName)
            if (grammarForScopeName != null) return grammarForScopeName
        }


        val grammar = doLoadGrammar(grammarDefinition)

        val defScopeName = grammarDefinition.scopeName
        if (defScopeName != null) {
            grammarFileName2ScopeName.put(languageName, defScopeName)
            scopeName2GrammarDefinition.put(grammar.getScopeName(), grammarDefinition)
        }
        return grammar
    }


    @Synchronized
    private fun doLoadGrammar(grammarDefinition: GrammarDefinition): IGrammar {
        val languageConfigurationPath = grammarDefinition.languageConfiguration

        if (languageConfigurationPath != null) {
            val languageConfigurationStream = FileProviderRegistry.getInstance()
                .tryGetInputStream(languageConfigurationPath)

            if (languageConfigurationStream != null) {
                val languageConfiguration = LanguageConfiguration.load(
                    InputStreamReader(languageConfigurationStream)
                )

                val defScopeName = grammarDefinition.scopeName
                if (defScopeName != null && languageConfiguration != null) {
                    languageConfigurationMap.put(defScopeName, languageConfiguration)
                }
            }
        }

        val grammar = if (!grammarDefinition.embeddedLanguages.isEmpty()) {
            registry!!.addGrammar(grammarDefinition.grammar)
        } else {
            registry!!.addGrammar(
                grammarDefinition.grammar,
                null,
                getOrPullGrammarId(grammarDefinition.scopeName),
                findGrammarIds(grammarDefinition.embeddedLanguages)
            )
        }

        check(!(grammarDefinition.scopeName != null && grammar.getScopeName() != grammarDefinition.scopeName)) {
            "The scope name loaded by the grammar file does not match the declared scope name, " +
                    "it should be ${grammar.scopeName} instead of ${grammarDefinition.scopeName}"
        }

        return grammar
    }


    private fun prepareLoadGrammars(grammarDefinitions: List<GrammarDefinition>) {
        for (grammar in grammarDefinitions) {
            getOrPullGrammarId(grammar.scopeName)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun setTheme(themeModel: ThemeModel) {
        if (!themeModel.isLoaded) {
            themeModel.load(registry!!.colorMap)
        }
        registry!!.setTheme(themeModel.theme)
    }


    @Synchronized
    private fun getOrPullGrammarId(scopeName: String?): Int {
        val id = scopeName2GrammarId[scopeName]
            ?: (scopeName2GrammarId.size + 2)

        if (scopeName != null) {
            scopeName2GrammarId.put(scopeName, id)
        }
        return id
    }


    @Synchronized
    private fun findGrammarIds(scopeName2LanguageName: Map<String, String>): Map<String, Int> {
        return scopeName2LanguageName.mapValues { (_, value) ->
            getOrPullGrammarId(value)
        }
    }

    private fun getGrammarScopeName(name: String?): String? {
        return if (scopeName2GrammarDefinition.containsKey(name)) {
            name
        } else {
            grammarFileName2ScopeName[name] ?: name
        }
    }

    @Synchronized
    fun dispose(closeParent: Boolean) {
        if (registry == null) {
            return
        }

        registry = null
        grammarFileName2ScopeName.clear()
        languageConfigurationMap.clear()
        scopeName2GrammarId.clear()
        scopeName2GrammarDefinition.clear()

        // if (parent == null) {
        // ? need?
        //FileProviderRegistry.getInstance().dispose();
        // }
        if (closeParent) {
            parent?.dispose(true)
        }
    }

    fun dispose() {
        dispose(false)
    }


    companion object {
        private var instance: GrammarRegistry? = null

        @Synchronized
        @JvmStatic
        fun getInstance(): GrammarRegistry {
            if (instance == null) {
                instance = GrammarRegistry().apply {
                    initThemeListener()
                }
            }
            return instance!!
        }
    }
}
