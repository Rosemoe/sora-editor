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

import io.github.rosemoe.sora.lang.styling.color.ResolvableColor

/** Null attributes preserve the corresponding syntax style. */
data class SemanticTokenStyle(
    val foreground: ResolvableColor? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null
) {
    /** Fill attributes not specified by this style using the language's syntax theme. */
    fun withFallback(fallback: SemanticTokenStyle?) = SemanticTokenStyle(
        foreground ?: fallback?.foreground, bold ?: fallback?.bold, italic ?: fallback?.italic)
}

/**
 * Resolves language-independent token classifications against a language's own theme.
 * Called on a background thread; returning null preserves syntax highlighting.
 */
fun interface SemanticTokenStyleProvider {
    fun getStyle(type: String, modifiers: Set<String>, languageId: String?): SemanticTokenStyle?

    /** Notify when cached semantic styles need resolving again. Close to unsubscribe. */
    fun observeChanges(listener: Runnable): AutoCloseable = AutoCloseable {}
}
