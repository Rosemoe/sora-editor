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

class ThemeDefaultColors(
    defaultColors: Map<String, String>,
    val oldTextMateStyle: Boolean = false
) {
    private val colors = mutableMapOf<String, String>()

    init {
        colors.putAll(defaultColors)
    }

    constructor() : this(emptyMap())

    operator fun get(key: String): String? {
        return colors[key]
    }

    fun putColors(map: Map<String, String>) {
        colors.putAll(map)
    }

    fun getColor(key: String): String? {
        return colors[key]
    }

    fun getColors(): Map<String, String> {
        return colors
    }

    override fun toString(): String {
        return "ThemeDefaultColors(colors=$colors)"
    }

    companion object {
        val EMPTY = ThemeDefaultColors()
    }

}