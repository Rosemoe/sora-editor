# 更新日志

## **[0.21.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.21.0) (2023-01-08)**

> 此版本包含了错误修复、小的改进以及一些 API 的变化。

### 错误修复

- 当 `renderFunctionCharacter` 打开时，水平滚动范围不正确
- 在访问 `TSQuery` 前未经验证即使用它
- 当超过 `maxIPCTextLength` 时，依赖于 `InputConnection#getSurroundingText` 的 IME 会获得无效的位置描述
- 通过 `EditorInputConnection#setComposingRegion` 可以设置无效的组合文本范围
- 在使用旧版 Gboard 快速删除字符时，文本变得脏乱
- 动画行背景位于错误的层级
- 在正则表达式中会重复匹配空文本，导致 OOM
- 在 `TextMateNewlineHandler` 中出现 `StringIndexOutOfBoundsException` (by **[@dingyi222666](https://github.com/dingyi222666)**)
- `CodeEditor#release` 未分离 `EditorColorScheme`
- 在 `AsyncIncrementalAnalyzeManager` 中发送消息时出现 NPE
- 由 `TextMateLanguage#updateLanguage` 泄漏的线程
- 可以添加不带其 `DeleteAction` 的嵌套撤销/重做
- 允许嵌套撤销/重做
- `JavaTextTokenizer` 未考虑当前标记 [#349](https://github.com/Rosemoe/sora-editor/issues/349)
- `AsyncIncrementalAnalyzeManager`
  在插入时未更新最后一行受影响的范围的跨度 [#350](https://github.com/Rosemoe/sora-editor/issues/350)
- 当没有行样式时，`Styles#eraseAllLineStyles` 会引发NPE

### 改进

- 更好的搜索体验，包括支持整个单词搜索、正则表达式搜索速度提升、循环跳转以及更好的滚动策略 [#321](https://github.com/Rosemoe/sora-editor/issues/321)
- 提升 Tree-sitter 谓词的性能
- 避免 `CodeEditor#ensurePositionVisible` 中的边缘效应
- 通过二进制搜索提高查询搜索结果的速度
- 当释放插入句柄时，在内置文本操作窗口上显示
- 在放大镜触发之前添加触摸斜率
- 添加 `I18nConfig` 以提供应用程序提供的替换字符串资源
- 添加 `DirectAccessProps#clipboardTextLengthLimit` 并在由于限制或 `TransactionTooLargeException`
  而无法复制文本时添加提示
- 更精确的内置文本操作窗口位置
- 编辑器在启动时使用全局默认颜色