/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

package io.github.rosemoe.sora.lsp.editor.semantic

import io.github.rosemoe.sora.editor.ts.TsLanguage
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.lang.styling.color.EditorColor

/** Optional adapter. The application must include language-treesitter to use this provider. */
class TreeSitterSemanticTokenStyleProvider(
    private val language: TsLanguage,
    styles: Map<String, SemanticTokenStyle> = emptyMap()
) : SemanticTokenStyleProvider {
    private val semanticRules = SemanticTokenStyleRules(styles)

    override fun observeChanges(listener: Runnable): AutoCloseable = language.observeThemeChanges(listener)

    override fun getStyle(type: String, modifiers: Set<String>, languageId: String?): SemanticTokenStyle? {
        val capture = when (type) {
            "namespace" -> "module"
            "class", "interface", "struct", "enum" -> "type"
            "typeParameter" -> "type.parameter"
            "parameter" -> "variable.parameter"
            "property" -> "variable.field"
            "enumMember" -> "constant"
            "method", "member" -> "function.method"
            "macro" -> "function.macro"
            "decorator" -> "attribute"
            "regexp" -> "string.regex"
            else -> type
        }
        val name = if ("readonly" in modifiers && (type == "variable" || type == "property")) "constant" else capture
        val theme = language.theme
        val style = (if ("defaultLibrary" in modifiers) theme.resolveStyleForCapture("$name.builtin") else 0L)
            .takeIf { it != 0L } ?: theme.resolveStyleForCapture(name)
        val fallback = style.takeIf { it != 0L }?.let {
            SemanticTokenStyle(
                TextStyle.getForegroundColorId(it).takeIf { id -> id != 0 }?.let(::EditorColor),
                TextStyle.isBold(it), TextStyle.isItalics(it))
        }
        return semanticRules.resolve(type, modifiers, languageId)?.withFallback(fallback) ?: fallback
    }

}
