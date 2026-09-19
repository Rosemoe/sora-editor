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

import io.github.rosemoe.sora.lang.styling.color.ConstColor
import io.github.rosemoe.sora.lang.styling.color.EditorColor
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.internal.grammar.ScopeStack
import org.eclipse.tm4e.core.internal.theme.FontStyle

/** Resolves semanticTokenColors first, then probes TextMate scopes in the active theme. */
class TextMateSemanticTokenStyleProvider(
    private val registry: ThemeRegistry
) : SemanticTokenStyleProvider {
    private data class ThemeStyles(val model: ThemeModel, val rules: SemanticTokenStyleRules)
    @Volatile private var cached: ThemeStyles? = null

    override fun getStyle(type: String, modifiers: Set<String>, languageId: String?): SemanticTokenStyle? {
        val model = registry.currentThemeModel
        val styles = cached?.takeIf { it.model === model } ?: ThemeStyles(model, readRules(model)).also { cached = it }
        val explicit = styles.rules.resolve(type, modifiers, languageId)
        var fallback: SemanticTokenStyle? = null
        for (probes in semanticTokenScopes(type, modifiers)) {
            // VS Code stops at the first styled scope in each default rule's probe list.
            val resolved = probes.firstNotNullOfOrNull { scope ->
                val match = model.theme.match(ScopeStack.from(scope))
                    ?: return@firstNotNullOfOrNull null
                if (match.foregroundId == 0 && match.fontStyle == FontStyle.NotSet) {
                    return@firstNotNullOfOrNull null
                }
                SemanticTokenStyle(
                    match.foregroundId.takeIf { it != 0 }?.let { EditorColor(it + 255) },
                    match.fontStyle.takeIf { it != FontStyle.NotSet }?.let { it and FontStyle.Bold != 0 },
                    match.fontStyle.takeIf { it != FontStyle.NotSet }?.let { it and FontStyle.Italic != 0 }
                )
            }
            fallback = fallback?.withFallback(resolved) ?: resolved
        }
        return explicit?.withFallback(fallback) ?: fallback
    }

    override fun observeChanges(listener: Runnable): AutoCloseable {
        val callback = ThemeRegistry.ThemeChangeListener { listener.run() }
        registry.addListener(callback)
        return AutoCloseable { registry.removeListener(callback) }
    }

    private fun readRules(model: ThemeModel): SemanticTokenStyleRules {
        val raw = model.rawTheme as? Map<*, *>
        val colors = raw?.get("semanticTokenColors") as? Map<*, *> ?: emptyMap<Any, Any>()
        val rules = linkedMapOf<String, SemanticTokenStyle>()
        for ((selector, value) in colors) {
            if (selector !is String) continue
            val settings = value as? Map<*, *>
            val fontStyle = settings?.get("fontStyle") as? String
            val fonts = fontStyle?.split(Regex("\\s+"))
            val foreground = (value as? String) ?: settings?.get("foreground") as? String
            rules[selector] = SemanticTokenStyle(
                foreground?.let(::parseColor),
                fonts?.contains("bold") ?: settings?.get("bold") as? Boolean,
                fonts?.contains("italic") ?: settings?.get("italic") as? Boolean
            )
        }
        return SemanticTokenStyleRules(rules)
    }

    // VS Code theme colors use #RGBA / #RRGGBBAA, while Android expects alpha first.
    private fun parseColor(value: String): ConstColor? {
        if (!value.startsWith('#')) return null
        val hex = value.substring(1).let {
            if (it.length == 3 || it.length == 4) it.flatMap { c -> listOf(c, c) }.joinToString("") else it
        }
        val number = hex.toLongOrNull(16) ?: return null
        val argb = when (hex.length) {
            6 -> number or 0xff000000L
            8 -> (number ushr 8) or ((number and 0xff) shl 24)
            else -> return null
        }
        return ConstColor(argb.toInt())
    }
}
