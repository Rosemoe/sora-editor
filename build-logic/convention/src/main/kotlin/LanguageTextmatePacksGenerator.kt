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

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.text.Normalizer
import java.util.Locale

internal data class TextMateLanguageEntry(
    val name: String,
    val scopeName: String,
    val grammar: String,
    val languageConfiguration: String?,
    val embeddedLanguages: Map<String, String>
)

internal data class TextMateThemeEntry(
    val fileName: String,
    val slug: String,
    val isDark: Boolean,
    val source: File
)

class LanguageTextmatePackProjectGenerator(
    private val rootDir: File,
    private val grammarsJson: File,
    private val grammarsDir: File,
    private val themesDir: File,
    private val projectsDir: File,
    private val logger: Logger? = null
) {

    private val templateDir = File(rootDir, "language-textmate")
    private val templateFiles by lazy {
        TemplateFiles(
            consumerRules = File(templateDir, "consumer-rules.pro").readText(),
            proguardRules = File(templateDir, "proguard-rules.pro").readText(),
            gradleProperties = File(templateDir, "gradle.properties").readText()
        )
    }

    private val templates by lazy {
        Templates(
            licenseHeader = loadResource("templates/license-header.txt"),
            buildGradle = loadResource("templates/build.gradle.kts.template"),
            languageClass = loadResource("templates/language-class.kt.template"),
            themeClass = loadResource("templates/theme-class.kt.template")
        )
    }

    private fun loadResource(path: String): String =
        javaClass.classLoader.getResourceAsStream(path)?.bufferedReader()?.readText()
            ?: error("Template resource not found: $path")

    fun generate(): List<String> {
        require(grammarsJson.exists()) {
            "Grammar definition file '${grammarsJson.absolutePath}' does not exist."
        }

        projectsDir.mkdirs()

        return buildList {
            addAll(generateLanguageModules())
            addAll(generateThemeModules())
        }
    }

    private fun parseLanguages(): List<TextMateLanguageEntry> {
        val root = JsonSlurper().parse(grammarsJson)
        val map = root as? Map<*, *>
            ?: error("Unexpected structure in ${grammarsJson.absolutePath}")
        val languageList = map["languages"] as? List<*>
            ?: error("Missing 'languages' array in ${grammarsJson.absolutePath}")

        return languageList.mapIndexed { index, element ->
            val obj = element as? Map<*, *>
                ?: error("Language at index $index is not a JSON object")
            val name = (obj["name"] as? String)?.trim().orEmpty()
            val scope = (obj["scopeName"] as? String)?.trim().orEmpty()
            val grammar = (obj["grammar"] as? String)?.trim().orEmpty()
            val langConfig = (obj["languageConfiguration"] as? String)?.trim()
            @Suppress("UNCHECKED_CAST")
            val embedded = (obj["embeddedLanguages"] as? Map<String, String>).orEmpty()

            require(name.isNotEmpty() && scope.isNotEmpty() && grammar.isNotEmpty()) {
                "Language entry at index $index is missing required properties"
            }

            TextMateLanguageEntry(name, scope, grammar, langConfig, embedded)
        }
    }

    private fun generateLanguageModules(): List<String> {
        val languages = parseLanguages()
        val usedSlugs = mutableSetOf<String>()
        val usedClassNames = mutableSetOf<String>()

        return languages.map { entry ->
            val slug = generateUniqueSlug(entry.name, entry.scopeName, usedSlugs)
            val className = generateUniqueClassName(entry.name, entry.scopeName, usedClassNames)
            val bundleId = entry.grammar.removePrefix("textmate-bundles/")
                .substringBefore('/').also { require(it.isNotEmpty()) {
                    "Unable to resolve bundle id from grammar path '${entry.grammar}'"
                } }
            val packageName = buildPackageName("io.github.rosemoe.sora.langs.textmate", slug)
            val moduleName = "language-$slug"
            val moduleDir = File(projectsDir, moduleName)

            moduleDir.mkdirs()
            generateLanguageProjectFiles(
                moduleDir, moduleName, entry, className, bundleId, packageName
            )
            moduleName
        }
    }

    private fun parseThemes(): List<TextMateThemeEntry> {
        if (!themesDir.exists()) return emptyList()

        return themesDir.listFiles { file ->
            file.isFile && file.extension.equals("json", ignoreCase = true)
        }?.sortedBy { it.name }?.map { file ->
            val slug = file.nameWithoutExtension
            TextMateThemeEntry(
                fileName = file.name,
                slug = slug,
                isDark = slug.lowercase(Locale.US).let { it == "ayu-dark" || it == "darcula" },
                source = file
            )
        } ?: emptyList()
    }

    private fun generateThemeModules(): List<String> {
        val themes = parseThemes()

        return themes.map { entry ->
            val slug = entry.slug
            val className = toPascalCase(slug) + "ColorScheme"
            val packageName = buildPackageName("io.github.rosemoe.sora.langs.textmate.theme", slug)
            val moduleName = "theme-$slug"
            val moduleDir = File(projectsDir, moduleName)

            moduleDir.mkdirs()
            generateThemeProjectFiles(moduleDir, moduleName, entry, packageName, className)
            moduleName
        }
    }

    private fun generateUniqueSlug(name: String, scopeName: String, used: MutableSet<String>): String {
        val base = slugify(name)
        if (used.add(base)) return base

        val scopeSlug = slugify(scopeName)
        if (scopeSlug.isNotEmpty()) {
            val candidate = "$base-$scopeSlug"
            if (used.add(candidate)) return candidate
        }

        return generateSequence(2) { it + 1 }
            .map { "$base-$it" }
            .first { used.add(it) }
    }

    private fun generateUniqueClassName(name: String, scopeName: String, used: MutableSet<String>): String {
        val base = toPascalCase(name) + "Language"
        if (used.add(base)) return base

        val scopeSuffix = toPascalCase(scopeName.replace('.', ' '))
        val candidate = base.removeSuffix("Language") + scopeSuffix + "Language"
        if (used.add(candidate)) return candidate

        return generateSequence(2) { it + 1 }
            .map { base.removeSuffix("Language") + it + "Language" }
            .first { used.add(it) }
    }

    private fun buildPackageName(prefix: String, slug: String): String {
        val suffix = slug.split('-')
            .filter { it.isNotBlank() }
            .joinToString(".") { part ->
                if (part.isEmpty() || !part[0].isLetter()) "lang$part" else part
            }
            .ifBlank { "lang" }
        return "$prefix.$suffix"
    }

    private fun generateLanguageProjectFiles(
        moduleDir: File,
        moduleName: String,
        entry: TextMateLanguageEntry,
        className: String,
        bundleId: String,
        packageName: String
    ) {
        val layout = prepareModuleLayout(moduleDir, packageName, moduleName)

        val kotlinFile = File(layout.packageDir, "$className.kt")
        writeFileIfChanged(kotlinFile, buildKotlinLanguageContent(entry, packageName, className))

        val sourceBundleDir = File(grammarsDir, bundleId)
        require(sourceBundleDir.exists()) {
            "Missing source bundle directory ${sourceBundleDir.absolutePath}"
        }
        val targetBundleDir = File(layout.assetsDir, "textmate-bundles/$bundleId")
        if (targetBundleDir.exists()) {
            targetBundleDir.deleteRecursively()
        }
        sourceBundleDir.copyRecursively(targetBundleDir, overwrite = true)
    }

    private fun generateThemeProjectFiles(
        moduleDir: File,
        moduleName: String,
        entry: TextMateThemeEntry,
        packageName: String,
        className: String
    ) {
        val layout = prepareModuleLayout(moduleDir, packageName, moduleName)
        val assetPath = "textmate-bundles/${entry.fileName}"
        val targetThemeFile = File(layout.assetsDir, assetPath)
        targetThemeFile.parentFile?.mkdirs()
        entry.source.copyTo(targetThemeFile, overwrite = true)

        val kotlinFile = File(layout.packageDir, "$className.kt")
        writeFileIfChanged(
            kotlinFile,
            buildKotlinThemeContent(entry, packageName, className, assetPath)
        )
    }

    private fun prepareModuleLayout(
        moduleDir: File,
        packageName: String,
        artifactId: String
    ): ModuleLayout {
        val srcDir = File(moduleDir, "src/main/java")
        val manifestDir = File(moduleDir, "src/main")
        val assetsDir = File(moduleDir, "src/main/assets")
        srcDir.mkdirs()
        manifestDir.mkdirs()
        assetsDir.mkdirs()

        writeFileIfChanged(
            File(moduleDir, "build.gradle.kts"),
            templates.licenseHeader + "\n\n" + templates.buildGradle.replace("%PACKAGE_NAME%", packageName)
        )
        writeFileIfChanged(File(moduleDir, ".gitignore"), "/build\n")
        writeFileIfChanged(
            File(moduleDir, "gradle.properties"),
            templateFiles.gradleProperties.lines().joinToString("\n", postfix = "\n") { line ->
                when {
                    line.startsWith("POM_ARTIFACT_ID=") -> "POM_ARTIFACT_ID=$artifactId"
                    line.startsWith("POM_NAME=") -> "POM_NAME=$artifactId"
                    else -> line
                }
            }
        )
        writeFileIfChanged(File(moduleDir, "consumer-rules.pro"), templateFiles.consumerRules)
        writeFileIfChanged(File(moduleDir, "proguard-rules.pro"), templateFiles.proguardRules)
        writeFileIfChanged(
            File(manifestDir, "AndroidManifest.xml"),
            templateFiles.manifest.replaceFirst("<manifest>", "<manifest package=\"$packageName\">")
        )

        // Kotlin classes use fixed package: io.github.rosemoe.sora.langs.textmate
        val kotlinPackageDir = File(srcDir, "io/github/rosemoe/sora/langs/textmate")
        kotlinPackageDir.mkdirs()

        return ModuleLayout(srcDir, kotlinPackageDir, assetsDir)
    }

    private data class ModuleLayout(
        val javaDir: File,
        val packageDir: File,
        val assetsDir: File
    )

    private fun buildKotlinLanguageContent(
        entry: TextMateLanguageEntry,
        packageName: String,
        className: String
    ): String {
        val embeddedLanguagesLiteral = if (entry.embeddedLanguages.isEmpty()) {
            "emptyMap()"
        } else {
            entry.embeddedLanguages.entries.joinToString(
                prefix = "mapOf(",
                postfix = ")"
            ) { (scope, language) ->
                "\"${scope.escapeKotlin()}\" to \"${language.escapeKotlin()}\""
            }
        }

        return templates.licenseHeader + "\n\n" + templates.languageClass
            .replace("%CLASS_NAME%", className)
            .replace("%LANGUAGE_NAME%", entry.name.escapeKotlin())
            .replace("%SCOPE_NAME%", entry.scopeName.escapeKotlin())
            .replace("%GRAMMAR_PATH%", entry.grammar.escapeKotlin())
            .replace("%LANGUAGE_CONFIG_PATH%", entry.languageConfiguration?.let { "\"${it.escapeKotlin()}\"" } ?: "null")
            .replace("%EMBEDDED_LANGUAGES%", embeddedLanguagesLiteral)
            .replace("%MISSING_GRAMMAR_MESSAGE%", "Unable to locate grammar for ${entry.scopeName} at ${entry.grammar}".escapeKotlin())
    }

    private fun buildKotlinThemeContent(
        entry: TextMateThemeEntry,
        packageName: String,
        className: String,
        assetPath: String
    ): String {
        return templates.licenseHeader + "\n\n" + templates.themeClass
            .replace("%CLASS_NAME%", className)
            .replace("%THEME_NAME%", entry.slug.escapeKotlin())
            .replace("%THEME_PATH%", assetPath.escapeKotlin())
            .replace("%IS_DARK%", entry.isDark.toString())
            .replace("%MISSING_THEME_MESSAGE%", "Unable to open theme file at $assetPath".escapeKotlin())
    }

    private fun writeFileIfChanged(file: File, content: String) {
        if (file.exists() && file.readText() == content) return
        file.parentFile?.mkdirs()
        file.writeText(content)
        logger?.info("Generated ${file.relativeToOrNull(rootDir) ?: file}")
    }

    private fun slugify(value: String): String =
        normalizeText(value)
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifEmpty { "lang" }

    private fun toPascalCase(value: String): String {
        val parts = normalizeText(value)
            .split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() }

        return if (parts.isEmpty()) {
            "Lang"
        } else {
            parts.joinToString("") {
                it.lowercase(Locale.US).replaceFirstChar { ch -> ch.titlecase(Locale.US) }
            }
        }
    }

    private fun normalizeText(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace("#", " sharp ")
            .replace("+", " plus ")
            .replace("&", " and ")

    private fun String.escapeKotlin(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private data class TemplateFiles(
        val consumerRules: String,
        val proguardRules: String,
        val gradleProperties: String,
        val manifest: String = "<manifest/>"
    )

    private data class Templates(
        val licenseHeader: String,
        val buildGradle: String,
        val languageClass: String,
        val themeClass: String
    )
}

abstract class GenerateLanguageTextmatePacksTask : DefaultTask() {

    @get:InputFile
    abstract val grammarsFile: RegularFileProperty

    @get:InputDirectory
    abstract val grammarsDir: DirectoryProperty

    @get:InputDirectory
    abstract val themesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val projectsDir: DirectoryProperty

    @get:Input
    abstract val rootDirProperty: Property<String>

    @get:Internal
    protected val generator by lazy {
        LanguageTextmatePackProjectGenerator(
            rootDir = File(rootDirProperty.get()),
            grammarsJson = grammarsFile.get().asFile,
            grammarsDir = grammarsDir.get().asFile,
            themesDir = themesDir.get().asFile,
            projectsDir = projectsDir.get().asFile,
            logger = logger
        )
    }

    @TaskAction
    fun generate() {
        generator.generate()
    }
}
