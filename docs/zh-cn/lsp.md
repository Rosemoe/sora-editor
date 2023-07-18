# 语言服务（LSP）

!> **实验性，正在进行开发中**

编辑器lsp模块是一个基于lsp4j的语言服务协议客户端，为sora编辑器提供语言服务协议支持。

基于该模块，您可以访问不同的语言服务器，为不同的语言提供功能，如自动完成、格式化等。

## 目前可用的功能
- `textDocument/formatting`
- `textDocument/rangeFormatting`
- `textDocument/diagnostic`

## 如何连接到语言服务器

我们建议使用套接字连接到语言服务器，您可以让语言服务器在另一个进程上运行，而不用担心语言服务器的崩溃会导致主进程崩溃。

请参阅[示例](https://github.com/Rosemoe/sora-editor/blob/main/app/src/main/java/io/github/rosemoe/sora/app/LspTestActivity.kt#L135)以查阅连接到语言服务器的示例