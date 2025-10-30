## About

**Experimental, work in progress**

`editor-lsp` module is a language service protocol client based on lsp4j, providing language service
protocol support for sora-editor.

Based on the module, you can access different language servers to provide features for different
languages, such as auto-completion, formatting, etc.

## Features(already available)

- `textDocument/formatting`
- `textDocument/rangeFormatting`
- `textDocument/diagnostic`
- `textDocument/signatureHelp`
- `textDocument/completion`
- `textDocument/publishDiagnostics`
- `textDocument/hover`
- `textDocument/codeAction`

## TODO

- [ ] `textDocument/semanticTokens`

## How to connect to the language server

We recommend to use socket to connect to the language server, you can let the language server run on
another process without worrying that a crash of the language server will cause the main process to
crash.

See [this](https://github.com/Rosemoe/sora-editor/blob/main/app/src/main/java/io/github/rosemoe/sora/app/lsp/LspTestActivity.kt)
to see example of connecting to a language server

