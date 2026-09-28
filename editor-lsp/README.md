## About

**Experimental, work in progress**

`editor-lsp` module is a language service protocol client based on lsp4j, providing language service
protocol support for sora-editor.

Based on the module, you can access different language servers to provide features for different
languages, such as auto-completion, formatting, etc.

## Supported features

The editor integrations below are available when the connected server advertises the corresponding
capability. Some features are opt-in through `LspEditor` settings.

| Feature | Protocol / API | Integration |
| --- | --- | --- |
| Document synchronization | `didOpen`, `didChange`, `didSave`, `didClose` | Full and incremental document changes |
| Completion | `textDocument/completion`, `completionItem/resolve` | Completion UI, snippets, additional text edits |
| Hover | `textDocument/hover` | Markdown popup and embedded code highlighting |
| Signature help | `textDocument/signatureHelp` | Parameter/signature popup |
| Diagnostics | `textDocument/publishDiagnostics`, `textDocument/diagnostic` | Push/pull diagnostics and editor markers |
| Formatting | `textDocument/formatting`, `textDocument/rangeFormatting` | Whole document / selected range formatting |
| Document highlights | `textDocument/documentHighlight` | Read/write/text occurrence highlighting |
| Inlay hints | `textDocument/inlayHint` | Inline hint rendering |
| Document colors | `textDocument/documentColor` | Inline color previews |
| Semantic tokens | `semanticTokens/full`, `semanticTokens/full/delta`, `semanticTokens/range`, `workspace/semanticTokens/refresh` | Theme providers and style patches; see below |
| Code actions | `textDocument/codeAction`, `workspace/executeCommand` | Action popup, text edits, command execution |
| Definition and references | `textDocument/definition`, `textDocument/references` | `LspEditor` request helpers; applications present/navigate results |
| Rename | `textDocument/prepareRename`, `textDocument/rename` | `LspEditor` helpers returning ranges and workspace edits |
| Text edits | Text edits from formatting, completion and workspace-edit helpers | Batch application to open documents; resource operations are unsupported |

Additional methods are available through `RequestManager`; these do not have automatic editor UI
or triggers:

- `textDocument/typeDefinition`, `textDocument/implementation`.
- `textDocument/documentSymbol`, `workspace/symbol`.
- `textDocument/codeLens`, `codeLens/resolve`.
- `textDocument/documentLink`, `documentLink/resolve`.
- `textDocument/foldingRange`, `textDocument/onTypeFormatting`.
- `textDocument/willSave`, `textDocument/willSaveWaitUntil`.
- Workspace configuration, watched-file and workspace-folder change notifications.

Client infrastructure includes multiple language servers per document, per-server feature disabling
through `LspFeature`, configurable request timeouts, TCP/local socket/custom stream connections,
server lifecycle callbacks, custom JSON-RPC requests/notifications, progress reporting/cancellation,
and trace/log/message callbacks. Workspace-folder queries return the current project folder.

Dynamic capability registration, workspace configuration responses, diagnostic refresh,
`textDocument/colorPresentation`, and workspace file create/rename/delete operations are not
implemented. Request-level support does not imply a complete UI for that feature.

## Semantic tokens

Semantic highlighting uses an explicit `SemanticTokenStyleProvider`. Create the adapter you need
and assign it to the LSP editor; ordinary `Language` implementations do not depend on semantic
tokens. All semantic token types, styles and selector rules live in `editor-lsp`.

```kotlin
// Requires language-textmate at runtime.
lspEditor.semanticTokenStyleProvider = TextMateSemanticTokenStyleProvider(ThemeRegistry.getInstance())

// Alternatively, requires language-treesitter at runtime.
lspEditor.semanticTokenStyleProvider = TreeSitterSemanticTokenStyleProvider(tsLanguage)
```

Both adapters are in `io.github.rosemoe.sora.lsp.editor.semantic`. Their language dependencies are
`compileOnly`: applications include the language modules they use. There is no fixed mapping from
LSP token types to editor color IDs:

- **TextMate** reads the active theme's `semanticTokenColors`, then fills unspecified attributes
  using VS Code's standard TextMate scope fallbacks.
- **Tree-sitter** resolves classifications through the supplied language's capture theme
  (`function.method`, `variable.parameter`, `type`, etc.). An optional `styles` constructor argument
  accepts explicit semantic rules.

Selectors such as `variable.readonly:java` and `*.declaration` resolve each attribute by specificity.
Theme changes restyle cached tokens without another server request. Without a provider, syntax
highlighting remains in use.

For application-defined styles:

```kotlin
lspEditor.semanticTokenStyleProvider = SemanticTokenStyleProvider { type, modifiers, languageId ->
    if (type == "variable" && "readonly" in modifiers) SemanticTokenStyle(bold = true) else null
}

lspEditor.isEnableSemanticTokens = false // Remove semantic decorations, keeping syntax colors.
```

Providers run on a background thread; null style attributes preserve syntax styles. A mutable
provider can implement `observeChanges()` to invalidate cached styles. Supported attributes are
foreground, bold and italic. A server definition can also disable `LspFeature.SemanticTokens`.

Full results, full/delta updates, range-only servers, and server refresh requests are supported.
Range-only servers are queried for the whole document. With multiple servers, the first enabled
semantic-token server supplies both the legend and token results. The client uses UTF-16 positions
and supports overlapping and multiline tokens, including LF/CRLF and supplementary Unicode characters.
Overlaps compose foreground, bold and italic independently in range order (start, then end);
later patches override only their specified attributes. Multiline lengths include line separators.
Dynamic semantic-token registration is not advertised.

The Java and Kotlin Lua examples bundle native EmmyLua Language Server 0.25.1, which advertises
full semantic tokens. Both examples configure the TextMate adapter explicitly. The existing Android
service bridges a local socket to the server process; no server download is needed at runtime.
See [the server build instructions](../app/servers/emmylua/README.md) for the four Android ABIs,
standard-library resources, licenses, and macOS/Linux/Windows rebuild scripts.

## Event tasks

`LspEventManager.emit()` remains synchronous. `emitAsync()` is suspending, while `emitJob()`
launches an asynchronous event and returns a `Job`. Use `join()` to wait for completion; use
`emitAsync()` when the result context is needed. An optional `after` job lets the
caller order dependent events without a central pending-event registry. `emitBlocking()` is for
worker threads. The document-change receiver owns its synchronization task.

## How to connect to the language server

We recommend to use socket to connect to the language server, you can let the language server run on
another process without worrying that a crash of the language server will cause the main process to
crash.

See [this](https://github.com/Rosemoe/sora-editor/blob/main/app/src/main/java/io/github/rosemoe/sora/app/lsp/LspTestActivity.kt)
to see example of connecting to a language server

