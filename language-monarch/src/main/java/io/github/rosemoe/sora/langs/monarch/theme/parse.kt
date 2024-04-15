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

import io.github.dingyi222666.monarch.types.FontStyle

fun List<ITokenThemeRule>.parseTokenTheme(): List<ParsedTokenThemeRule> {
    val result = mutableListOf<ParsedTokenThemeRule>()

    forEachIndexed { index, it ->

        var fontStyle = FontStyle.NotSet

        val segments = it.fontStyle?.split(' ')

        segments?.forEach { segment ->
            fontStyle = fontStyle or when (segment) {
                "italic" -> FontStyle.Italic;
                "bold" -> FontStyle.Bold;
                "underline" -> FontStyle.Underline
                "strikethrough" -> FontStyle.Strikethrough;
                else -> 0
            }
        }

        result.add(
            ParsedTokenThemeRule(
                token = it.token,
                index = index,
                background = it.background,
                foreground = it.foreground,
                fontStyle = fontStyle
            )
        )
    }

    return result
}

fun List<ParsedTokenThemeRule>.resolveParsedTokenThemeRules(
    customTokenColors: List<String> = emptyList(),
    themeDefaultColors: ThemeDefaultColors = ThemeDefaultColors(),
    themeType: String = "light",
    name: String
): TokenTheme {
    // Sort rules lexicographically, and then by index if necessary
    // this.sortWith(compareBy({ it.token }, { it.index }))
    val parsedThemeRules = sortedWith(compareBy({ it.token }, { it.index })).toMutableList()

    // Determine defaults
    var defaultFontStyle = FontStyle.None
    var defaultForeground = "#000000"
    var defaultBackground = "#ffffff"
    while (parsedThemeRules.isNotEmpty() && parsedThemeRules[0].token == "") {
        val incomingDefaults = parsedThemeRules[0]
        parsedThemeRules.removeFirst()
        if (incomingDefaults.fontStyle != FontStyle.NotSet) {
            defaultFontStyle = incomingDefaults.fontStyle
        }
        if (incomingDefaults.foreground != null) {
            defaultForeground = incomingDefaults.foreground
        }
        if (incomingDefaults.background != null) {
            defaultBackground = incomingDefaults.background
        }
    }
    val colorMap = ColorMap()

    // Start with token colors from custom token themes
    for (color in customTokenColors) {
        colorMap.getId(color)
    }

    val foregroundColorId = colorMap.getId(defaultForeground)
    val backgroundColorId = colorMap.getId(defaultBackground)

    val defaults = ThemeTrieElementRule(defaultFontStyle, foregroundColorId, backgroundColorId)
    val root = ThemeTrieElement(defaults)
    for (rule in parsedThemeRules) {
        root.insert(
            rule.token, rule.fontStyle, colorMap.getId(
                rule.foreground
            ),
            colorMap.getId(rule.background)
        )
    }

    return TokenTheme(colorMap, root, themeDefaultColors, themeType, name)
}