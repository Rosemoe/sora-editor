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

import io.github.dingyi222666.monarch.types.MetadataConsts
import io.github.dingyi222666.monarch.types.StandardTokenType

class TokenTheme internal constructor(
    private val colorMap: ColorMap,
    private val root: ThemeTrieElement
) {
    private val cache: MutableMap<String, Int> = mutableMapOf()

    fun getColorMap(): List<String> {
        return colorMap.getColorMap()
    }

    fun getThemeTrieElement(): ExternalThemeTrieElement {
        return root.toExternalThemeTrieElement()
    }

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

    fun match(languageId: Int, token: String): Int {
        return (match(token) or (languageId shl MetadataConsts.LANGUAGEID_OFFSET)).toInt()
    }

    companion object {
        fun createFromRawTokenTheme(source: List<ITokenThemeRule>, customTokenColors: Array<String>): TokenTheme {
            return createFromParsedTokenTheme(source.parseTokenTheme(), customTokenColors)
        }

        fun createFromParsedTokenTheme(source: List<ParsedTokenThemeRule>, customTokenColors: Array<String>): TokenTheme {
            return source.resolveParsedTokenThemeRules( customTokenColors)
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