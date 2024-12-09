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

import io.github.rosemoe.sora.langs.monarch.registry.model.GrammarDefinition


abstract class LanguageDefinitionListBuilder<T, R : LanguageDefinitionBuilder> {
    protected val allBuilder = mutableListOf<R>()

    abstract fun language(name: String, block: R.() -> Unit)

    abstract fun build(): List<GrammarDefinition<T>>
}

open class LanguageDefinitionBuilder(var name: String) {
    lateinit var grammar: String
    var scopeName: String? = null
    var languageConfiguration: String? = null

    var embeddedLanguages: MutableMap<String, String>? = null

    fun defaultScopeName(prefix: String = "source") {
        scopeName = "$prefix.$name"
    }

    fun embeddedLanguages(block: LanguageEmbeddedLanguagesDefinitionBuilder.() -> Unit) {
        embeddedLanguages = embeddedLanguages ?: mutableMapOf<String, String>()
        LanguageEmbeddedLanguagesDefinitionBuilder(checkNotNull(embeddedLanguages)).also(block)
    }

    fun embeddedLanguage(scopeName: String, languageName: String) {
        embeddedLanguages = embeddedLanguages ?: mutableMapOf<String, String>()
        embeddedLanguages?.put(scopeName, languageName)
    }
}

class LanguageEmbeddedLanguagesDefinitionBuilder(private val map: MutableMap<String, String>) {


    infix fun String.to(languageName: String) {
        map[this] = languageName
    }

}

