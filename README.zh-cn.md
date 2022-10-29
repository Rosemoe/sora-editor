<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)

sora-editor是安卓平台上很棒的高效代码编辑器

</div>

* 阅读其他的语言文档: [English](README.md), [简体中文](README.zh-cn.md).*
  ***正在开发中*** 这个项目仍在缓慢开发中.   
  请从[Releases](https://github.com/Rosemoe/CodeEditor/releases)
  下载最新的源代码，而不是直接克隆此项目。**欢迎提交问题和合并请求**

## 特色

- [x] 语法高亮
- [x] 自动补全 (包含 [代码块](https://macromates.com/manual/en/snippets))
- [x] 自动缩进
- [x] 代码块辅助线
- [x] 文本缩放
- [x] 撤销/重做
- [x] 文本的搜索及替换
- [x] 自动换行
- [x] 显示不可打印的字符
- [x] 错误/警告/错字/弃用指示器
- [x] 文本放大镜
- [x] 文本增量分析
- [x] 高亮显示括号对
- [x] 事件系统

## 按键绑定

使用物理键盘时，您可以使用按键绑定来执行各种文本操作。一般情况下，编辑器默认支持某些按键的绑定。
但是,
您可以实现 [`KeyBindingEvent`](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/event/KeyBindingEvent.java)
并添加自己的按键绑定事件，甚至可以覆盖默认绑定的按键事件并执行自己定义的操作。

当前支持的键绑定大多类似于 Android Studio/Intellij IDEA.
参见 [支持的按键绑定](./keybindings.md).

## 截图

<div style="overflow: hidden">
<img src="/images/general.jpg" alt="GeneralAppearance" width="40%" align="bottom" />
<img src="/images/problem_indicators.jpg" alt="ProblemIndicator" width="40%" align="bottom" />
</div>

## 快速开始

为您的项目添加依赖:

```Gradle
implementation 'io.github.Rosemoe.sora-editor:<moduleName>:<versionName>'
```

可用模块:

- editor   
  包含基本内容的框架。
- editor-lsp   
  可以使用语言服务器协议（简称LSP）创建语言的便捷工具库。
- language-java   
  包含Java高亮和自动补全的基本实现。
- language-textmate   
  一个高效的编辑器高亮渲染器。依赖这个工具库后，你可以使用并加载textmate语言配置文件。
  内部实现来自[tm4e](https://github.com/eclipse/tm4e)。

你可以从顶部的徽章或者[Releases](https://github.com/Rosemoe/CodeEditor/releases)查看是否有最新的版本。

## 讨论

* QQ群:[216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* [Telegram Group](https://t.me/rosemoe_code_editor)

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
提供的例如[IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor)等IDE的免费许可证。
[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)
