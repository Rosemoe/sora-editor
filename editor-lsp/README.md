## About

**Experimental, work in progress**

`editor-lsp` module is a language service protocol client based on lsp4j, providing language service
protocol support for sora-editor.

Based on the module, you can access different language servers to provide features for different
languages, such as auto-completion, formatting, etc.

## Features(already available)

1. `textDocument/rangeFormatting`
2. `textDocument/diagnostic`

## How to connect to the language server

We recommend to use socket to connect to the language server, you can let the language server run on
another process without worrying that a crash of the language server will cause the main process to
crash.

See [this](https://github.com/dingyi222666/sora-editor/blob/5bf156ce45252eefb09028810b6685c2827baa90/app/src/main/java/io/github/rosemoe/sora/app/LspTestActivity.kt#L134)
to see example of connecting to a language server

