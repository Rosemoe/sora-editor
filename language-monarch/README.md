# language-monarch

**Work In Progress**

## About

The `language-monarch` module is a dynamic syntax highlighting and functionality module based on the [Monarch syntax definition](https://microsoft.github.io/monaco-editor/monarch.html) of the Monaco editor.

## Comparison with the Textmate Module

1. Monarch uses simpler regular expression syntax, suitable for general cases where complex information is not required for code highlighting.
2. Compared to the Textmate module, Monarch may offer a 10% to 40% speed improvement in rendering for the same language (due to simpler syntax definitions and fewer regular expressions).
3. You can use Kotlin to [write syntax definitions](https://github.com/dingyi222666/monarch-kt/blob/main/monarch-language-pack/src/main/kotlin/io/github/dingyi222666/monarch/languages/LanguageKotlin.kt) instead of dynamically loading grammar files.

## Features (Already Available)

1. File highlighting based on syntax rules
2. Load color themes from files
3. Code block lines based on indentation and rules

## How to Get Syntax and Theme Files

### Syntax Packs

You can obtain syntax packs from [monarch-kt](https://github.com/dingyi222666/monarch-kt/tree/main/monarch-language-pack/src/main/resources/language_packs). Syntax packs are available in both Kotlin and JSON versions.

#### Kotlin Version Example:

```kotlin
import io.github.dingyi222666.monarch.languages.KotlinLanguage

MonarchGrammarRegistry.INSTANCE.loadGrammars(
    monarchLanguages {
        language("kotlin") {
            monarchLanguage = KotlinLanguage
            defaultScopeName()
            languageConfiguration = "textmate/kotlin/language-configuration.json"
        }
    }
)
```

#### JSON Version Example:
```kotlin
MonarchGrammarRegistry.INSTANCE.loadGrammars(
    monarchLanguages {
        language("kotlin") {
            grammar = "xxx/kotlin.json"
            defaultScopeName()
            languageConfiguration = "textmate/kotlin/language-configuration.json"
        }
    }
)
```


### Theme Files

We have attempted to make theme files compatible with Textmate themes. Therefore, by default, you can directly use Textmate theme files.