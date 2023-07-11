<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)

sora-editor is a cool and optimized code editor on Android platform

</div>

Read this in other languages: [English](README.md), [简体中文](README.zh-cn.md), [Español](README.es.md), [日本語](README.jp.md).

Note that statements in other languages may not be up-to-date.

***Work In Progress*** This project is still developing slowly.   
Download the newest sources from [Releases](https://github.com/Rosemoe/CodeEditor/releases)
instead of cloning this repository directly.
**Issues and pull requests are welcome.**

## Features

- [x] Syntax highlighting
- [x] Auto-completion (with [code snippets](https://macromates.com/manual/en/snippets))
- [x] Auto indent
- [x] Code block lines
- [x] Scale text
- [x] Undo/redo
- [x] Search and replace
- [x] Auto wordwrap
- [x] Show non-printable characters
- [x] Error/Warning/Typo/Deprecated indicators
- [x] Text magnifier
- [x] Incremental highlight analysis
- [x] Sticky Scroll
- [x] Highlight bracket pairs
- [x] Event System

## Key bindings

When working with a physical keyboard, you can use key bindings for performing various text
actions.
The editor provides support for some key bindings by default.
However, you can subscribe
to [`KeyBindingEvent`](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/event/KeyBindingEvent.java)
and add your own key bindings. You can even override the default key bindings and perform actions of
your own.

The currently supported key bindings are mostly similar to Android Studio/Intellij IDEA.
See the [supported key bindings](./keybindings.md).

## Screenshots

<div style="overflow: hidden">
<img src="/images/general.jpg" alt="GeneralAppearance" width="40%" align="bottom" />
<img src="/images/problem_indicators.jpg" alt="ProblemIndicator" width="40%" align="bottom" />
</div>

## Get started

Add to your app's dependencies:

```Gradle
dependencies {
    implementation(platform("io.github.Rosemoe.sora-editor:bom:<versionName>"))
    implementation("io.github.Rosemoe.sora-editor:<moduleName>")
}
```

Available modules:

- editor   
  Widget library containing all basic things of the framework
- editor-lsp   
  A convenient library for creating languages by using Language Server Protocol (aka LSP)
- language-java   
  A simple implementation for Java highlighting and identifier auto-completion
- language-textmate   
  An advanced highlighter for the editor. You can find textmate language bundles and themes and load
  them by using this
  module. The internal implementation of textmate is from [tm4e](https://github.com/eclipse/tm4e).
- language-treesitter   
  Offer [tree-sitter](https://tree-sitter.github.io/tree-sitter/) support for editor. This can be used to 
  parse the code to an AST fast and incrementally, which is helpful for accurate highlighting and providing completions.
  Note that this module only provides incremental paring and highlighting. Thanks to Java bindings [android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter/)

Check the newest version from the badge above
or [Releases](https://github.com/Rosemoe/CodeEditor/releases).

### Using Snapshot Versions
Snapshot releases are automatically generated on repository push. You may combine current released version name
and short commit hash to make a snapshot version name. For example, if the latest released version name is '0.21.1' and
short commit hash is '97c4963', you may use version name '0.21.1-97c4963-SNAPSHOT' to import the snapshot version to your project.

## Discuss

* Official QQ Group:[216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* Official [Telegram Group](https://t.me/rosemoe_code_editor)

## Contributors

<a href="https://github.com/Rosemoe/sora-editor/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Rosemoe/sora-editor" />
</a>

## License

```
sora-editor - the awesome code editor for Android
https://github.com/Rosemoe/sora-editor
Copyright (C) 2020-2023  Rosemoe

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
USA

Please contact Rosemoe by email 2073412493@qq.com if you need
additional information or have any questions
```

## Acknowledgements

Thanks to [JetBrains](https://www.jetbrains.com/?from=CodeEditor) for allocating free open-source
licences for IDEs such as [IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor).   
[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)
