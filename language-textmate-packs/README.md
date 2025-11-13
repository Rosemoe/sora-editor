# TextMate Language Packs

## Introduction

The `language-textmate-packs` module transforms the core [language-textmate](../language-textmate) module into a complete packs of ready-to-use grammars and color schemes. Each pack under [projects](./projects) is a standalone Android library that provides TextMate language or color scheme implementations, allowing applications to leverage advanced syntax highlighting without duplicating grammar files or maintaining custom build processes.

## Architecture

The pack generator in [LanguageTextmatePacksGenerator.kt](../build-logic/convention/src/main/kotlin/LanguageTextmatePacksGenerator.kt) processes [grammars/grammars.json](./grammars/grammars.json), reads grammar files from [grammars](./grammars), and scans theme JSON files in [themes](./themes).

Templates located in [build-logic/convention/src/main/resources/templates](../build-logic/convention/src/main/resources/templates) define how each Android module is generated, including Gradle configuration, Kotlin/Java wrappers, and Maven publishing settings.

After the generator runs, each folder under `projects` is automatically registered as `:<dir-name>` in [settings.gradle.kts](../settings.gradle.kts), ensuring the packs integrate seamlessly with Gradle's dependency resolution just like hand-written modules.

## Getting Started

Before using TextMate language packs, ensure you have included the `language-textmate` module in your project. Refer to the [Getting Started](../getting-started.md) guide for basic setup instructions.

### Add Dependencies

Newest Version: [![Maven Central](https://img.shields.io/maven-central/v/io.github.rosemoe/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.rosemoe%20editor))

Language and theme packs are distributed through Maven Central. Use the editor BOM to keep versions aligned:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.rosemoe:editor-bom:0.23.7"))
    implementation("io.github.rosemoe:language-textmate")    // TextMate engine
    implementation("io.github.rosemoe:language-textmate-lua")         // Lua language pack
    implementation("io.github.rosemoe:theme-textmate-ayu-dark")       // Ayu Dark theme pack
}
```

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.rosemoe:editor-bom:0.23.7"))
    implementation 'io.github.rosemoe:language-textmate'    // TextMate engine
    implementation 'io.github.rosemoe:language-textmate-lua'         // Lua language pack
    implementation 'io.github.rosemoe:theme-textmate-ayu-dark'       // Ayu Dark theme pack
}
```

> [!NOTE]
> Replace `0.23.7` with the current editor version. You can find the newest version from the badge above, or visit the GitHub [Releases](https://github.com/Rosemoe/sora-editor/releases) page.
> For local development, you can depend on `project(":language-textmate-lua")` instead of using Maven coordinates.

### Register File Provider

Language and theme packs bundle their assets in AAR files. Register an `AssetsFileResolver` to allow the TextMate engine to access these resources. This setup is typically performed once in your `Application` class or activity's `onCreate()`.

```kotlin
FileProviderRegistry.getInstance().addFileProvider(
    AssetsFileResolver(applicationContext.assets)
)
```

```java
FileProviderRegistry.getInstance().addFileProvider(
    new AssetsFileResolver(getApplicationContext().getAssets())
);
```

> [!warning]
> Always use `applicationContext.assets` instead of activity context to ensure the file provider survives throughout the application lifecycle.

## Using Theme Packs

Theme packs provide pre-packaged TextMate color schemes. Each theme pack includes a `ColorScheme` implementation that automatically handles theme loading and registration.

### Available Themes

| Theme Name  | Module Name        | Dark Mode |
|-------------|--------------------|-----------|
| Ayu Dark    | `io.github.rosemoe:theme-textmate-ayu-dark`  | Yes       |
| Ayu Light   | `io.github.rosemoe:theme-textmate-ayu-light`   | No        |
| Darcula     | `io.github.rosemoe:theme-textmate-darcula`    | Yes       |
| Quiet Light | `io.github.rosemoe:theme-textmate-quite-light`   | No        |

### Apply Theme Pack

Use the theme pack to apply the color scheme directly to your editor:

```Kotlin [Kotlin]
// Apply Ayu Dark theme
editor.colorScheme = AyuDarkColorScheme()
```

```Java [Java]
// Apply Ayu Dark theme
editor.setColorScheme(AyuDarkColorScheme());
```

The theme pack automatically:

- Loads the theme JSON from its bundled assets
- Registers the theme with `ThemeRegistry` if not already registered
- Applies the theme to `ThemeRegistry`
- Create `TextMateColorScheme`

### Manual Theme Loading (Advanced)

If you need more control over theme loading, you can manually load and register themes:

```Kotlin [Kotlin]
val themeRegistry = ThemeRegistry.getInstance()

// Load theme from pack assets
val themeModel = ThemeModel(
    IThemeSource.fromInputStream(
        FileProviderRegistry.getInstance().tryGetInputStream("textmate-bundles/ayu-dark.json"),
        "textmate-bundles/ayu-dark.json",
        null
    ),
    "ayu-dark"
).apply {
    setDark(true) // Mark as dark theme
}

// Register theme
themeRegistry.loadTheme(themeModel)

// Set as active theme
themeRegistry.setTheme("ayu-dark")

// Apply to editor
editor.colorScheme = TextMateColorScheme.create(themeRegistry)
```

```Java
var themeRegistry = ThemeRegistry.getInstance();

// Load theme from pack assets
var themeModel = new ThemeModel(
    IThemeSource.fromInputStream(
        FileProviderRegistry.getInstance().tryGetInputStream("textmate-bundles/ayu-dark.json"),
        "textmate-bundles/ayu-dark.json",
        null
    ),
    "ayu-dark"
);
themeModel.setDark(true); // Mark as dark theme

// Register theme
themeRegistry.loadTheme(themeModel);

// Set as active theme
themeRegistry.setTheme("ayu-dark");

// Apply to editor
editor.setColorScheme(TextMateColorScheme.create(themeRegistry));
```

## Using Language Packs

Language packs provide complete TextMate language implementations with syntax highlighting and auto-completion support. Each pack automatically registers its grammar and language configuration with the TextMate engine.

### Apply Language Pack

Use the language pack to set the editor language:

```Kotlin [Kotlin]
editor.editorLanguage = LuaLanguage()

// Or create a new instance with custom settings
editor.editorLanguage = LuaLanguage(
    createIdentifiers = true // Enable auto-completion
)
```

```Java [Java]
// Apply Lua language with auto-completion
editor.setEditorLanguage(LuaLanguage());

// Or create a new instance with custom settings
editor.setEditorLanguage(LuaLanguage(
    true // Enable auto-completion
));
```

The language pack automatically:

- Loads the grammar file from bundled assets
- Registers the grammar with `GrammarRegistry` if not already registered
- Loads language configuration (bracket matching, comment rules, etc.)
- Create `TextMateLanguage`

### Language Pack Features

Each language pack provides:

* Token-based highlighting using TextMate grammar rules
* Identifier-based auto-completion (when enabled)
* Auto-pairing brackets, indentation rules

## Supported Languages

The following language packs are currently available:

| Language     | Module Name                                            | Scope Name         |
|--------------|--------------------------------------------------------|--------------------|
| Assembly     | `io.github.rosemoe:language-textmate-assembly`       | source.asm         |
| Batch        | `io.github.rosemoe:language-textmate-bat`            | source.batchfile   |
| C            | `io.github.rosemoe:language-textmate-c`              | source.c           |
| C#           | `io.github.rosemoe:language-textmate-c-sharp`        | source.cs          |
| C++          | `io.github.rosemoe:language-textmate-cpp`            | source.cpp         |
| Coq          | `io.github.rosemoe:language-textmate-coq`            | source.coq         |
| CSS          | `io.github.rosemoe:language-textmate-css`            | source.css         |
| Dart         | `io.github.rosemoe:language-textmate-dart`           | source.dart        |
| Go           | `io.github.rosemoe:language-textmate-go`             | source.go          |
| Groovy       | `io.github.rosemoe:language-textmate-groovy`         | source.groovy      |
| HTML         | `io.github.rosemoe:language-textmate-html`           | text.html.basic    |
| HTMX         | `io.github.rosemoe:language-textmate-htmx`           | text.html.htmx     |
| Ignore Files | `io.github.rosemoe:language-textmate-ignore`         | source.gitignore   |
| INI          | `io.github.rosemoe:language-textmate-ini`            | source.ini         |
| Java         | `io.github.rosemoe:language-textmate-java`           | source.java        |
| JavaScript   | `io.github.rosemoe:language-textmate-javascript`     | source.js          |
| JSON         | `io.github.rosemoe:language-textmate-json`           | source.json        |
| JSX          | `io.github.rosemoe:language-textmate-jsx`            | source.js.jsx      |
| Kotlin       | `io.github.rosemoe:language-textmate-kotlin`         | source.kotlin      |
| LaTeX        | `io.github.rosemoe:language-textmate-latex`          | text.tex.latex     |
| Less         | `io.github.rosemoe:language-textmate-less`           | source.css.less    |
| Lisp         | `io.github.rosemoe:language-textmate-lisp`           | source.lisp        |
| Log Files    | `io.github.rosemoe:language-textmate-log`            | text.log           |
| Lua          | `io.github.rosemoe:language-textmate-lua`            | source.lua         |
| Markdown     | `io.github.rosemoe:language-textmate-markdown`       | text.html.markdown |
| Nim          | `io.github.rosemoe:language-textmate-nim`            | source.nim         |
| Pascal       | `io.github.rosemoe:language-textmate-pascal`         | source.pascal      |
| PHP          | `io.github.rosemoe:language-textmate-php`            | source.php         |
| PHP (Source) | `io.github.rosemoe:language-textmate-php-source-php` | text.html.php      |
| PowerShell   | `io.github.rosemoe:language-textmate-powershell`     | source.powershell  |
| Properties   | `io.github.rosemoe:language-textmate-properties`     | source.properties  |
| Python       | `io.github.rosemoe:language-textmate-python`         | source.python      |
| Ruby         | `io.github.rosemoe:language-textmate-ruby`           | source.ruby        |
| Rust         | `io.github.rosemoe:language-textmate-rust`           | source.rust        |
| SCSS         | `io.github.rosemoe:language-textmate-scss`           | source.css.scss    |
| Shell Script | `io.github.rosemoe:language-textmate-shellscript`    | source.shell       |
| Smali        | `io.github.rosemoe:language-textmate-smali`          | source.smali       |
| SQL          | `io.github.rosemoe:language-textmate-sql`            | source.sql         |
| Swift        | `io.github.rosemoe:language-textmate-swift`          | source.swift       |
| Plain Text   | `io.github.rosemoe:language-textmate-text`           | text.plain         |
| TOML         | `io.github.rosemoe:language-textmate-toml`           | source.toml        |
| TSX          | `io.github.rosemoe:language-textmate-tsx`            | source.tsx         |
| TypeScript   | `io.github.rosemoe:language-textmate-typescript`     | source.ts          |
| XML          | `io.github.rosemoe:language-textmate-xml`            | text.xml           |
| YAML         | `io.github.rosemoe:language-textmate-yaml`           | source.yaml        |
| Zig          | `io.github.rosemoe:language-textmate-zig`            | source.zig         |

## Contributing

### Adding New Languages or Themes

Follow these steps to contribute new language or theme packs:

1. Add Resource Files
    * For languages: Place grammar files (`.tmLanguage` or `.tmLanguage.json`) and optionally `language-configuration.json` in a subdirectory under [grammars](./grammars)
    * For themes: Place theme JSON files in [themes](./themes)

2. Update Configuration
    * Edit [grammars/grammars.json](./grammars/grammars.json) to describe your language or theme
    * Required fields: `name`, `scopeName`, `grammar`, optionally `languageConfiguration` and `embeddedLanguages`
    * Refer to `TextMateLanguageEntry` and `TextMateThemeEntry` in [LanguageTextmatePacksGenerator.kt](../build-logic/convention/src/main/kotlin/LanguageTextmatePacksGenerator.kt) for all available configuration options

3. Generate Packs

   ```bash
   ./gradlew generateLanguageTextmatePacks
   ```

   The generator creates new modules under [projects](./projects) with proper Gradle metadata, Kotlin/Java wrappers, and Maven publishing configuration.

4. Verify and Test
    * Sync Gradle to ensure [settings.gradle.kts](../settings.gradle.kts) registered the new `:textmate-language-<name>` or `:textmate-theme-<name>` module
    * Build locally to verify the generated AAR compiles successfully
    * Test the pack in the demo app before submitting your pull request

## Acknowledgements

Most grammars are sourced from the excellent [Xed-Editor](https://github.com/Xed-Editor/Xed-Editor) project. Their work on curating and validating TextMate assets enables `language-textmate-packs` to offer a comprehensive language catalog with minimal maintenance overhead.
