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
package io.github.rosemoe.sora.langs.textmate

import android.os.Bundle
import androidx.annotation.WorkerThread
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.IdentifierAutoComplete
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition.Companion.withGrammarSource
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.textmate.utils.StringUtils.getFileNameWithoutExtension
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import org.eclipse.tm4e.core.grammar.IGrammar
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration
import java.io.Reader

class TextMateLanguage private constructor(
    grammar: IGrammar,
    var languageConfiguration: LanguageConfiguration?,
    var grammarRegistry: GrammarRegistry,
    var themeRegistry: ThemeRegistry,
    val createIdentifiers: Boolean
) : EmptyLanguage() {
    /**
     * Set tab size. The tab size is used to compute code blocks.
     */
    var tabSize: Int = 4

    private var useTab = false

    val autoCompleter = IdentifierAutoComplete()
    var isAutoCompleteEnabled: Boolean = true

    var textMateAnalyzer: TextMateAnalyzer? = null

    lateinit var newlineHandlers: Array<TextMateNewlineHandler>

    // this.grammar = grammar;
    val symbolPairMatch = TextMateSymbolPairMatch(this)

    lateinit var newlineHandler: TextMateNewlineHandler

    init {
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }


    /**
     * When you update the [TextMateColorScheme] for editor, you need to synchronize the updates here
     *
     * @param theme IThemeSource creates from file
     */
    @WorkerThread
    @Deprecated("Use {@link ThemeRegistry#setTheme(String)}")
    @Throws(Exception::class)
    fun updateTheme(theme: IThemeSource) {
        //if (textMateAnalyzer != null) {
        //  textMateAnalyzer.updateTheme(theme);
        //}
        themeRegistry.loadTheme(theme)
    }


    private fun createAnalyzerAndNewlineHandler(
        grammar: IGrammar,
        languageConfiguration: LanguageConfiguration?
    ) {
        val old = textMateAnalyzer
        if (old != null) {
            old.receiver = null
            old.destroy()
        }
        try {
            textMateAnalyzer = TextMateAnalyzer(
                this,
                grammar,
                languageConfiguration,  /*grammarRegistry,*/
                themeRegistry
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.languageConfiguration = languageConfiguration
        val newHandler = TextMateNewlineHandler(this)
        newlineHandler = newHandler
        newlineHandlers = arrayOf(newHandler)
        if (languageConfiguration != null) {
            // because the editor will only get the symbol pair matcher once
            // (caching object to stop repeated new object created),
            // the symbol pair needs to be updated inside the symbol pair matcher.
            symbolPairMatch.updatePair()
        }
    }

    fun updateLanguage(scopeName: String) {
        val grammar = grammarRegistry.findGrammar(scopeName)
        val languageConfiguration =
            grammarRegistry.findLanguageConfiguration(grammar!!.getScopeName())
        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }

    fun updateLanguage(grammarDefinition: GrammarDefinition) {
        val grammar = grammarRegistry.loadGrammar(grammarDefinition)

        val languageConfiguration =
            grammarRegistry.findLanguageConfiguration(grammar.getScopeName())

        createAnalyzerAndNewlineHandler(grammar, languageConfiguration)
    }

    override fun getAnalyzeManager(): AnalyzeManager {
        return textMateAnalyzer ?: EmptyAnalyzeManager.INSTANCE
    }

    override fun destroy() {
        super.destroy()
    }


    override fun useTab(): Boolean {
        return useTab
    }

    fun useTab(useTab: Boolean) {
        this.useTab = useTab
    }

    override fun getSymbolPairs(): TextMateSymbolPairMatch {
        return symbolPairMatch
    }

    override fun getNewlineHandlers(): Array<NewlineHandler>? {
        return newlineHandlers.map { it as NewlineHandler }.toTypedArray()
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        if (!this.isAutoCompleteEnabled) {
            return
        }
        val prefix = CompletionHelper.computePrefix(
            content,
            position
        ) { key: Char -> MyCharacter.isJavaIdentifierPart(key) }
        val idt = textMateAnalyzer?.syncIdentifiers
        autoCompleter.requireAutoComplete(content, position, prefix, publisher, idt)
    }

    fun setCompleterKeywords(keywords: Array<String?>?) {
        autoCompleter.setKeywords(keywords, false)
    }

    companion object {
        @Deprecated("")
        fun prepareLoad(
            grammarSource: IGrammarSource,
            languageConfiguration: Reader?,
            themeSource: IThemeSource
        ): IGrammar {
            val definition = withGrammarSource(
                grammarSource,
                getFileNameWithoutExtension(grammarSource.getFilePath()),
                null
            )
            val languageRegistry = GrammarRegistry.getInstance()
            val grammar = languageRegistry.loadGrammar(definition)
            if (languageConfiguration != null) {
                languageRegistry.languageConfigurationToGrammar(
                    LanguageConfiguration.load(
                        languageConfiguration
                    )!!, grammar
                )
            }
            val themeRegistry = ThemeRegistry.getInstance()
            try {
                themeRegistry.loadTheme(themeSource)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return grammar
        }

        @Deprecated("")
        fun create(
            grammarSource: IGrammarSource,
            languageConfiguration: Reader?,
            themeSource: IThemeSource
        ): TextMateLanguage {
            val grammar: IGrammar = prepareLoad(grammarSource, languageConfiguration, themeSource)
            return create(grammar.getScopeName(), true)
        }

        @Deprecated("")
        fun create(grammarSource: IGrammarSource, themeSource: IThemeSource): TextMateLanguage {
            val grammar: IGrammar = prepareLoad(grammarSource, null, themeSource)
            return create(grammar.getScopeName(), true)
        }

        @Deprecated("")
        fun createNoCompletion(
            grammarSource: IGrammarSource,
            languageConfiguration: Reader?,
            themeSource: IThemeSource
        ): TextMateLanguage {
            val grammar: IGrammar = prepareLoad(grammarSource, languageConfiguration, themeSource)
            return create(grammar.getScopeName(), false)
        }

        @Deprecated("")
        fun createNoCompletion(
            grammarSource: IGrammarSource,
            themeSource: IThemeSource
        ): TextMateLanguage {
            val grammar: IGrammar = prepareLoad(grammarSource, null, themeSource)
            return create(grammar.getScopeName(), false)
        }

        @JvmStatic
        fun create(languageScopeName: String?, autoCompleteEnabled: Boolean): TextMateLanguage {
            return create(languageScopeName, GrammarRegistry.getInstance(), autoCompleteEnabled)
        }

        @JvmStatic
        fun create(
            languageScopeName: String?,
            grammarRegistry: GrammarRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            return create(
                languageScopeName,
                grammarRegistry,
                ThemeRegistry.getInstance(),
                autoCompleteEnabled
            )
        }

        @JvmStatic
        fun create(
            languageScopeName: String?,
            grammarRegistry: GrammarRegistry,
            themeRegistry: ThemeRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            val grammar = grammarRegistry.findGrammar(languageScopeName)

            requireNotNull(grammar) { "Language with $grammarRegistry scope name not found" }

            val languageConfiguration =
                grammarRegistry.findLanguageConfiguration(grammar.getScopeName())

            return TextMateLanguage(
                grammar,
                languageConfiguration,
                grammarRegistry,
                themeRegistry,
                autoCompleteEnabled
            )
        }

        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            return create(grammarDefinition, GrammarRegistry.getInstance(), autoCompleteEnabled)
        }

        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition,
            grammarRegistry: GrammarRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            return create(
                grammarDefinition,
                grammarRegistry,
                ThemeRegistry.getInstance(),
                autoCompleteEnabled
            )
        }

        @JvmStatic
        fun create(
            grammarDefinition: GrammarDefinition,
            grammarRegistry: GrammarRegistry,
            themeRegistry: ThemeRegistry,
            autoCompleteEnabled: Boolean
        ): TextMateLanguage {
            val grammar = grammarRegistry.loadGrammar(grammarDefinition)

            val languageConfiguration =
                grammarRegistry.findLanguageConfiguration(grammar.getScopeName())

            return TextMateLanguage(
                grammar,
                languageConfiguration,
                grammarRegistry,
                themeRegistry,
                autoCompleteEnabled
            )
        }
    }
}
