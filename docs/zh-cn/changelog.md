# 更新日志

!> 0.21.1之前的更新日志由[Claude](https://claude.ai)翻译

## **[0.21.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.21.1) (2023-03-10)**

> 此版本包含了一些小的bug修复和新功能。

### 修复的Bug

- 当添加空代码块项时渲染器可能崩溃
- 在CR和LF之间的选择时索引越界
- `CharArrayWrapper#subSequence`引用了错误的文本开始位置
- 在CRLF之间错误插入文本
- 编辑器在释放后仍在监听文本更改,导致内存泄漏
- 在`GraphicTextRow`中测量文本时偶尔索引越界

### 改进

- 添加`EditorReleaseEvent`,允许在销毁时进行一些清理
- 添加`ContentIO`用于创建/保存`Content`文本
- 在配色方案中添加删除线颜色
- 如果可能，尽量将文本更新合成插入或删除 [#357](https://github.com/Rosemoe/sora-editor/issues/357)
- 语言服务器协议的签名帮助窗口 **[@dingyi222666](https://github.com/dingyi222666)**
- 为语言服务器协议添加`ThemeModel#isDark` **[@dingyi222666](https://github.com/dingyi222666)**

### 注意

- `ContentReader`应该迁移到`ContentIO`。`ContentReader`将在未来删除。

## **[0.21.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.21.0) (2023-01-08)**

> 此版本包含了bug修复和少量改进以及小的API变化。

### 修复的Bug

- 当`renderFunctionCharacter`开启时,水平滚动范围不正确
- 在访问它之前没有验证`TSQuery`
- 当`maxIPCTextLength`超过时,依赖`InputConnection#getSurroundingText`的IME获取无效的位置描述
- 通过`EditorInputConnection#setComposingRegion`可以设置无效的组成文本范围
- 使用旧的Gboard快速删除字符时文本变为不可编辑
- 动画行背景层错误
- 空文本在正则表达式中被重复匹配,导致OOM
- `StringIndex OutOfBoundsException` in `TextMateNewlineHandler` **[@dingyi222666](https://github.com/dingyi222666)**
- `CodeEditor#release`不会分离`EditorColorScheme`
- 在`AsyncIncrementalAnalyzeManager`中发送消息时产生空指针异常
- 通过`TextMateLanguage#updateLanguage`泄露线程
- 使用不支持的符号对仍在textmate中使用
- `ReplaceAction`可以添加而不嵌套其`DeleteAction`撤销/重做是可能的
- `JavaTextTokenizer`没有获取当前token [#349](https://github.com/Rosemoe/sora-editor/issues/349)
- `AsyncIncrementalAnalyzeManager`
  不会更新最后一行受插入影响的跨度 [#350](https://github.com/Rosemoe/sora-editor/issues/350)
- 当没有行样式存在时`Styles#eraseAllLineStyles`引发空指针异常

### 改进

- 更好的搜索体验,包括支持整词搜索,正则表达式搜索速度提升,循环跳转和更好的滚动策略 [#321](https://github.com/Rosemoe/sora-editor/issues/321)
- tree-sitter谓词性能改进
- 避免`CodeEditor#ensurePositionVisible`中的边界效应
- 通过二分查找提高搜索结果查询速度
- 显示内置文本操作窗口在插入手柄释放时
- 添加触摸滞后再触发放大镜
- 添加可配置的文本操作替换字符串资源`I18nConfig`
- 添加`DirectAccessProps#clipboardTextLengthLimit`并在由于限制或`TransactionTooLargeException`无法复制文本时添加提示
- 更精确的内置文本操作窗口位置
- 编辑器启动时使用全局默认配色方案
- 增强了`DirectAccessProps#disallowSuggestions`的功能(在最新版Gboard和小米UI上的Sogou输入法上测试通过)
- 适应MIUI系统的长截图的视图参数
- 从Android M开始添加两个新的辅助功能动作

### 更新的API

- 新函数`Content#substring`
- 新的事件`PublishSearchResultEvent`,在主线程中搜索结果可用或停止搜索时调用
- 新的`I18nConfig`类用于替换字符串资源
- 新的函数`CodeEditor#isAntiWordBreaking`
- **[破坏性]** `EditorColorScheme`中用于全局默认主题的新API。所有新创建的编辑器都使用全局默认主题。对全局默认主题的修改将反映在这些编辑器中。
- **[破坏性]** `CodeEditor#getScroller`现在具有`EditorScroller`的返回类型。通过`EditorScroller#getImplScroller`
  获取原始的`OverScroller`
- **[破坏性]** `AsyncIncrementalAnalyzeManager`和`TsAnalyzeManager`在退出时会接收线程中断

## **[0.20.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.4) (2022-12-30)**

### 已修复的bugs

- tree-sitter中的成员变量高亮显示
- 当跨度损坏时渲染突出显示的delimeters时编辑器崩溃
- 无法从编辑器组件中获取`EditorDiagnosticTootipWindow`
- 无法禁用`EventManager`

### 新功能

- `CodeEditor#createSubEventManager()`

## **[0.20.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.2) (2022-12-27)**

### 已修复的bugs

- 键绑定`Ctrl + D`中缩进意外加倍
- 除非滚动列表,否则默认完成布局无法在动画后单击
- 在主线程中编辑`TSTree`时使用无效索引
- 在追加文本时索引越界`IndexOutOfBoundsException`
- 在tree-sitter中运行谓词后无法编辑文本

### 新功能

- 现在信息面板样式是泡泡式的 [#283](https://github.com/Rosemoe/sora-editor/issues/283)
- 添加`Content.getDocumentVersion()`以检查文档修改
- `DiagnosticDetail`用于描述诊断信息和内置的`EditorDiagnosticTooltipWindow` [#314](https://github.com/Rosemoe/sora-editor/issues/314)
- 实验性的`Quickfix` 项目
- 支持tree-sitter中成员范围(更多信息请参阅`LocalsCaptureSpec`)并提高定义查找速度
- 信息面板的自定义文本提供程序 `LineNumberTipTextProvider`

## **[0.20.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.1) (2022-12-21)**

- 修复了0.20.0版本中tree-sitter代码块的关键bug

## **[0.20.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.20.0) (2022-12-21)**

!> 改用`0.20.1`版本

### 已修复的bugs

- `LanguageServerWrapper`中的内存泄漏
- `EditorSearcher.gotoPrevious()`在启用正则表达式时总是跳转到第一个项目
- 选择手柄可绘制中的透明线([#270](https://github.com/Rosemoe/sora-editor/issues/270), 由 [@tegajoel](https://github.com/tegajoel)修复 [#312](https://github.com/Rosemoe/sora-editor/pull/312))
- 当文本包含任何单个长行时,用户缩放时整个文档变为黑色
- `SimpleAnalyzeManager`忽略换行符
- `OnigRegExp`在多线程访问中返回损坏的缓存 ([#315](https://github.com/Rosemoe/sora-editor/issues/315) by [@xyzxqs](https://github.com/xyzxqs))
- `TextReference.toString()`不返回备份序列的字符串
- 文本上下文区域可能超出文本边界 [#318](https://github.com/Rosemoe/sora-editor/issues/318)
- 换行处理程序修复 (by [#319](https://github.com/Rosemoe/sora-editor/issues/319) [@dingyi222666](https://github.com/dingyi222666))
- 在单核心设备上`corePoolSize`超过`maximumPoolSize` [#320](https://github.com/Rosemoe/sora-editor/issues/320)

### 新功能

- 添加tree-sitter支持,包括编辑器的突出显示、代码块和括号以及tree-sitter谓词支持

### 特别感谢

- **@itsaky for [android-tree-sitter](https://github.com/AndroidIDEOfficial/android-tree-sitter)**

## **[0.19.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.19.0) (2022-12-15)**

### 已修复的bugs

- 无法添加范围不为“source”的语言 ([#288](https://github.com/Rosemoe/sora-editor/issues/288) by [@dingyi222666](https://github.com/dingyi222666))
- 标记为非空的可空数据包在 ImePrivateCommandEvent 中 [#293](https://github.com/Rosemoe/sora-editor/issues/293)
- 在预设组合状态下渲染时索引越界 [#294](https://github.com/Rosemoe/sora-editor/issues/294)
- 放大镜在窗口大小非法时显示 [#298](https://github.com/Rosemoe/sora-editor/issues/298)
- 引入行分隔符类型后Content#deleteInternal的部分破坏逻辑 [#299](https://github.com/Rosemoe/sora-editor/issues/299)
- 当大小太小无法显示内容时,不调整完成窗口大小
- 在启用自动换行且禁用页边距后,缩放后不重新创建布局
- 完成窗口高度偶尔无效
- 渲染长线时偶尔空指针异常
- 与textmate Java中的'<' '>'的意外符号匹配
- 粘贴文本时的意外符号匹配 (AndroidIDE#593)
- 在某些情况下,选择跨度可能超出范围(AndroidIDE#579)
- 在启用自动换行的模式下,光标在某些RTL自然语言中移位无效
- 在自动换行模式下无法输入文本
- `MatchHelper#startsWith`工作不正确
- 当用户使用最新版Gboard和其他一些IME时,编辑器保存冗余的撤销操作
- `CodeEditor#commitTab`忽略语言设置
- 无法拦截`EditorKeyEvent`

### 新功能与改进

- 更新的自动完成功能和排序方法(by **[@dingyi222666](https://github.com/dingyi222666)**)
- lua语言服务器在示例应用中(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 对语言-textmate的可选代码完成(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 更新后的符号对API(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 添加bom模块(by **[@keta1](https://github.com/keta1)**)
- 在textmate中支持indentationRules(by **[@dingyi222666](https://github.com/dingyi222666)**)

## **[0.18.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.18.1) (2022-11-05)**

> 这包含一组bug修复和小的改进。涵盖了0.18.0和0.18.1的更新。

### 改进

- 更多内置键绑定(by **[@itsaky](https://github.com/itsaky)**)
- 更新lsp4j至0.17.0
- 针对英语的单词自动换行的反单词断行选项(默认启用)
- 淡入淡出滚动栏的动画
- 编辑器代码片段现在支持复杂变量和占位符(带转换或选择)以及 `\u` 和插值shell代码的使用
- 不再在`CompletionThread`中记录`CompletionCancelledException`
- 添加`SnippetEvent` 用于代码片段事件
- `SnippetController`中导出更多API
- 添加`QuickQuoteHandler`(从`SymbolPairMatch`中分离出来)
- 在编辑器片段支持下的编辑器lsp代码补全(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 添加`StylesUtils#checkNoCompletion`
- 增强的`NewlineHandler`接口

### 已修复的bugs

- [重要]向增量分析进程传递错误状态时插入影响行状态的文本
- 移动行时未更新选择锚点 (by **[@itsaky](https://github.com/itsaky)**)
- 低API设备上的潜在崩溃错误 [#246](https://github.com/Rosemoe/sora-editor/issues/246)
- 验证measure缓存之前使用
- 选择文本存在时意外取消选择动画
- 设置新选择位置时选择缩放动画造成视觉滞后
- 自动换行模式下表情符号意外被截断
- 设置包装语言时可能回收编辑器中的LspEditor
- 在复杂文本方向的渲染中生成IndexOutOfBoundsException
- 自动换行模式下RTL文本的无效选择位置
- 在缩放文本后在自动换行模式下无法输入文本
- `MatchHelper#startsWith`工作不正确
- 当用户使用最新版Gboard和其他一些IME时,编辑器保存冗余的撤销操作
- `CodeEditor#commitTab`忽略语言设置
- 无法拦截`EditorKeyEvent`

### 迁移

- 由于textmate的新API,一些方法被弃用。在0.20.0左右它们仍向后兼容。有关详细信息,请参阅[#282](https://github.com/Rosemoe/sora-editor/issues/282)。
- `NewlineHandler`应该通过使用文本位置手动切片给定的文本来获取textBefore和textAfter
- 请参考[新的符号对接口](https://github.com/Rosemoe/sora-editor/blob/b343f0ae71ab3f8fe06576fcd9a813eb78554935/editor/src/main/java/io/github/rosemoe/sora/widget/SymbolPairMatch.java)

## **[0.17.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.17.2) (2022-10-08)**

### 已修复的bugs

- 修复文本不可编辑时意外的光标动画
- 修复低API设备上的文本选择问题 [#238](https://github.com/Rosemoe/sora-editor/issues/238)
- 修复textmate中使用不支持的符号对
- 当布局初始化时通知输入法
- 修复在调用`CodeEdditor#updateStyle`时可能出现的空指针
- 修复引用`ContentReference.RefReader`中忽略了行分隔符类型的bug [#260](https://github.com/Rosemoe/sora-editor/issues/260)
- 当用户在选项卡间切换时,完成窗口不会被隐藏
- 修复与Android 11及以下版本上的反向滚动冲突的边缘效果
- 修复显示完成窗口时未触发`NewlineHandler`
- 修复在完成窗口中错误插入的行分隔符

### 改进

- 当高亮更新时添加`StyleUpdateRange`以提高性能
- (破坏性更改)从`Span`中分离一些使用不频繁的字段到 `AdvancedSpan`
- 在textmate中支持 `highlightedDelimetersForeground` 颜色属性([#247](https://github.com/Rosemoe/sora-editor/pull/247)by [@PranavPurwar](https://github.com/PranavPurwar))
- 添加HOME和END增强功能的可选功能
- 重新排序内容编辑事件和完成请求的调度
- 支持自定义滚动条样式([#255](https://github.com/Rosemoe/sora-editor/pull/255) by [@MuntashirAkon](https://github.com/@MuntashirAkon))
- 现在可以将行号信息面板位置更改为其他位置([#305](https://github.com/Rosemoe/sora-editor/pull/259) by [@summerain0](https://github.com/summerain0))
- 改进语言服务器的连接速度([#262](https://github.com/Rosemoe/sora-editor/pull/262) by [@dingyi222666](https://github.com/dingyi222666))
- 添加`verticalExtraSpaceFactor`用于垂直视口中的额外空间大小

### 依赖项

- 删除未使用的xerces依赖项([#243](https://github.com/Rosemoe/sora-editor/pull/243) by [@PranavPurwar](https://github.com/PranavPurwar))
- Kotlin更新至1.7.20
- snakeyaml更新至1.33

## **[0.17.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.17.1) (2022-09-04)**

### 已修复的bugs

- 修复在没有完成实现的情况下销毁lsp服务器时的空指针
- 修复代码段结束处的FORMAT字符串导致索引越界
- `JavaTextTokenizer`中的bug导致完成停止

### 改进

- 更新自动完成UI和排序方法(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 不再在`CompletionThread`中记录`CompletionCancelledException`
- 添加`SnippetEvent`用于代码段事件
- `SnippetController`中导出更多API
- 添加`QuickQuoteHandler`(从`SymbolPairMatch`中分离出来)
- 基于编辑器代码段的编辑器lsp代码补全(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 添加`StylesUtils#checkNoCompletion`
- 增强的`NewlineHandler`接口

## **[0.17.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.17.0) (2022-09-03)**

### 已修复的bugs

- 组合文本中的一个潜在bug
- 使用textmate时emoji文本显示无效
- lsp中的一个异常(by **[@dingyi222666](https://github.com/dingyi222666)**)
- `copyLine()`中的堆栈溢出(by **[@itsaky](https://github.com/itsaky)**)
- 用户选择完成项目时的潜在空指针
- 文本样式更新后代码块行显示无效
- 编辑器不可编辑时意外修改文本的操作窗口
- 突出显示delimeters时渲染意外取消选择动画
- emoji字符在自动换行模式下意外被截断
- 设置包装语言时可能回收编辑器中的LspEditor
- 在某些情况下的自动滚动问题(启用自动换行时选择文本)
- `MatchHelper#startsWith`工作不正确
- editor保存冗余的撤销操作(用户使用最新版的Gboard和其他一些IME时)
- `CodeEditor#commitTab`忽略语言设置
- 无法拦截`EditorKeyEvent`

### 新特性与改进

- 可选的粗体高亮分隔符(by **[@ikws4](https://github.com/ikws4)**)
- lsp中的更好的自动补全(by **[@dingyi222666](https://github.com/dingyi222666)**)
- tm4e的上游更新(by **[@dingyi222666](https://github.com/dingyi222666)**)
- 当前行的行号颜色
- 非常基本的代码片段支持(简单的选项卡停止,变量和占位符。尚不可用的选择和规则)

### 破坏性更改

> tm4e上游代码有很大变化。您需要参考我们的示例应用程序进行迁移旧代码。

## **[0.16.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.4) (2022-08-14)**

### 更新

- 修复格式化线程在调用setText()后继续的潜在bug
- 在撤销堆栈中将组合文本编辑与组合文本提交合并
- 当文本大小更改后平滑更新布局
- 在`CodeEditor#ensurePositionVisible`中可选动画
- 减少Paint的不必要内存使用
- 修复`BlockIntList`中的bug,这会影响没有自动换行时的水平滚动范围
- 支持左右独立的分隔符边距
- 更新默认文本操作窗口的样式
- Gson依赖项更新
- 内部字段命名更新

### 注意

> 依赖使用内部字段的用户应检查其反射目标。

## **[0.16.3](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.3) (2022-08-11)**

### 更新

- 在textmate自动补全器中设置关键字的快捷方法
- 如果标识符与关键字重复,则不显示标识符`IdentifierAutoCompleter`
- 修复启用自动换行时渲染文本背景的错误

## **[0.16.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.2) (2022-08-11)**

### 更新

- 修复ContentCreator在读取CRLF文本时的异常
- 修复Content#copyText中的bug
- 修复调用`ContentBidi`和`CachedIndexer`时偶发的并发修改错误
- 文本复制的性能增强
- 一些内部代码风格统一
- 添加一些潜在的组合字符集群

## **[0.16.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.1) (2022-08-10)**

### 更新

- 为编辑器中的新行指定行分隔符类型
- 修复CRLF文本中`Content#subSequence`的bug
- 在特定情况下修复跟踪组合文本的bug(当`trackComposingText`启用时)
- 未选择文本时不突出显示分隔符
- 移除调试日志
- ContentLine#subSequence中的性能提升

## **[0.16.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.16.0) (2022-08-10)**

- 这主要关注于sora编辑器的性能和显示正确性的一个重大更新

### 改进

- 选中文本可选是否用其他颜色绘制(将SELECTED_TEXT颜色设置为0)
- 在所选区域中绘制空格
- 聚焦时缩放文本
- 为选中的空行绘制短背景
- 完成窗口随文本滚动
- 更新选择手柄样式
- 按住时,选择手柄随拇指
- 优化文本操作弹出窗口
- 更好的连字支持
- 避免自动换行模式下视图宽度过小导致无法显示内容时的OOM
- 快速删除空行
- 一按DEL可批量删除空格
- 异步和可自定义的自动完成API
- 添加选项以强制删除键盘建议
- 开放的代码分析框架
- textmate的增量高亮分析
- 对Content的线程安全支持
- 控制编辑器的IPC文本长度最大值
- 更好的自动完成调度
- 为某些区域设置no completion标志
- 可删除的内置组件
- 改进了RTL自然语言的光标移动
- 及时中断自动完成线程
- 暴露更多公共API

### 已修复的bugs

- language-textmate中的无效代码块线

### 破坏性更改

几乎都有变化，但是,一些重要的提示:

- 将旧的addIfNeeded()调用直接与颜色ID迁移到由`TextStyle#makeStyle(...)`生成的经检查的调用,以指定样式和一些其他属性(例如nocompletion)。除非您确信值有效(担心前景色ID的位数在未来版本中可能会减少。当前为20位,远远大于我们需要的)。
- 旧的分析可以通过使用`SimpleAnalyzeManager`快速迁移
- Span不总是由内部的`List<List<Span>>`存储。 使用Reader和Modifier API访问其内容,除非您确定实例的实现。
- 建议您同时只使用一个`Spans.Reader`和一个`Spans.Modifier`。
- 新完成系统只提供文本中完成的确切位置。 如何完成取决于您。
- `SimpleCompletionItem`不能取代旧的`CompletionItem`
- 自动完成和分析线程之间的共享数据不由编辑器维护。 您必须自己管理。
- CodeEditor添加了一个新方法`release()`,用于停止完成线程和分析。 强烈建议在编辑器不再使用时调用此方法(例如永久从视图中删除或活动结束时)。
- 避免使用CodeEditor的受保护`drawXxx()`方法,它们很可能会改变。 此外,用注解`@UnsupportedUserUsage`标记的方法或字段不应该使用。与绘制相关的类(`HwAccerelatedRenderer`和`GraphicTextRow`)不应该使用。
- EditorPopupWindow提供了一些有用的操作,以在编辑器中使用良好支持创建自己的窗口
- NavigationItem被删除。 它应该由你的`Language`实现。
- `BlockLine`重命名为`CodeBlock`
- `DirectAccessProps`中的字段可以直接修改。 设置它们时编辑器不需要它们。 修改在实际值下次被编辑器访问时生效。

### 感谢

- 对这个项目做出贡献的人
- 测试应用程序并提供反馈的人
- star此仓库的人

## **[0.8.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.8.4) (2021-11-14)**

> 这包含一组bug修复和小的改进。

### 修复的bugs

- 下一行的背景颜色遮盖了错误下划线

### 改进

- 更长时间的光标动画
- 放大因子设置为Android默认

### 注意

- 0.11.2版本的maven仓库损坏。改用`0.11.3`。

## **[0.8.3](https://github.com/Rosemoe/sora-editor/releases/tag/0.8.3) (2021-10-30)**

### 更新日志

- 默认情况下不显示选择的完成项目背景,除非使用方向按钮
- 在高API级别上使用PixelCopy提高放大镜中的性能
- 修复水平滚动条的显示

## **[0.8.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.8.2) (2021-10-30)**

### 更新日志

> 由于一些bug,0.8.1没有发布或上传到maven

- 编辑器的一部分SymbolInputView
- 改进完成窗口项目滚动
- 为span添加粗体和斜体字体样式
- 当前行背景动画
- 修复WordwrapLayout中的无效布局偏移

## **[0.8.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.8.0) (2021-10-30)**

### 更新日志

- 添加可选的文本放大镜,按住选择手柄时显示(默认启用)
- 为所有弹出窗口应用高度效果
- 动画光标
- 修复输入文本时完成窗口闪烁问题
- 增加边缘快速滚动的最大速度

## **[0.7.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.7.2) (2021-10-21)**

### 更新日志

- 添加控制完成窗口位置方案的方法
- textmate模块支持块线[@Coyamo](https://github.com/Coyamo)
- textmate在示例应用中支持低API设备

## **[0.7.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.7.1) (2021-10-21)**

### 更新日志

- 修复输入符号时崩溃的bug

## **[0.7.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.7.0) (2021-10-16)**

- 添加textmate支持(需要更高的API版本,直到Android 8) [@Coyamo](https://github.com/Coyamo)
- 添加AutoSurroundPair和符号对匹配的条件检查接口 [@dingyi222666](https://github.com/dingyi222666)

### 生成的注意事项

- 通过 [@mnixry](https://github.com/mnixry) 添加GitHub工作流CI在 https://github.com/Rosemoe/CodeEditor/pull/101
- 基于tm4e-0.4.2的tm4e核心模块的简单移植。作者 [@Coyamo](https://github.com/Coyamo) 在 https://github.com/Rosemoe/CodeEditor/pull/97
- 在Replacement中添加shouldDoReplace() by [@dingyi222666](https://github.com/dingyi222666) 在 https://github.com/Rosemoe/CodeEditor/pull/99

## **[0.6.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.6.0) (2021-10-09)**

修复错误下划线被下一行的行背景所覆盖

## **[0.6.0-dev-4](https://github.com/Rosemoe/sora-editor/releases/tag/0.6.0-dev-4) (2021-10-03)**

- 修复生成的pom文件中的未指定依赖项。
- 解决构建应用程序时的这个问题:

![image](https://user-images.githubusercontent.com/28822819/135739646-653b9625-d84a-4d5b-8447-514fce34e80a.png)

## **[0.6.0-dev-3](https://github.com/Rosemoe/sora-editor/releases/tag/0.6.0-dev-3) (2021-10-02)**

!> 不要再使用 0.6.0-dev-2。

- 修复0.6.0-dev-2中的bug。

## **[0.6.0-dev-2](https://github.com/Rosemoe/sora-editor/releases/tag/0.6.0-dev-2) (2021-10-01)**

- 无

## **[0.6.0-dev-1](https://github.com/Rosemoe/sora-editor/releases/tag/0.6.0-dev-1) (2021-09-21)**

### 更新

- 基础包重命名为io.github.rosemoe.sora
- 移除BlockLinkedList
- 所有内置配色方案都非final
- 区块线绘制属性(bcfdbbd)
- ViewPager的兼容方法(97a1225)
- 添加语言-css3
- 选择改变事件
- 获取完成项高度
- 添加错误/警告/打字错误/弃用标记的方法
- 连字开关,默认关闭
- 修复双字符表情符之间的无效光标位置
- 优化选择手柄

### 提示

对于此版本及更高版本,使用

```Gradle
implementation 'io.github.Rosemoe.sora-editor:<module>:<version>'
```

## **[0.5.4](https://github.com/Rosemoe/sora-editor/releases/tag/0.5.4) (2021-08-08)**

- 仅降级gradle插件版本以运行Jitpack

## **[0.5.3](https://github.com/Rosemoe/sora-editor/releases/tag/0.5.3) (2021-08-01)**

### 许可证更新

- 从`0.5.3`开始,CodeEditor的许可证已更改为通用公共许可证V3。
  确保您的项目与此没有冲突。

### 更新说明

- 添加选项可在横向模式下禁用全屏(作者 **[@itsaky](https://github.com/itsaky)**)
- 在左边固定行号
- 即使首行在自动换行模式下不可见,也为第一行显示行号
- 传递给输入法的最大文本长度增加。

## **[0.5.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.5.2) (2021-07-02)**

### 0.5.2版本

- 修复自动换行模式下表情符号分隔问题 [#72](https://github.com/Rosemoe/sora-editor/issues/72)

### 注意

- 更新计划由于我的时间安排已更改。
- 主要新版本开发推迟。

## **[0.5.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.5.1) (2021-06-07)**

- **重要:这是编辑器项目重构之前的最后一个版本**
- **下一个版本将几乎完全不同**

### 更新

- 修复测量表情符号时的错误 (#57 #58 由 @MoFanChenXuan)
- 移除SymbolChannel的内容限制
- 新的文本操作弹出窗口(#63 由 @RandunuRtx)
- 通过点击空白区域取消选择文本(#64 由 @itsaky)
- 修复再次删除并添加编辑器视图后意外的光标闪烁
- 添加换行符号的颜色不正确
- 修复执行回车键时的bug [#67](https://github.com/Rosemoe/sora-editor/issues/67)
- 空组合文本问题 [#69](https://github.com/Rosemoe/sora-editor/issues/69)

## **[0.5.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.5.0) (2021-05-01)**

这是CodeEditor的一个重大更新,包含许多补丁和特性。
并做出了一些轻微的不兼容更改。
感谢 [@itsaky](https://github.com/itsaky) 和 [@RandunuRtx](https://github.com/RandunuRtx)的贡献!

### 不兼容的更改

- 接口不具有默认方法,为了兼容性,语言实现必须显式重写方法[#48](https://github.com/Rosemoe/sora-editor/issues/48)
- 删除了'language-s5d'模块

### 新特性和改进

- 语言-python 由 [@itsaky](https://github.com/itsaky)
- 当光标不可见时不再自动重绘视图
- 更快的行号渲染速度
- 添加一些符号对 [#44](https://github.com/Rosemoe/sora-editor/issues/44) 由 @itsaky
- 添加对制作符号栏的支持 [#45](https://github.com/Rosemoe/sora-editor/issues/45)
- 插入时显示操作面板 [#51](https://github.com/Rosemoe/sora-editor/issues/51)
- 滑块悬停在边缘时自动滚动 [#50](https://github.com/Rosemoe/sora-editor/issues/50)

### 修复的bugs

- 低API级别崩溃 [#48](https://github.com/Rosemoe/sora-editor/issues/48)
- Android 11上获得焦点时意外的背景色 [#41](https://github.com/Rosemoe/sora-editor/issues/41)
- 适配键盘行为 ([#41](https://github.com/Rosemoe/sora-editor/issues/41), [#56](https://github.com/Rosemoe/sora-editor/issues/56))
- 选择全部后无法取消选择 [#46](https://github.com/Rosemoe/sora-editor/issues/46) 由 [@itsaky](https://github.com/itsaky)
- 自定义适配器上的无效转换 [#53](https://github.com/Rosemoe/sora-editor/issues/53) 由 [@itsaky](https://github.com/itsaky)
- 字体对文本偏移的问题 [#55](https://github.com/Rosemoe/sora-editor/issues/55)
- Java语言中的自动缩进无效 [#56](https://github.com/Rosemoe/sora-editor/issues/56)

## **[0.4.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.4.0) (2021-02-27)**

- 注意:此版本包含不兼容的更改
- API要求上升到Android 5.0(API 21)
- 模块编辑器

### 错误修复

- 无效的EdgeEffect
- MyCharacter未初始化
- Word wrap布局宽度
- 调用deleteSurroundingText时无效 [#34](https://github.com/Rosemoe/sora-editor/issues/34)

### 新特性

- 提供组合文本的补全 [#32](https://github.com/Rosemoe/sora-editor/issues/32)
- 缩放大小范围可以控制
- 新模块语言-html (by **[@itsaky](https://github.com/itsaky)**)
- 符号对自动完成 [#36](https://github.com/Rosemoe/sora-editor/issues/36)
- 输入新行的处理程序
- 自动完成的自定义适配器支持
- 模块语言-java

### 新特性

- 在输入新行时自动放入'{}'

## **[0.3.2](https://github.com/Rosemoe/sora-editor/releases/tag/0.3.2) (2021-02-08)**

### 已修复的错误

- 光标位置错误
- 使用EmptyLanguage时行开头显示EOL
- 绘制期间跨度的并发读写
- EdgeEffect方向错误
- 输入法设置选择区域为零大小后退出选择操作模式

### 新特性

- 可以控制行号的可见性

## **[0.3.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.3.1) (2020-08-29)**

### 错误修复

- 启用自动换行时选择文本时的自动滚动错误

### 更改

- 项目现在使用Java 8语言级别

### Javadoc

- 修复拼写错误

## **[0.3.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.3.0) (2020-08-23)**

### 新特性

- 自动换行
- 光标闪烁
- 显示不可打印字符
- 新的配色方案

### 性能

- 提高复制Content的速度
- 提高绘制行的速度

### 用户体验

- 精确的可滚动范围(当文本非常大时,在设置文本大小或字体后可能会阻止UI)
- 中文翻译

### 错误修复

- 下一次搜索时崩溃
- 使用tab的文本中从触摸选择错误的错误选择位置

## **[0.2.0](https://github.com/Rosemoe/sora-editor/releases/tag/0.2.0) (2020-08-19)**

### 错误修复

- 自动滚动
- 选项卡绘制
- 跨度移动
- 格式状态
- 文本搜索

### 改进

- 更好的性能
- 更好的水平滚动限制
- 更好的自动完成管理
- 输入法交互
- 更好的配色方案
- 独立的行信息面板文本大小

### 突破性变化

- 将StringBuilder替换为ContentLine

## **[0.1.1](https://github.com/Rosemoe/sora-editor/releases/tag/0.1.1) (2020-08-06)**

### 新功能

- 添加一种新的方式来呈现选择文本时的文本操作(可选ActionMode或PopupWindow)

### 改进

- 修复文本选择的bug(使用滚动栏时可能会选择文本)
- 校正发送到输入法的组合文本区域

### 突破性更改

- 将TextColors重命名为TextAnalyzeResult,因为不仅包含文本颜色
- TextActionWindow不再存在

## **[0.1.0-beta](https://github.com/Rosemoe/sora-editor/releases/tag/0.1.0-beta) (2020-07-29)**

- CodeEditor的首个版本。所有基本模块都已完成。
- 大多数输入和显示错误都已修复。
- 此版本不可用于生产,仅用于测试。