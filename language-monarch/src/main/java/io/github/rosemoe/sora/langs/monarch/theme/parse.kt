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
        parsedThemeRules.removeAt(0)
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

    return TokenTheme(colorMap, root, themeDefaultColors, name, themeType)
}