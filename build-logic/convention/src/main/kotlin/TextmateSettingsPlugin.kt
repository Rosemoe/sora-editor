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

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File

class TextmateSettingsPlugin : Plugin<Settings> {

    private val logger: Logger = Logging.getLogger(TextmateSettingsPlugin::class.java)

    override fun apply(settings: Settings) {
        val rootDir = settings.rootDir
        val grammarsDir = File(rootDir, "language-textmate-packs/grammars")
        val grammarsJson = File(grammarsDir, "grammars.json")
        val themesDir = File(rootDir, "language-textmate-packs/themes")
        val projectsDir = File(rootDir, "language-textmate-packs/projects")

        val generator = LanguageTextmatePackProjectGenerator(
            rootDir = rootDir,
            grammarsJson = grammarsJson,
            grammarsDir = grammarsDir,
            themesDir = themesDir,
            projectsDir = projectsDir,
            logger = logger
        )

        generator.generate()
    }
}
