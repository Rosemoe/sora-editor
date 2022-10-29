<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)

sora-editor是一款在安卓平台上运行的高效代码编辑器

</div>

阅读其他语言的文档: [English](README.md), [简体中文](README.zh-cn.md).

***这个项目仍在缓慢开发中，欢迎提交建议和合并请求***

请从[Releases](https://github.com/Rosemoe/CodeEditor/releases)下载最新的源代码，而不是直接克隆此项目。

## 特色

- [x] 语法高亮
- [x] 自动补全 (含[代码块](https://macromates.com/manual/en/snippets))
- [x] 自动缩进
- [x] 代码块辅助线
- [x] 文本缩放
- [x] 撤销/重做
- [x] 文本搜索及替换
- [x] 自动换行
- [x] 显示不可打印的字符
- [x] 错误/警告/拼写错误/弃用指示器
- [x] 文本放大镜
- [x] 文本增量分析
- [x] 高亮显示括号对
- [x] 事件系统

## 按键绑定（快捷键）

使用物理键盘时，你可以使用快捷键来执行各种文本操作。

一般情况下，sora-editor默认实现了部分快捷键，你也可以实现[`KeyBindingEvent`](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/event/KeyBindingEvent.java)
并绑定相应事件，此外你也可以覆盖默认快捷键。

目前默认支持的快捷键大多与 Android Studio/Intellij IDEA 类似，可参考[默认支持的快捷键](./keybindings.md).

## 截图

<div style="overflow: hidden">
<img src="/images/general.jpg" alt="GeneralAppearance" width="40%" align="bottom" />
<img src="/images/problem_indicators.jpg" alt="ProblemIndicator" width="40%" align="bottom" />
</div>

## 快速开始

```Gradle
implementation 'io.github.Rosemoe.sora-editor:<moduleName>:<versionName>'
```

目前可用模块:

- editor   
  sora-editor的基本框架
- editor-lsp   
  可以使用语言服务器协议（LSP）构建语言的工具库。
- language-java
  实现了Java语法高亮和自动补全的工具库
- language-textmate   
  一个高效的编辑器高亮渲染器。你可以加载外部的textmate配置文件。

  内部实现来自[tm4e](https://github.com/eclipse/tm4e)。

你可以从顶部的徽章或[Releases](https://github.com/Rosemoe/CodeEditor/releases)获取最新版本。

## 讨论

* QQ群:[216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* [Telegram群组](https://t.me/rosemoe_code_editor)

## 贡献者

<a href="https://github.com/Rosemoe/sora-editor/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Rosemoe/sora-editor" />
</a>

## 许可证

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

## 鸣谢

感谢[JetBrains](https://www.jetbrains.com/?from=CodeEditor)
提供的如[IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor)等IDE的免费许可证。

[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)
