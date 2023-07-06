<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)

sora-editor は効率的な Android コードエディターです

</div>


他の言語のドキュメントを読む: [English](README.md), [简体中文](README.zh-cn.md), [Español](README.es.md), [日本語](README.jp.md).

***このプロジェクトはまだ開発中ですから、IssueやPRは歓迎です***

このプロジェクトを直接クーロンするではなく、[Releases](https://github.com/Rosemoe/CodeEditor/releases) からダウンロードしてください。


## 特徴

- [x] 構文の強調表示
- [x] オートコンプリート ([コード スニペット](https://macromates.com/manual/en/snippets)のサポートを含む)
- [x] 自動インデント
- [x] コード ブロック ヘルパー行
- [x] ジェスチャーズーム
- [x] 元に戻す/やり直す
- [x] テキストの検索と置換
- [x] ワードラップ
- [x] 印刷できない文字が表示可能
- [x] エラー/警告/タイプミス/非推奨インジケーター
- [x] テキスト拡大鏡
- [x] テキスト増分分析
- [x] 括弧のペアを強調表示可能
- [x] イベントシステム


## ショートカットキーの割り当て

物理キーボードを使用する場合、ショートカット キーを使用してさまざまなテキスト操作を実行できます。

エディターはデフォルトでいくつかのショートカット キーをサポートしていますが、[`KeyBindingEvent`](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/event/KeyBindingEvent.java) をサブスクライブすることができます。それを処理して独自のショートカット キーを実装します。もちろん、デフォルトのショートカット キーのアクションをオーバーライドしてカスタム アクションを実行することもできます。

現在のエディターでサポートされているショートカット キーのほとんどは、Android Studio/Intellij IDEA のショートカット キーと似ています。[サポートされているショートカット キー](./keybindings.md) を参照してください。


## エディターのプレビュー画像

<div style="overflow: hidden">
<img src="/images/general.jpg" alt="GeneralAppearance" width="40%" align="bottom" />
<img src="/images/problem_indicators.jpg" alt="ProblemIndicator" width="40%" align="bottom" />
</div>

## 始めに

依存関係をプロジェクトに追加してください:

```Gradle
dependencies {
    implementation(platform("io.github.Rosemoe.sora-editor:bom:<versionName>"))
    implementation("io.github.Rosemoe.sora-editor:<moduleName>")
}
```

以下は利用可能なモジュールです:

- editor   
  エディターのコア フレームワークが含まれています。
- editor-lsp   
  Language Server Protocol (略して LSP) を使用して言語を作成するための便利なツールのライブラリです。  
- language-java   
  Java の強調表示とオートコンプリートを含む言語ライブラリ。
- language-textmate   
　高度なハイライト分析ライブラリ。これを使用して、textmate 言語構成ファイルをロードし、このエディターに適用できます。   
  内部実装は [tm4e](https://github.com/eclipse/tm4e) から取得されます。
- language-treesitter   
　エディターに [tree-sitter](https://tree-sitter.github.io/tree-sitter/) サポートを提供します。これを使用すると、コードを抽象構文ツリーに迅速かつ段階的に解析することができ、正確な強調表示と補完の提供に役立ちます。このモジュールはトランジションとハイライトのサポートのみを提供することに注意してください。   
   [android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter/) プロジェクトによって提供される Java バインディング ライブラリを感謝します。

最新のエディターのバージョンは、上部のバッジまたは [Releases](https://github.com/Rosemoe/CodeEditor/releases) から見つけることができます。

## ディスカッション

* QQ グループ: [216632648](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* [Telegram グループ](https://t.me/rosemoe_code_editor)

## 貢献者

<a href="https://github.com/Rosemoe/sora-editor/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Rosemoe/sora-editor" />
</a>

## ライセンス

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

## 謝辞

このプロジェクトに [IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor) などの IDE の無料ライセンス
を提供してくださった [JetBrains](https://www.jetbrains.com/?from=CodeEditor) に感謝します。


[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)