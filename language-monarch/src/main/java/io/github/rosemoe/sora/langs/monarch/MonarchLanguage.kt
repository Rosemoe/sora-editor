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

package io.github.rosemoe.sora.langs.monarch

import android.os.Bundle
import io.github.dingyi222666.monarch.language.Language
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration
import io.github.rosemoe.sora.langs.monarch.registry.MonarchGrammarRegistry
import io.github.rosemoe.sora.langs.monarch.registry.model.GrammarDefinition
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.SymbolPairMatch

class MonarchLanguage(
    private var grammar: Language,
    internal var languageConfiguration: LanguageConfiguration?,
    private var grammarRegistry: MonarchGrammarRegistry,
    private var autoCompleteEnabled: Boolean
) : EmptyLanguage() {

    val autoCompleter by lazy(LazyThreadSafetyMode.NONE) {
        IdentifierAutoComplete()
    }

    private var monarchAnalyzer: MonarchAnalyzer? = null

    private val symbolPairMatch by lazy(LazyThreadSafetyMode.NONE) {
        MonarchSymbolPairMatch(this)
    }

    internal val createIdentifiers
        get() = autoCompleteEnabled

    var tabSize = 4

    var useTab = false

    internal var localNewlineHandlers: Array<NewlineHandler> = emptyArray()

    init {
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return symbolPairMatch
    }

    override fun getAnalyzeManager(): AnalyzeManager {
        return monarchAnalyzer ?: EmptyAnalyzeManager.INSTANCE
    }

    override fun useTab() = useTab

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val monarchAnalyzer = monarchAnalyzer
        if (!autoCompleteEnabled || monarchAnalyzer == null) {
            return
        }
        val prefix = CompletionHelper.computePrefix(
            content, position
        ) { key -> MyCharacter.isJavaIdentifierPart(key) }
        val idt = monarchAnalyzer.syncIdentifiers
        autoCompleter.requireAutoComplete(content, position, prefix, publisher, idt)
    }

    override fun getNewlineHandlers(): Array<NewlineHandler> = localNewlineHandlers

    fun setCompleterKeywords(keywords: Array<String>) {
        autoCompleter.setKeywords(keywords, false)
    }

    private fun createAnalyzerAndNewlineHandler(
        grammar: Language,
        languageConfiguration: LanguageConfiguration?
    ) {
        val old = monarchAnalyzer
        if (old != null) {
            old.receiver = null
            old.destroy()
        }
        try {
            monarchAnalyzer = MonarchAnalyzer(
                this,
                grammarRegistry.languageRegistry.getTokenizer(grammar.languageId)
                    ?: throw Exception("No tokenizer found for language ${grammar.languageId}"),
                languageConfiguration
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.languageConfiguration = languageConfiguration
        localNewlineHandlers = arrayOf(MonarchNewlineHandler(this))
        if (languageConfiguration != null) {
            // because the editor will only get the symbol pair matcher once
            // (caching object to stop repeated new object created),
            // the symbol pair needs to be updated inside the symbol pair matcher.
            symbolPairMatch.updatePair()
        }
    }

    fun updateLanguage(
        languageScopeName: String,
    ) {
        val grammar = grammarRegistry.findGrammar(languageScopeName)
            ?: throw IllegalArgumentException(
                String.format(
                    "Language with %s scope name not found",
                    grammarRegistry
                )
            )

        val languageConfiguration =
            grammarRegistry.findLanguageConfiguration(
                languageScopeName
            )

        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }

    companion object {

        @JvmStatic
        fun create(languageScopeName: String, autoCompleteEnabled: Boolean): MonarchLanguage {
            return create(
                languageScopeName,
                MonarchGrammarRegistry.INSTANCE,
                autoCompleteEnabled
            )
        }

        @JvmStatic
        fun create(
            languageScopeName: String,
            grammarRegistry: MonarchGrammarRegistry,
            autoCompleteEnabled: Boolean
        ): MonarchLanguage {
            val grammar = grammarRegistry.findGrammar(languageScopeName)
                ?: throw IllegalArgumentException(
                    String.format(
                        "Language with %s scope name not found",
                        languageScopeName
                    )
                )

            val languageConfiguration =
                grammarRegistry.findLanguageConfiguration(
                    languageScopeName
                )

            return MonarchLanguage(
                grammar,
                languageConfiguration,
                grammarRegistry,
                autoCompleteEnabled
            )
        }


        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition<Language>,
            autoCompleteEnabled: Boolean
        ): MonarchLanguage {
            return create(
                grammarDefinition,
                MonarchGrammarRegistry.INSTANCE,
                autoCompleteEnabled
            )
        }

        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition<Language>,
            grammarRegistry: MonarchGrammarRegistry,
            autoCompleteEnabled: Boolean
        ): MonarchLanguage {
            val grammar = grammarRegistry.loadGrammar(grammarDefinition)

            val languageConfiguration =
                grammarRegistry.findLanguageConfiguration(
                    grammarDefinition.scopeName
                )

            return MonarchLanguage(
                grammar,
                languageConfiguration,
                grammarRegistry,
                autoCompleteEnabled
            )
        }

        init {
            //GlobalRegexLib.defaultRegexLib = Re2JRegexLib()
        }
    }
}