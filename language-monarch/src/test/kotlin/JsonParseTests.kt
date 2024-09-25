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

import io.github.rosemoe.sora.langs.monarch.theme.toLanguageConfiguration
import io.github.rosemoe.sora.langs.monarch.theme.toTokenTheme
import org.junit.Test
import java.io.File


class JsonParseTests {

    @Test
    fun parseLanguageConfiguration() {
        val languageJson = File("src/test/resources/javascript-language-configuration.json")

        val languageConfiguration = languageJson.readText().toLanguageConfiguration()

        println(languageConfiguration)
    }

    @Test
    fun parseTheme() {
        val themeJson = File("src/test/resources/sakura-color-theme.json")

        val tokenTheme = themeJson.readText().toTokenTheme()

        println(tokenTheme)
    }
}