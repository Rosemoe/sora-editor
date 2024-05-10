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
): ITokenTheme {
    private val cache: MutableMap<String, Int> = mutableMapOf()

    val colorMap: List<String>
        get() = privateColorMap.colorMap

    val themeTrieElement: ExternalThemeTrieElement
        get() = root.toExternalThemeTrieElement()

    val defaults: ThemeDefaultColors
        get() = themeDefaultColors

    private fun _match(token: String): ThemeTrieElementRule {
        return root.match(token)
    }

    fun match(token: String): Int {
        val result = cache.getOrPut(token) {
            val rule = _match(token)
            val standardToken = token.toStandardTokenType()
            (rule.metadata or (standardToken shl MetadataConsts.TOKEN_TYPE_OFFSET)).toInt()
        }
        return result
    }

    override fun match(languageId: Int, token: String): Int {
        return (match(token) or (languageId shl MetadataConsts.LANGUAGEID_OFFSET)).toInt()
    }


    override fun toString(): String {
        return "TokenTheme(colorMap=$colorMap, root=$root, themeDefaultColors=$themeDefaultColors, themeType='$themeType', cache=$cache)"
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