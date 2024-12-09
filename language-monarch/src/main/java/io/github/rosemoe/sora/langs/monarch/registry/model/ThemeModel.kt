/*
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
 */
package io.github.rosemoe.sora.langs.monarch.registry.model

import io.github.rosemoe.sora.langs.monarch.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.monarch.theme.TokenTheme
import io.github.rosemoe.sora.langs.monarch.theme.toTokenTheme


class ThemeModel {
    private var themeSource: ThemeSource? = null
    lateinit var value: TokenTheme
        private set

    var name: String
        private set

    var isDark = false

    constructor(themeSource: ThemeSource) {
        this.themeSource = themeSource
        this.name = themeSource.name
    }

    internal constructor(name: String) {
        this.name = name
        value = TokenTheme.createFromParsedTokenTheme(emptyList())
    }


    @Throws(Exception::class)
    fun load() {
        val rawThemeString = themeSource?.let { source ->
            source.rawSource ?: FileProviderRegistry.resolve(source.path)?.use {
                it.bufferedReader().readText()
            }
        }

        if (rawThemeString == null) {
            throw Exception("Theme source is null")
        }

        value = rawThemeString.toTokenTheme()

        if (value.themeType == "dark") {
            isDark = true
        }

    }

    val isLoaded: Boolean
        get() = ::value.isInitialized


    companion object {
        val EMPTY = ThemeModel("EMPTY")
    }

}


data class ThemeSource(
    val path: String,
    val name: String,
    val rawSource: String? = null
)

