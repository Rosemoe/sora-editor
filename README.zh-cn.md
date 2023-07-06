<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)

sora-editor是一款高效的安卓代码编辑器

</div>

阅读其他语言的文档: [English](README.md), [简体中文](README.zh-cn.md), [Español](README.es.md), [日本語](README.jp.md).

***这个项目仍在缓慢开发中，欢迎提交问题和合并请求***

请从[Releases](https://github.com/Rosemoe/CodeEditor/releases)
下载最新的源代码，而不是直接克隆此项目。

## 特色

- [x] 语法高亮
- [x] 自动补全 (包含对[代码块（Code Snippets）](https://macromates.com/manual/en/snippets)的支持)
- [x] 自动缩进
- [x] 代码块辅助线
- [x] 手势缩放
- [x] 撤销/重做
- [x] 搜索和替换文本
- [x] 自动换行
- [x] 显示不可打印的字符
- [x] 错误/警告/错字/弃用指示器
- [x] 文本放大镜
- [x] 文本增量分析
- [x] 高亮显示括号对
- [x] 事件系统

## 快捷键绑定

使用物理键盘时，您可以使用快捷键来执行各种文本操作。

编辑器默认支持了一些快捷键，但是你可以订阅 [`KeyBindingEvent`](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/event/KeyBindingEvent.java)
并处理它来实现自己的快捷键，当然也可以覆盖默认的快捷键操作并执行自定义操作。

当前编辑器支持的快捷键大多与 Android Studio/Intellij IDEA 的快捷键类似，可参见[支持的快捷键](./keybindings.md).

## 编辑器预览图

<div style="overflow: hidden">
<img src="/images/general.jpg" alt="GeneralAppearance" width="40%" align="bottom" />
<img src="/images/problem_indicators.jpg" alt="ProblemIndicator" width="40%" align="bottom" />
</div>

## 快速使用

为您的项目添加依赖:

```Gradle
dependencies {
    implementation(platform("io.github.Rosemoe.sora-editor:bom:<versionName>"))
    implementation("io.github.Rosemoe.sora-editor:<moduleName>")
}
```

可用模块:

- editor   
  包含编辑器的核心框架。
- editor-lsp   
  可以使用语言服务器协议（简称LSP）创建语言的便捷工具库。
- language-java   
  包含Java高亮和自动补全的语言库。
- language-textmate   
  一个高级的高亮分析库。你可以借助它来加载textmate语言配置文件并应用于本编辑器。
  内部实现来自[tm4e](https://github.com/eclipse/tm4e)。
- language-treesitter   
  为编辑器提供[tree-sitter](https://tree-sitter.github.io/tree-sitter/)支持。tree-sitter可用于快速、增量地将代码转换
  成抽象语法树，以便您向用户提供精确的高亮和自动补全功能。注意此模块仅提供了转换和高亮支持。感谢[android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter/)项目提供的Java绑定库。

你可以从顶部的徽章或者[Releases](https://github.com/Rosemoe/CodeEditor/releases)找到最新的编辑器版本。

## 讨论

* QQ群:[216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* [Telegram 群组](https://t.me/rosemoe_code_editor)

## 贡献者

<a href="https://github.com/Rosemoe/sora-editor/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Rosemoe/sora-editor" />
</a>

## 许可证

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

## 鸣谢

感谢[JetBrains](https://www.jetbrains.com/?from=CodeEditor)为本项目
提供的[IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor)等IDE的免费许可证。

[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)
