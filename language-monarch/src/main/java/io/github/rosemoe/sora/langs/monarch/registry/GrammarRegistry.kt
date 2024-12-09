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
package io.github.rosemoe.sora.langs.monarch.registry


import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration
import io.github.rosemoe.sora.langs.monarch.registry.dsl.LanguageDefinitionBuilder
import io.github.rosemoe.sora.langs.monarch.registry.dsl.LanguageDefinitionListBuilder
import io.github.rosemoe.sora.langs.monarch.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.monarch.registry.model.ThemeModel

abstract class GrammarRegistry<T> {
    private var parent: GrammarRegistry<T>? = null

    private val languageConfigurationMap =
        mutableMapOf<String, LanguageConfiguration>()

    private val scopeName2GrammarId = mutableMapOf<String, Int>()

    private val grammarFileName2ScopeName = mutableMapOf<String, String?>()

    private val scopeName2GrammarDefinition =
        mutableMapOf<String, GrammarDefinition<T>>()

    private val themeChangeListener: ThemeChangeListener =
        ThemeChangeListener { newTheme ->
            try {
                setTheme(newTheme)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

    init {
        initThemeListener()
    }

    constructor(parent: GrammarRegistry<T>?) {
        this.parent = parent
    }

    private fun initThemeListener() {
        if (!ThemeRegistry.hasListener(themeChangeListener)) {
            ThemeRegistry.addListener(themeChangeListener)
        }
    }


    fun findGrammar(scopeName: String): T? {
        return findGrammar(scopeName, true)
    }

    fun findGrammar(scopeName: String, findInParent: Boolean): T? {
        val grammar = doSearchGrammar(scopeName)

        if (grammar != null) {
            return grammar
        }

        if (!findInParent) {
            return null
        }


        return parent?.findGrammar(scopeName, true)
    }

    fun findLanguageConfiguration(scopeName: String): LanguageConfiguration? {
        return findLanguageConfiguration(scopeName, true)
    }


    fun findLanguageConfiguration(
        scopeName: String,
        findInParent: Boolean
    ): LanguageConfiguration? {
        val languageConfiguration: LanguageConfiguration? = languageConfigurationMap[scopeName]

        if (languageConfiguration != null) {
            return languageConfiguration
        }

        if (!findInParent) {
            return null
        }


        return parent?.findLanguageConfiguration(scopeName, true)
    }


    fun loadLanguageAndLanguageConfiguration(grammarDefinition: GrammarDefinition<T>): Pair<T, LanguageConfiguration?> {
        val grammar = loadGrammar(grammarDefinition)

        val languageConfiguration: LanguageConfiguration? =
            findLanguageConfiguration(grammarDefinition.scopeName, false)

        return grammar to languageConfiguration
    }

    fun <R : LanguageDefinitionBuilder> loadGrammars(builder: LanguageDefinitionListBuilder<T, R>): List<T> {
        return loadGrammars(builder.build())
    }


    fun loadGrammars(list: List<GrammarDefinition<T>>): List<T> {
        prepareLoadGrammars(list)
        return list.map { loadGrammar(it) }
    }

    open fun loadGrammars(jsonPath: String): List<T> {
        return loadGrammars(doLoadGrammarsFromJsonPath(jsonPath))
    }

    /**
     * Adapted to use streams to read and load language configuration files by yourself [TextMateLanguage.create].
     *
     * @param languageConfiguration loaded language configuration
     * @param grammar               Binding to grammar
     */
    fun languageConfigurationToGrammar(
        languageConfiguration: LanguageConfiguration,
        scopeName: String
    ) {
        languageConfigurationMap[scopeName] = languageConfiguration
    }


    @Synchronized
    fun loadGrammar(grammarDefinition: GrammarDefinition<T>): T {
        val languageName = grammarDefinition.scopeName

        if (grammarFileName2ScopeName.containsKey(languageName) && grammarDefinition.scopeName.isNotEmpty()) {
            //loaded
            return doSearchGrammar(grammarDefinition.scopeName)
                ?: throw RuntimeException("Grammar cannot be find by scope name: $languageName")
        }


        val grammar = doLoadGrammar(grammarDefinition)

        if (grammarDefinition.scopeName.isNotEmpty()) {
            grammarFileName2ScopeName[languageName] = grammarDefinition.scopeName
            scopeName2GrammarDefinition[grammarDefinition.scopeName] = grammarDefinition
            grammarDefinition.languageConfiguration?.also {
                languageConfigurationMap[grammarDefinition.scopeName] = it
            }
        }

        return grammar
    }


    private fun prepareLoadGrammars(grammarDefinitions: List<GrammarDefinition<T>>) {
        for (grammar in grammarDefinitions) {
            getOrPullGrammarId(grammar.scopeName)
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun setTheme(themeModel: ThemeModel) {
        if (!themeModel.isLoaded) {
            themeModel.load()
        }
        doSetGrammarRegistryTheme(themeModel)
    }


    @Synchronized
    private fun getOrPullGrammarId(scopeName: String): Int {
        var id = scopeName2GrammarId[scopeName]

        if (id == null) {
            id = scopeName2GrammarId.size + 2
        }

        scopeName2GrammarId[scopeName] = id

        return id
    }


    @Synchronized
    private fun findGrammarIds(scopeName2LanguageName: Map<String, String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for ((key, value) in scopeName2LanguageName) {
            // scopeName (entry#getKey)
            result[key] = getOrPullGrammarId(
                getGrammarScopeName(value)
            )
        }
        return result
    }

    private fun getGrammarScopeName(name: String): String {
        if (scopeName2GrammarDefinition.containsKey(name)) {
            return name
        }
        val grammarName = grammarFileName2ScopeName[name]
        return grammarName ?: name
    }

    @Synchronized
    fun dispose(closeParent: Boolean) {
        grammarFileName2ScopeName.clear()
        languageConfigurationMap.clear()
        scopeName2GrammarId.clear()
        scopeName2GrammarDefinition.clear()

        // if (parent == null) {
        // ? need?
        //FileProviderRegistry.getInstance().dispose();
        // }
        if (parent != null && closeParent) {
            parent?.dispose(true)
        }
    }

    fun dispose() {
        dispose(false)
    }


    protected abstract fun doLoadGrammar(grammarDefinition: GrammarDefinition<T>): T

    protected abstract fun doSetGrammarRegistryTheme(themeModel: ThemeModel)

    protected abstract fun doLoadGrammarsFromJsonPath(jsonPath: String): List<GrammarDefinition<T>>

    protected abstract fun doSearchGrammar(scopeName: String): T?
}

