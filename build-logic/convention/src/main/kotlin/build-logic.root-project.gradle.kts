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


require(project == rootProject) {
    "This script must be apply to the root project."
}

applyTextMateLanguagePacks()

fun applyTextMateLanguagePacks() {
    val grammarsJson = layout.projectDirectory.file("language-textmate-packs/grammars/grammars.json")
    val textmateGrammarsDir = layout.projectDirectory.dir("language-textmate-packs/grammars")
    val textmateThemesDir = layout.projectDirectory.dir("language-textmate-packs/themes")
    val packsProjectsDir = layout.projectDirectory.dir("language-textmate-packs/projects")

    val generateLanguageTextmatePacks = tasks.register<GenerateLanguageTextmatePacksTask>("generateLanguageTextmatePacks") {
        grammarsFile.set(grammarsJson)
        grammarsDir.set(textmateGrammarsDir)
        themesDir.set(textmateThemesDir)
        projectsDir.set(packsProjectsDir)
        rootDirProperty.set(rootDir.absolutePath)
    }

    tasks.matching { it.name == "prepareKotlinBuildScriptModel" }.configureEach {
        dependsOn(generateLanguageTextmatePacks)
    }
}
