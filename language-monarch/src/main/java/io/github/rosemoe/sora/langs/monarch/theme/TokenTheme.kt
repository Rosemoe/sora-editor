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

import com.squareup.moshi.JsonClass
import io.github.dingyi222666.monarch.types.ITokenTheme
import io.github.dingyi222666.monarch.types.MetadataConsts
import io.github.dingyi222666.monarch.types.StandardTokenType

@JsonClass(generateAdapter = false)
class TokenTheme internal constructor(
    private val privateColorMap: ColorMap,
    private val root: ThemeTrieElement,
    private val themeDefaultColors: ThemeDefaultColors = ThemeDefaultColors.EMPTY,
    val name: String = "default",
    val themeType: String = "light"
) : ITokenTheme {
    private val cache = mutableMapOf<String, Int>()

    val colorMap: ColorMap
        get() = privateColorMap

    val themeTrieElement: ExternalThemeTrieElement
        get() = root.toExternalThemeTrieElement()

    val defaults: ThemeDefaultColors
        get() = themeDefaultColors

    private fun _match(token: String): ThemeTrieElementRule {
        return root.match(token)
    }

    fun match(token: String): Int {
        val result = cache.getOrPut(token) {
            var rule = _match(token)

            if (rule === root.mainRule) {
                when {
                    // monarch -> textmate
                    token.startsWith("identifier") -> {
                        // ?
                        rule = _match(token.replace("identifier", "source"))
                    }

                    token.startsWith("attribute") -> {
                        rule = _match(token.replace("attribute", "entity.other.attribute-name"))
                    }

                    token.startsWith("regexp") -> {
                        rule = _match(token.replace("regexp", "string.regexp"))
                    }

                    token.startsWith("type") -> {
                        rule = _match(token.replace("type", "entity.name.type"))
                    }

                    token.startsWith("delimiter") -> {
                        rule = _match(token.replace("delimiter", "punctuation"))
                    }

                    token.startsWith("annotation") -> {
                        rule = _match(token.replace("annotation", "storage.type.annotation"))
                    }

                    token.startsWith("tag") -> {
                        rule = _match(token.replace("tag", "entity.name.tag"))
                    }

                    token.startsWith("number") -> {
                        rule = _match(token.replace("number", "constant.numeric"))
                    }
                }
            }

            val standardToken = token.toStandardTokenType()
            (rule.metadata or (standardToken shl MetadataConsts.TOKEN_TYPE_OFFSET))
        }
        return result
    }

    override fun match(languageId: Int, token: String): Int {
        return (match(token) or (languageId shl MetadataConsts.LANGUAGEID_OFFSET))
    }


    override fun toString(): String {
        return "TokenTheme(name=$name, colorMap=$colorMap, root=$root, themeDefaultColors=$themeDefaultColors, themeType='$themeType', cache=$cache)"
    }


    companion object {
        fun createFromRawTokenTheme(
            source: List<ITokenThemeRule>,
            customTokenColors: List<String> = emptyList(),
            themeDefaultColors: ThemeDefaultColors = ThemeDefaultColors(),
            themeType: String = "light",
            name: String = "default"
        ): TokenTheme {
            return createFromParsedTokenTheme(
                source.parseTokenTheme(),
                customTokenColors,
                themeDefaultColors,
                themeType,
                name
            )
        }

        fun createFromParsedTokenTheme(
            source: List<ParsedTokenThemeRule>,
            customTokenColors: List<String> = emptyList(),
            themeDefaultColors: ThemeDefaultColors = ThemeDefaultColors(),
            themeType: String = "light",
            name: String = "default"
        ): TokenTheme {
            return source.resolveParsedTokenThemeRules(
                customTokenColors,
                themeDefaultColors,
                themeType,
                name
            )
        }

    }
}

val STANDARD_TOKEN_TYPE_REGEXP = Regex("\\b(comment|string|regex|regexp)\\b")

fun String.toStandardTokenType(): Int {
    val match = STANDARD_TOKEN_TYPE_REGEXP.find(this)
    val token = match?.groups?.get(1)?.value
    return when (token) {
        "comment" -> StandardTokenType.Comment
        "string" -> StandardTokenType.String
        "regex", "regexp" -> StandardTokenType.RegEx
        else -> StandardTokenType.Other
    }
}