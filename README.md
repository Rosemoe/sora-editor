<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)   


sora-editor is a cool and optimized code editor on Android platform   
With good performance and nice features
  
</div>

***Work In Progress*** This project is still developing slowly. Note that APIs are unstable.
It is **not** recommended using this project for production use.
Download the newest sources from [Releases](https://github.com/Rosemoe/CodeEditor/releases) instead of cloning this repository directly.
**Issues and pull requests are welcome.**
## Features
- [x] Syntax highlighting
- [x] Auto-completion
- [x] Auto indent
- [x] Code block lines
- [x] Scale text
- [x] Undo/redo
- [x] Search and replace
- [x] Auto wordwrap
- [x] Show non-printable characters
- [x] Error/Warning/Typo/Deprecated indicators
- [x] Text magnifier
- [ ] Code folding
- [ ] Incremental highlight Analysis
- [ ] Plugin System
## Language Supported  
* Java, JavaScript, C, C++, HTML, Python, PHP, CSS3 (Basic Support:highlight, code block line,identifier and keyword auto-completion). Code block line isn't available for HTML Language
* [Textmate support](/textmate-core/README.md)
## Screenshots
![Wordwrap](/images/wordwrap.png)
<img src="/images/curlylines.jpg" alt="ErrorIndicator" width="40%" align="bottom" />
## How to use this editor  
Add to your app's dependencies:
```Gradle
implementation 'io.github.Rosemoe.sora-editor:<moduleName>:<versionName>'
```
Available modules:     
* editor 
* language-base
* language-java
* language-html
* language-python
* language-css3
* language-xml
* language-universal
* [language-textmate](/language-textmate/README.md)


Detailed: [Get started](https://rosemoe.github.io/2021/08/22/editor-get-started/)
## License
```
sora-editor - the awesome code editor for Android
https://github.com/Rosemoe/sora-editor
Copyright (C) 2020-2022  Rosemoe

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
## Discuss
* Official QQ Group:[216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* [Official Telegram Group](https://t.me/rosemoe_code_editor)
## Acknowledgements
Thanks to [JetBrains](https://www.jetbrains.com/?from=CodeEditor) for allocating free open-source licences for IDEs such as [IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor).   
[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)
