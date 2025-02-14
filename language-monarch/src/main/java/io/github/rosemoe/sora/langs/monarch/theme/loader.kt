/*******************************************************************************
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Contributors:
 * Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 * Sebastian Thomschke (Vegard IT GmbH) - add previousLineText support
 *
 ******************************************************************************/

package io.github.rosemoe.sora.langs.monarch.theme

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import io.github.dingyi222666.monarch.loader.json.addLast
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.LanguageConfigurationAdapter
import io.github.rosemoe.sora.langs.monarch.languageconfiguration.model.LanguageConfiguration

class TokenThemeAdapter : JsonAdapter<TokenTheme>() {
    override fun fromJson(reader: JsonReader): TokenTheme {
        reader.isLenient = true
        val tokenThemeRuleList = mutableListOf<TokenThemeRule>()
        val themeColorsMap = mutableMapOf<String, String>()
        var themeType = "light"
        var themeName = ""
        var isOldTextMateTheme = false

        // ignore name

        reader.beginObject()

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> {
                    themeType = reader.nextString()
                }

                "tokenColors" -> {
                    readTokenColors(reader, tokenThemeRuleList)
                }

                "colors" -> {
                    readThemeColors(reader, themeColorsMap)
                }

                "settings" -> {
                    isOldTextMateTheme = true
                    readTextMateTokenColors(reader, tokenThemeRuleList, themeColorsMap)
                }

                "name" -> {
                    themeName = reader.nextString()
                }

                else -> {
                    reader.skipValue()
                }
            }
        }

        reader.endObject()

        return TokenTheme.createFromRawTokenTheme(
            tokenThemeRuleList,
            emptyList(),
            ThemeDefaultColors(themeColorsMap, isOldTextMateTheme),
            themeType,
            themeName
        )

    }

    private fun readThemeColors(
        reader: JsonReader,
        themeColorsMap: MutableMap<String, String>
    ) {
        reader.beginObject()

        while (reader.hasNext()) {
            val key = reader.nextName()
            themeColorsMap[key] = reader.nextString()
        }

        reader.endObject()
    }

    private fun readTokenColors(
        reader: JsonReader,
        tokenThemeRuleList: MutableList<TokenThemeRule>
    ) {
        reader.beginArray()

        while (reader.hasNext()) {
            readTokenColor(reader, tokenThemeRuleList)
        }

        reader.endArray()
    }

    private fun readTokenColor(
        reader: JsonReader,
        tokenThemeRuleList: MutableList<TokenThemeRule>,
        themeColorsMap: MutableMap<String, String>? = null
    ) {
        reader.beginObject()
        val tokenList = mutableListOf<String>()
        var foreground: String? = null
        var background: String? = null
        var fontStyle: String? = null

        val processToken = { token: String ->
            if (token.contains(",")) {
                tokenList.addAll(token.split(",").map {
                    it.trim()
                })
            } else {
                tokenList.add(token)
            }
        }

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "scope" -> {
                    if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            processToken(reader.nextString())
                        }
                        reader.endArray()
                    } else {
                        val token = reader.nextString()

                        processToken(token)
                    }
                }

                "settings" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (val key = reader.nextName()) {
                            "foreground" -> {
                                foreground = reader.nextString()
                            }

                            "background" -> {
                                background = reader.nextString()
                            }

                            "fontStyle" -> {
                                fontStyle = reader.nextString()
                            }

                            else -> {
                                if (themeColorsMap != null) {
                                    themeColorsMap[key] = reader.nextString()
                                } else {
                                    reader.skipValue()
                                }
                            }
                        }
                    }
                    reader.endObject()
                }

                else -> {
                    reader.skipValue()
                }
            }

        }

        reader.endObject()

        if (tokenList.isEmpty()) {
            tokenList.add("")
        }

        for (token in tokenList) {
            tokenThemeRuleList.add(
                TokenThemeRule(
                    token,
                    foreground,
                    background,
                    fontStyle
                )
            )
        }

        val updates = mapOf(
            "foreground" to foreground,
            "background" to background,
            "fontStyle" to fontStyle
        )

        if (themeColorsMap == null) return

        updates
            .filterValues { it != null }
            .mapValues { requireNotNull(it.value) }
            .let {
                themeColorsMap.putAll(it)
            }


    }

    private fun readTextMateTokenColors(
        reader: JsonReader,
        tokenThemeRuleList: MutableList<TokenThemeRule>,
        themeColorsMap: MutableMap<String, String>
    ) {
        reader.beginArray()

        //  "settings": [{
        //        "settings": {
        //            "background": "#242424",
        //            "foreground": "#cccccc",
        //            "lineHighlight": "#2B2B2B",
        //            "selection": "#214283",
        //            "highlightedDelimitersForeground": "#57f6c0"
        //        }
        //    },
        //        {
        //            "name": "Comment",
        //            "scope": "comment",
        //            "settings": {
        //                "foreground": "#707070"
        //            }
        //        },

        // object 0: settings object


        readTokenColor(reader, tokenThemeRuleList, themeColorsMap)


        // other: object

        while (reader.hasNext()) {
            readTokenColor(reader, tokenThemeRuleList)
        }

        reader.endArray()
    }

    override fun toJson(p0: JsonWriter, p1: TokenTheme?) {}

}

internal val MoshiRoot: Moshi = Moshi.Builder()
    .apply {
        addLast<TokenTheme>(TokenThemeAdapter())
        addLast<LanguageConfiguration>(LanguageConfigurationAdapter())
    }
    .build()

internal inline fun <reified T> Moshi.adapter(): JsonAdapter<T> {
    return this.adapter(T::class.java)
}


fun String.toTokenTheme(): TokenTheme {
    return MoshiRoot.adapter<TokenTheme>().fromJson(this)!!
}

fun String.toLanguageConfiguration(): LanguageConfiguration {
    return MoshiRoot.adapter<LanguageConfiguration>().fromJson(this)!!
}

internal data class TokenThemeRule(
    override var token: String,
    override var foreground: String?,
    override var background: String?,
    override var fontStyle: String?
) : ITokenThemeRule