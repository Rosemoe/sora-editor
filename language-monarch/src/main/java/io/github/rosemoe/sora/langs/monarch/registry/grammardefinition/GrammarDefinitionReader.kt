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

package io.github.rosemoe.sora.langs.monarch.registry.grammardefinition

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration
import io.github.rosemoe.sora.langs.monarch.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.monarch.registry.model.GrammarDefinition
import io.github.rosemoe.sora.langs.monarch.theme.toLanguageConfiguration

abstract class GrammarDefinitionReader<T> : JsonAdapter<ParsedGrammarDefinitionList<T>>() {
    override fun fromJson(reader: JsonReader): ParsedGrammarDefinitionList<T> {
        reader.isLenient = true

        val result = mutableListOf<GrammarDefinition<T>>()


        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "languages" -> {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val grammarDefinition = readGrammarDefinition(reader)
                        result.add(grammarDefinition)
                    }
                    reader.endArray()
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        return ParsedGrammarDefinitionList(result)
    }

    private fun readGrammarDefinition(
        reader: JsonReader
    ): GrammarDefinition<T> {
        var name = ""
        val grammar: T?
        val embeddedLanguages = mutableMapOf<String, String>()
        var grammarPath = ""
        var languageConfiguration: LanguageConfiguration? = null
        var scopeName = ""

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> {
                    name = reader.nextString()
                }

                "embeddedLanguages" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        embeddedLanguages[reader.nextName()] = reader.nextString()
                    }
                    reader.endObject()
                }

                "languageConfiguration" -> {

                    languageConfiguration = kotlin.runCatching {
                        val path = reader.nextString()

                        val rawText =
                            FileProviderRegistry.resolve(path)?.bufferedReader()?.readText()

                        rawText?.toLanguageConfiguration()
                    }.getOrNull()
                }

                "scopeName" -> {
                    scopeName = reader.nextString()
                }

                "grammar" -> {
                    grammarPath = reader.nextString()
                }
            }
        }

        reader.endObject()


        grammar = readGrammar(
            GrammarDefinition(
                name,
                Unit,
                embeddedLanguages,
                languageConfiguration,
                scopeName
            ), grammarPath
        )


        return GrammarDefinition(
            name,
            grammar ?: throw IllegalStateException("Grammar is null"),
            embeddedLanguages,
            languageConfiguration,
            scopeName
        )
    }

    override fun toJson(p0: JsonWriter, p1: ParsedGrammarDefinitionList<T>?) {}

    abstract fun readGrammar(grammarDefinition: GrammarDefinition<Unit>, path: String): T?
}

data class ParsedGrammarDefinitionList<T>(
    val grammarDefinition: List<GrammarDefinition<T>>,
) {
    companion object {
        private val EMPTY = ParsedGrammarDefinitionList<Unit>(emptyList())

        fun <T> empty(): ParsedGrammarDefinitionList<T> {
            return EMPTY as ParsedGrammarDefinitionList<T>
        }
    }
}