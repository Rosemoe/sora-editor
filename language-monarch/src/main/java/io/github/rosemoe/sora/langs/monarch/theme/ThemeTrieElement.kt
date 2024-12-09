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

open class ThemeTrieElement(val mainRule: ThemeTrieElementRule) {
    private val children = mutableMapOf<String, ThemeTrieElement>()

    fun toExternalThemeTrieElement(): ExternalThemeTrieElement {
        val children = children.mapValues { it.value.toExternalThemeTrieElement() }.toMutableMap()
        return ExternalThemeTrieElement(mainRule, children)
    }

    fun match(token: String): ThemeTrieElementRule {
        if (token.isEmpty()) {
            return mainRule
        }

        val dotIndex = token.indexOf('.')
        val (head, tail) = if (dotIndex == -1) {
            token to ""
        } else {
            token.substring(0, dotIndex) to token.substring(dotIndex + 1)
        }

        val child = children[head]
        return child?.match(tail) ?: mainRule
    }

    fun insert(token: String, fontStyle: Int, foreground: Int, background: Int) {
        if (token.isEmpty()) {
            mainRule.acceptOverwrite(fontStyle, foreground, background)
            return
        }

        val dotIndex = token.indexOf('.')
        val (head, tail) = if (dotIndex == -1) {
            token to ""
        } else {
            token.substring(0, dotIndex) to token.substring(dotIndex + 1)
        }

        val child = children.getOrPut(head) { ThemeTrieElement(mainRule.clone()) }
        child.insert(tail, fontStyle, foreground, background)
    }

    override fun toString(): String {
        return "ThemeTrieElement(mainRule=$mainRule, children=$children)"
    }


}
