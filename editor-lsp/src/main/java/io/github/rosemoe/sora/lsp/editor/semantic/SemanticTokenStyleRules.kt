/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *--------------------------------------------------------------------------------------------*/
package io.github.rosemoe.sora.lsp.editor.semantic

/**
 * VS Code semantic selectors: type.modifier:language, with per-attribute specificity.
 * Adapted from tokenClassificationRegistry.ts and colorThemeData.ts.
 */
class SemanticTokenStyleRules(styles: Map<String, SemanticTokenStyle>) {
    private data class Rule(val type: String, val modifiers: List<String>, val language: String?, val style: SemanticTokenStyle) {
        fun score(type: String, modifiers: Set<String>, language: String?): Int {
            if (this.type != "*" && this.type != type || this.language != null && this.language != language ||
                !modifiers.containsAll(this.modifiers)) return -1
            return (if (this.type == "*") 0 else 100) + this.modifiers.size * 100 +
                (if (this.language == null) 0 else 10)
        }
    }

    private val rules = styles.map { (selector, style) ->
        val parts = selector.substringBefore(':').split('.')
        Rule(parts.first(), parts.drop(1), selector.substringAfter(':', "").ifEmpty { null }, style)
    }

    fun resolve(type: String, modifiers: Set<String>, languageId: String?): SemanticTokenStyle? {
        var result: SemanticTokenStyle? = null
        var foregroundScore = -1
        var boldScore = -1
        var italicScore = -1
        for (rule in rules) {
            val score = rule.score(type, modifiers, languageId)
            if (score < 0) continue
            var style = result ?: SemanticTokenStyle()
            if (rule.style.foreground != null && score >= foregroundScore) {
                style = style.copy(foreground = rule.style.foreground)
                foregroundScore = score
            }
            if (rule.style.bold != null && score >= boldScore) {
                style = style.copy(bold = rule.style.bold)
                boldScore = score
            }
            if (rule.style.italic != null && score >= italicScore) {
                style = style.copy(italic = rule.style.italic)
                italicScore = score
            }
            result = style
        }
        return result
    }
}
