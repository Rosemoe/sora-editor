# TextMate Support

## Overview

This module provide language support and theme configuration based
on [TextMate](https://macromates.com/) rule files.

The core implementation of TextMate functionality is
from [tm4e](https://github.com/eclipse-tm4e/tm4e).

## Features

* MultiLanguage Registry
* Syntax Highlighting based on TextMate Grammars
* TextMate Themes
* Folding Regions
* Indentation Rules
* Symbol Pair Auto-Completion

## Language Bundles and Themes

We do not currently maintain a repository of TextMate language bundles and themes.

- You can obtain relevant documents from [TextMate Projects](https://github.com/textmate).
- Eclipse also uses TextMate, and you can also get files from its related repository.
- TextMate is also used in [VSCode](https://github.com/microsoft/vscode/tree/main/extensions).
  You can get the configuration file from its source code
    - We don't guarantee that all language bundles can be correctly analyzed, due to regex library
      difference
    - Include `oniguruma-native` module to use the same regex library as VSCode

Read
our [documentation](https://project-sora.github.io/sora-editor-docs/guide/using-language#language-textmate)
for more information.