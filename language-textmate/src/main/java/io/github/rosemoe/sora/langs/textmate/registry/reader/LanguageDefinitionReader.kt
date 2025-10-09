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
package io.github.rosemoe.sora.langs.textmate.registry.reader

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.DefaultGrammarDefinition.Companion.withLanguageConfiguration
import io.github.rosemoe.sora.langs.textmate.registry.model.GrammarDefinition
import org.eclipse.tm4e.core.registry.IGrammarSource
import java.io.BufferedReader
import java.lang.reflect.Type
import java.nio.charset.Charset

object LanguageDefinitionReader {
    fun read(path: String): List<GrammarDefinition> {
        val stream = FileProviderRegistry.getInstance().tryGetInputStream(path)
        return stream?.bufferedReader()?.use {
            read(it)
        } ?: emptyList()
    }

    private fun read(bufferedReader: BufferedReader): List<GrammarDefinition> {
        // use kotlinx.serialization in the future
        return GsonBuilder().registerTypeAdapter(
            GrammarDefinition::class.java,
            JsonDeserializer { json: JsonElement, typeOfT: Type, context: JsonDeserializationContext ->
                val jsonObject = json.getAsJsonObject()
                val grammarPath = jsonObject.get("grammar").asString
                val name = jsonObject.get("name").asString
                val scopeName = jsonObject.get("scopeName").asString
                val embeddedLanguages = runCatching {
                    jsonObject.get("embeddedLanguages").asJsonObject
                }.getOrNull()
                val languageConfiguration = when (val element = jsonObject.get("languageConfiguration")) {
                    is JsonPrimitive -> element.asString
                    else -> null
                }

                val grammarInput = FileProviderRegistry.getInstance().tryGetInputStream(
                    grammarPath
                )
                requireNotNull(grammarInput) { "grammar file can not be opened" }
                val grammarSource = IGrammarSource.fromInputStream(
                    grammarInput,
                    grammarPath,
                    Charset.defaultCharset()
                )

                val grammarDefinition =
                    withLanguageConfiguration(grammarSource, languageConfiguration, name, scopeName)
                if (embeddedLanguages != null) {
                    val embeddedLanguagesMap = embeddedLanguages.asMap().mapNotNull { (key, value) ->
                        if (!value.isJsonNull) {
                            key to value.asString
                        } else {
                            null
                        }
                    }.toMap()

                    return@JsonDeserializer grammarDefinition.withEmbeddedLanguages(
                        embeddedLanguagesMap
                    )
                } else {
                    return@JsonDeserializer grammarDefinition
                }
            })
            .create()
            .fromJson(
                bufferedReader,
                LanguageDefinitionList::class.java
            ).languageDefinition
    }


    data class LanguageDefinitionList(
        @SerializedName("languages")
        var languageDefinition: List<GrammarDefinition>
    )
}
